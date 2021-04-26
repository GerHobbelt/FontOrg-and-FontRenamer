/*
  Find Duplicate Files #3 - Find Duplicate Files With MD5 Checksums
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Wednesday, 29 October 2008
  Java class name: FindDupFiles3
  Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.

  When collecting a large number of files of any kind, there will be duplicates
  with the same file under two different names or in more than one place.  This
  is a Java 1.4 application to find duplicate files by searching for files that
  have the same size and the same MD5 checksum.  It won't find files that are
  merely similar, such as two consecutive photos of the same subject, or two
  MP3 songs encoded at different times.  Possible duplicates are reported to
  the user, who can then verify that the files are identical, either by
  inspection or by doing a byte-by-byte comparison with the "comp" command in
  DOS/Windows or the "cmp" command in UNIX.  What to do with files is the
  user's choice; the program does nothing except report the duplicates.  The
  probability of two files with different contents having the same size and MD5
  checksum is extremely small.

  To avoid wasting CPU time, MD5 checksums are only calculated if two or more
  files have the same size.  This program took two minutes on an Intel Pentium
  4 processor at 2.4 GHz to scan a collection of 16,362 font files of various
  sizes up to 39.6 MB and using a total of 5.2 GB.  Almost half of the files
  (7,393) had the same size as another file, which forced the MD5 to be
  computed.  Peak memory usage was under 23 MB when run as a graphical
  application, and 12 MB when run as a console application.

  See the DeleteDupFiles application to delete duplicate files when there is a
  "known good" folder and a folder of unknown files.  See the CompareFolders
  application for comparing two folders to determine if files and subfolders
  are identical.  See the FileChecksum application to generate or test
  checksums for a single file.

  GNU General Public License (GPL)
  --------------------------------
  FindDupFiles3 is free software: you can redistribute it and/or modify it
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

      java  FindDupFiles3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  FindDupFiles3  -s  d:\fonts  >report.txt

  The console application will return an exit status equal to the number of
  duplicate files found.  The graphical interface can be very slow when the
  output text area gets too big, which will happen if thousands of files are
  reported.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.security.*;           // MD5 and SHA1 message digests (checksums)
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class FindDupFiles3
{
  /* constants */

  static final long BIG_FILE_SIZE = 5 * 1024 * 1024; // "big" means over 5 MB
  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Find Duplicate Files With MD5 Checksums - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates

  /* class variables */

  static boolean aliasFlag;       // true if we detect aliases for same file
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static int checkCount;          // number of MD5 checksums calculated
  static long checkSize;          // total size of all checksums calculated
  static boolean consoleFlag;     // true if running as a console application
  static boolean debugFlag;       // true if we show debug information
  static JTextArea errorText;     // error messages for GUI applications
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static int fileCount;           // number of files found (any file type)
  static int folderCount;         // number of folders found
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static NumberFormat formatPointOne; // formats with one decimal digit
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static JFrame mainFrame;        // this application's window if GUI
  static int matchCount;          // number of duplicate files
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report if running as GUI
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JButton saveButton;      // "Save" button for writing output text
  static TreeMap sizeList;        // mapping from file sizes to list of files
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static JCheckBox zeroCheckbox;  // graphical option for <zeroFlag>
  static boolean zeroFlag;        // true if we report zero-byte empty files

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

    aliasFlag = true;             // by default, detect aliases for same file
    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    consoleFlag = false;          // assume no files or folders on command line
    debugFlag = false;            // by default, don't show debug information
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hiddenFlag = true;            // by default, process hidden files, folders
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    recurseFlag = true;           // by default, process subfolders
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;
    zeroFlag = false;             // by default, ignore zero-byte empty files

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatPointOne = NumberFormat.getInstance(); // current locale
    formatPointOne.setGroupingUsed(true); // use commas or digit groups
    formatPointOne.setMaximumFractionDigits(1); // force one decimal digit
    formatPointOne.setMinimumFractionDigits(1);

    /* Initialize the mapping from file sizes to list of files. */

    clearFileData();              // clear counters, reset size mappings, etc

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
        System.exit(0);           // exit application after printing help
      }

      else if (word.equals("-a") || (mswinFlag && word.equals("/a"))
        || word.equals("-a1") || (mswinFlag && word.equals("/a1")))
      {
        aliasFlag = true;         // recognize aliases for the same file
      }
      else if (word.equals("-a0") || (mswinFlag && word.equals("/a0")))
        aliasFlag = false;        // don't detect aliases or symbolic links

      else if (word.equals("-d") || (mswinFlag && word.equals("/d")))
      {
        debugFlag = true;         // show debug information
        System.err.println("main args.length = " + args.length);
        for (int k = 0; k < args.length; k ++)
          System.err.println("main args[" + k + "] = <" + args[k] + ">");
      }

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
          System.exit(-1);        // exit application after printing help
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
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.equals("-z") || (mswinFlag && word.equals("/z"))
        || word.equals("-z1") || (mswinFlag && word.equals("/z1")))
      {
        zeroFlag = true;          // process zero-byte empty files
      }
      else if (word.equals("-z0") || (mswinFlag && word.equals("/z0")))
        zeroFlag = false;         // ignore zero-byte empty files

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(-1);          // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume that this is a file
        or folder name.  We ignore <cancelFlag> because the user has no way of
        interrupting us at this point (no graphical interface). */

        consoleFlag = true;       // don't allow GUI methods to be called
        processFileOrFolder(new File(args[i]));
      }
    }

    /* If running as a console application, print a summary of what we found.
    Exit to the system with an integer status equal to the number of duplicate
    files. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      if (fileCount > 0)          // were any of the parameters usable?
        processFileData();        // examine collected file data and report
      else                        // user doesn't know how to use this program
        showHelp();               // show help summary

      System.exit(matchCount);    // exit from application with status
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

    action = new FindDupFiles3User(); // create our shared action listener
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

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 5));

    openButton = new JButton("Open File/Folder...");
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

    panel3.add(Box.createHorizontalStrut(30));

    recurseCheckbox = new JCheckBox("process subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(recurseCheckbox);

    panel3.add(Box.createHorizontalStrut(20));

    zeroCheckbox = new JCheckBox("show empty files", zeroFlag);
    if (buttonFont != null) zeroCheckbox.setFont(buttonFont);
    zeroCheckbox.setToolTipText("Select to report zero-byte empty files.");
    zeroCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(zeroCheckbox);

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
      "\nFind duplicate files with the same size and the same MD5 checksum."
      + "\n\nChoose your options; then open files or folders to search.\n\n"
      + COPYRIGHT_NOTICE + "\n");

    /* Create a smaller scrolling text area for error messages, etc. */

    errorText = new JTextArea(4, 40);
    errorText.setEditable(false); // user can't change this text area
    errorText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    errorText.setLineWrap(false); // don't wrap text lines
    errorText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    errorText.setText("");        // completely empty to begin with

    /* Create a split panel for the output text (top) and errors (bottom). */

    JSplitPane panel5 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
      new JScrollPane(outputText), new JScrollPane(errorText));
    panel5.setOneTouchExpandable(true); // fast widgets to expand/contract
    panel5.setResizeWeight(0.8);  // give most of the space to the top

    /* Create an entire panel just for the status message.  We do this so that
    we have some control over the margins.  Put the status text in the middle
    of a BorderLayout so that it expands with the window size. */

    JPanel panel6 = new JPanel(new BorderLayout(0, 0));
    statusDialog = new JLabel(EMPTY_STATUS, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    statusDialog.setToolTipText(
      "Running status as files are processed by the Open button.");
    panel6.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
    panel6.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel6.add(statusDialog, BorderLayout.CENTER);
    panel6.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel6.add(Box.createVerticalStrut(3), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons and
    options above the text area.  Keep text in the center so that it expands
    horizontally and vertically.  Put status message at the bottom, which also
    expands. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel7 = mainFrame.getContentPane(); // where content meets frame
    panel7.setLayout(new BorderLayout(0, 0));
    panel7.add(panel4, BorderLayout.NORTH); // buttons and options
    panel7.add(panel5, BorderLayout.CENTER); // text area (split panel)
    panel7.add(panel6, BorderLayout.SOUTH); // status message

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
  calculateChecksum() method

  Given a File object, return the MD5 checksum for that file as a hexadecimal
  string.  If the checksum can not be calculated, then a string beginning with
  "unknown" is returned instead.

  Reading the input one byte at a time is a very slow way to calculate the
  checksum: about one second per megabyte on an Intel Pentium 4 at 3 GHz.
  Reading the input in a large byte buffer, and passing this buffer to the
  message digest, is over 30 times faster.
*/
  static String calculateChecksum(File givenFile)
  {
    byte[] buffer;                // input buffer for reading file
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    int i;                        // index variable
    FileInputStream inStream;     // input file stream
    MessageDigest messDigest;     // object for calculating MD5 checksum
    String result;                // our result (the checksum as a string)
    long sizeDone;                // how much of <fileSize> has been finished
    String sizeText;              // pre-formatted portion of size message
    long sizeUser;                // last <sizeDone> reported to user

    /* It is tempting to return a constant for files whose size is zero, but
    that might bypass legitimate I/O error messages (no such file, etc). */

    filePath = givenFile.getPath(); // get name of caller's file, with path
    fileSize = givenFile.length(); // get size of caller's file in bytes
    sizeDone = sizeUser = 0;      // we haven't read anything yet
    sizeText = null;              // don't format big size message until needed

    if (consoleFlag == false)     // only format this message if running as GUI
      setStatusMessage("Checksum " + formatComma.format(fileSize)
        + " bytes for " + filePath);

    try
    {
      buffer = new byte[BUFFER_SIZE]; // allocate bigger, faster input buffer
      inStream = new FileInputStream(givenFile); // open file for reading bytes
      messDigest = MessageDigest.getInstance("MD5"); // initialize MD5 digest
      while ((i = inStream.read(buffer, 0, BUFFER_SIZE)) > 0)
      {
        /* The user may cancel our processing if this is a very big file.  We
        must always return a String result, even when things go wrong. */

        if (cancelFlag)           // stop if user hit the panic button
        {
          inStream.close();       // try to close input file early
          return("unknown: cancelled by user for " + filePath);
        }

        /* Update the checksum calculation with the new data. */

        messDigest.update(buffer, 0, i); // update checksum with input bytes
        sizeDone += i;            // add to number of bytes finished

        /* Update the GUI status if this is a big file. */

        if ((consoleFlag == false) && ((sizeDone - sizeUser) > BIG_FILE_SIZE))
        {
          if (sizeText == null)   // have we formatted the constant portion?
          {
            sizeText = " of " + formatMegabytes(fileSize) + " MB for "
              + filePath;
          }
          sizeUser = sizeDone;    // remember what we last told the user
          setStatusMessage("Checksum " + formatMegabytes(sizeDone) + sizeText);
        }
      }
      inStream.close();           // try to close input file
      result = formatHexBytes(messDigest.digest()); // convert to hex string
      checkCount ++;              // one more checksum successfully completed
      checkSize += fileSize;      // add size of this file to total calculated
    }
    catch (IOException ioe)       // file may be locked, invalid, etc
    {
      result = "unknown: file I/O error for " + filePath;
    }
    catch (NoSuchAlgorithmException nsae) // report our failure as a result
    {
      result = "unknown: bad algorithm for " + filePath;
    }

    if (debugFlag)                // does user want to see what we're doing?
      putError(filePath + " size " + formatComma.format(fileSize)
        + " checksum " + result);
    return(result);               // return calculated MD5 checksum to caller

  } // end of calculateChecksum() method


/*
  clearFileData() method

  Initialize all global variables related to the accumulated results: counters
  and the mapping from file sizes to list of files.
*/
  static void clearFileData()
  {
    checkCount = fileCount = folderCount = matchCount = 0; // nothing found yet
    checkSize = 0;                // no bytes used yet in checksum calculations
    sizeList = new TreeMap();     // empty mapping from sizes to list of files
  }


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
    putError("Cancelled by user."); // print message and scroll
  }


/*
  doOpenButton() method

  Allow the user to select one or more files or folders for processing.
*/
  static void doOpenButton()
  {
    /* Ask the user for input files or folders. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Open Files or Folders...");
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
    clearFileData();              // clear counters, reset size mappings, etc
    errorText.setText("");        // clear error text area
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    setStatusMessage(EMPTY_STATUS); // clear status message at bottom of window
    statusTimer.start();          // start updating the status message

    openFilesThread = new Thread(new FindDupFiles3User(), "doOpenRunner");
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

    try                           // catch most "out of memory" errors
    {
      /* Loop once for each file name selected.  Don't assume that these are
      all valid file names. */

      for (i = 0; i < openFileList.length; i ++)
      {
        if (cancelFlag) break;    // exit from <for> loop if user cancelled
        processFileOrFolder(openFileList[i]); // process this file or folder
      }

      /* Print a summary and scroll the output, even if we were cancelled. */

      processFileData();          // examine collected file data and report
    }
    catch (OutOfMemoryError oome) // for this thread only, not the GUI thread
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Not enough memory to complete your request.\nPlease close this program, then try increasing\nthe Java heap size with the -Xmx option on the\nJava command line.");
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
      putError("Can't write to text file: " + ioe.getMessage());
    }
  } // end of doSaveButton() method


/*
  formatHexBytes() method

  Format a raw array of binary bytes as a hexadecimal string.
*/
  static String formatHexBytes(byte[] raw)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'}; // for converting binary to hexadecimal
    int i;                        // index variable
    int value;                    // one byte value from raw array

    buffer = new StringBuffer(raw.length * 2);
                                  // allocate empty string buffer for result
    for (i = 0; i < raw.length; i ++)
    {
      value = raw[i];             // get one byte value from raw array
      buffer.append(hexDigits[(value >> 4) & 0x0F]); // hex high-order nibble
      buffer.append(hexDigits[value & 0x0F]); // hex low-order nibble
    }
    return(buffer.toString());    // give caller our converted string

  } // end of formatHexBytes() method


/*
  formatMegabytes() method

  Given a file size in bytes, return a formatted string with the size in real
  megabytes.  The caller must append any "MB" tag.
*/
  static String formatMegabytes(long filesize)
  {
    return(formatPointOne.format(((double) filesize) / 1048576.0));
  }


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
  processFileData() method

  Examine the collected data and print a report.  The very dense code below is
  really doing list processing.  We know that two files with different sizes
  can't be identical, so our initial processing is by size, which is very fast.
  If two or more files have the same size, then we compute the much slower MD5
  checksums.  Only if the sizes and the checksums are the same do we report
  particular files.
*/
  static void processFileData()
  {
    TreeMap checkEntry;           // one entry from <checkList>
    Iterator checkIterate;        // for iterating over elements in <checkList>
    String checkKey;              // one index key from <checkList>
    Set checkKeySet;              // index keys from sorted list of checksums
    TreeMap checkList;            // mapping from checksums to list of files

    File fileEntry;               // one entry from <fileValues>
    Iterator fileIterate;         // for iterating over elements in <fileValues>
    Collection fileValues;        // values from a sorted list of File objects

    TreeMap sizeEntry;            // one entry from <sizeList>
    Iterator sizeIterate;         // for iterating over elements in <sizeList>
    Long sizeKey;                 // one index key from <sizeList>
    Set sizeKeySet;               // index keys from sorted list of file sizes

    long spaceWasted;             // total bytes occupied by duplicate files

    /* Tell the user what stage the processing is at. */

    if (debugFlag)                // does user want debug information?
      putError("processFileData called");
    if (cancelFlag == false)      // do nothing more if user cancelled
      putError("Comparing file sizes and MD5 checksums.");

    /* For each file size, check the number of files that have the same file
    size.  If there are two or more, then we may have duplicate files. */

    sizeKeySet = sizeList.keySet(); // get sorted list of file sizes
    sizeIterate = sizeKeySet.iterator(); // get iterator for those keys
    spaceWasted = 0;              // no bytes wasted yet by duplicate files
    while ((!cancelFlag) && sizeIterate.hasNext()) // any more file sizes?
    {
      sizeKey = (Long) sizeIterate.next(); // get one file size as an object
      sizeEntry = (TreeMap) sizeList.get(sizeKey);
                                  // get mapping of files indexed by file name
      if (sizeEntry.size() > 1)   // more than one file for this file size?
      {
        /* This file size has more than one file with the same size in bytes.
        The files may be duplicates, or this may just be a coincidence.  Start
        a new sorted list using the MD5 checksums as the index key. */

        checkList = new TreeMap(); // mapping from checksums to list of files
        fileValues = sizeEntry.values(); // get list of files sorted by name
        fileIterate = fileValues.iterator(); // get iterator for those values
        while ((!cancelFlag) && fileIterate.hasNext()) // any more files?
        {
          fileEntry = (File) fileIterate.next(); // get file object from list
          checkKey = calculateChecksum(fileEntry); // calculate MD5 checksum
          if (checkList.containsKey(checkKey) == false)
                                  // is there an entry for this checksum?
            checkList.put(checkKey, new TreeMap());
                                  // no entry, create an empty TreeMap
          ((TreeMap) checkList.get(checkKey)).put(fileEntry.getPath(),
            fileEntry);           // put file object by checksum then by name
        }

        /* The variable <checkList> now contains a sorted list first by MD5
        checksum, then by file name.  Go through this list looking for more
        than one file with the same checksum.  It is safe to call keySet() and
        iterator() even if we were cancelled above. */

        checkKeySet = checkList.keySet(); // get sorted list of checksums
        checkIterate = checkKeySet.iterator(); // get iterator for those keys
        while ((!cancelFlag) && checkIterate.hasNext()) // any more checksums?
        {
          checkKey = (String) checkIterate.next(); // get one checksum
          checkEntry = (TreeMap) checkList.get(checkKey);
                                  // get mapping of files indexed by file name
          if (checkEntry.size() > 1) // more than one file for this checksum?
          {
            matchCount += (checkEntry.size() - 1); // first copy is *not* a
                                  // ... duplicate, but all other copies are
            spaceWasted += (checkEntry.size() - 1) * sizeKey.longValue();
                                  // add second or more files to wasted space
            putOutput("");        // blank line
            putOutput(formatComma.format(checkEntry.size()) + " files size "
              + formatComma.format(sizeKey.longValue()) + " checksum "
              + checkKey + ":");
            fileValues = checkEntry.values();
                                  // get list of files sorted by name
            fileIterate = fileValues.iterator();
                                  // get iterator for those values
            while ((!cancelFlag) && fileIterate.hasNext()) // any more files?
            {
              fileEntry = (File) fileIterate.next(); // get next file object
              putOutput("   " + fileEntry.getPath()); // print full file name
            }

            /* Ultimately, we only use the Java File objects to print the file
            names, which we could have done earlier from the keys that we used
            to index the File objects!  However, this code was written with the
            possibility of doing something more. */

          } // end of more than one file for this checksum
        }
      } // end of more than one file for this file size
    }

    /* Print a summary and scroll the output, even if we were cancelled. */

    if ((consoleFlag == false) || (matchCount > 0)) // might need a spacer
      putOutput("");              // blank line in output text, don't scroll

    if ((cancelFlag == false) && (checkCount > 0))
      putError("Calculated checksums for " + prettyPlural(checkCount, "file")
        + " with total size " + prettyPlural(checkSize, "byte") + ".");

    putOutput(("Found " + prettyPlural(fileCount, "file") + " and "
      + prettyPlural(folderCount, "folder") + " with "
      + prettyPlural(matchCount, "possible duplicate") + "."), true); // scroll

    if ((cancelFlag == false) && (spaceWasted > 0))
      putOutput(("Deleting duplicates would save "
        + prettyPlural(spaceWasted, "byte") + " of disk space."), true);

  } // end of processFileData() method


/*
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  Search all files.  Get folder contents and process each file
  found, doing subfolders only if the <recurseFlag> is true.
*/
  static void processFileOrFolder(File givenFile)
  {
    File[] contents;              // contents if <givenFile> is a folder
    int i;                        // index variable
    File next;                    // next File object from <contents>

    if (cancelFlag) return;       // stop if user hit the panic button
    if (debugFlag)                // does user want debug information?
      putError("processFileOrFolder called, " + givenFile.getPath());

    /* Decide what kind of File object this is, or if it's even real!  The code
    when we find a subfolder mimics the overall structure of this method, with
    the exception of hidden files or folders.  We always process files given to
    us by the user, whether hidden or not. */

    if (givenFile.isDirectory())  // is this "file" actually a folder?
    {
      folderCount ++;             // found one more folder, contents unknown
      putError("Searching folder " + givenFile.getPath());
//    setStatusMessage("Searching folder " + givenFile.getPath());
      contents = sortFileList(givenFile.listFiles()); // no filter, but sorted
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
        {
          if (debugFlag)          // does user want debug information?
            putError("processFileOrFolder ignoring hidden, " + next.getPath());
        }
        else if (next.isDirectory()) // a subfolder inside caller's folder?
        {
          if (recurseFlag)        // do subfolders only if option selected
            processFileOrFolder(next); // call ourself to handle subfolders
          else if (debugFlag)     // does user want debug information?
            putError("processFileOrFolder ignoring subfolder, "
              + next.getPath());
        }
        else if (next.isFile())   // we do want to look at normal files
        {
          processUnknownFile(next); // figure out what to do with this file
        }
        else if (debugFlag)       // does user want debug information?
        {
          putError("processFileOrFolder bad directory entry, "
            + next.getPath());
        }
      }
    }
    else if (givenFile.isFile())  // we do want to look at normal files
    {
      processUnknownFile(givenFile); // figure out what to do with this file
    }
    else                          // user gave bad file or folder name
    {
      putError("Not a file or folder: " + givenFile.getPath());
    }
  } // end of processFileOrFolder() method


/*
  processUnknownFile() method

  The caller gives us a Java File object that is known to be a file, not a
  directory.
*/
  static void processUnknownFile(File givenFile)
  {
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    Long sizeKey;                 // file size converted to an object

    /* Go through some trouble to get the exact file name.  Java's canonical
    file names can be slower than abstract file names, but recognize aliases
    for the same file. */

    if (cancelFlag) return;       // stop if user hit the panic button
    fileCount ++;                 // found one more file, contents unknown

    filePath = null;              // assume we will use the abstract path name
    if (aliasFlag)                // do we detect aliases and symbolic links?
    {
      try { filePath = givenFile.getCanonicalPath(); } // full name resolution
      catch (IOException ioe) { filePath = null; } // or else invalidate result
    }
    if (filePath == null)         // do we have a fully-resolved file name?
      filePath = givenFile.getPath(); // accept abstract file name (no errors)

    fileSize = givenFile.length(); // get size of caller's file in bytes
    if (debugFlag)                // does user want debug information?
      putError("processUnknownFile called, " + filePath + " size "
        + formatComma.format(fileSize));
    if (consoleFlag == false)     // only format this message if running as GUI
      setStatusMessage("Scanning file " + filePath);

    if (zeroFlag || (fileSize > 0)) // should we ignore empty files?
    {
      /* Index the Java File object first in a list by size, then in a sublist
      by file name including the path.  A TreeMap is better for a sublist here
      than a Vector because it maintains a sorted order, and it keeps only the
      most recent value for any index key.  That safely ignores mistakes where
      the user gives us the same folder or subfolder twice. */

      sizeKey = new Long(fileSize); // convert file size to an object
      if (sizeList.containsKey(sizeKey) == false)
                                // is there an entry for this file size?
        sizeList.put(sizeKey, new TreeMap());
                                // no entry, create an empty TreeMap
      ((TreeMap) sizeList.get(sizeKey)).put(filePath, givenFile);
                                // put file object by size then by name
    }
  } // end of processUnknownFile() method


/*
  putError() method

  Similar to putOutput() except write on standard error if running as a console
  application.  See putOutput() for details.  This method is more for tracing
  execution than for generating a report.
*/
  static void putError(String text)
  {
    if (consoleFlag)              // are we running as a console application?
      System.err.println(text);   // console output goes onto standard error
    else
    {
      errorText.append(text + "\n"); // graphical output goes into text area
      errorText.select(999999999, 999999999); // force scroll to end of text
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
    putOutput(text, false);       // by default, do not scroll output lines
  }

  static void putOutput(String text, boolean scroll)
  {
    if (consoleFlag)              // are we running as a console application?
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      if (scroll)                 // does caller want us to scroll?
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
    System.err.println("  java  FindDupFiles3  [options]  file or folder names");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -a0 = don't detect aliases or symbolic links (faster)");
    System.err.println("  -a1 = -a = recognize aliases for the same file (default)");
    System.err.println("  -d = show debug information (may be verbose)");
    System.err.println("  -h0 = ignore hidden files or folders except given by user");
    System.err.println("  -h1 = -h = process hidden files and folders (default)");
    System.err.println("  -s0 = do only given files or folders, no subfolders");
    System.err.println("  -s1 = -s = process files, folders, and subfolders (default)");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println("  -z0 = don't show zero-byte empty files (default)");
    System.err.println("  -z1 = -z = include zero-byte empty files in report");
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
  buttons, in the context of the main FindDupFiles3 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
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
      errorText.setFont(new Font(fontName, Font.PLAIN, fontSize));
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      errorText.setFont(new Font(fontName, Font.PLAIN, fontSize));
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == openButton) // "Open" button for files or folders
    {
      doOpenButton();             // open files or folders for processing
    }
    else if (source == recurseCheckbox) // recursion for folders, subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == statusTimer) // update timer for status message text
    {
      if (statusPending.equals(statusDialog.getText()) == false)
        statusDialog.setText(statusPending); // new message, update the display
    }
    else if (source == zeroCheckbox) // report zero-byte empty files
    {
      zeroFlag = zeroCheckbox.isSelected();
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of FindDupFiles3 class

// ------------------------------------------------------------------------- //

/*
  FindDupFiles3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FindDupFiles3User implements ActionListener, Runnable
{
  /* empty constructor */

  public FindDupFiles3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FindDupFiles3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FindDupFiles3.doOpenRunner();
  }

} // end of FindDupFiles3User class

/* Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License. */
