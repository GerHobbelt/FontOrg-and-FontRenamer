/*
  Font Rename #4 - Change File Names for OpenType and TrueType Fonts
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Thursday, 12 February 2009
  Java class name: FontRename4
  Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 application to rename OpenType and TrueType font files
  with their internal "font full name" using only plain text characters (ASCII)
  plus an extension for the type (*.OTF, *.TTC, *.TTF), or using the full
  Unicode character set.  This gives consistent names to font files, no matter
  what their source.  The contents of the files are not changed, only the names
  in the system file directory.  Don't use this program on system folders with
  installed fonts.

  To display all name tables inside a font, in all languages, see the FontNames
  utility.  For more information on the internal format of font files, start
  with the following on-line references:

      Microsoft TrueType Font Properties Extension
        http://www.microsoft.com/typography/TrueTypeProperty21.mspx

      The OpenType Font File
        http://www.microsoft.com/typography/otspec/otff.htm
        http://www.microsoft.com/typography/otspec/name.htm

      The TrueType Font File
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6.html
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6name.html

  The older PostScript font format is not supported.  FontRename duplicates
  some features already found in:

      TrueType Renamer 1.00
      by Fabrizio Giustina (1998)
      http://www.simtel.net/pub/pd/5496.html

      RedEar's FontRenamer 2.1.6
      by Philip L. Engel (2008)
      http://fontorg.us/download.html

  RedEar's program is more general, with better features, and fully supports
  multi-file PostScript fonts; however, it requires the Microsoft .NET
  framework and runs only on Windows.  This FontRename Java application will
  run on any computer with Java installed: Linux, Macintosh, Solaris, Windows,
  etc.

  GNU General Public License (GPL)
  --------------------------------
  FontRename4 is free software: you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the Free Software
  Foundation, either version 3 of the License or (at your option) any later
  version.  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
  more details.

  You should have received a copy of the GNU General Public License along with
  this program.  If not, see the http://www.gnu.org/licenses/ web page.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or file and folder names.  If no
  file or folder names are given on the command line, then this program runs as
  a graphical or "GUI" application with the usual dialog boxes and windows.
  See the "-?" option for a help summary:

      java  FontRename4  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is::

      java  FontRename4  -s  d:\fonts  >report.txt

  The console application will return an exit status equal to the number of
  files whose names have been successfully changed.  The graphical interface
  can be very slow when the output text area gets too big, which will happen if
  thousands of files are reported.

  Restrictions and Limitations
  ----------------------------
  A plain text file called "FontRename4.txt" is expected to be in the current
  working directory and contains the preferred search order for name table
  entries in font files.  You may edit this file to select other languages, or
  to convert characters before files are renamed.  Please read comments in the
  file for further instructions.

  Not all font files are correctly structured.  Before reporting an error in
  this program, make sure that the error isn't in the font file.  Select the
  highest message level for more detailed information about a particular file
  (the -m4 option on the command line, or the graphical "show all files,
  details" option).  The MacRoman conversion table may be optional on some JRE;
  you should install the complete JRE, not the default settings.

  Suggestions for New Features
  ----------------------------
  (1) Allow more than one character to be replaced by each "convert" command.
      The 2-character Hiragana sequence "Ryo" in Japanese would otherwise
      appear as "RiYo" when converted one character at a time to plain text.
      See the PlainText Java application for similar code.  KF, 2008-06-20.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import java.util.zip.*;           // CRC32 checksums
import javax.swing.*;             // newer Java GUI support

public class FontRename4
{
  /* constants */

  static final long BIG_FILE_SIZE = 5 * 1024 * 1024; // "big" means over 5 MB
  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License.";
  static final int[][] DEFAULT_ACCEPT = { // default name table entries
    {3, 1, 1033, 4},              // Windows Unicode English
    {3, 0, 1033, 4},              // Windows Symbol English
    {1, 0, 0, 4}};                // Macintosh Roman English
  static final String[] DEFAULT_CHARSET = { // must match <DEFAULT_ACCEPT>
    "US-ASCII", "US-ASCII", "US-ASCII"};
  static final String DEFAULT_FILE = "FontRename4.txt"; // configuration data
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final long INT_MASK = 0x00000000FFFFFFFFL;
                                  // logical mask for one int as long value
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Change File Names for OpenType and TrueType Fonts - by: Keith Fenske";
  static final char REPLACE_CHAR = '-'; // replace illegal file name characters
  static final String[] REPORT_CHOICES = {"only summary, errors",
    "successful changes", "failures to change", "all files, summary",
    "all files, details"};        // descriptions for <reportIndex> values
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates
  static final String UNICODE_BMP = "UTF-16"; // Basic Multilingual Plane

  /* class variables */

  static String[] acceptCharset;  // character set names for <acceptEntries>
  static int[][] acceptEntries;   // preferred order for name table entries
  static boolean allTypesFlag;    // true if we check all file name extensions
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static int changeCount;         // number of file names changed
  static boolean checksumFlag;    // true if we append CRC32 checksums to names
  static boolean consoleFlag;     // true if running as a console application
  static TreeMap convertMap;      // conversion map for file name characters
  static String dataFile;         // text file with configuration data
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static int fileCount;           // number of files found (any file type)
  static int folderCount;         // number of folders found
  static javax.swing.filechooser.FileFilter fontFilter;
                                  // our shared file filter for fonts
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static String forcedName;       // use this name plus checksum, if not null
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static NumberFormat formatPointOne; // formats with one decimal digit
  static String genericName;      // use this plus checksum, if name not found
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report if running as GUI
  static boolean readonlyFlag;    // true if we try to rename read-only files
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JComboBox reportDialog;  // graphical option for <reportIndex>
  static int reportIndex;         // user's selection from <REPORT_CHOICES>
  static JButton saveButton;      // "Save" button for writing output text
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message

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

    acceptCharset = DEFAULT_CHARSET; // default character set names
    acceptEntries = DEFAULT_ACCEPT; // default name table entries
    allTypesFlag = false;         // by default, check only known file types
    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    changeCount = fileCount = folderCount = 0; // no files or folders found yet
    checksumFlag = false;         // by default, don't append checksum to names
    consoleFlag = false;          // assume no files or folders on command line
    convertMap = new TreeMap();   // empty mapping for file name characters
    dataFile = DEFAULT_FILE;      // default file name for configuration data
    fontFilter = new FontRename4Filter(); // create our shared file filter
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    forcedName = null;            // by default, use internal names from file
    genericName = null;           // by default, do nothing if name not found
    hiddenFlag = false;           // by default, don't process hidden files
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    readonlyFlag = false;         // by default, don't rename read-only files
    recurseFlag = false;          // by default, don't process subfolders
    reportIndex = 1;              // by default, show only successful changes
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatPointOne = NumberFormat.getInstance(); // current locale
    formatPointOne.setGroupingUsed(true); // use commas or digit groups
    formatPointOne.setMaximumFractionDigits(1); // force one decimal digit
    formatPointOne.setMinimumFractionDigits(1);

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
        allTypesFlag = true;      // check all file types, ignore extensions
      }
      else if (word.equals("-a0") || (mswinFlag && word.equals("/a0")))
        allTypesFlag = false;     // check only known file types (extensions)

      else if (word.equals("-c") || (mswinFlag && word.equals("/c"))
        || word.equals("-c1") || (mswinFlag && word.equals("/c1")))
      {
        checksumFlag = true;      // append CRC32 checksum to file names
      }
      else if (word.equals("-c0") || (mswinFlag && word.equals("/c0")))
        checksumFlag = false;     // don't append checksum to file names

      else if (word.startsWith("-d") || (mswinFlag && word.startsWith("/d")))
        dataFile = args[i].substring(2); // accept anything for data file name

      else if (word.startsWith("-f") || (mswinFlag && word.startsWith("/f")))
      {
        /* The user wants to rename all files with a given string followed by
        the calculated checksum.  This is sometimes good for East Asian fonts
        when it is known in advance that some or most won't have English name
        tables.  The name string is usually the name of the font company.  We
        check only that the file name string is not empty, under the assumption
        that the user knows better than we do about what he/she wants. */

        forcedName = collapseString(args[i].substring(2)); // strip, clean up
        if (forcedName.length() == 0) // is there anything left from string?
        {
          System.err.println("Empty forced name: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.startsWith("-g") || (mswinFlag && word.startsWith("/g")))
      {
        /* If a valid font name is not found, the user wants to rename files
        with a given string followed by the calculated checksum.  Most often,
        the string is "Unicode" for fonts that simply have no usable English
        name.  Compare this with the more brutal -f option. */

        genericName = collapseString(args[i].substring(2)); // strip, clean up
        if (genericName.length() == 0) // is there anything left from string?
        {
          System.err.println("Empty generic name: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
      {
        hiddenFlag = true;        // process hidden files and folders
      }
      else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
        hiddenFlag = false;       // ignore hidden files or subfolders

      else if (word.startsWith("-m") || (mswinFlag && word.startsWith("/m")))
      {
        /* This option is followed by an index into <REPORT_CHOICES> for the
        message level.  We don't assign any meaning to the index here. */

        try                       // try to parse remainder as unsigned integer
        {
          reportIndex = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          reportIndex = -1;       // set result to an illegal value
        }
        if ((reportIndex < 0) || (reportIndex >= REPORT_CHOICES.length))
        {
          System.err.println("Message option must be from -m0 to -m"
            + (REPORT_CHOICES.length - 1) + " not: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-r") || (mswinFlag && word.equals("/r"))
        || word.equals("-r1") || (mswinFlag && word.equals("/r1")))
      {
        readonlyFlag = true;      // rename read-only files if permitted
      }
      else if (word.equals("-r0") || (mswinFlag && word.equals("/r0")))
        readonlyFlag = false;     // don't try to rename read-only files

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

        if (consoleFlag == false) // is this the first file or folder?
        {
          consoleFlag = true;     // don't allow GUI methods to be called
          loadConfig();           // load configuration data file if available
        }
        processFileOrFolder(new File(args[i]));
      }
    }

    /* If running as a console application, print a summary of what we found
    and/or changed.  Exit to the system with an integer status that has the
    number of files whose names were successfully changed. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      printAlways("Found " + prettyPlural(fileCount, "file") + " and "
        + prettyPlural(folderCount, "folder") + " with "
        + prettyPlural(changeCount, "change") + ".");
      System.exit(changeCount);   // exit from application with status
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

    action = new FontRename4User(); // create our shared action listener
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

    recurseCheckbox = new JCheckBox("subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(recurseCheckbox);

    panel3.add(Box.createHorizontalStrut(20));

    reportDialog = new JComboBox(REPORT_CHOICES);
    reportDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) reportDialog.setFont(buttonFont);
    reportDialog.setSelectedIndex(reportIndex); // select default level
    reportDialog.setToolTipText("Select level of detail for messages.");
    reportDialog.addActionListener(action); // do last so don't fire early
    panel3.add(reportDialog);

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
        "Rename OpenType and TrueType font files with their internal"
      + "\n\"font full name\" using only plain text characters (ASCII) plus"
      + "\nan extension for the type (*.OTF, *.TTC, *.TTF), or using the"
      + "\nfull Unicode character set.  This gives consistent names to font"
      + "\nfiles, no matter what their source.  The contents of the files"
      + "\nare not changed, only the names in the system file directory."
      + "\nDon't use this program on system folders with installed fonts."
      + "\n\nChoose your options; then open files or folders to search.\n\n"
      + COPYRIGHT_NOTICE + "\n");

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

    loadConfig();                 // always load configuration data file here

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  calculateChecksum() method

  Given a File object, return the CRC32 checksum for that file as a hexadecimal
  string.  If the checksum can not be calculated, then the string "CRC32BAD" is
  returned instead.

  Reading the input one byte at a time is a very slow way to calculate the
  checksum: about one second per megabyte on an Intel Pentium 4 at 3 GHz.
  Reading the input in a large byte buffer, and passing this buffer to the
  message digest, is over 30 times faster.
*/
  static String calculateChecksum(File givenFile)
  {
    byte[] buffer;                // input buffer for reading file
    CRC32 crcDigest;              // object for calculating CRC32 checksum
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    int i;                        // index variable
    FileInputStream inStream;     // input file stream
    String result;                // our result (the checksum as a string)
    long sizeDone;                // how much of <fileSize> has been finished
    String sizeText;              // pre-formatted portion of size message
    long sizeUser;                // last <sizeDone> reported to user

    /* It is tempting to return "00000000" here for files whose size is zero,
    but that might bypass legitimate I/O error messages (no such file, etc). */

    filePath = givenFile.getPath(); // get name of caller's file, with path
    fileSize = givenFile.length(); // get size of caller's file in bytes
    result = "CRC32BAD";          // default checksum used for failure
    sizeDone = sizeUser = 0;      // we haven't read anything yet
    sizeText = null;              // don't format big size message until needed

    if (consoleFlag == false)     // only format this message if running as GUI
      setStatusMessage("Checksum " + formatComma.format(fileSize)
        + " bytes for " + filePath);

    try
    {
      buffer = new byte[BUFFER_SIZE]; // allocate bigger, faster input buffer
      crcDigest = new CRC32();    // allocate, initialize CRC32 checksum object
      inStream = new FileInputStream(givenFile); // open file for reading bytes
      while ((i = inStream.read(buffer, 0, BUFFER_SIZE)) > 0)
      {
        /* The user may cancel our processing if this is a very big file.  We
        must always return a String result, even when things go wrong. */

        if (cancelFlag)           // stop if user hit the panic button
        {
          inStream.close();       // try to close input file early
          printDebug(filePath + " - CRC32 calculation cancelled by user");
          return(result);         // give caller default checksum
        }

        /* Update the checksum calculation with the new data. */

        crcDigest.update(buffer, 0, i); // update checksum with input bytes
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
      result = "00000000" + Long.toHexString(crcDigest.getValue());
      result = result.substring(result.length() - 8); // force 8 hex digits
    }
    catch (IOException ioe)       // file may be locked, invalid, etc
    {
      printDebug(filePath + " - file I/O error: " + ioe.getMessage());
      return(result);             // give caller default checksum
    }

    printDebug(filePath + " - size " + formatComma.format(fileSize)
      + " checksum " + result);
    return(result);               // return calculated CRC32 checksum to caller

  } // end of calculateChecksum() method


/*
  collapseString() method

  Remove leading, trailing, and repeated spaces from a string.  There will be
  exactly one space between any two "words" in the result.
*/
  static String collapseString(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from caller's string
    int i;                        // index variable
    int length;                   // size of caller's string in characters
    boolean pending;              // white space found but not yet inserted
    boolean skippy;               // true while skipping space at beginning

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get size of caller's string in characters
    pending = false;              // we are not waiting to insert a space
    skippy = true;                // ignore all space at beginning of string
    for (i = 0; i < length; i ++) // do all characters in caller's string
    {
      ch = text.charAt(i);        // get one character from caller's string
//    if (Character.isWhitespace(ch)) // general search for any Unicode spaces
      if (ch == ' ')              // be very specific about what a space is
        pending = (skippy == false); // yes, remember if not at beginning
      else                        // not a space, must copy printable character
      {
        if (pending)              // are we waiting to insert a space?
          buffer.append(' ');     // yes, insert delayed standard space now
        buffer.append(ch);        // append printable character to the result
        pending = false;          // don't insert the same space again
        skippy = false;           // stop ignoring spaces once something found
      }
    }
    return(buffer.toString());    // give caller our converted string

  } // end of collapseString() method


/*
  convertFilename() method

  The caller gives us a string to be used as a file name.  We delete or replace
  characters that we don't like, or that the user wants translated (converted).
*/
  static String convertFilename(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters
    String text;                  // caller's input after some manipulation

    /* Convert (translate) characters, according to user's configuration. */

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = input.length();      // get size of input string in characters
    for (i = 0; i < length; i ++)
    {
      ch = input.charAt(i);       // get one character from input string
      text = (String) convertMap.get(new Integer(ch));
      if (text == null)           // is there a translation for this character?
        buffer.append(ch);        // no, use the original character
      else
        buffer.append(text);      // yes, use the character's translated string
    }
    text = buffer.toString();     // convert buffer back to normal string

    /* Remove or replace characters that are not allowed in file names.  Expand
    this list to include all prohibited characters in all operating systems. */

    buffer.setLength(0);          // empty string buffer for another search
    length = text.length();       // get size of converted string in characters
    for (i = 0; i < length; i ++)
    {
      ch = text.charAt(i);        // get one character from converted string
      if (Character.isISOControl(ch) || (ch == '\"') || (ch == '*')
        || (ch == '/') || (ch == ':') || (ch == '<') || (ch == '>')
        || (ch == '?') || (ch == '\\') || (ch == '|'))
      {
        buffer.append(REPLACE_CHAR); // replace illegal file name characters
      }
      else
        buffer.append(ch);        // accept everything else without change
    }

    return(buffer.toString());    // give caller our converted string

  } // end of convertFilename() method


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
    printAlways("Cancelled by user.", true); // print message and scroll
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
    fileChooser.setFileFilter(allTypesFlag ? fileChooser
      .getAcceptAllFileFilter() : fontFilter); // choose default file filter
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
    changeCount = fileCount = folderCount = 0; // no files or folders found yet
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    setStatusMessage(EMPTY_STATUS); // clear status message at bottom of window
    statusTimer.start();          // start updating the status message

    openFilesThread = new Thread(new FontRename4User(), "doOpenRunner");
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
      processFileOrFolder(openFileList[i]); // process this file or folder
    }

    /* Print a summary and scroll the output, even if we were cancelled. */

    printAlways(("Found " + prettyPlural(fileCount, "file") + " and "
      + prettyPlural(folderCount, "folder") + " with "
      + prettyPlural(changeCount, "change") + "."), true);

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
      printAlways(("Can't write to text file: " + ioe.getMessage()), true);
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
  loadConfig() method

  Load configuration data from a text file in the current working directory,
  which is usually the same folder as the program's *.class files.  Should we
  encounter an error, then print a message, but continue normal execution.
  None of the file data is critical to the operation of this program.
*/
  static void loadConfig()
  {
    Pattern acceptPattern;        // compiled regular expression
    Vector acceptVector;          // variable-sized for name table entries
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input line
    Vector charsetVector;         // variable-sized for character set names
    Pattern convertPattern;       // compiled regular expression
    int i;                        // index variable
    BufferedReader inputFile;     // input character stream from text file
    int length;                   // size of a string in characters
    Matcher matcher;              // pattern matcher for regular expression
    String text;                  // one input line from file, or otherwise
    String word;                  // first command word on input line

    acceptPattern = Pattern.compile(
      "\\s*(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)(?:\\s+([-.:_0-9A-Za-z]+))?(?:\\s+#.*)?\\s*");
    acceptVector = new Vector();  // empty vector for name table entries
    buffer = new StringBuffer();  // re-use same string buffer for each line
    charsetVector = new Vector(); // empty vector for character set names
    convertMap = new TreeMap();   // empty mapping for file name characters
    convertPattern = Pattern.compile(
      "\\s*[Uu]\\+([0-9A-Fa-f]+)\\s*=\\s*\"([^\"]*)\"(?:\\s+#.*)?\\s*");

    /* Open and read lines from the configuration data file. */

    try                           // catch specific and general I/O errors
    {
      inputFile = new BufferedReader(new InputStreamReader(new
        FileInputStream(dataFile), "UTF-8")); // UTF-8 encoded text file
      inputFile.mark(4);          // we may need to back up a few bytes
      i = inputFile.read();       // read byte-order marker if present
      if ((i >= 0) && (i != '\uFEFF') && (i != '\uFFFE')) // skip BOM or EOF?
        inputFile.reset();        // no, regular text, go back to beginning

      while ((text = inputFile.readLine()) != null)
      {
        /* Find the first word on the input line, which determines whether this
        is a command or a comment. */

        buffer.setLength(0);      // empty string buffer for command word
        i = 0;                    // start from beginning of input line
        length = text.length();   // number of characters to consider
        while ((i < length) && Character.isWhitespace(text.charAt(i)))
          i ++;                   // ignore leading white space (blanks, tabs)
        while ((i < length) && Character.isLetterOrDigit(ch = text.charAt(i)))
        {
          buffer.append(Character.toLowerCase(ch)); // accumulate first word
          i ++;                   // proceed to next input character
        }
        word = buffer.toString(); // convert buffer back to normal string
        if (word.equals("accept"))
        {
          /* Input line should be the numbers for a name table entry, followed
          by an optional character set name. */

          matcher = acceptPattern.matcher(text.substring(i)); // parse syntax
          if (matcher.matches())  // only if the entire substring matches
          {
            int[] array = new int[4]; // allocate space for name table entries
            for (i = 0; i < 4; i ++) // convert from characters to binary
            {
              try { array[i] = Integer.parseInt(matcher.group(i + 1)); }
              catch (NumberFormatException nfe) { array[i] = -999999999; }
            }

            String charset = matcher.group(5); // optional character set name
            if ((charset != null) && (charset.length() == 0))
              charset = null;     // only want <null> if string is empty

            if ((array[0] >= 0) && (array[0] <= 0xFFFF) // platform ID
              && (array[1] >= -1) && (array[1] <= 0xFFFF) // encoding ID
              && (array[2] >= -1) && (array[2] <= 0xFFFF) // language ID
              && (array[3] >= 0) && (array[3] <= 0xFFFF) // name ID
              && ((charset == null) // no character set, or if supported name
                || java.nio.charset.Charset.isSupported(charset)))
            {
              acceptVector.add(array); // good enough for a name table entry
              charsetVector.add(charset); // and character set name if given
            }
            else
              System.err.println("Invalid name table accept value: " + text);
          }
          else
            System.err.println("Invalid name table accept syntax: " + text);
        }
        else if (word.equals("convert"))
        {
          /* Input line should be the conversion of a single Unicode character
          to an arbitrary Unicode string.  The syntax does not allow for double
          quotes (") inside the string, which is unimportant, since quotes are
          illegal in file names on Windows anyway. */

          matcher = convertPattern.matcher(text.substring(i)); // parse syntax
          if (matcher.matches())  // only if the entire substring matches
          {
            try { i = Integer.parseInt(matcher.group(1), 16); } // parse U+ hex
            catch (NumberFormatException nfe) { i = -999999999; } // invalidate
            if ((i >= Character.MIN_VALUE) && (i <= Character.MAX_VALUE))
            {
              Integer key = new Integer(i); // convert U+ number to an object
              if (convertMap.containsKey(key))
                System.err.println("Duplicate character conversion: " + text);
              else
                convertMap.put(key, matcher.group(2)); // save substitution
            }
            else                  // character number can't be Unicode
              System.err.println("Invalid character convert value: " + text);
          }
          else
            System.err.println("Invalid character convert syntax: " + text);
        }
        else if (word.length() > 0)
          System.err.println("Unknown configuration command: " + text);
        else if ((i < length) && (text.charAt(i) != '#'))
          System.err.println("Invalid configuration comment: " + text);
      }
      inputFile.close();          // try to close input file
    }

    catch (FileNotFoundException fnfe) // if our data file does not exist
    {
      /* Put special code here if you want to ignore the missing file. */

      if (dataFile.equals(DEFAULT_FILE) == false)
      {
        System.err.println("Configuration data file not found: " + dataFile);
        System.err.println("in current working directory "
          + System.getProperty("user.dir"));
      }
    }

    catch (IOException ioe)       // for all other file I/O errors
    {
      System.err.println("Unable to read configuration data from file "
        + dataFile);
      System.err.println("in current working directory "
        + System.getProperty("user.dir"));
      System.err.println(ioe.getMessage());
    }

    /* Use our default name table entries if the configuration file was not
    found, or if no entries were specified in the file. */

    if (acceptVector.size() == 0) // did the user specify any name tables?
    {
      acceptCharset = DEFAULT_CHARSET; // no, use default character set names
      acceptEntries = DEFAULT_ACCEPT; // ... and default name table entries
    }
    else                          // yes, convert vector into fixed array
    {
      length = acceptVector.size(); // same size for both arrays
      acceptCharset = new String[length]; // allocate smaller, faster arrays
      acceptEntries = new int[length][]; // because vectors are slower to use
      for (i = 0; i < length; i ++) // copy each entry from vector to array
      {
        acceptCharset[i] = (String) charsetVector.get(i); // charset or <null>
        acceptEntries[i] = (int[]) acceptVector.get(i); // name table entries
      }
    }
  } // end of loadConfig() method


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
  printAlways() method

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
  static void printAlways(String text)
  {
    printAlways(text, false);     // by default, don't scroll output text area
  }

  static void printAlways(String text, boolean scroll)
  {
    if (consoleFlag)              // are we running as a console application?
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      if (scroll)                 // does caller want us to scroll?
        outputText.select(999999999, 999999999); // force scroll to end of text
    }
  } // end of printAlways() method


/*
  printChange(), printDebug(), printFailure(), printSummary() methods

  Print a complete line of output depending upon the user's choice for the
  message level.
*/
  static void printChange(String text) // successfully changed file name
  {
    if ((reportIndex == 1) || (reportIndex > 2))
      printAlways(text);
  }

  static void printFailure(String text) // failed to change file name
  {
    if (reportIndex >= 2)
      printAlways(text);
  }

  static void printSummary(String text) // final status of each file or folder
  {
    if (reportIndex >= 3)
      printAlways(text);
  }

  static void printDebug(String text) // detailed information for debugging
  {
    if ((cancelFlag == false) && (reportIndex >= 4))
      printAlways(text);
  }


/*
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  Check file extensions (file types) and try to process as
  OpenType or TrueType fonts.  Get folder contents and process each file found,
  doing subfolders only if the <recurseFlag> is true.
*/
  static void processFileOrFolder(File givenFile)
  {
    File[] contents;              // contents if <givenFile> is a folder
    int i;                        // index variable
    File next;                    // next File object from <contents>

    if (cancelFlag) return;       // stop if user hit the panic button

    /* Despite warnings to the contrary, users still try to use this program to
    rename installed fonts in system folders.  We are a little over-protective
    here and may occasionally reject files/folders that could be changed.  This
    only catches fully-specified path names from the GUI "file open" dialog
    box; it won't catch path names relative to the current working directory in
    a command shell, when that directory begins inside a system folder. */

    String lowpath = givenFile.getPath().toLowerCase(); // for comparison only
    if (lowpath.startsWith("/system") // Apple Macintosh
      || (lowpath.indexOf("/library/fonts") >= 0) // Apple Macintosh
      || (lowpath.indexOf(":\\windows") > 0) // Windows 98/ME/XP or later
      || (lowpath.indexOf(":\\winnt") > 0)) // Windows NT4/2000
    {
      printAlways(givenFile.getPath() + " - not safe with system folders");
      return;                     // refuse to proceed if this may be unsafe
    }

    /* Decide what kind of File object this is, or if it's even real!  The code
    when we find a subfolder mimics the overall structure of this method. */

    if (givenFile.isDirectory())  // is this "file" actually a folder?
    {
      folderCount ++;             // found one more folder, contents unknown
      setStatusMessage("Searching folder " + givenFile.getPath());
      contents = sortFileList(givenFile.listFiles()); // no filter, but sorted
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
        {
          printSummary(next.getPath() + " - ignoring hidden file or folder");
        }
        else if (next.isDirectory()) // a subfolder inside caller's folder?
        {
          if (recurseFlag)        // do subfolders only if option selected
          {
            printSummary(next.getPath() + " - searching subfolder");
            processFileOrFolder(next); // call ourself to handle subfolders
          }
          else
            printSummary(next.getPath() + " - ignoring subfolder");
        }
        else if (next.isFile())   // we do want to look at normal files
        {
          processUnknownFile(next, allTypesFlag); // do file only if font type
        }
        else                      // file directory has an invalid entry
        {
          printSummary(next.getPath() + " - not a file or folder");
        }
      }
    }
    else if (givenFile.isFile())  // we do want to look at normal files
    {
      processUnknownFile(givenFile, true); // always open files given by user
    }
    else                          // user gave bad file or folder name
    {
      printAlways(givenFile.getPath() + " - not a file or folder");
    }
  } // end of processFileOrFolder() method


/*
  processTrueType() method

  The caller gives us a Java File object that should be an OpenType (*.OTF) or
  TrueType (*.TTC, *.TTF) font file.  The file header contains some information
  that we use; most of what we want is in the "name" table.

  For a list of the possible character set names, as supported by Java 5.0
  (1.5), see the following web page:

      http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html

  While this program is being developed on Java 1.4, the newer version has a
  better web page, and it's actually expected that this program will be run on
  Java 1.5 or newer.
*/
  static void processTrueType(
    File givenFile,               // file that we are to open, read, and rename
    String oldName)               // must be *exact* current file name, no path
  {
    int acceptIndex;              // current name table entry in "accept" list
    int fileIndex;                // current name table entry from font file
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    String fileType;              // only the part after the last period (dot)
    int i;                        // index variable
    boolean localChecksum;        // local (modified) copy of <checksumFlag>
    String nameFound;             // our best choice for a name string
    long nameTable;               // location of "name" table in file
    File newFile;                 // used to construct new file name(s)
    int numNames;                 // number of entries in "name" table
    int numTables;                // number of entries in table directory
    long offsetTable;             // location of (first) Offset Table in file
    RandomAccessFile ramFile;     // file stream for reading font file
    long signature;               // signature bytes from beginning of file
    long stringOffset;            // start of strings, relative to <nameTable>

    /* Initialize some local variables.  Many others are initialized or even
    declared at the beginning of sections where they are used. */

    filePath = givenFile.getPath(); // get name of caller's file, with path
    fileSize = givenFile.length(); // get size of caller's file in bytes
    localChecksum = checksumFlag; // default to using global flag for checksums

    /* Print a debugging trace if the user wants detailed information. */

    if (cancelFlag) return;       // stop if user hit the panic button
    printDebug(filePath + " - processTrueType called, oldName = <" + oldName
      + ">");

    /* Open the file, index through the tables, and extract possible names. */

    try                           // catch file I/O errors
    {
      ramFile = new RandomAccessFile(givenFile, "r"); // open file for reading

      /* Figure out what type of file this is, if it's even a font file! */

      fileType = null;            // assume that we can't determine file type
      offsetTable = -1;           // assume failure, mark as invalid
      if (fileSize < 16)          // need this many bytes for signature
      {
        printDebug(filePath + " - file too small for OTF/TTC/TTF signature");
      }
      else
      {
        signature = ((long) ramFile.readInt()) & INT_MASK;
                                  // read four signature bytes
        if ((signature == 0x00010000L) // binary version 1.0 for TrueType
          || (signature == 0x74727565L)) // "true" for Macintosh TrueType
        {
          /* This is a TrueType file containing a single font. */

          fileType = "ttf";       // correct extension (type) for file name
          offsetTable = 0;        // conveniently located at start of file
        }
        else if (signature == 0x4F54544FL) // "OTTO" for OpenType
        {
          /* This is an OpenType file containing a single font. */

          fileType = "otf";       // correct extension (type) for file name
          offsetTable = 0;        // conveniently located at start of file
        }
        else if (signature == 0x74746366L) // "ttcf" for TrueType collection
        {
          /* This is a TrueType collection containing at least one font.  Well,
          we assume that there is at least one internal font! */

          fileType = "ttc";       // correct extension (type) for file name
          ramFile.skipBytes(8);   // skip TTC version, number of internal fonts
          offsetTable = ((long) ramFile.readInt()) & INT_MASK;
                                  // location of Offset Table for first font
        }
        else                      // unknown signature, not known font type
        {
          printDebug(filePath
            + " - not recognized as OTF/TTC/TTF font, signature = 0x"
            + Long.toHexString(signature));
        }
      }

      if (offsetTable >= 0)       // did we find the Offset Table?
      {
        printDebug(filePath + " - Offset Table is at 0x"
          + Long.toHexString(offsetTable) + ", file type is <" + fileType
          + ">");
      }

      /* Find the "name" table, if any. */

      nameTable = -1;             // assume failure, mark as invalid
      if (cancelFlag || (forcedName != null))
      {
        /* Do nothing if errors found, or if we already have a forced name. */
      }
      else if (offsetTable >= 0)  // do we have an Offset Table to work with?
      {
        if (fileSize < (offsetTable + 12)) // file big enough for Offset Table?
        {
          printDebug(filePath + " - file too small for Offset Table header");
        }
        else
        {
          ramFile.seek(offsetTable + 4); // location of "number of tables"
          numTables = ramFile.readUnsignedShort(); // get entries in table
          if ((numTables < 1) || (numTables > 299)) // check arbitrary limits
          {
            printDebug(filePath + " - too many entries in Offset Table ("
              + numTables + ")");
          }
          else if (fileSize < (offsetTable + 12 + (16 * numTables)))
          {
            printDebug(filePath
              + " - file too small for Offset Table entries");
          }
          else
          {
            ramFile.skipBytes(6); // skip search ranges
            for (i = 0; i < numTables; i ++)
            {
              if ((((long) ramFile.readInt()) & INT_MASK) == 0x6E616D65L)
                                  // is this entry for the "name" table?
              {
                ramFile.skipBytes(4); // skip checksum
                nameTable = ((long) ramFile.readInt()) & INT_MASK;
                break;            // exit early from <for> loop
              }
              else                // no, continue looking
                ramFile.skipBytes(12); // skip remainder of table entry
            }

            if (nameTable > 0)    // did we find the "name" table?
            {
              printDebug(filePath + " - \"name\" table is at 0x"
                + Long.toHexString(nameTable));
            }
            else
            {
              printDebug(filePath + " - \"name\" table not found");
            }
          }
        }
      }

      /* Look through the "name" table for entries that match our desired
      platform, encoding, and language.  The obvious loop would be to decode
      each name string, and then compare it against our "accept" list.  Most
      name strings are of no interest to us, and since each string is found by
      indexing through the file, the cost per string is fairly high.  It is
      better to decode a name string only after it matches an entry in the
      "accept" list, and to do this decoding only once per string.  (More than
      one "accept" entry may match the same name table entry.)  Another option
      would be to buffer the name table (usually less than a kilobyte) and the
      string table (can be hundreds of kilobytes), and index through those
      buffered copies as byte arrays. */

      nameFound = null;           // assume failure, mark as invalid
      if (cancelFlag || (forcedName != null))
      {
        /* Do nothing if errors found, or if we already have a forced name. */
      }
      else if (nameTable > 0)     // did we find the "name" table?
      {
        if (fileSize < (nameTable + 6)) // file big enough for "name" header?
        {
          printDebug(filePath + " - file too small for \"name\" header");
        }
        else
        {
          ramFile.seek(nameTable + 2); // location of "number of name records"
          numNames = ramFile.readUnsignedShort(); // get entries in table
          if ((numNames < 1) || (numNames > 9999)) // check arbitrary limits
          {
            printDebug(filePath + " - too many entries in \"name\" table ("
              + numNames + ")");
          }
          else if (fileSize < (nameTable + 6 + (12 * numNames)))
          {
            printDebug(filePath
              + " - file too small for \"name\" table entries");
          }
          else                    // safe to read name table entries from file
          {
            stringOffset = ramFile.readUnsignedShort(); // from <nameTable>

            /* Initialize values for the best name table entry found so far,
            where smaller index numbers are better.  The default "best" is one
            more than the last legal index. */

            int bestAccept = acceptEntries.length; // best is 0, then 1, etc
            int bestEntryEncoding = -1; // ID from name table entry
            int bestEntryLanguage = -1; // ID from name table entry
            int bestEntryName = -1; // ID from name table entry (usually 4)
            int bestEntryPlatform = -1; // ID from name table entry

            /* Compare each name table entry with our desired names. */

            for (fileIndex = 0; fileIndex < numNames; fileIndex ++)
            {
              /* Read the numbers for one name table entry.  All are unsigned
              short integers, and have non-negative values. */

              ramFile.seek(nameTable + 6 + (12 * fileIndex)); // position file
              int entryPlatformID = ramFile.readUnsignedShort();
              int entryEncodingID = ramFile.readUnsignedShort();
              int entryLanguageID = ramFile.readUnsignedShort();
              int entryNameID = ramFile.readUnsignedShort();
              int entryLength = ramFile.readUnsignedShort();
              int entryOffset = ramFile.readUnsignedShort();

              /* Check entries in our "accept" list until we find an entry that
              matches, or we exhaust the list.  We limit our search to indexes
              better (smaller) than our current best. */

              String name = null; // show that we haven't decoded name bytes
              for (acceptIndex = 0; acceptIndex < bestAccept; acceptIndex ++)
              {
                /* Do the ID numbers match for these name table entries? */

                if (((acceptEntries[acceptIndex][0] >= 0)
                  && (entryPlatformID != acceptEntries[acceptIndex][0]))
                || ((acceptEntries[acceptIndex][1] >= 0)
                  && (entryEncodingID != acceptEntries[acceptIndex][1]))
                || ((acceptEntries[acceptIndex][2] >= 0)
                  && (entryLanguageID != acceptEntries[acceptIndex][2]))
                || ((acceptEntries[acceptIndex][3] >= 0)
                  && (entryNameID != acceptEntries[acceptIndex][3])))
                {
                  continue;       // no, next step in <for acceptIndex> loop
                }
                printDebug(filePath + " - comparing (" + entryPlatformID
                  + ", " + entryEncodingID + ", " + entryLanguageID + ", "
                  + entryNameID + ") against table ("
                  + acceptEntries[acceptIndex][0] + ", "
                  + acceptEntries[acceptIndex][1] + ", "
                  + acceptEntries[acceptIndex][2] + ", "
                  + acceptEntries[acceptIndex][3]
                  + ((acceptCharset[acceptIndex] == null) ? ")" :
                    (") " + acceptCharset[acceptIndex])));

                /* The entry IDs match, but the optional character set may not.
                Only decode the name bytes once from the file. */

                if (name == null) // do we need to decode the name bytes?
                {
                  /* For this name table entry, determine the character set. */

                  String charset = null; // assume failure, mark as invalid

                  /* Generic Unicode platform.  Officially, this platform is
                  not to be used for name tables, but we have found a few fonts
                  that only have generic Unicode names (apparently an attempt
                  to prevent them from being used on some operating systems).
                  The encoding and language are completely unreliable and are
                  ignored. */

                  if (entryPlatformID == 0)
                  {
                    charset = UNICODE_BMP; // assume standard 16-bit Unicode
                  }

                  /* Apple Macintosh platform.  There are many more encodings
                  than for Windows, and some encodings may also depend on the
                  language.  We only do encodings where Java has a matching
                  conversion table.  There are enough badly-encoded Macintosh
                  font names without us guessing with incorrect tables! */

                  else if (entryPlatformID == 1)
                  {
                    switch (entryEncodingID)
                    {
                      case (0): charset = "MacRoman"; break; // Latin/Roman
                      case (1): charset = "Shift_JIS"; break; // Japanese
                      case (2): charset = "Big5"; break; // Trad. Chinese
                      case (3): charset = "EUC-KR"; break; // Korean
                      case (4): charset = "MacArabic"; break; // Arabic
                      case (5): charset = "MacHebrew"; break; // Hebrew
                      case (6): charset = "MacGreek"; break; // Greek
                      case (7): charset = "MacCyrillic"; break; // Russian
//                    case (8): charset = "unknown"; break; // RSymbol
//                    case (9): charset = "unknown"; break; // Devanagari
//                    case (10): charset = "unknown"; break; // Gurmukhi
//                    case (11): charset = "unknown"; break; // Gujarati
//                    case (12): charset = "unknown"; break; // Oriya
//                    case (13): charset = "unknown"; break; // Bengali
//                    case (14): charset = "unknown"; break; // Tamil
//                    case (15): charset = "unknown"; break; // Telugu
//                    case (16): charset = "unknown"; break; // Kannada
//                    case (17): charset = "unknown"; break; // Malayalam
//                    case (18): charset = "unknown"; break; // Sinhalese
//                    case (19): charset = "unknown"; break; // Burmese
//                    case (20): charset = "unknown"; break; // Khmer
                      case (21): charset = "MacThai"; break; // Thai
//                    case (22): charset = "unknown"; break; // Laotian
//                    case (23): charset = "unknown"; break; // Georgian
//                    case (24): charset = "unknown"; break; // Armenian
                      case (25): charset = "GBK"; break; // Simp. Chinese
//                    case (26): charset = "unknown"; break; // Tibetan
//                    case (27): charset = "unknown"; break; // Mongolian
//                    case (28): charset = "unknown"; break; // Geez
//                    case (29): charset = "unknown"; break; // Slavic
//                    case (30): charset = "unknown"; break; // Vietnamese
//                    case (31): charset = "unknown"; break; // Sindhi
                      default: /* do nothing */ break;
                    }
                  } // end of Macintosh platform ID

                  /* Microsoft Windows platform.  Almost all Microsoft fonts
                  have standard 16-bit Unicode name tables, but may also have
                  special encodings for Chinese, Japanese, or Korean.  Extended
                  Unicode (24-bit) names can't be properly handled by this Java
                  version 1.4. */

                  else if (entryPlatformID == 3)
                  {
                    switch (entryEncodingID)
                    {
                      case (0): charset = UNICODE_BMP; break; // symbol (16-bit Unicode)
                      case (1): charset = UNICODE_BMP; break; // regular 16-bit Unicode
                      case (2): charset = "Shift_JIS"; break; // Japanese Shift-JIS
                      case (3): charset = "GBK"; break; // Simplified Chinese (China)
                      case (4): charset = "Big5"; break; // Traditional Chinese (Taiwan)
                      case (5): charset = "EUC-KR"; break; // Korean Wansung
                      case (6): charset = "x-Johab"; break; // Korean Johab
                      case (10): charset = UNICODE_BMP; break; // extended Unicode
                      default: /* do nothing */ break;
                    }
                  } // end of Microsoft platform ID

                  /* Ignore all other platform IDs. */

                  /* Stop looking at this name table entry from the font file
                  if we don't know what the correct character set is. */

                  if (charset == null) // is it safe to decode this name?
                  {
                    printDebug(filePath + " - no character set for entry ("
                      + entryPlatformID + ", " + entryEncodingID + ", "
                      + entryLanguageID + ", " + entryNameID + ")");
                    break;        // exit early from <for acceptIndex> loop
                  }

                  /* Can we can read file data for this name table entry? */

                  if ((entryLength < 1) || (entryLength > 255) || (fileSize
                    < (nameTable + stringOffset + entryOffset + entryLength)))
                  {
                    printDebug(filePath + " - bad length 0x"
                      + Integer.toHexString(entryLength) + " or offset 0x"
                      + Integer.toHexString(entryOffset) + " for entry ("
                      + entryPlatformID + ", " + entryEncodingID + ", "
                      + entryLanguageID + ", " + entryNameID + ")");
                    break;        // exit early from <for acceptIndex> loop
                  }

                  /* Read the raw (encoded) data bytes for the name string. */

                  byte[] raw = new byte[entryLength]; // allocate for file data
                  ramFile.seek(nameTable + stringOffset + entryOffset);
                  ramFile.read(raw); // read encoded data bytes for name

                  /* Show the name table entry with string data in hex. */

                  printDebug(filePath + " - name entry (" + entryPlatformID
                    + ", " + entryEncodingID + ", " + entryLanguageID + ", "
                    + entryNameID + ") " + charset + " 0x"
                    + formatHexBytes(raw));

                  /* Philip L. Engel has observed that some East Asian font
                  vendors (Chinese, Japanese, Korean) insert extra null bytes
                  in name strings, perhaps to make each character 16 bits long,
                  whether it normally appears as a single byte or in a double-
                  byte shifting pair. */

                  byte[] adjust = raw; // assume no changes to data bytes
                  int length = raw.length; // assume no changes to length
                  if (charset.equals(UNICODE_BMP) == false) // not Unicode
                  {
                    adjust = new byte[raw.length]; // extract to new array
                    length = 0;   // put next non-null byte at this index
                    for (i = 0; i < raw.length; i ++)
                    {
                      if ((raw[i] != 0x00) && (raw[i] != 0x7F))
                        adjust[length ++] = raw[i]; // all but nulls, deletes
                    }
                  }

                  /* Decode the name string, if there's anything left. */

                  try             // catch invalid character set names
                  {
                    name = new String(adjust, 0, length, charset);
                    printDebug(filePath + " - decoded font name is <" + name
                      + ">");
                  }
                  catch (UnsupportedEncodingException uee)
                  {
                    printDebug(filePath + " - encoding <" + charset
                      + "> not supported on this computer");
                  }
                } // end of "name is null"

                if ((name == null) || (name.length() == 0)) // any usable name?
                  break;          // exit early from <for acceptIndex> loop

                /* If the user gave us a character set name for this name table
                entry, accept the name only it can be encoded correctly in that
                character set.  Many Asian fonts have local names incorrectly
                encoded as "MacRoman English" or "Windows 1033 English".  We
                should be safe from <UnsupportedEncodingException> because we
                previously checked all character set names when they were read
                from the configuration file.  If not, they will be caught by
                our general <IOException> handler. */

                if (acceptCharset[acceptIndex] == null)
                {
                  /* User didn't specify a character set.  Do nothing. */
                }
                else if (name.equals(new String(name.getBytes(acceptCharset
                    [acceptIndex]), acceptCharset[acceptIndex])))
                {
                  printDebug(filePath + " - name matches character set <"
                    + acceptCharset[acceptIndex] + ">");
                }
                else
                {
                  printDebug(filePath + " - name not in character set <"
                    + acceptCharset[acceptIndex] + ">");
                  continue;       // no, next step in <for acceptIndex> loop
                }

                /* Keep this name table entry if it is better than anything we
                have found so far.  The comparison of <acceptIndex> less than
                <bestAccept> is not strictly necessary, since we are inside a
                <for> loop that says exactly the same thing.  However, this
                allows us to change the <for> loop to search all entries all
                the time, and to report detailed information during the search.
                The <break> statements would also need to be disabled for this
                to happen. */

                if (acceptIndex < bestAccept)
                {
                  bestAccept = acceptIndex;
                  bestEntryEncoding = entryEncodingID;
                  bestEntryLanguage = entryLanguageID;
                  bestEntryName = entryNameID;
                  bestEntryPlatform = entryPlatformID;
                  nameFound = name;
                }
                if (bestAccept == 0) break; // stop looking, can't find better

              } // end of <for acceptIndex> loop

              if (bestAccept == 0) break; // stop looking, can't find better

            } // end of <for fileIndex> loop

            /* If we found a name, we may also want to convert (translate) some
            of the characters. */

            if (nameFound != null) // did we find a name string that we like?
            {
              printDebug(filePath + " - using entry (" + bestEntryPlatform
                + ", " + bestEntryEncoding + ", " + bestEntryLanguage + ", "
                + bestEntryName + ") with name <" + nameFound + ">");

              String replace = convertFilename(nameFound); // reduce plain text
              if (nameFound.equals(replace) == false) // is this different?
              {
                nameFound = replace; // yes, use the modified string instead
                printDebug(filePath + " - replacing some characters <"
                  + nameFound + ">");
              }

              replace = collapseString(nameFound); // remove extra spaces
              if (nameFound.equals(replace) == false) // is this different?
              {
                nameFound = replace; // yes, use the modified string instead
                printDebug(filePath + " - removing extra spaces <" + nameFound
                  + ">");
              }
            }
          } // end of "safe to read name table entries"
        }
      } // end of "look through name table"

      /* Choose between an internal font name or a constructed name.  "Forced"
      or "generic" names will be used only if a valid OTF/TTC/TTF signature was
      found.  We don't want to rename files that are clearly not fonts, but do
      want to rename badly corrupted font files.  How correct should a file be
      before we consider renaming it? */

      if (fileType == null)       // do we know what type of file this is?
      {
        /* Do nothing if we couldn't find an OTF/TTC/TTF signature. */
      }
      else if (forcedName != null) // did the user give us a forced name?
      {
        localChecksum = true;     // append file name with CRC32 checksum
        nameFound = forcedName;   // assign user's name to this font file
        printDebug(filePath + " - ignoring font name, using forced <"
          + nameFound + "> plus checksum");
      }
      else if ((nameFound != null) && (nameFound.length() > 0))
      {
        /* Do nothing if we found an acceptable internal font name. */
      }
      else if (genericName != null) // did the user give us a default name?
      {
        localChecksum = true;     // append file name with CRC32 checksum
        nameFound = genericName;  // assign user's name to this font file
        printDebug(filePath + " - no valid font name, using generic <"
          + nameFound + "> plus checksum");
      }
      else if (localChecksum)     // do we have something unique for file?
      {
        nameFound = "Unknown";    // assign default name to this font file
        printDebug(filePath + " - no valid font name, using default <"
          + nameFound + "> plus checksum");
      }
      else                        // might get here with zero-length name
        nameFound = null;         // cancel anything that we don't recognize

      /* Close the input font file before trying to rename the file. */

      ramFile.close();            // try to close input file
      if (cancelFlag) return;     // stop if user hit the panic button

      /* Now put the extracted font name together with the necessary file type
      and see if this is already the correct name for the font file.  First try
      renaming to the desired file name.  If that doesn't work, then try adding
      a numeric suffix from 1 to 99. */

      newFile = null;             // assume failure, mark as invalid
      if ((fileType != null) && (nameFound != null)) // need both to make name
      {
        if (localChecksum)        // should we append CRC32 checksum to name?
        {
          nameFound += "_" + calculateChecksum(givenFile); // may be slow
        }
        File givenParent = givenFile.getParentFile(); // fetch this only once
        for (i = 0; i < 100; i ++) // zero is a special case for no suffix
        {
          if (cancelFlag) return; // stop if user hit the panic button

          /* Construct a new File object, with or without a suffix. */

          if (i > 0)              // should we append a numeric suffix?
          {
            newFile = new File(givenParent, (nameFound + "(" + i + ")."
              + fileType));
          }
          else                    // no suffix, try desired name only
          {
            newFile = new File(givenParent, (nameFound + "." + fileType));
          }

          /* Check if this is already the file name, or can be a new name. */

          if (oldName.equals(newFile.getName())) // identical names for file?
          {
            printSummary(filePath + " - file name is correct");
            break;                // exit early from <for> loop
          }
          else if (givenFile.equals(newFile)) // similar name for same file?
          {
            renameFile(givenFile, newFile); // rename uppercase vs. lowercase
            break;                // exit early from <for> loop
          }
          else if (newFile.exists() == false) // can we use the new file name?
          {
            renameFile(givenFile, newFile); // doesn't exist, use new name
            break;                // exit early from <for> loop
          }
          else
            newFile = null;       // suffixed file exists, invalidate object
        }

        /* If we found something that should have worked, then the <newFile>
        variable won't be null.  If it is, then we tried all possibilities. */

        if (newFile == null)      // if we failed to find a new file name
        {
          printFailure(filePath + " - can't rename as <" + nameFound + "."
            + fileType + "> or similar");
        }
      }
      else                        // missing internal font name or file type
        printFailure(filePath + " - can't find usable font name");
    }

    /* Catch any file I/O errors, here or in called methods. */

    catch (IOException ioe)
    {
      printFailure(filePath + " - can't read file");
    }
  } // end of processTrueType() method


/*
  processUnknownFile() method

  The caller gives us a Java File object that is known to be a file, not a
  directory.  Get the file extension (file type) from the end of the file name,
  and use that to decide what type of font this is, or if the file is not a
  font and should simply be ignored.
*/
  static void processUnknownFile(
    File givenFile,               // file that we are to open, read, and rename
    boolean forceOpen)            // true if we always open file as a font file
  {
    String fileName;              // name of caller's file (root + dot + type)
    String filePath;              // name of caller's file, including path
    String fileRoot;              // only the part before the last period (dot)
    String fileType;              // only the part after the last period (dot)
    int i;                        // index variable

    if (cancelFlag) return;       // stop if user hit the panic button
    fileCount ++;                 // found one more file, of unknown type
//  fileName = givenFile.getName(); // get name of caller's file, no path
    filePath = givenFile.getPath(); // get name of caller's file, with path
    setStatusMessage("Reading file " + filePath);

    /* Go through some trouble to get the exact file name.  The name that the
    user types on a command line or dialog box may be in all lowercase, for
    example, when the real file name is in mixed case ("arial.ttf" instead of
    "Arial.ttf").  This happens on Windows, where case is not important in file
    names, but will be for us later when we compare old and new names. */

    try                           // catch I/O errors during directory search
    {
      fileName = givenFile.getCanonicalFile().getName(); // full resolution
    }
    catch (IOException ioe)       // if the system couldn't handle file name
    {
      fileName = givenFile.getName(); // accept abstract file name (no errors)
    }

    /* Break the file name into a root and an extension, using the last period
    (dot) in the string, if any.  Most operating systems consider uppercase and
    lowercase to be different in file names; only Windows considers them to be
    the same.  We convert the file type to lowercase, because we don't use it
    again with any real file I/O system calls. */

    i = fileName.lastIndexOf('.'); // index of last period (dot), or else -1
    if (i >= 0)                   // if a period (dot) was found in the name
    {
      fileRoot = fileName.substring(0, i); // root is part before period (dot)
      fileType = fileName.substring(i + 1).toLowerCase(); // type is after dot
    }
    else                          // no period (dot) means no file type
    {
      fileRoot = fileName;        // whole file name becomes the root
      fileType = "";              // and the file type becomes empty
    }

    /* How we process a font file depends upon the file extension (type).  We
    could look for signature bytes inside the file, but using only the name
    allows us to skip many files quickly without opening them. */

    if (forceOpen                 // this option overrides actual file type
      || fileType.equals("otf")   // OpenType single font
      || fileType.equals("ttc")   // TrueType collection
      || fileType.equals("ttf"))  // TrueType single font
    {
      processTrueType(givenFile, fileName); // generic OpenType or TrueType
    }
    else if (fileType.equals("afm") // various PostScript extensions
      || fileType.equals("cfg")
      || fileType.equals("inf")
      || fileType.equals("mmm")
      || fileType.equals("pfa")
      || fileType.equals("pfb")
      || fileType.equals("pfm"))
    {
      printSummary(filePath
        + " - ignoring PostScript font file (not supported)");
    }
    else if (fileType.equals("fon")) // Windows bitmapped screen font
    {
      printSummary(filePath
        + " - ignoring bitmapped font file (not supported)");
    }
    else if (fileType.equals("txt")) // plain text file
    {
      printSummary(filePath
        + " - can't read Elmer Fudd, Hacker, Klingon, or Pig Latin"); // joke
    }
    else                          // file type not recognized as a font
    {
      printSummary(filePath + " - not a recognized font file type <"
        + fileType.toUpperCase() + ">");
    }
  } // end of processUnknownFile() method


/*
  renameFile() method

  This is a helper method to rename a file, given an old File object and a new
  File object, and to print a message with the result.
*/
  static void renameFile(File oldFile, File newFile)
  {
    String newName = newFile.getName(); // get new file name, without path
    String oldPath = oldFile.getPath(); // get old file name, including path

    if ((readonlyFlag == false) && (oldFile.canWrite() == false))
    {
      printFailure(oldPath + " - can't rename read-only file to <" + newName
        + ">");
    }
    else if (oldFile.renameTo(newFile)) // try to rename the file
    {
      changeCount ++;             // count successful file name changes
      printChange(oldPath + " - renamed to <" + newName + ">");
    }
    else                          // rename operation failed
    {
      printFailure(oldPath + " - failed to rename as <" + newName + ">");
    }
  } // end of renameFile() method


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
    System.err.println("  java  FontRename4  [options]  file or folder names");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -a0 = only check files with known extensions .OTF .TTC .TTF (default)");
    System.err.println("  -a1 = -a = check all file types, ignore current file name extensions");
    System.err.println("  -c0 = don't append CRC32 checksum to file names (default)");
    System.err.println("  -c1 = -c = append CRC32 checksum to file names (may be slow)");
    System.err.println("  -d# = text file with configuration data; default is -d\"" + DEFAULT_FILE + "\"");
    System.err.println("  -f# = ignore internal names; force given name \"#\" plus checksum");
    System.err.println("  -g# = generic name \"#\" plus checksum if no valid font name found");
    System.err.println("  -h0 = ignore hidden files or folders (default)");
    System.err.println("  -h1 = -h = process hidden files and folders");
    System.err.println("  -m0 = show only program summary, critical errors");
    System.err.println("  -m1 = show files with successful changes (default)");
    System.err.println("  -m2 = show only files that couldn't be changed");
    System.err.println("  -m3 = show all files, with summary for each file");
    System.err.println("  -m4 = show all files, with details for each file");
    System.err.println("  -r0 = don't try to rename read-only files (default)");
    System.err.println("  -r1 = -r = rename read-only files if permitted by system");
    System.err.println("  -s0 = do only given files or folders, no subfolders (default)");
    System.err.println("  -s1 = -s = process files, folders, and subfolders");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
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
  buttons, in the context of the main FontRename4 class.
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
    else if (source == reportDialog) // level of detail for messages
    {
      reportIndex = reportDialog.getSelectedIndex();
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
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of FontRename4 class

// ------------------------------------------------------------------------- //

/*
  FontRename4Filter class

  This class limits the files shown in the file open dialog box to font files.
*/

class FontRename4Filter extends javax.swing.filechooser.FileFilter
                implements java.io.FileFilter // not the same as filechooser.*
{
  /* empty constructor */

  public FontRename4Filter() { }

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

} // end of FontRename4Filter class

// ------------------------------------------------------------------------- //

/*
  FontRename4User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FontRename4User implements ActionListener, Runnable
{
  /* empty constructor */

  public FontRename4User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FontRename4.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FontRename4.doOpenRunner();
  }

} // end of FontRename4User class

/* Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License. */
