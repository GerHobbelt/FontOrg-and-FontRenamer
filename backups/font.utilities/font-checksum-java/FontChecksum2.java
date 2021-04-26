/*
  Font Checksum #2 - Verify Checksums in OpenType and TrueType Font Files
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Wednesday, 7 March 2007
  Java class name: FontChecksum2
  Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 application to verify the internal checksums in OpenType
  and TrueType font files.  Sometimes when you download a file, or copy a file
  from a worn CD/DVD, what you receive differs from the original.  After
  downloading or copying twice, you may have two files with the same size, the
  same date, and the same apparent contents.  When compared byte-by-byte with
  commands such as "comp" on DOS/Windows or "cmp" on UNIX, only a few bits or
  bytes are different.  The rest of the files are the same.  Your question is
  which file is correct.  One of them must be wrong.  (Both could be wrong!)
  Since font files contain internal checksums, you can use this information to
  test which of the files is more correct.  Note that freeware and shareware
  fonts produced with older font tools often have numerous checksum errors.

  All proper OpenType and TrueType font files have a file checksum value of
  0xB1B0AFBA, calculated by adding the file as 32-bit words and ignoring
  overflow.  This is done originally by first setting a word called
  "checkSumAdjustment" in the "head" table to zero, then summing the file as
  words, then setting the adjustment to 0xB1B0AFBA minus the zero-based sum.
  Since all correct OTF and TTF font files have this same external checksum, it
  should not be necessary to understand the internal format of the files.
  Unfortunately, TrueType collection (TTC) files do not sum to 0xB1B0AFBA.
  This may have to do with the tools used to assemble TTC files.  Being much
  bigger than OTF or TTF files, TTC files are more likely to suffer data
  corruption.  To test OTF, TTC, and TTF font files, this program reads all
  "table directory" entries in each "Offset Table" for each internal font.
  (OTF and TTF files have one internal font; TTC files have more than one.)
  The overall file checksum is calculated for OTF and TTF files, but not for
  TTC files.  If all tested checksums are correct, then the total file is
  assumed to be correct.  For information on the format of font files, start
  with the following on-line references:

      Microsoft TrueType Font Properties Extension
        http://www.microsoft.com/typography/TrueTypeProperty21.mspx

      The OpenType Font File
        http://www.microsoft.com/typography/otspec/otff.htm
        http://www.microsoft.com/typography/otspec/head.htm

      The TrueType Font File
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6.html
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6head.html

  See the CompareFolders application for comparing two folders to determine if
  all files and subfolders are identical.  See the FileChecksum application to
  generate or test checksums for a single file.  See the FindDupFiles
  application to look for duplicate files based on MD5 checksums.

  GNU General Public License (GPL)
  --------------------------------
  FontChecksum2 is free software: you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by the Free
  Software Foundation, either version 3 of the License or (at your option) any
  later version.  This program is distributed in the hope that it will be
  useful, but WITHOUT ANY WARRANTY, without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
  Public License for more details.

  You should have received a copy of the GNU General Public License along with
  this program.  If not, see the http://www.gnu.org/licenses/ web page.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or file and folder names.  If no
  file or folder names are given on the command line, then this program runs as
  a graphical or "GUI" application with the usual dialog boxes and windows.
  See the "-?" option for a help summary:

      java  FontChecksum2  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  FontChecksum2  -s  d:\fonts  >errors.txt

  The console application will return an exit status of 1 for success, -1 for
  failure, and 0 for unknown.  The graphical interface can be very slow when
  the output text area gets too big, which will happen if thousands of files
  are reported.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class FontChecksum2
{
  /* constants */

  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)
  static final int BYTE_MASK = 0x000000FF;
                                  // logical mask for one byte as int value
  static final int[] BYTE_SHIFTS = {24, 16, 8, 0}; // first byte is high-order
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final long INT_MASK = 0x00000000FFFFFFFFL;
                                  // logical mask for one int as long value
  static final long MAX_FILE_SIZE = 0x7FFFFFFCL;
                                  // maximum 32-bit file size we can handle
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Verify Checksums in OpenType and TrueType Font Files - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates
  static final long TTF_CHECKSUM = 0xB1B0AFBAL; // checksum for OTF/TTF files

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static boolean checkSizeFlag;   // true if we check if file size is correct
  static boolean consoleFlag;     // true if running as a console application
  static boolean cornishFlag;     // custom option to rename checksum errors
  static JCheckBox detailCheckbox; // graphical option for <detailFlag>
  static boolean detailFlag;      // true if we show file checksum details
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static javax.swing.filechooser.FileFilter fontFilter;
                                  // our shared file filter for fonts
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static byte[] inputBuffer;      // reuse this same buffer for all file I/O
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report if running as GUI
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JButton saveButton;      // "Save" button for writing output text
  static JCheckBox showAllCheckbox; // graphical choice for <showAllFlag>
  static boolean showAllFlag;     // true if we show all files, not just errors
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static String systemFileSep;    // file/folder separator for local system
  static int totalCorrect;        // number of font files no errors, warnings
  static int totalErrors;         // number of font files with bad checksums
  static int totalFiles;          // number of files found (any file type)
  static int totalWarning;        // number of font files with warnings only

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    checkSizeFlag = true;         // by default, check if file size is correct
    consoleFlag = false;          // assume no files or folders on command line
    cornishFlag = false;          // by default, disable this custom option
    detailFlag = false;           // by default, show file summary only
    fontFilter = new FontChecksum2Filter(); // create our shared file filter
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hiddenFlag = false;           // by default, don't process hidden files
    inputBuffer = new byte[BUFFER_SIZE]; // allocate big/faster input buffer
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    recurseFlag = false;          // by default, don't process subfolders
    showAllFlag = false;          // by default, show only files with errors
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    totalCorrect = totalErrors = totalFiles = totalWarning = 0; // counters
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    /* Get the system's local separator for file and folder names. */

    systemFileSep = " / ";        // force spaced-out UNIX style, ignore ...
//  systemFileSep = System.getProperty("file.separator"); // ... local system

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a file or folder name. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore null parameters, which are more common that you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
//      || word.equals("-h") || (mswinFlag && word.equals("/h")) // see: hidden
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.equals("-a") || (mswinFlag && word.equals("/a"))
        || word.equals("-a1") || (mswinFlag && word.equals("/a1")))
      {
        showAllFlag = true;       // show all files, not just files with errors
      }
      else if (word.equals("-a0") || (mswinFlag && word.equals("/a0")))
        showAllFlag = false;      // show only files with checksum errors

      else if (word.equals("-cornish") || (mswinFlag && word.equals("/cornish")))
      {
        cornishFlag = true;       // enable this custom option (not documented)
      }

      else if (word.equals("-d") || (mswinFlag && word.equals("/d"))
        || word.equals("-d1") || (mswinFlag && word.equals("/d1")))
      {
        detailFlag = true;        // show file checksum details
      }
      else if (word.equals("-d0") || (mswinFlag && word.equals("/d0")))
        detailFlag = false;       // show file summary only

      else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
      {
        hiddenFlag = true;        // process hidden files and folders
      }
      else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
        hiddenFlag = false;       // ignore hidden files or subfolders

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // start doing subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // stop doing subfolders

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        fontSize = size;          // use same point size for output text font
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.equals("-z") || (mswinFlag && word.equals("/z"))
        || word.equals("-z1") || (mswinFlag && word.equals("/z1")))
      {
        checkSizeFlag = true;     // compare file size with internal tables
      }
      else if (word.equals("-z0") || (mswinFlag && word.equals("/z0")))
        checkSizeFlag = false;    // don't check if file size is correct

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume that this is a file
        or folder name.  We ignore <cancelFlag> because the user has no way of
        interrupting us at this point (no graphical interface). */

        consoleFlag = true;       // don't allow GUI methods to be called
        openFileFolder(new File(args[i]), "");
                                  // use original parameter, not lowercase word
      }
    }

    /* Run as a console application if file or folder names were given on the
    command line.  Exit to the system with an integer status: +1 for success,
    -1 for failure, and 0 for unknown. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      putError("Found " // + prettyPlural(totalFiles, "file") + ": "
        + prettyPlural(totalCorrect, "correct font") + ", "
        + prettyPlural(totalErrors, "with", "with") + " errors, and "
        + prettyPlural(totalWarning, "with", "with") + " only warnings.");
      if ((totalCorrect > 0) && (totalCorrect == totalFiles))
        System.exit(EXIT_SUCCESS); // all files were fonts and were correct
      else if (totalErrors > 0)   // did any files have checksum errors?
        System.exit(EXIT_FAILURE); // yes, even one error means failure
      else
        System.exit(EXIT_UNKNOWN); // somehow we got here by doing nothing
    }

    /* There were no file or folder names on the command line.  Open the
    graphical user interface (GUI).  We don't need to be inside an if-then-else
    construct here because the console application called System.exit() above.
    The standard Java interface style is the most reliable, but you can switch
    to something closer to the local system, if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new FontChecksum2User(); // create our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser
    statusTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update status message on clock ticks only

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = SYSTEM_FONT;     // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel1, panel2, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
    panel1.add(Box.createVerticalStrut(9)); // extra space at panel top

    /* Create a horizontal panel for the action buttons. */

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));

    openButton = new JButton("Open Font File/Folder...");
    openButton.addActionListener(action);
    if (buttonFont != null) openButton.setFont(buttonFont);
    openButton.setMnemonic(KeyEvent.VK_O);
    openButton.setToolTipText("Start finding/opening files.");
    panel2.add(openButton);

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding/opening files.");
    panel2.add(cancelButton);

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    if (buttonFont != null) saveButton.setFont(buttonFont);
    saveButton.setMnemonic(KeyEvent.VK_S);
    saveButton.setToolTipText("Copy output text to a file.");
    panel2.add(saveButton);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel2.add(exitButton);

    panel1.add(panel2);
    panel1.add(Box.createVerticalStrut(2)); // extra space between panels

    /* Create a horizontal panel for the options. */

    JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for output text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel3.add(fontNameDialog);

    TreeSet sizelist = new TreeSet(); // collect font sizes 10 to 99 in order
    word = String.valueOf(fontSize); // convert number to a string we can use
    sizelist.add(word);           // add default or user's chosen font size
    for (i = 0; i < FONT_SIZES.length; i ++) // add our preferred size list
      sizelist.add(FONT_SIZES[i]); // assume sizes are all two digits (10-99)
    fontSizeDialog = new JComboBox(sizelist.toArray()); // give user nice list
    fontSizeDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setSelectedItem(word); // selected item is our default size
    fontSizeDialog.setToolTipText("Point size for output text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel3.add(fontSizeDialog);

    panel3.add(Box.createHorizontalStrut(20));

    detailCheckbox = new JCheckBox("details", detailFlag);
    if (buttonFont != null) detailCheckbox.setFont(buttonFont);
    detailCheckbox.setToolTipText("Select to show detailed checksums.");
    detailCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(detailCheckbox);

    panel3.add(Box.createHorizontalStrut(10));

    showAllCheckbox = new JCheckBox("show all files", showAllFlag);
    if (buttonFont != null) showAllCheckbox.setFont(buttonFont);
    showAllCheckbox.setToolTipText(
      "Select to show all files, not just errors.");
    showAllCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(showAllCheckbox);

    panel3.add(Box.createHorizontalStrut(10));

    recurseCheckbox = new JCheckBox("subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(recurseCheckbox);

    panel1.add(panel3);
    panel1.add(Box.createVerticalStrut(6)); // extra space at panel bottom

    /* Put above boxed options in a panel that is centered horizontally.  Use
    FlowLayout's horizontal gap to add padding on the left and right sides. */

    JPanel panel4 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel4.add(panel1);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(12, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setText(
      "\nVerify the checksums inside OpenType and TrueType font files.\n"
      + "\nChoose your options; then open files or folders to search.\n\n"
      + COPYRIGHT_NOTICE + "\n\n");

    /* Create an entire panel just for the status message.  We do this so that
    we have some control over the margins.  Put the status text in the middle
    of a BorderLayout so that it expands with the window size. */

    JPanel panel5 = new JPanel(new BorderLayout(0, 0));
    statusDialog = new JLabel(EMPTY_STATUS, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    statusDialog.setToolTipText(
      "Running status as files are processed by the Open button.");
    panel5.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
    panel5.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel5.add(statusDialog, BorderLayout.CENTER);
    panel5.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel5.add(Box.createVerticalStrut(3), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons and
    options above the text area.  Keep text in the center so that it expands
    horizontally and vertically.  Put status message at the bottom, which also
    expands. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel6 = mainFrame.getContentPane(); // where content meets frame
    panel6.setLayout(new BorderLayout(0, 0));
    panel6.add(panel4, BorderLayout.NORTH); // buttons and options
    panel6.add(new JScrollPane(outputText), BorderLayout.CENTER); // text area
    panel6.add(panel5, BorderLayout.SOUTH); // status message

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  checkFile() method

  Open one OpenType or TrueType font file and test the checksums.  All other
  methods in this program eventually come here.  We print our result and
  increment global counters for good and bad files.
*/
  static void checkFile(
    File givenFile,               // caller's Java File object (may be valid)
    String givenName)             // caller's name for file, or empty, or null
  {
    boolean badNameChar;          // if illegal character in entry tag name
    boolean badNameOrder;         // if entry tag names not in correct order
    final String CANCEL_TEXT = "cancelled by user"; // message when cancelled
    char ch;                      // one character from entry tag name
    int curFont;                  // current font index, up to <numFonts>
    int curTable;                 // current table index, up to <numTables>
    Vector detailBuffer;          // each string is one line in detail report
    long entryChecksum;           // table directory checksum value
    long entryLength;             // table directory length in bytes
    String entryName;             // <entryTag> converted to a string
    StringBuffer entryNameBuffer; // intermediate buffer to create <entryName>
    long entryOffset;             // table directory file offset
    long entryTag;                // table directory tag name (as integer)
    int errorCount;               // local number of errors for this file
    String fileName;              // our name for the file
    long fileSize;                // length of file in bytes
    final String FILE_TEXT = "can't read from file"; // message for I/O errors
    long[] fontOffsets;           // for each font, offset from beginning of
                                  // ... file to start of its "Offset Table"
    int i;                        // index variable
    long maxFileUsed;             // maximum extent of all tables in font file
    String message;               // for building up temporary messages
    boolean multiFlag;            // true if processing TrueType collection
    int numFonts;                 // number of fonts in this file
    int numTables;                // number of entries in "Offset Table"
    String prevTagName;           // previous entry tag name or null
    RandomAccessFile ramFile;     // file stream for reading font file
    long signature;               // signature bytes from beginning of file
    boolean stopFlag;             // local flag to stop processing file
    String stopText;              // message for why we stopped processing
    long table;                   // current location in "Offset Table"
    int warnCount;                // local number of warnings for this file

    /* Initialize local variables. */

    detailBuffer = new Vector();  // always generate, but sometimes print
    entryNameBuffer = new StringBuffer("ABCD"); // always exactly 4 characters
    errorCount = 0;               // no errors reported yet
    fontOffsets = null;           // just to keep compiler happy
    maxFileUsed = 0;              // assume that all font tables are empty
    multiFlag = false;            // just to keep compiler happy
    numFonts = 0;                 // just to keep compiler happy
    stopFlag = false;             // everything is okay so far
    stopText = null;              // no reason for stopping yet
    totalFiles ++;                // number of files found (any file type)
    warnCount = 0;                // no warnings reported yet

    /* Use the caller's name for the file, if one was given.  This allows the
    caller to display a cleaner name, such as a relative path name. */

    if ((givenName != null) && (givenName.length() > 0))
      fileName = givenName;       // caller gave us a non-empty string
    else
      fileName = givenFile.getPath(); // get full path name plus file name

    fileSize = givenFile.length(); // get length of file in bytes

    /* If printing detailed information, then start with the file name. */

//  detailBuffer.add("");         // put blank line before file name
    detailBuffer.add(fileName);   // put file name in detail report
    setStatusMessage("Reading file " + fileName); // for graphical applications

    /* Open the file and start reading header information.  Errors are divided
    into three broad categories:

    (1) Fatal errors set <stopFlag>.  The file is either not a font file, or is
        too badly damaged to be a font file.  Setting <stopFlag> prevents the
        undocumented <cornishFlag> option from taking effect.

    (2) Checksum errors increment <errorCount> and later <totalErrors>, and the
        file may be renamed by the <cornishFlag> option.

    (3) Warnings increment <warnCount> but by themselves do not consider a file
        to be correct or incorrect.

    These categories won't please everyone.  They are, however, workable for
    most font files of reasonable quality. */

    try                           // catch file I/O errors
    {
      ramFile = new RandomAccessFile(givenFile, "r"); // open file for reading

      /* Figure out what type of file this is.  Errors are fatal at this stage
      and are not counted in <errorCount> because the file is most likely not a
      font file. */

      if (fileSize < 12)          // need this many bytes for signature
      {
        stopFlag = true;          // stop looking at this file
        stopText = "file too small for OTF/TTC/TTF signature";
        detailBuffer.add("  " + stopText); // append full report
      }
      else if (fileSize > MAX_FILE_SIZE) // signed 32 bits rounded down 4 bytes
      {
        stopFlag = true;          // stop looking at this file
        stopText = "file bigger than " + formatComma.format(MAX_FILE_SIZE)
          + " bytes or 0x" + Long.toHexString(MAX_FILE_SIZE);
        detailBuffer.add("  " + stopText); // append full report
      }
      else
      {
        signature = ((long) ramFile.readInt()) & INT_MASK;
                                  // read four signature bytes
        if ((signature == 0x00010000L) // binary version 1.0 for TrueType
          || (signature == 0x4F54544FL) // "OTTO" for OpenType
          || (signature == 0x74727565L)) // "true" for Macintosh TrueType
        {
          /* This is an OpenType or TrueType file containing a single font. */

          multiFlag = false;      // this is not a TrueType collection
          numFonts = 1;           // there is only one font in this file
          fontOffsets = new long[1]; // so we only look at one "Offset Table"
          fontOffsets[0] = 0;     // conveniently located at start of file
        }
        else if (signature == 0x74746366L) // "ttcf" for TrueType collection
        {
          /* This is a TrueType collection containing at least one font. */

          multiFlag = true;       // now processing a collection of fonts
          long ttcVersion = ((long) ramFile.readInt()) & INT_MASK;
                                  // may need version number of TTC header
          numFonts = ramFile.readInt(); // get number of fonts in this file
          if ((numFonts < 1) || (numFonts > 29))
          {                       // error check with our arbitrary limits
            stopFlag = true;      // stop looking at this file
            stopText = "too many internal fonts in TTC file (" + numFonts
              + ")";
            detailBuffer.add("  " + stopText); // append full report
          }
          else if (fileSize < ((4 * numFonts) + 24))
          {                       // check maximum TTC header size (version 2)
            stopFlag = true;      // stop looking at this file
            stopText = "file too small for list of TTC offsets";
            detailBuffer.add("  " + stopText); // append full report
          }
          else
          {
            fontOffsets = new long[numFonts]; // multiple "Offset Tables"
            for (curFont = 0; curFont < numFonts; curFont ++)
              fontOffsets[curFont] = ((long) ramFile.readInt()) & INT_MASK;
                                  // save file offset to each "Offset Table"

            /* TTC files with version 2 headers may have a digital signature
            (DSIG) table.  The space allocated to the table must be accounted
            for in the maximum file size used.  Verifying DSIG tables is much
            too complicated for this simple Java application.

            Note: due to compatibility problems with older software, almost all
            TTC files with DSIG tables have their TTC headers marked as version
            1 when in fact they are version 2.  We ignore <ttcVersion> and look
            for a magic number equal to "DSIG" as text. */

            if (true) // (ttcVersion == 0x00020000L) // look for DSIG table
            {
              long ulDsigTag = ((long) ramFile.readInt()) & INT_MASK;
              long ulDsigLength = ((long) ramFile.readInt()) & INT_MASK;
              long ulDsigOffset = ((long) ramFile.readInt()) & INT_MASK;
              if (ulDsigTag != 0x44534947L) // magic number for "DSIG" as text
              {
                /* No DSIG table.  Do nothing more here. */
              }
              else if ((ulDsigOffset + ulDsigLength) <= MAX_FILE_SIZE)
              {
                /* The current value of <maxFileUsed> is always zero, so we
                don't need Math.max() here, but for the sake of consistency,
                we will do this the same way as below. */

                maxFileUsed = Math.max(maxFileUsed, (ulDsigOffset
                  + ulDsigLength));
              }
              else                // numbers are bigger than signed 32-bit
              {
                stopFlag = true;  // stop looking at this file
                stopText = "bad DSIG offset " + formatHexEight(ulDsigOffset)
                  + " or length " + formatHexEight(ulDsigLength);
                detailBuffer.add("  " + stopText); // append full report
              }
            }
          }
        }
        else
        {
          stopFlag = true;        // stop looking at this file
          stopText = "file signature " + formatHexEight(signature)
            + " not recognized as OpenType or TrueType font";
          detailBuffer.add("  " + stopText); // append full report
        }
      }

      /* No matter what type of font file this started as, we now have a common
      list of the locations (offsets) to the start of each "Offset Table" for
      each font contained in the file.  This information must all be checked,
      before being used, because the user may have given us a bad file.  Errors
      are now counted in <errorCount> and we try to continue. */

      if ((!cancelFlag) && (!stopFlag)) // continue only if no errors so far
      {
        for (curFont = 0; curFont < numFonts; curFont ++) // first <for> loop
        {
          if (cancelFlag)         // stop if user hit the panic button
          {
            stopFlag = true;      // stop looking at this file
            stopText = CANCEL_TEXT; // why we stopped
            break;                // exit from first <for> loop
          }

          /* Do we identify each internal font for TTC files? */

          if (multiFlag)
            detailBuffer.add("  internal font #" + (curFont + 1) + ":");

          /* See if we can read the entire "Offset Table" and if the size makes
          sense. */

          table = fontOffsets[curFont]; // get start of this "Offset Table"
          if (fileSize < (table + 12)) // can we read start of this table?
          {
            errorCount ++;        // can't do more with this Offset Table
            stopText = "file too small for start of Offset Table at "
              + formatHexEight(table);
            detailBuffer.add("    " + stopText); // append full report
            continue;             // restart first <for> loop
          }
          if ((table % 4) != 0)   // should be multiple of four bytes
          {
            warnCount ++;         // alignment problems are only a warning
            stopText = "Offset Table at " + formatHexEight(table)
              + " should start on 4-byte boundary (warning)";
            detailBuffer.add("    " + stopText); // append full report
          }

          ramFile.seek(table);    // position to start of "Offset Table"
          signature = ((long) ramFile.readInt()) & INT_MASK;
                                  // read four signature bytes
          if ((signature != 0x00010000L) // binary version 1.0 for TrueType
            && (signature != 0x4F54544FL) // "OTTO" for OpenType
            && (signature != 0x74727565L)) // "true" for Macintosh TrueType
          {
            errorCount ++;        // can't do more with this Offset Table
            stopText = "bad signature " + formatHexEight(signature)
              + " in Offset Table";
            detailBuffer.add("    " + stopText); // append full report
            continue;             // restart first <for> loop
          }

          /* Looks like the start of a valid "Offset Table".  See if we can
          read the whole table. */

          numTables = ramFile.readUnsignedShort(); // get entries in table
          if ((numTables < 1) || (numTables > 299)) // check arbitrary limits
          {
            errorCount ++;        // can't do more with this Offset Table
            stopText = "too many entries in Offset Table (" + numTables + ")";
            detailBuffer.add("    " + stopText); // append full report
            continue;             // restart first <for> loop
          }
          if (fileSize < (table + 12 + (16 * numTables)))
          {
            errorCount ++;        // can't do more with this Offset Table
            stopText = "file too small for Offset Table at "
              + formatHexEight(table) + " with "
              + prettyPlural(numTables, "entry", "entries");
            detailBuffer.add("    " + stopText); // append full report
            continue;             // restart first <for> loop
          }

          /* Calculate and compare checksums for each table directory listed in
          the current "Offset Table". */

          badNameOrder = false;   // assume entry tag names in correct order
          prevTagName = null;     // there is no previous entry tag name
          for (curTable = 0; curTable < numTables; curTable ++)
                                  // second <for> loop
          {
            if (cancelFlag)       // stop if user hit the panic button
            {
              stopFlag = true;    // stop looking at this file
              stopText = CANCEL_TEXT; // why we stopped
              break;              // exit from second <for> loop
            }

            /* Read the table directory entry tag (name) as a 4-byte integer
            and convert to a string.  Check that all characters are printable.
            Since all entry names are exactly four bytes (characters) long, we
            re-use the same StringBuffer for building up the names. */

            ramFile.seek(table + 12 + (16 * curTable)); // reset file position
            entryTag = ((long) ramFile.readInt()) & INT_MASK;
                                  // get entry tag (name) as integer
            entryChecksum = ((long) ramFile.readInt()) & INT_MASK;
            entryOffset = ((long) ramFile.readInt()) & INT_MASK;
            entryLength = ((long) ramFile.readInt()) & INT_MASK;

            badNameChar = false;  // assume that table name is acceptable
            for (i = 0; i < 4; i ++) // convert four bytes to characters
            {
              ch = (char) ((entryTag >> BYTE_SHIFTS[i]) & BYTE_MASK);
                                  // get one byte/character from integer tag
              if ((ch >= 0x20) && (ch <= 0x7E)) // is printable US-ASCII?
                entryNameBuffer.setCharAt(i, ch); // insert character in name
              else                // illegal byte/character in table name
              {
                badNameChar = true; // handle this later, after <for> loop
                entryNameBuffer.setCharAt(i, '?'); // substitution character
              }
            }
            entryName = entryNameBuffer.toString(); // convert tag to name

            if (badNameChar)      // was there a problem with the table name?
            {
              warnCount ++;       // character problems are only a warning
              stopText = "bad bytes " + formatHexEight(entryTag)
                + " for table name \"" + entryName + "\" at offset "
                + formatHexEight(entryOffset) + " (warning)";
              detailBuffer.add("    " + stopText); // append full report
            }
            else if (!badNameOrder) // if no warnings yet about name order
            {
              if ((prevTagName != null) // if there was a previous table name
                && (prevTagName.compareTo(entryName) >= 0)) // and wrong order
              {
                badNameOrder = true; // limit one warning per internal font
                warnCount ++;     // sorting problems are only a warning
                stopText = "table names should be in US-ASCII order: \""
                  + prevTagName + "\" and \"" + entryName + "\" (warning)";
                detailBuffer.add("    " + stopText); // append full report
              }
              prevTagName = entryName; // save new name for next comparison
            }

            /* Compute the checksum for the indicated table and compare against
            what is stored in the font file.  The "head" table is special in
            that we must subtract a precomputed file checksum. */

            if (fileSize < (entryOffset + entryLength))
            {
              errorCount ++;      // count this as a checksum error
              stopText = "file too small for table \"" + entryName
                + "\" offset " + formatHexEight(entryOffset)
                + " length " + formatHexEight(entryLength);
              detailBuffer.add("    " + stopText); // append full report
              continue;           // restart second <for> loop
            }
            if ((entryOffset % 4) != 0) // should be multiple of four bytes
            {
              warnCount ++;       // alignment problems are only a warning
              stopText = "table \"" + entryName + "\" offset "
                + formatHexEight(entryOffset)
                + " should start on 4-byte boundary (warning)";
              detailBuffer.add("    " + stopText); // append full report
            }

            maxFileUsed = Math.max(maxFileUsed, (entryOffset + entryLength));
            signature = checkFileRegion(ramFile, entryOffset, entryLength);
            if (cancelFlag)       // stop if user hit the panic button
            {
              stopFlag = true;    // stop looking at this file
              stopText = CANCEL_TEXT; // why we stopped
              break;              // exit from second <for> loop
            }
            if (entryName.equals("head") && (entryLength >= 12))
            {
              ramFile.seek(entryOffset + 8); // find "checkSumAdjustment"
              long checkSumAdjustment = ((long) ramFile.readInt()) & INT_MASK;
              signature = (signature - checkSumAdjustment) & INT_MASK;
            }
            if (entryChecksum == signature) // did we get the same checksum?
            {
              detailBuffer.add("    table \"" + entryName + "\" offset "
                + formatHexEight(entryOffset) + " length "
                + formatHexEight(entryLength) + " checksum "
                + formatHexEight(entryChecksum) + " correct");
            }
            else                  // our checksum differs from what's in file
            {
              errorCount ++;      // count the number of checksum errors
              stopText = "table \"" + entryName + "\" offset "
                + formatHexEight(entryOffset) + " length "
                + formatHexEight(entryLength) + " checksum "
                + formatHexEight(entryChecksum) + " error, calculated "
                + formatHexEight(signature);
              detailBuffer.add("    " + stopText); // append full report
            }
          } // end of second <for> loop

          if (stopFlag)           // did second <for> loop end early?
            break;                // yes, exit from first <for> loop

        } // end of first <for> loop
      }

      /* If this is not a TTC file, just a regular OTF or TTF file, then
      compute the overall file checksum and compare against a known value. */

      if ((!cancelFlag) && (!stopFlag)) // continue only if no errors so far
      {
        signature = checkFileRegion(ramFile, 0, fileSize); // do whole file
        if (cancelFlag)           // stop if user hit the panic button
        {
          stopFlag = true;        // stop looking at this file
          stopText = CANCEL_TEXT; // why we stopped
        }
        else if (multiFlag)       // report only, don't compare for TTC files
        {
          detailBuffer.add("  file checksum " + formatHexEight(signature)
            + " reported but not compared for TTC file");
        }
        else if (signature == TTF_CHECKSUM) // correct for all OTF, TTF files?
        {
          detailBuffer.add("  file checksum " + formatHexEight(signature)
            + " correct for OTF/TTF file");
        }
        else                      // like many cheap TTF files, bad checksum
        {
          errorCount ++;          // count the number of checksum errors
          stopText = "file checksum " + formatHexEight(signature)
            + " should be " + formatHexEight(TTF_CHECKSUM)
            + " for OTF/TTF file";
          detailBuffer.add("  " + stopText); // append full report
        }
      }

      /* Close the input font file. */

      ramFile.close();            // try to close input file
    }

    /* Catch any file I/O errors, here or in called methods. */

    catch (IOException ioe)
    {
      stopFlag = true;            // stop looking at this file
      stopText = FILE_TEXT;       // why we stopped
      detailBuffer.add("  " + stopText); // append full report
    }

    /* While it is possible to append arbitrary data to the end of a font file,
    and still have that file fully functional in most operating systems, we
    check if the file size matches the end of the last known table (rounded up
    to the nearest multiple of four bytes).  This only makes sense if there
    were no serious errors above. */

    if ((!cancelFlag) && checkSizeFlag && (!stopFlag))
    {
      maxFileUsed = ((maxFileUsed + 3) / 4) * 4; // round up multiple 4 bytes
      if (fileSize != maxFileUsed) // if file size is not precisely correct
      {
        warnCount ++;             // size problems are only a warning
        stopText = "file size " + formatComma.format(fileSize)
          + " bytes should be " + formatComma.format(maxFileUsed) + " or 0x"
          + Long.toHexString(maxFileUsed) + " (warning)";
        detailBuffer.add("  " + stopText); // append full report
      }
    }

    /* Print a summary and increment global counters.  Any code above that sets
    <errorCount> or <warnCount> is assumed to put a message in <stopText>. */

    if (!cancelFlag)              // continue only if user is still happy
    {
      if (stopFlag)               // if there was a fatal error
      {
        /* Fatal error messages take priority over all other messages. */
      }
      else if ((errorCount + warnCount) == 0) // if no errors or warnings
      {
        stopText = "all checksums correct"; // no problems found in this file
        detailBuffer.add("  " + stopText); // append full report
      }
      else                        // at least one error or warning
      {
        if (warnCount > 0)        // if there were any warnings
          message = "found " + prettyPlural(errorCount, "error") + " and "
            + prettyPlural(warnCount, "warning");
        else                      // if errors but no warnings
          message = "found " + prettyPlural(errorCount, "error");
        detailBuffer.add("  " + message); // append our message to report
        if ((errorCount + warnCount) > 1) // if more than one error or warning
          stopText = message;     // replace last message with our text
      }

      if ((errorCount > 0) || showAllFlag || stopFlag || (warnCount > 0))
      {
        if (detailFlag)           // detail report already has file name
        {
          /* We blast the entire detail report onto the screen at once for GUI
          applications, leaving line-by-line for consoles.  This makes the GUI
          look faster, and is also easier to read (less scrolling). */

          int lines = detailBuffer.size(); // get number of detail lines
          if (consoleFlag)        // print line-by-line with system newlines
          {
            for (i = 0; i < lines; i ++) // each vector element is one line
              putOutput((String) detailBuffer.get(i)); // print with newline
            putOutput("");        // put blank line after detail report
          }
          else                    // combine text for graphical application
          {
            StringBuffer buffer = new StringBuffer(); // empty string buffer
            for (i = 0; i < lines; i ++) // each vector element is one line
            {
              buffer.append((String) detailBuffer.get(i)); // one text line
              buffer.append("\n"); // plus standard Java newline character
            }
            putOutput(buffer.toString()); // called method adds final newline
          }
        }
        else                      // simple summary must show file name
          putOutput(fileName + " - " + stopText); // plus the stop message
      }

      if (stopFlag)               // was there something horribly wrong?
        { /* don't add this file to global counters */ }
      else if (errorCount > 0)    // were there any checksum errors?
        totalErrors ++;           // yes, count this file as an error
      else if (warnCount > 0)     // no errors, but were there warnings?
        totalWarning ++;          // yes, count this file as a warning
      else                        // no errors, no warnings
        totalCorrect ++;          // count this file as a valid font
    }

    /* A customized option is to rename files with errors.  Files with fatal
    errors are excluded because they couldn't be opened or properly read. */

    if ((!cancelFlag) && cornishFlag && (errorCount > 0) && (!stopFlag))
    {
      cornishRename(givenFile);   // rename this file
    }
  } // end of checkFile() method


/*
  checkFileRegion() method

  Calculate the checksum for a region of the font file.  The caller gives us
  the starting offset in the file and the number of bytes to read.  We assume
  that the caller has already checked that this offset and length are within
  the proper size of the file.  We watch <cancelFlag> because reading the font
  file may take a long time for Chinese/Japanese/Korean/Unicode fonts.

  Font file checksums are simple sums of 4-byte integers, ignoring overflow.
  If the given length to read is not a multiple of four bytes, then we supply
  imaginary zero bytes.
*/
  static long checkFileRegion(
    RandomAccessFile ramFile,     // file stream for reading font file
    long offset,                  // starting offset in file
    long length)                  // length to read in bytes
    throws IOException            // comes from read() and seek()
  {
    int bytesRead;                // number of bytes successfully read this I/O
    long bytesWanted;             // maximum number of bytes wanted on this I/O
    int i;                        // index variable
    long result;                  // our calculated checksum
    long totalRead;               // total bytes read and processed for all I/O

    ramFile.seek(offset);         // position to start of selected region
    result = 0;                   // simple checksums start with zero sum
    totalRead = 0;                // no bytes read and processed yet
    while ((!cancelFlag) && (totalRead < length))
    {
      bytesWanted = length - totalRead; // can we do all bytes at once?
      if (bytesWanted > BUFFER_SIZE) // no, asking for too much
        bytesWanted = BUFFER_SIZE; // so reduce to actual size of buffer
      bytesRead = ramFile.read(inputBuffer, 0, (int) bytesWanted);
      if (bytesRead <= 0)         // did we read anything?
        break;                    // no, error or end of file
      for (i = 0; i < bytesRead; i ++) // process all bytes read
      {
        result += ((long) (inputBuffer[i] & BYTE_MASK)) << BYTE_SHIFTS[(int)
          (totalRead & 0x03)];    // shift byte to position, then add to total
        result &= INT_MASK;       // and throw away any overflow
        totalRead ++;             // increment total bytes read and processed
      }
    }
    return(result);               // return our calculated checksum

  } // end of checkFileRegion() method


/*
  cornishRename() method

  This is a customized method to rename files with checksum errors by adding a
  prefix to the name, if the prefix is not already part of the name.
*/
  static void cornishRename(File oldFile)
  {
    final String CORNISH_PREFIX = "(CHK)"; // prefix for renaming files
    File newFile;                 // file object for new file name
    String oldName;               // old file name (before renaming)

    oldName = oldFile.getName();  // get existing file name
    if (oldName.startsWith(CORNISH_PREFIX) // have we been here before?
      || oldName.startsWith("(BAD)") // already flagged by Phil's FontRenamer?
      || oldName.startsWith("(INV)"))
    {
      return;                     // yes, don't rename the same file twice
    }
    newFile = new File(oldFile.getParentFile(), (CORNISH_PREFIX + oldName));
                                  // create file object for new file name
    if (newFile.exists())         // is there already a file by this name?
    {
      putError("Can't rename, file name in use: " + oldFile.getPath() + " to "
        + newFile.getPath());
      return;                     // renaming would delete existing file
    }
    if (oldFile.renameTo(newFile) == false) // rename and check status
    {
      putError("Can't rename, operation failed: " + oldFile.getPath() + " to "
        + newFile.getPath());
    }
  } // end of cornishRename() method


/*
  doCancelButton() method

  This method is called while we are opening files or folders if the user wants
  to end the processing early, perhaps because it is taking too long.  We must
  cleanly terminate any secondary threads.  Leave whatever output has already
  been generated in the output text area.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    putOutput("Cancelled by user.");
  }


/*
  doOpenButton() method

  Allow the user to select one or more font files or folders.
*/
  static void doOpenButton()
  {
    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.addChoosableFileFilter(fontFilter); // our filter is a choice
    fileChooser.setDialogTitle("Open Font Files or Folders...");
    fileChooser.setFileFilter(fontFilter); // choose default file filter
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(true); // allow more than one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    openFileList = sortFileList(fileChooser.getSelectedFiles());
                                  // get list of files selected by user

    /* We have a list of files or folders.  Disable the "Open" button until we
    are done, and enable a "Cancel" button in case our secondary thread runs
    for a long time and the user panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    setStatusMessage(EMPTY_STATUS); // clear status message at bottom of window
    statusTimer.start();          // start updating the status message
    totalCorrect = totalErrors = totalFiles = totalWarning = 0; // counters

    openFilesThread = new Thread(new FontChecksum2User(), "doOpenRunner");
    openFilesThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    openFilesThread.start();      // run separate thread to open files, report

  } // end of doOpenButton() method


/*
  doOpenRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void doOpenRunner()
  {
    int i;                        // index variable

    /* Loop once for each file name selected.  Don't assume that these are all
    valid file names or even fonts. */

    for (i = 0; i < openFileList.length; i ++)
    {
      if (cancelFlag) break;      // exit from <for> loop if user cancelled
      openFileFolder(openFileList[i], ""); // process this file or folder
    }

    /* If we weren't cancelled, print a summary. */

    if (!cancelFlag)
    {
      putOutput("Found " // + prettyPlural(totalFiles, "file") + ": "
        + prettyPlural(totalCorrect, "correct font") + ", "
        + prettyPlural(totalErrors, "with", "with") + " errors, and "
        + prettyPlural(totalWarning, "with", "with") + " only warnings.");
    }

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button
    openButton.setEnabled(true);  // enable "Open" button
    statusTimer.stop();           // stop updating status on timer ticks
    setStatusMessage(EMPTY_STATUS); // and clear any previous status message

  } // end of doOpenRunner() method


/*
  doSaveButton() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void doSaveButton()
  {
    FileWriter output;            // output file stream
    File userFile;                // file chosen by the user

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile();

    /* See if we can write to the user's chosen file. */

    if (userFile.isDirectory())   // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isFile() == false) // if file doesn't exist
    {
      /* Maybe we can create a new file by this name.  Do nothing here. */
    }
    else if (userFile.canWrite() == false) // file exists, but is read-only
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      return;
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      output = new FileWriter(userFile); // try to open output file
      outputText.write(output);   // couldn't be much easier for writing!
      output.close();             // try to close output file
    }
    catch (IOException ioe)
    {
      putOutput("Can't write to text file: " + ioe.getMessage());
    }
  } // end of doSaveButton() method


/*
  formatHexEight()

  Format an integer parameter as an 8-digit hexadecimal string with leading
  zeros if necessary.  Eight digits are enough for 32-bit integers ("int").
*/
  static String formatHexEight(long number)
  {
    String text;                  // intermediate result

    text = "00000000" + Long.toHexString(number); // pad with leading zeros
    return(text.substring(text.length() - 8)); // return formatted result
  }


/*
  openFileFolder() method

  The caller gives us a file or folder.  We always process files given to us
  (good, bad, or hidden).  For folders, we check each file in the folder, and
  process subfolders only if the recursion flag is true.
*/
  static void openFileFolder(
    File givenFile,               // caller's Java File object (may be valid)
    String givenPath)             // caller's relative path string, or empty
  {
    File[] contents;              // contents if <givenFile> is a folder
    int i;                        // index variable
    String name;                  // our name for the file
    File next;                    // next File object from <contents>

    name = givenPath + ((givenPath.length() > 0) ? systemFileSep : "")
      + givenFile.getName();      // create file name relative to given path

    /* Decide what kind of File object this is, or if it's even real!  The code
    when we find a subfolder mimics the overall structure of this method. */

    if (givenFile.isDirectory())  // is this "file" actually a folder?
    {
      if (consoleFlag)            // write on standard error for consoles
        putError("Searching folder " + name);
      else                        // show running status message for GUI
        setStatusMessage("Searching folder " + name);

      /* Get contents of this folder and filter for font file names, if that
      option hasn't been changed by the user. */

      if (consoleFlag || (fileChooser.getFileFilter() == fontFilter))
        contents = givenFile.listFiles((java.io.FileFilter) fontFilter);
      else
        contents = givenFile.listFiles(); // no filter: all files, subfolders

      contents = sortFileList(contents); // put directory in preferred order

      /* Process each file or subfolder found in this folder. */

      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
        {
          /* Silently ignore hidden files and folders. */
        }
        else if (next.isDirectory()) // a subfolder inside caller's folder?
        {
          if (recurseFlag)        // do subfolders only if option selected
            openFileFolder(next, name); // yes, do recursion
          else
            { /* Silently ignore subfolders. */ }
        }
        else if (next.isFile())   // we do want to look at normal files
        {
          openFileFolder(next, name); // always do files found in folder
        }
        else                      // file directory has an invalid entry
        {
          /* Silently ignore unknown directory entries. */
        }
      }
    }
    else if (givenFile.isFile())  // we do want to look at normal files
    {
      checkFile(givenFile, name); // always open files given by user
    }
    else                          // user gave bad file or folder name
    {
      putError("Unable to open file or folder: " + givenFile.getPath());
    }
  } // end of openFileFolder() method


/*
  prettyPlural() method

  Return a string that formats a number and appends a lowercase "s" to a word
  if the number is plural (not one).  Also provide a more general method that
  accepts both a singular word and a plural word.
*/
  static String prettyPlural(
    long number,                  // number to be formatted
    String singular)              // singular word
  {
    return(prettyPlural(number, singular, (singular + "s")));
  }

  static String prettyPlural(
    long number,                  // number to be formatted
    String singular,              // singular word
    String plural)                // plural word
  {
    final String[] names = {"zero", "one", "two"};
                                  // names for small counting numbers
    String result;                // our converted result

    if ((number >= 0) && (number < names.length))
      result = names[(int) number]; // use names for small counting numbers
    else
      result = formatComma.format(number); // format number with digit grouping

    if (number == 1)              // is the number singular or plural?
      result += " " + singular;   // append singular word
    else
      result += " " + plural;     // append plural word

    return(result);               // give caller our converted string

  } // end of prettyPlural() method


/*
  putError() method

  Similar to putOutput() except write on standard error if running as a console
  application.  See putOutput() for details.  This method is more for tracing
  execution than for generating a report.  Routines that can only be called
  from within GUI applications should always call putOutput().
*/
  static void putError(String text)
  {
    if (consoleFlag)              // are we running as a console application?
      System.err.println(text);   // console output goes onto standard error
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  putOutput() method

  Append a complete line of text to the end of the output text area.  We add a
  newline character at the end of the line, not the caller.  By forcing all
  output to go through this same method, one complete line at a time, the
  generated output is cleaner and can be redirected.

  The output text area is forced to scroll to the end, after the text line is
  written, by selecting character positions that are much too large (and which
  are allowed by the definition of the JTextComponent.select() method).  This
  is easier and faster than manipulating the scroll bars directly.  However, it
  does cancel any selection that the user might have made, for example, to copy
  text from the output area.
*/
  static void putOutput(String text)
  {
    if (consoleFlag)              // are we running as a console application?
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  setStatusMessage() method

  Set the text for the status message if we are running as a GUI application.
  This gives the user some indication of our progress if processing is slow.
  If the update timer is running, then this message will not appear until the
  timer kicks in.  This prevents the status from being updated too often, and
  hence being unreadable.
*/
  static void setStatusMessage(String text)
  {
    statusPending = text;         // save caller's message for later
    if (consoleFlag)              // are we running as a console application?
    {
      /* Do nothing: console doesn't show running status messages. */
    }
    else if (statusTimer.isRunning()) // are we updating on a timed basis?
    {
      /* Do nothing: wait for timer to kick in and update GUI text. */
    }
    else
      statusDialog.setText(text); // show the status message now
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  FontChecksum2  [options]  [file or folder names]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -a0 = show only files with checksum errors (default)");
    System.err.println("  -a1 = -a = show result for all font files found");
//  System.err.println("  -cornish = rename files with checksum errors, prefix with (CHK)");
    System.err.println("  -d0 = show summary for each file reported (default)");
    System.err.println("  -d1 = -d = show detailed checksum information");
    System.err.println("  -h0 = ignore hidden files or folders (default)");
    System.err.println("  -h1 = -h = process hidden files and folders");
    System.err.println("  -s0 = do only given files or folders, no subfolders (default)");
    System.err.println("  -s1 = -s = process files, folders, and subfolders");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println("  -z0 = don't check if file size is correct");
    System.err.println("  -z1 = -z = compare file size with internal tables (default)");
    System.err.println();
    System.err.println("Output may be redirected with the \">\" operator.  If no file or folder names");
    System.err.println("are given on the command line, then a graphical interface will open.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  sortFileList() method

  When we ask for a list of files or subfolders in a directory, the list is not
  likely to be in our preferred order.  Java does not guarantee any particular
  order, and the observed order is whatever is supplied by the underlying file
  system (which can be very jumbled for FAT16/FAT32).  We would like the file
  names to be sorted, and since we recurse on subfolders, we also want the
  subfolders to appear in order.

  The caller's parameter may be <null> and this may happen if the caller asks
  File.listFiles() for the contents of a protected system directory.  All calls
  to listFiles() in this program are wrapped inside a call to us, so we replace
  a null parameter with an empty array as our result.
*/
  static File[] sortFileList(File[] input)
  {
    String fileName;              // file name without the path
    int i;                        // index variable
    TreeMap list;                 // our list of files
    File[] result;                // our result
    StringBuffer sortKey;         // created sorting key for each file

    if (input == null)            // were we given a null pointer?
      result = new File[0];       // yes, replace with an empty array
    else if (input.length < 2)    // don't sort lists with zero or one element
      result = input;             // just copy input array as result array
    else
    {
      /* First, create a sorted list with our choice of index keys and the File
      objects as data.  Names are sorted as files or folders, then in lowercase
      to ignore differences in uppercase versus lowercase, then in the original
      form for systems where case is distinct. */

      list = new TreeMap();       // create empty sorted list with keys
      sortKey = new StringBuffer(); // allocate empty string buffer for keys
      for (i = 0; i < input.length; i ++)
      {
        sortKey.setLength(0);     // empty any previous contents of buffer
        if (input[i].isDirectory()) // is this "file" actually a folder?
          sortKey.append("2 ");   // yes, put subfolders after files
        else                      // must be a file or an unknown object
          sortKey.append("1 ");   // put files before subfolders

        fileName = input[i].getName(); // get the file name without the path
        sortKey.append(fileName.toLowerCase()); // start by ignoring case
        sortKey.append(" ");      // separate lowercase from original case
        sortKey.append(fileName); // then sort file name on original case
        list.put(sortKey.toString(), input[i]); // put file into sorted list
      }

      /* Second, now that the TreeMap object has done all the hard work of
      sorting, pull the File objects from the list in order as determined by
      the sort keys that we created. */

      result = (File[]) list.values().toArray(new File[0]);
    }
    return(result);               // give caller whatever we could find

  } // end of sortFileList() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main FontChecksum2 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if (source == detailCheckbox) // show detailed checksum information
    {
      detailFlag = detailCheckbox.isSelected();
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* We can safely assume that the font name is valid, because we obtained
      the names from getAvailableFontFamilyNames(), and the user can't edit
      this dialog field. */

      fontName = (String) fontNameDialog.getSelectedItem();
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == openButton) // "Open" button for files or folders
    {
      doOpenButton();             // open font files or folders
    }
    else if (source == recurseCheckbox) // recursion for folders, subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == showAllCheckbox) // show all files, or just errors
    {
      showAllFlag = showAllCheckbox.isSelected();
    }
    else if (source == statusTimer) // update timer for status message text
    {
      if (statusPending.equals(statusDialog.getText()) == false)
        statusDialog.setText(statusPending); // new message, update the display
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of FontChecksum2 class

// ------------------------------------------------------------------------- //

/*
  FontChecksum2Filter class

  This class limits the files shown in the file open dialog box to font files.
*/

class FontChecksum2Filter extends javax.swing.filechooser.FileFilter
                implements java.io.FileFilter // not the same as filechooser.*
{
  /* empty constructor */

  public FontChecksum2Filter() { }

  /* file filter: accept files of given types */

  public boolean accept(File givenFile)
  {
    String name = givenFile.getName().toLowerCase(); // get name of file
    if (givenFile.isDirectory()   // allow user to navigate directories
      || name.endsWith(".otf")    // accept this list of file types
      || name.endsWith(".ttc")
      || name.endsWith(".ttf"))
    {
      return(true);               // accept directories and most font files
    }
    return(false);                // reject anything else
  }

  /* file filter: return description of files that we accept */

  public String getDescription()
  {
    return("OpenType and TrueType font files");
  }

} // end of FontChecksum2Filter class

// ------------------------------------------------------------------------- //

/*
  FontChecksum2User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FontChecksum2User implements ActionListener, Runnable
{
  /* empty constructor */

  public FontChecksum2User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FontChecksum2.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FontChecksum2.doOpenRunner();
  }

} // end of FontChecksum2User class

/* Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License. */
