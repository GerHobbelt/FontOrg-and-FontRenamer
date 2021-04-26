/*
  Font Names #3 - Extract OpenType and TrueType Font Names
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Sunday, 11 March 2007
  Java class name: FontNames3
  Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 application to extract font names from OpenType and
  TrueType font files.  Each font can have more than one name in different
  languages, and TrueType collections can contain more than one font.  Only
  Unicode names are recognized; most other country or language specific names
  are reported in US-ASCII.  For information on the format of font files, start
  with the following on-line references:

      Microsoft TrueType Font Properties Extension
        http://www.microsoft.com/typography/TrueTypeProperty21.mspx

      The OpenType Font File
        http://www.microsoft.com/typography/otspec/otff.htm
        http://www.microsoft.com/typography/otspec/name.htm
        http://www.microsoft.com/typography/otspec/os2.htm

      The TrueType Font File
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6.html
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6name.html
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6OS2.html

  OpenType (*.OTF) and TrueType (*.TTC, *.TTF) files are supported.  Adobe
  PostScript (*.PFB, *.PFM) files are not supported.

  There aren't many options.  You can choose some of the optional name fields
  (copyright, version, etc).  Then open one or more font files with the "Open"
  button.  A summary will be shown in a scrolling text window.  You may save
  the results to a file with the "Save Output" button, but the output file will
  be in your system's default character set.  If the display has characters
  from other languages such as Chinese or Eastern European, then it is better
  to copy and paste the text directly into a Unicode-aware application like
  Microsoft Word.  This program works best if you have the "Arial Unicode MS"
  font installed.

  To rename font files using their internal names, see the FontRename Java
  application.

  GNU General Public License (GPL)
  --------------------------------
  FontNames3 is free software: you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the Free Software
  Foundation, either version 3 of the License or (at your option) any later
  version.  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
  more details.

  You should have received a copy of the GNU General Public License along with
  this program.  If not, see the http://www.gnu.org/licenses/ web page.

  Name Fields Reported
  --------------------
  FontNames reports several strings from the "name" table and one field from
  the "OS/2" table:

    * always reported: font family name (name ID #1);

    * optional detailed names: font subfamily (name ID #2), unique subfamily
      (#3), font full name (#4), PostScript name (#6), preferred family name
      (#16), preferred subfamily (#17), compatible name (#18), PostScript CID
      "findfont" name (#20);

    * optional designer information: vendor (name ID #8), designer (#9), vendor
      URL (#11), designer URL (#12), OS/2 vendor ID;

    * optional legal information: copyright (name ID #0), trademark (#7),
      license (#13), license URL (#14); and

    * optional miscellaneous information: version (name ID #5), description
      (#10), sample text (#19), plus any unknown name ID numbers.

  These categories are somewhat arbitrary and may be changed in the future.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or file and folder names.  If no
  file or folder names are given on the command line, then this program runs as
  a graphical or "GUI" application with the usual dialog boxes and windows.
  See the "-?" option for a help summary:

      java  FontNames3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  The report will be in your system's default
  character set.  An example command line is:

      java  FontNames3  -s  d:\fonts  >report.txt

  The console application will return an exit status of 1 for success, -1 for
  failure, and 0 for unknown.  The graphical interface can be very slow when
  the output text area gets too big, which will happen if thousands of files
  are reported.

  Restrictions and Limitations
  ----------------------------
  Not all font files are correctly structured.  Before reporting an error in
  this program, make sure that the error isn't in the font file.  Frequent
  mistakes by font vendors are incorrect character set encodings, missing name
  tables, etc.  It is known that this program lacks the ability to convert some
  Macintosh character sets to Unicode.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class FontNames3
{
  /* constants */

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
  static final int FIELD_COUNT = 21; // fields #0 to #20 defined by standard
  static final String[] FIELD_NAMES = {"copyright", "font family name",
    "font subfamily", "unique subfamily", "font full name", "version",
    "PostScript name", "trademark", "vendor", "designer", "description",
    "vendor URL", "designer URL", "license", "license URL", "reserved #15",
    "preferred family name", "preferred subfamily", "compatible name",
    "sample text", "PostScript CID findfont", // standard fields
    "unknown name ID", "OS/2 vendor ID"}; // plus two special fields for us
  static final boolean[] FIELD_RELAX = {true, false, false, false, false, true,
    false, true, true, true, true, false, false, true, false, false, false,
    false, false, true, false, true, false};
                                  // true if we relax rules for allowed chars
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final long INT_MASK = 0x00000000FFFFFFFFL;
                                  // logical mask for one int as long value
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Extract OpenType and TrueType Font Names - by: Keith Fenske";
  static final char REPLACE_CHAR = '\uFFFD'; // Unicode replacement character
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static boolean consoleFlag;     // true if running as a console application
  static JButton exitButton;      // "Exit" button for ending this application
  static boolean[] fieldFlags;    // true if we report given name ID numbers
  static int fieldOs2ID;          // special index to report OS/2 vendor ID
  static int fieldUnknown;        // special index to report unknown name IDs
  static JFileChooser fileChooser; // asks for input and output file names
  static javax.swing.filechooser.FileFilter fontFilter;
                                  // our shared file filter for fonts
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static JCheckBox hexCheckbox;   // graphical option for <hexFlag>
  static boolean hexFlag;         // true if we report all name fields in hex
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report if running as GUI
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JButton saveButton;      // "Save" button for writing output text
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static String systemFileSep;    // file/folder separator for local system
  static int totalErrors;         // total number of files with problems
  static int totalFiles;          // total number of files found (good or bad)

  /* The following class variables are for the pop-up menu that selects which
  name ID numbers will be reported.  It's ugly but it works. */

  static JButton menuButton;      // button for invoking "Show Names" menu
  static JCheckBoxMenuItem menuField00, menuField01, menuField02, menuField03,
    menuField04, menuField05, menuField06, menuField07, menuField08,
    menuField09, menuField10, menuField11, menuField12, menuField13,
    menuField14, menuField15, menuField16, menuField17, menuField18,
    menuField19, menuField20, menuFieldOs2ID, menuFieldUnknown;
  static JMenu menuLegal, menuMaker, menuNames, menuOther;
  static JMenuItem menuLegalAll, menuLegalNone, menuMakerAll, menuMakerNone,
    menuNamesAll, menuNamesNone, menuOtherAll, menuOtherNone, menuPopupAll,
    menuPopupNone;
  static JPopupMenu menuPopup;    // pop-up menu invoked by <menuButton>

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
    consoleFlag = false;          // assume no files or folders on command line
    fontFilter = new FontNames3Filter(); // create our shared file filter
    fontName = "Arial Unicode MS"; // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hexFlag = false;              // by default, convert name fields to text
    hiddenFlag = false;           // by default, don't process hidden files
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    recurseFlag = false;          // by default, don't process subfolders
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    totalErrors = 0;              // total number of files with problems
    totalFiles = 0;               // total number of files found (good or bad)
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

    /* Set the boolean flags that tell us which name IDs to report. */

    fieldFlags = new boolean[FIELD_COUNT + 2]; // standard fields plus specials
    for (i = 0; i < fieldFlags.length; i ++)
      fieldFlags[i] = false;      // default all fields to false
    fieldFlags[1] = true;         // always show font family name (name ID #1)
    fieldFlags[4] = true;         // default show font full name (name ID #4)
    fieldFlags[15] = true;        // this name ID is reserved, error if found
    fieldOs2ID = FIELD_COUNT + 1; // special index for OS/2 vendor ID
    fieldUnknown = FIELD_COUNT + 0; // special index for any unknown fields

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

      else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
      {
        hiddenFlag = true;        // process hidden files and folders
      }
      else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
        hiddenFlag = false;       // ignore hidden files or subfolders

      else if (word.equals("-k") || (mswinFlag && word.equals("/k"))
        || word.equals("-k1") || (mswinFlag && word.equals("/k1")))
      {
        hexFlag = true;           // report all name fields in hexadecimal
      }
      else if (word.equals("-k0") || (mswinFlag && word.equals("/k0")))
        hexFlag = false;          // convert name fields to text characters

      else if (word.startsWith("-n") || (mswinFlag && word.startsWith("/n")))
      {
        /* This option is followed by the number of name ID (field).  We enable
        reporting for that name ID.  Note that there is no console option to
        disable reporting of name IDs, because all but essential name IDs are
        disabled by default. */

        int num = -1;             // default value for number of name ID
        try                       // try to parse remainder as unsigned integer
        {
          num = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          num = -1;               // set result to an illegal value
        }
        if ((num < 0) || (num >= fieldFlags.length))
        {
          System.err.println(
            "Option -n must be followed by a name ID from 0 to "
              + (fieldFlags.length - 1) + ": " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        fieldFlags[num] = true;   // report this name ID, whatever it is
      }

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
      System.err.println("Found " + prettyPlural(totalFiles, "file") + " with "
        + prettyPlural(totalErrors, "error") + ".");
      if (totalErrors > 0)        // did any of the files have errors?
        System.exit(EXIT_FAILURE); // yes, even one error means failure
      else if (totalFiles > 0)    // no errors, but were there any files?
        System.exit(EXIT_SUCCESS); // yes, no errors, good file(s) mean success
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

    action = new FontNames3User(); // create our shared action listener
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
    saveButton.setToolTipText("Save output text to a file.");
    panel2.add(saveButton);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel2.add(exitButton);

    panel1.add(panel2);
    panel1.add(Box.createVerticalStrut(2)); // extra space between panels

    /* Create a horizontal panel for the options.  First do the font name and
    font size for the output text area.  Later will be the declarations for a
    "Show Names" pop-up menu. */

    JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for displayed text.");
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
    fontSizeDialog.setToolTipText("Point size for displayed text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel3.add(fontSizeDialog);

    panel3.add(Box.createHorizontalStrut(20));

    /* This continuation of <panel3> creates the pop-up menu activated by
    <menuButton>, which will select the font name IDs to be reported. */

    menuButton = new JButton("Show Names...");
    menuButton.addActionListener(action);
    if (buttonFont != null) menuButton.setFont(buttonFont);
    menuButton.setMnemonic(KeyEvent.VK_N);
    menuButton.setToolTipText("Select optional name IDs to report.");
    panel3.add(menuButton);

    menuPopup = new JPopupMenu();
    menuPopupAll = new JMenuItem("Select All");
    menuPopupAll.addActionListener(action);
    if (buttonFont != null) menuPopupAll.setFont(buttonFont);
    menuPopup.add(menuPopupAll);
    menuPopupNone = new JMenuItem("Select None");
    menuPopupNone.addActionListener(action);
    if (buttonFont != null) menuPopupNone.setFont(buttonFont);
    menuPopup.add(menuPopupNone);
    menuPopup.addSeparator();

    menuMaker = new JMenu("Designer/Vendor");
    if (buttonFont != null) menuMaker.setFont(buttonFont);
    menuMakerAll = new JMenuItem("Select Group");
    menuMakerAll.addActionListener(action);
    if (buttonFont != null) menuMakerAll.setFont(buttonFont);
    menuMaker.add(menuMakerAll);
    menuMakerNone = new JMenuItem("Select None");
    menuMakerNone.addActionListener(action);
    if (buttonFont != null) menuMakerNone.setFont(buttonFont);
    menuMaker.add(menuMakerNone);
    menuMaker.addSeparator();
    menuField08 = new JCheckBoxMenuItem(("08 " + FIELD_NAMES[8]),
      fieldFlags[8]);
    menuField08.addActionListener(action);
    if (buttonFont != null) menuField08.setFont(buttonFont);
    menuMaker.add(menuField08);
    menuField09 = new JCheckBoxMenuItem(("09 " + FIELD_NAMES[9]),
      fieldFlags[9]);
    menuField09.addActionListener(action);
    if (buttonFont != null) menuField09.setFont(buttonFont);
    menuMaker.add(menuField09);
    menuField11 = new JCheckBoxMenuItem(("11 " + FIELD_NAMES[11]),
      fieldFlags[11]);
    menuField11.addActionListener(action);
    if (buttonFont != null) menuField11.setFont(buttonFont);
    menuMaker.add(menuField11);
    menuField12 = new JCheckBoxMenuItem(("12 " + FIELD_NAMES[12]),
      fieldFlags[12]);
    menuField12.addActionListener(action);
    if (buttonFont != null) menuField12.setFont(buttonFont);
    menuMaker.add(menuField12);
    menuFieldOs2ID = new JCheckBoxMenuItem(FIELD_NAMES[fieldOs2ID],
      fieldFlags[fieldOs2ID]);
    menuFieldOs2ID.addActionListener(action);
    if (buttonFont != null) menuFieldOs2ID.setFont(buttonFont);
    menuMaker.add(menuFieldOs2ID);
    menuPopup.add(menuMaker);

    menuNames = new JMenu("Detailed Names");
    if (buttonFont != null) menuNames.setFont(buttonFont);
    menuNamesAll = new JMenuItem("Select Group");
    menuNamesAll.addActionListener(action);
    if (buttonFont != null) menuNamesAll.setFont(buttonFont);
    menuNames.add(menuNamesAll);
    menuNamesNone = new JMenuItem("Select None");
    menuNamesNone.addActionListener(action);
    if (buttonFont != null) menuNamesNone.setFont(buttonFont);
    menuNames.add(menuNamesNone);
    menuNames.addSeparator();
    menuField01 = new JCheckBoxMenuItem(("01 " + FIELD_NAMES[1]),
      fieldFlags[1]);
    menuField01.addActionListener(action);
    menuField01.setEnabled(false); // always show font family name (name ID #1)
    if (buttonFont != null) menuField01.setFont(buttonFont);
    menuNames.add(menuField01);
    menuField02 = new JCheckBoxMenuItem(("02 " + FIELD_NAMES[2]),
      fieldFlags[2]);
    menuField02.addActionListener(action);
    if (buttonFont != null) menuField02.setFont(buttonFont);
    menuNames.add(menuField02);
    menuField03 = new JCheckBoxMenuItem(("03 " + FIELD_NAMES[3]),
      fieldFlags[3]);
    menuField03.addActionListener(action);
    if (buttonFont != null) menuField03.setFont(buttonFont);
    menuNames.add(menuField03);
    menuField04 = new JCheckBoxMenuItem(("04 " + FIELD_NAMES[4]),
      fieldFlags[4]);
    menuField04.addActionListener(action);
    menuField04.setEnabled(true); // default show font full name (name ID #4)
    if (buttonFont != null) menuField04.setFont(buttonFont);
    menuNames.add(menuField04);
    menuField06 = new JCheckBoxMenuItem(("06 " + FIELD_NAMES[6]),
      fieldFlags[6]);
    menuField06.addActionListener(action);
    if (buttonFont != null) menuField06.setFont(buttonFont);
    menuNames.add(menuField06);
    menuField16 = new JCheckBoxMenuItem(("16 " + FIELD_NAMES[16]),
      fieldFlags[16]);
    menuField16.addActionListener(action);
    if (buttonFont != null) menuField16.setFont(buttonFont);
    menuNames.add(menuField16);
    menuField17 = new JCheckBoxMenuItem(("17 " + FIELD_NAMES[17]),
      fieldFlags[17]);
    menuField17.addActionListener(action);
    if (buttonFont != null) menuField17.setFont(buttonFont);
    menuNames.add(menuField17);
    menuField18 = new JCheckBoxMenuItem(("18 " + FIELD_NAMES[18]),
      fieldFlags[18]);
    menuField18.addActionListener(action);
    if (buttonFont != null) menuField18.setFont(buttonFont);
    menuNames.add(menuField18);
    menuField20 = new JCheckBoxMenuItem(("20 " + FIELD_NAMES[20]),
      fieldFlags[20]);
    menuField20.addActionListener(action);
    if (buttonFont != null) menuField20.setFont(buttonFont);
    menuNames.add(menuField20);
    menuPopup.add(menuNames);

    menuLegal = new JMenu("Legal Notes");
    if (buttonFont != null) menuLegal.setFont(buttonFont);
    menuLegalAll = new JMenuItem("Select Group");
    menuLegalAll.addActionListener(action);
    if (buttonFont != null) menuLegalAll.setFont(buttonFont);
    menuLegal.add(menuLegalAll);
    menuLegalNone = new JMenuItem("Select None");
    menuLegalNone.addActionListener(action);
    if (buttonFont != null) menuLegalNone.setFont(buttonFont);
    menuLegal.add(menuLegalNone);
    menuLegal.addSeparator();
    menuField00 = new JCheckBoxMenuItem(("00 " + FIELD_NAMES[0]),
      fieldFlags[0]);
    menuField00.addActionListener(action);
    if (buttonFont != null) menuField00.setFont(buttonFont);
    menuLegal.add(menuField00);
    menuField07 = new JCheckBoxMenuItem(("07 " + FIELD_NAMES[7]),
      fieldFlags[7]);
    menuField07.addActionListener(action);
    if (buttonFont != null) menuField07.setFont(buttonFont);
    menuLegal.add(menuField07);
    menuField13 = new JCheckBoxMenuItem(("13 " + FIELD_NAMES[13]),
      fieldFlags[13]);
    menuField13.addActionListener(action);
    if (buttonFont != null) menuField13.setFont(buttonFont);
    menuLegal.add(menuField13);
    menuField14 = new JCheckBoxMenuItem(("14 " + FIELD_NAMES[14]),
      fieldFlags[14]);
    menuField14.addActionListener(action);
    if (buttonFont != null) menuField14.setFont(buttonFont);
    menuLegal.add(menuField14);
    menuPopup.add(menuLegal);

    menuOther = new JMenu("Miscellaneous");
    if (buttonFont != null) menuOther.setFont(buttonFont);
    menuOtherAll = new JMenuItem("Select Group");
    menuOtherAll.addActionListener(action);
    if (buttonFont != null) menuOtherAll.setFont(buttonFont);
    menuOther.add(menuOtherAll);
    menuOtherNone = new JMenuItem("Select None");
    menuOtherNone.addActionListener(action);
    if (buttonFont != null) menuOtherNone.setFont(buttonFont);
    menuOther.add(menuOtherNone);
    menuOther.addSeparator();
    menuField05 = new JCheckBoxMenuItem(("05 " + FIELD_NAMES[5]),
      fieldFlags[5]);
    menuField05.addActionListener(action);
    if (buttonFont != null) menuField05.setFont(buttonFont);
    menuOther.add(menuField05);
    menuField10 = new JCheckBoxMenuItem(("10 " + FIELD_NAMES[10]),
      fieldFlags[10]);
    menuField10.addActionListener(action);
    if (buttonFont != null) menuField10.setFont(buttonFont);
    menuOther.add(menuField10);
    menuField19 = new JCheckBoxMenuItem(("19 " + FIELD_NAMES[19]),
      fieldFlags[19]);
    menuField19.addActionListener(action);
    if (buttonFont != null) menuField19.setFont(buttonFont);
    menuOther.add(menuField19);
    menuFieldUnknown = new JCheckBoxMenuItem(FIELD_NAMES[fieldUnknown],
      fieldFlags[fieldUnknown]);
    menuFieldUnknown.addActionListener(action);
    if (buttonFont != null) menuFieldUnknown.setFont(buttonFont);
    menuOther.add(menuFieldUnknown);
    menuPopup.add(menuOther);

    /* This continuation of <panel3> is for options other than selecting which
    name IDs to report. */

    panel3.add(Box.createHorizontalStrut(13));

    hexCheckbox = new JCheckBox("hex", hexFlag);
    if (buttonFont != null) hexCheckbox.setFont(buttonFont);
    hexCheckbox.setToolTipText("Select to report names in hexadecimal.");
    hexCheckbox.addActionListener(action); // do last so don't fire early
    panel3.add(hexCheckbox);

    panel3.add(Box.createHorizontalStrut(5));

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
      "\nExtract font names from OpenType and TrueType font files.\n"
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

  Open one OpenType or TrueType font file and read the internal names.  All
  other methods in this program eventually come here.  We print our result and
  increment global counters for good and bad files.
*/
  static void checkFile(
    File givenFile,               // caller's Java File object (may be valid)
    String givenName)             // caller's name for file, or empty, or null
  {
    final String CANCEL_TEXT = "cancelled by user"; // message when cancelled
    String charname;              // our nice name for name table character set
    String charset;               // official name for name table character set
    int curFont;                  // current font index, up to <numFonts>
    int curName;                  // current name index, up to <numNames>
    int curTable;                 // current table index, up to <numTables>
    byte[] entryBytes;            // name table entry: raw bytes for name
    int entryEncoding;            // name table entry: specific encoding ID
    int entryLanguage;            // name table entry: language ID
    int entryLength;              // name table entry: length of string
    int entryNameID;              // name table entry: name ID
    int entryOffset;              // name table entry: offset to string
    int entryPlatform;            // name table entry: platform ID
    String fileName;              // our name for the file
    long fileSize;                // length of file in bytes
    long[] fontOffsets;           // for each font, offset from beginning of
                                  // ... file to start of its "Offset Table"
    int i;                        // index variable
    boolean multiFlag;            // true if processing TrueType collection
    long name;                    // offset to start of current name table
    int nameTableSize;            // size of current name table in bytes
    long nameTableStrings;        // start of strings for current name table
    int numFonts;                 // number of fonts in this file
    int numNames;                 // number of name table entries
    int numTables;                // number of entries in "Offset Table"
    long os2;                     // offset to start of current OS/2 table
    int os2TableSize;             // size of current OS/2 table in bytes
    int os2TableVersion;          // version number of current OS/2 table
    String os2VendorID;           // extracted 4-character OS/2 vendor ID
    String os2VendorIdUpper;      // uppercase conversion of <os2VendorID>
    String os2VendorName;         // our interpreted name for <os2VendorID>
    boolean printFlag;            // true if we print current name table entry
    String printName;             // printable name for current name table entry
    RandomAccessFile ramFile;     // file stream for reading font file
    boolean relaxFlag;            // true if we relax rules for allowed chars
    long signature;               // signature bytes from beginning of file
    boolean stopFlag;             // local flag to stop processing file
    String stopText;              // message for why we stopped processing
    long table;                   // current location in "Offset Table"

    /* Initialize local variables. */

    fontOffsets = null;           // just to keep compiler happy
    multiFlag = false;            // just to keep compiler happy
    numFonts = 0;                 // just to keep compiler happy
    stopFlag = false;             // everything is okay so far
    stopText = "";                // no reason for stopping yet
    totalFiles ++;                // count the number of files we try to open

    /* Use the caller's name for the file, if one was given.  This allows the
    caller to display a cleaner name, such as a relative path name. */

    if ((givenName != null) && (givenName.length() > 0))
      fileName = givenName;       // caller gave us a non-empty string
    else
      fileName = givenFile.getPath(); // get full path name plus file name
    fileSize = givenFile.length(); // get length of file in bytes
    putOutput("", false);         // blank line
    putOutput(("file #" + totalFiles + ": " + fileName), false);
    setStatusMessage("Reading file " + fileName); // let user know where we are

    /* Open the file and start reading the header information. */

    try                           // catch file I/O errors
    {
      ramFile = new RandomAccessFile(givenFile, "r"); // open file for reading

      /* Figure out what type of file this is, if it's even a font file! */

      if (fileSize < 12)          // need this many bytes for signature
      {
        stopFlag = true;          // stop looking at this file
        stopText = "File size too small for OTF/TTC/TTF signature, "
          + prettyPlural(fileSize, "byte") + ".";
                                  // summary reason for why we stopped
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
          ramFile.skipBytes(4);   // skip over TTC version number
          numFonts = ramFile.readInt(); // get number of fonts in this file
          if ((numFonts < 1) || (numFonts > 29)) // error check arbitrary limits
          {
            stopFlag = true;      // stop looking at this file
            stopText = "Too many internal fonts in TTC file (" + numFonts
              + ").";
          }
          else if (fileSize < ((4 * numFonts) + 12)) // can read offset list?
          {
            stopFlag = true;      // stop looking at this file
            stopText = "File size too small for list of " + numFonts
              + " TTC offsets.";
          }
          else
          {
            fontOffsets = new long[numFonts]; // multiple "Offset Tables"
            for (curFont = 0; curFont < numFonts; curFont ++)
              fontOffsets[curFont] = ((long) ramFile.readInt()) & INT_MASK;
                                  // save file offset to each "Offset Table"
          }
        }
        else
        {
          stopFlag = true;        // stop looking at this file
          stopText =
            "File not recognized as OpenType or TrueType font, 0x"
            + Long.toHexString(signature) + " signature.";
        }
      }

      /* No matter what type of font file this started as, we now have a common
      list of the locations (offsets) to the start of each "Offset Table" for
      each font contained in the file.  This information must all be checked,
      before being used, because the user may have given us a bad file. */

      if (!stopFlag)              // continue only if no errors so far
      {
        for (curFont = 0; curFont < numFonts; curFont ++) // first <for> loop
        {
          if (cancelFlag)         // stop if user hit the panic button
          {
            stopFlag = true;      // stop looking at this file
            stopText = CANCEL_TEXT;
            break;                // exit from first <for> loop
          }

          /* Do we identify each internal font for TTC files? */

          if (multiFlag)
            putOutput(("   internal font #" + (curFont + 1) + " of " + numFonts
              + ":"), false);

          /* See if we can read the entire "Offset Table" and if the size makes
          sense. */

          table = fontOffsets[curFont]; // get start of this "Offset Table"
          if (fileSize < (table + 12)) // can we read start of this table?
          {
            stopFlag = true;      // stop looking at this file
            stopText = "File size too small for start of Offset Table, "
              + prettyPlural(fileSize, "byte") + ".";
            break;                // exit from first <for> loop
          }

          ramFile.seek(table);    // position to start of "Offset Table"
          signature = ((long) ramFile.readInt()) & INT_MASK;
                                  // read four signature bytes
          if ((signature != 0x00010000L) // binary version 1.0 for TrueType
            && (signature != 0x4F54544FL) // "OTTO" for OpenType
            && (signature != 0x74727565L)) // "true" for Macintosh TrueType
          {
            stopFlag = true;      // stop looking at this file
            stopText = "Bad signature in Offset Table, 0x"
              + Long.toHexString(signature) + ".";
            break;                // exit from first <for> loop
          }

          /* Looks like the start of a valid "Offset Table".  See if we can
          read the whole table. */

          numTables = ramFile.readUnsignedShort(); // get entries in table
          if ((numTables < 1) || (numTables > 299)) // check arbitrary limits
          {
            stopFlag = true;      // stop looking at this file
            stopText = "Too many entries in Offset Table (" + numTables + ").";
            break;                // exit from first <for> loop
          }
          if (fileSize < (table + 12 + (16 * numTables)))
          {
            stopFlag = true;      // stop looking at this file
            stopText = "File size too small to contain Offset Table at 0x"
              + Long.toHexString(table) + ".";
            break;                // exit from first <for> loop
          }

          /* Look for name table entries.  We don't check for duplicate tables,
          and we don't make any promises about which table we will use if there
          are duplicates. */

          name = nameTableSize = -1; // assume we can't find proper name table
          os2 = os2TableSize = -1;  // assume we can't find proper OS/2 table
          ramFile.skipBytes(6);   // ignore the search range and shift numbers
          for (curTable = 0; curTable < numTables; curTable ++)
          {
            signature = ((long) ramFile.readInt()) & INT_MASK; // get four bytes
            if (signature == 0x6E616D65L) // is this a "name" table?
            {
              ramFile.skipBytes(4); // yes, skip over checksum
              name = ((long) ramFile.readInt()) & INT_MASK;
                                  // get starting location of name table
              nameTableSize = ramFile.readInt(); // get size of table in bytes
            }
            else if (signature == 0x4F532F32L) // is this an "OS/2" table?
            {
              ramFile.skipBytes(4); // yes, skip over checksum
              os2 = ((long) ramFile.readInt()) & INT_MASK;
                                  // get starting location of OS/2 table
              os2TableSize = ramFile.readInt(); // get size of table in bytes
            }
            else
              ramFile.skipBytes(12); // not a table we want, skip to next entry
          }

          /* Check if we can read the name table. */

          if ((name < 0) || (nameTableSize < 16)) // was there a name table?
          {
            stopFlag = true;      // stop looking at this file
            stopText = "Missing entry for \"name\" table in Offset Table.";
            break;                // exit from first <for> loop
          }
          if (fileSize < (name + nameTableSize))
          {
            stopFlag = true;      // stop looking at this file
            stopText = "File size too small to contain \"name\" table at 0x"
              + Long.toHexString(name) + ".";
            break;                // exit from first <for> loop
          }
          ramFile.seek(name);     // go to start of name table
          signature = ((long) ramFile.readUnsignedShort()) & INT_MASK;
                                  // check structure of table
//        if (signature != 0)     // disable code: ignore signature bytes
//        {
//          stopFlag = true;      // stop looking at this file
//          stopText = "Bad signature in \"name\" table, 0x"
//            + Long.toHexString(signature) + ".";
//          break;                // exit from first <for> loop
//        }
          numNames = ramFile.readUnsignedShort(); // number name table entries
          if ((numNames < 1) || (numNames > 9999)) // check arbitrary limits
          {
            stopFlag = true;      // stop looking at this file
            stopText = "Too many entries in \"name\" table (" + numNames
              + ").";
            break;                // exit from first <for> loop
          }
          nameTableStrings = name + ramFile.readUnsignedShort();
                                  // starting location of all strings in table
          if ((nameTableStrings < name)
            || (nameTableStrings > (name + nameTableSize)))
          {
            stopFlag = true;      // stop looking at this file
            stopText = "Bad starting offset for strings in \"name\" table, 0x"
              + Long.toHexString(nameTableStrings) + ".";
            break;                // exit from first <for> loop
          }

          /* Loop through the entries in the name table.  Select the ones most
          useful to us. */

          for (curName = 0; curName < numNames; curName ++)
                                  // second <for> loop
          {
            if (cancelFlag)       // stop if user hit the panic button
            {
              stopFlag = true;    // stop looking at this file
              stopText = CANCEL_TEXT;
              break;              // exit from second <for> loop
            }
            ramFile.seek(name + 6 + (curName * 12)); // start of this entry
            entryPlatform = ramFile.readUnsignedShort(); // platform ID
            entryEncoding = ramFile.readUnsignedShort(); // specific encoding
            entryLanguage = ramFile.readUnsignedShort(); // language ID
            entryNameID = ramFile.readUnsignedShort(); // name ID
            entryLength = ramFile.readUnsignedShort(); // length of string
            entryOffset = ramFile.readUnsignedShort(); // string offset

            if ((entryOffset < 0) || (entryLength < 0)
              || ((entryOffset + entryLength) > nameTableSize))
            {
              stopFlag = true;    // stop looking at this file
              stopText = "Bad string offset in name table entry, 0x"
                + Integer.toHexString(entryOffset) + " with length 0x"
                + Integer.toHexString(entryLength) + ".";
              break;              // exit from second <for> loop
            }

            /* Before going through the numerous <if> and <switch> statements
            below, decide if we will be printing this name table entry.  We
            have a boolean array <fieldFlags> to tell us which name IDs to
            print, and we have a boolean array <FIELD_RELAX> to tell us whether
            we should relax the rules for allowable characters. */

            if ((entryNameID >= 0) && (entryNameID < FIELD_COUNT)
              && (entryNameID != 15)) // #15 is reserved, treat as unknown ID
            {
              printFlag = fieldFlags[entryNameID]; // true if we print this ID
              printName = FIELD_NAMES[entryNameID]; // printable name for field
              relaxFlag = FIELD_RELAX[entryNameID]; // true if we relax rules
            }
            else
            {
              printFlag = fieldFlags[fieldUnknown]; // true if we print unknowns
              printName = FIELD_NAMES[fieldUnknown] + " " + entryNameID;
              relaxFlag = FIELD_RELAX[fieldUnknown]; // true if we relax rules
            }
            if (printFlag)        // only do more work if we really will print
            {
              /* Figure out which character set this byte string is in.  For
              some of the Chinese-Japanese-Korean character sets, we are
              guessing about the encoding.  Font makers aren't all that
              consistent, so our guess is often as good as theirs....  For most
              of the Macintosh languages, we simply don't know what the correct
              encoding is. */

              if (entryPlatform == 0) // Unicode
              {
                charname = "Unicode " + entryEncoding + " " + entryLanguage;
                                  // our name for this character set
                charset = "UTF-16"; // assume all bytes are encoded in Unicode
              }
              else if (entryPlatform == 1) // Macintosh
              {
                /* First, do the encoding, which affects how we interpret the
                raw bytes forming the name.  We don't have enough information
                about some of the Macintosh encodings, so we default to
                US-ASCII for those. */

                charname = "Macintosh " + entryEncoding;
                                  // our name for this character set
                charset = "US-ASCII"; // default to ASCII if don't know better
                switch (entryEncoding)
                {
                  case (0):
                    charname += " Roman"; // our name for this character set
                    charset = "MacRoman"; // special encoding for Roman/Latin
                    break;
                  case (1): charname += " Japanese"; charset = "Shift_JIS"; break;
                  case (2): charname += " Trad. Chinese"; charset = "Big5"; break;
                  case (3): charname += " Korean"; charset = "EUC-KR"; break;
                  case (4): charname += " Arabic"; charset = "MacArabic"; break;
                  case (5): charname += " Hebrew"; charset = "MacHebrew"; break;
                  case (6): charname += " Greek"; charset = "MacGreek"; break;
                  case (7): charname += " Russian"; charset = "MacCyrillic"; break;
                  case (8): charname += " RSymbol"; break; // assume ASCII
                  case (9): charname += " Devanagari"; break; // assume ASCII
                  case (10): charname += " Gurmukhi"; break; // assume ASCII
                  case (11): charname += " Gujarati"; break; // assume ASCII
                  case (12): charname += " Oriya"; break; // assume ASCII
                  case (13): charname += " Bengali"; break; // assume ASCII
                  case (14): charname += " Tamil"; break; // assume ASCII
                  case (15): charname += " Telugu"; break; // assume ASCII
                  case (16): charname += " Kannada"; break; // assume ASCII
                  case (17): charname += " Malayalam"; break; // assume ASCII
                  case (18): charname += " Sinhalese"; break; // assume ASCII
                  case (19): charname += " Burmese"; break; // assume ASCII
                  case (20): charname += " Khmer"; break; // assume ASCII
                  case (21): charname += " Thai"; charset = "MacThai"; break;
                  case (22): charname += " Laotian"; break; // assume ASCII
                  case (23): charname += " Georgian"; break; // assume ASCII
                  case (24): charname += " Armenian"; break; // assume ASCII
                  case (25): charname += " Simp. Chinese"; charset = "GBK"; break;
                  case (26): charname += " Tibetan"; break; // assume ASCII
                  case (27): charname += " Mongolian"; break; // assume ASCII
                  case (28): charname += " Geez"; break; // assume ASCII
                  case (29): charname += " Slavic"; break; // assume ASCII
                  case (30): charname += " Vietnamese"; break; // assume ASCII
                  case (31): charname += " Sindhi"; break; // assume ASCII
                  default: /* do nothing */ break;
                }

                /* Second, append the language, which is purely informational.
                We may find later that the encoding must be combined with the
                language to find the correct Macintosh character set. */

                charname += " " + entryLanguage; // append language identifier
                switch (entryLanguage)
                {
                  case (0): charname += " English"; break;
                  case (1): charname += " French"; break;
                  case (2): charname += " German"; break;
                  case (3): charname += " Italian"; break;
                  case (4): charname += " Dutch"; break;
                  case (5): charname += " Swedish"; break;
                  case (6): charname += " Spanish"; break;
                  case (7): charname += " Danish"; break;
                  case (8): charname += " Portuguese"; break;
                  case (9): charname += " Norwegian"; break;
                  case (10): charname += " Hebrew"; break;
                  case (11): charname += " Japanese"; break;
                  case (12): charname += " Arabic"; break;
                  case (13): charname += " Finnish"; break;
                  case (14): charname += " Greek"; break;
                  case (15): charname += " Icelandic"; break;
                  case (16): charname += " Maltese"; break;
                  case (17): charname += " Turkish"; break;
                  case (18): charname += " Croatian"; break;
                  case (19): charname += " Trad. Chinese"; break;
                  case (20): charname += " Urdu"; break;
                  case (21): charname += " Hindi"; break;
                  case (22): charname += " Thai"; break;
                  case (23): charname += " Korean"; break;
                  case (24): charname += " Lithuanian"; break;
                  case (25): charname += " Polish"; break;
                  case (26): charname += " Hungarian"; break;
                  case (27): charname += " Estonian"; break;
                  case (28): charname += " Latvian"; break;
                  case (29): charname += " Sami"; break;
                  case (30): charname += " Faroese"; break;
                  case (31): charname += " Farsi/Persian"; break;
                  case (32): charname += " Russian"; break;
                  case (33): charname += " Simp. Chinese"; break;
                  case (34): charname += " Flemish"; break;
                  case (35): charname += " Irish Gaelic"; break;
                  case (36): charname += " Albanian"; break;
                  case (37): charname += " Romanian"; break;
                  case (38): charname += " Czech"; break;
                  case (39): charname += " Slovak"; break;
                  case (40): charname += " Slovenian"; break;
                  case (41): charname += " Yiddish"; break;
                  case (42): charname += " Serbian"; break;
                  case (43): charname += " Macedonian"; break;
                  case (44): charname += " Bulgarian"; break;
                  case (45): charname += " Ukrainian"; break;
                  case (46): charname += " Byelorussian"; break;
                  case (47): charname += " Uzbek"; break;
                  case (48): charname += " Kazakh"; break;
                  case (49): charname += " Azerbaijani (Cyrillic)"; break;
                  case (50): charname += " Azerbaijani (Arabic)"; break;
                  case (51): charname += " Armenian"; break;
                  case (52): charname += " Georgian"; break;
                  case (53): charname += " Moldavian"; break;
                  case (54): charname += " Kirghiz"; break;
                  case (55): charname += " Tajiki"; break;
                  case (56): charname += " Turkmen"; break;
                  case (57): charname += " Mongolian (Mongolian)"; break;
                  case (58): charname += " Mongolian (Cyrillic)"; break;
                  case (59): charname += " Pashto"; break;
                  case (60): charname += " Kurdish"; break;
                  case (61): charname += " Kashmiri"; break;
                  case (62): charname += " Sindhi"; break;
                  case (63): charname += " Tibetan"; break;
                  case (64): charname += " Nepali"; break;
                  case (65): charname += " Sanskrit"; break;
                  case (66): charname += " Marathi"; break;
                  case (67): charname += " Bengali"; break;
                  case (68): charname += " Assamese"; break;
                  case (69): charname += " Gujarati"; break;
                  case (70): charname += " Punjabi"; break;
                  case (71): charname += " Oriya"; break;
                  case (72): charname += " Malayalam"; break;
                  case (73): charname += " Kannada"; break;
                  case (74): charname += " Tamil"; break;
                  case (75): charname += " Telugu"; break;
                  case (76): charname += " Sinhalese"; break;
                  case (77): charname += " Burmese"; break;
                  case (78): charname += " Khmer"; break;
                  case (79): charname += " Lao"; break;
                  case (80): charname += " Vietnamese"; break;
                  case (81): charname += " Indonesian"; break;
                  case (82): charname += " Tagalog"; break;
                  case (83): charname += " Malay (Roman)"; break;
                  case (84): charname += " Malay (Arabic)"; break;
                  case (85): charname += " Amharic"; break;
                  case (86): charname += " Tigrinya"; break;
                  case (87): charname += " Galla"; break;
                  case (88): charname += " Somali"; break;
                  case (89): charname += " Swahili"; break;
                  case (90): charname += " Kinyarwanda/Ruanda"; break;
                  case (91): charname += " Rundi"; break;
                  case (92): charname += " Nyanja/Chewa"; break;
                  case (93): charname += " Malagasy"; break;
                  case (94): charname += " Esperanto"; break;
                  case (128): charname += " Welsh"; break;
                  case (129): charname += " Basque"; break;
                  case (130): charname += " Catalan"; break;
                  case (131): charname += " Latin"; break;
                  case (132): charname += " Quechua"; break;
                  case (133): charname += " Guarani"; break;
                  case (134): charname += " Aymara"; break;
                  case (135): charname += " Tatar"; break;
                  case (136): charname += " Uighur"; break;
                  case (137): charname += " Dzongkha"; break;
                  case (138): charname += " Javanese (Roman)"; break;
                  case (139): charname += " Sundanese (Roman)"; break;
                  case (140): charname += " Galician"; break;
                  case (141): charname += " Afrikaans"; break;
                  case (142): charname += " Breton"; break;
                  case (143): charname += " Inuktitut"; break;
                  case (144): charname += " Scottish Gaelic"; break;
                  case (145): charname += " Manx Gaelic"; break;
                  case (146): charname += " Irish Gaelic (dot above)"; break;
                  case (147): charname += " Tongan"; break;
                  case (148): charname += " Greek (polytonic)"; break;
                  case (149): charname += " Greenlandic"; break;
                  case (150): charname += " Azerbaijani (Roman)"; break;
                  default: /* do nothing */ break;
                }
              }
              else if ((entryPlatform == 3) && ((entryEncoding == 0)
                || (entryEncoding == 1) || (entryEncoding == 10))) // Windows
              {
                charname = "Windows " + entryEncoding + " " + entryLanguage;
                                  // our name for this character set
                charset = "UTF-16"; // assume all bytes are encoded in Unicode
                switch (entryLanguage)
                {
                  case (1025): charname += " Arabic (Saudi Arabia)"; break;
                  case (1026): charname += " Bulgarian"; break;
                  case (1027): charname += " Catalan"; break;
                  case (1028): charname += " Trad. Chinese (Taiwan)"; break;
                  case (1029): charname += " Czech"; break;
                  case (1030): charname += " Danish"; break;
                  case (1031): charname += " German"; break;
                  case (1032): charname += " Greek"; break;
                  case (1033): charname += " English"; break;
                  case (1034): charname += " Spanish (Traditional)"; break;
                  case (1035): charname += " Finnish"; break;
                  case (1036): charname += " French (France)"; break;
                  case (1037): charname += " Hebrew"; break;
                  case (1038): charname += " Hungarian"; break;
                  case (1039): charname += " Icelandic"; break;
                  case (1040): charname += " Italian"; break;
                  case (1041): charname += " Japanese"; break;
                  case (1042): charname += " Korean"; break;
                  case (1043): charname += " Dutch"; break;
                  case (1044): charname += " Norwegian"; break;
                  case (1045): charname += " Polish"; break;
                  case (1046): charname += " Portuguese (Brazil)"; break;
                  case (1047): charname += " Rhaeto-Romanic"; break;
                  case (1048): charname += " Romanian"; break;
                  case (1049): charname += " Russian"; break;
                  case (1050): charname += " Croatian"; break;
                  case (1051): charname += " Slovak"; break;
                  case (1052): charname += " Albanian"; break;
                  case (1053): charname += " Swedish"; break;
                  case (1054): charname += " Thai"; break;
                  case (1055): charname += " Turkish"; break;
                  case (1056): charname += " Urdu"; break;
                  case (1057): charname += " Indonesian"; break;
                  case (1058): charname += " Ukrainian"; break;
                  case (1059): charname += " Belarusian"; break;
                  case (1060): charname += " Slovenian"; break;
                  case (1061): charname += " Estonian"; break;
                  case (1062): charname += " Latvian"; break;
                  case (1063): charname += " Lithuanian"; break;
                  case (1064): charname += " Tajik"; break;
                  case (1065): charname += " Farsi"; break;
                  case (1066): charname += " Vietnamese"; break;
                  case (1067): charname += " Armenian"; break;
                  case (1068): charname += " Azeri (Latin)"; break;
                  case (1069): charname += " Basque"; break;
                  case (1070): charname += " Sorbian"; break;
                  case (1071): charname += " FYRO Macedonian"; break;
                  case (1072): charname += " Sesotho/Sutu"; break;
                  case (1073): charname += " Tsonga"; break;
                  case (1074): charname += " Tswana"; break;
                  case (1075): charname += " Venda"; break;
                  case (1076): charname += " Xhosa"; break;
                  case (1077): charname += " Zulu"; break;
                  case (1078): charname += " Afrikaans"; break;
                  case (1079): charname += " Georgian"; break;
                  case (1080): charname += " Faroese"; break;
                  case (1081): charname += " Hindi"; break;
                  case (1082): charname += " Maltese"; break;
                  case (1083): charname += " Sami Lappish"; break;
                  case (1084): charname += " Gaelic Scotland"; break;
                  case (1086): charname += " Malay (Malaysia)"; break;
                  case (1087): charname += " Kazakh"; break;
                  case (1088): charname += " Kyrgyz (Cyrillic)"; break;
                  case (1089): charname += " Swahili"; break;
                  case (1090): charname += " Turkmen"; break;
                  case (1091): charname += " Uzbek (Latin)"; break;
                  case (1092): charname += " Tatar"; break;
                  case (1093): charname += " Bengali"; break;
                  case (1094): charname += " Punjabi"; break;
                  case (1095): charname += " Gujarati"; break;
                  case (1096): charname += " Oriya"; break;
                  case (1097): charname += " Tamil"; break;
                  case (1098): charname += " Telugu"; break;
                  case (1099): charname += " Kannada"; break;
                  case (1100): charname += " Malayalam"; break;
                  case (1101): charname += " Assamese"; break;
                  case (1102): charname += " Marathi"; break;
                  case (1103): charname += " Sanskrit"; break;
                  case (1104): charname += " Mongolian (Cyrillic)"; break;
                  case (1105): charname += " Tibetan"; break;
                  case (1106): charname += " Welsh"; break;
                  case (1107): charname += " Khmer"; break;
                  case (1108): charname += " Lao"; break;
                  case (1109): charname += " Burmese"; break;
                  case (1110): charname += " Galician"; break;
                  case (1111): charname += " Konkani"; break;
                  case (1112): charname += " Manipuri"; break;
                  case (1113): charname += " Sindhi"; break;
                  case (1114): charname += " Syriac"; break;
                  case (1120): charname += " Kashmiri"; break;
                  case (1121): charname += " Nepali"; break;
                  case (1122): charname += " Frisian (Netherlands)"; break;
                  case (1125): charname += " Divehi"; break;
                  case (2049): charname += " Arabic (Iraq)"; break;
                  case (2052): charname += " Simp. Chinese (China)"; break;
                  case (2055): charname += " German (Switzerland)"; break;
                  case (2057): charname += " English (United Kingdom)"; break;
                  case (2058): charname += " Spanish (Mexico)"; break;
                  case (2060): charname += " French (Belgium)"; break;
                  case (2064): charname += " Italian (Switzerland)"; break;
                  case (2067): charname += " Dutch (Belgium)"; break;
                  case (2068): charname += " Norwegian (Nynorsk)"; break;
                  case (2070): charname += " Portuguese (Portugal)"; break;
                  case (2072): charname += " Romanian (Moldova)"; break;
                  case (2073): charname += " Russian Moldova"; break;
                  case (2074): charname += " Serbian (Latin)"; break;
                  case (2077): charname += " Swedish (Finland)"; break;
                  case (2092): charname += " Azeri (Cyrillic)"; break;
                  case (2108): charname += " Gaelic Ireland"; break;
                  case (2110): charname += " Malay (Brunei Darussalam)"; break;
                  case (2115): charname += " Uzbek (Cyrillic)"; break;
                  case (3073): charname += " Arabic (Egypt)"; break;
                  case (3076): charname += " Chinese (Hong Kong S.A.R.)"; break;
                  case (3079): charname += " German (Austria)"; break;
                  case (3081): charname += " English (Australia)"; break;
                  case (3082): charname += " Spanish (International)"; break;
                  case (3084): charname += " French (Canada)"; break;
                  case (3098): charname += " Serbian (Cyrillic)"; break;
                  case (4097): charname += " Arabic (Libya)"; break;
                  case (4100): charname += " Chinese (Singapore)"; break;
                  case (4103): charname += " German (Luxembourg)"; break;
                  case (4105): charname += " English (Canada)"; break;
                  case (4106): charname += " Spanish (Guatemala)"; break;
                  case (4108): charname += " French (Switzerland)"; break;
                  case (5121): charname += " Arabic (Algeria)"; break;
                  case (5124): charname += " Chinese (Macau S.A.R.)"; break;
                  case (5127): charname += " German (Liechtenstein)"; break;
                  case (5129): charname += " English (New Zealand)"; break;
                  case (5130): charname += " Spanish (Costa Rica)"; break;
                  case (5132): charname += " French (Luxembourg)"; break;
                  case (6145): charname += " Arabic (Morocco)"; break;
                  case (6153): charname += " English (Ireland)"; break;
                  case (6154): charname += " Spanish (Panama)"; break;
                  case (6156): charname += " French (Monaco)"; break;
                  case (7169): charname += " Arabic (Tunisia)"; break;
                  case (7177): charname += " English (South Africa)"; break;
                  case (7178): charname += " Spanish (Dominican Republic)"; break;
                  case (7180): charname += " French (West Indies)"; break;
                  case (8193): charname += " Arabic (Oman)"; break;
                  case (8201): charname += " English (Jamaica)"; break;
                  case (8202): charname += " Spanish (Venezuela)"; break;
                  case (9217): charname += " Arabic (Yemen)"; break;
                  case (9225): charname += " English (Caribbean)"; break;
                  case (9226): charname += " Spanish (Colombia)"; break;
                  case (9228): charname += " French (Congo, DRC)"; break;
                  case (10241): charname += " Arabic (Syria)"; break;
                  case (10249): charname += " English (Belize)"; break;
                  case (10250): charname += " Spanish (Peru)"; break;
                  case (10252): charname += " French (Senegal)"; break;
                  case (11265): charname += " Arabic (Jordan)"; break;
                  case (11273): charname += " English (Trinidad)"; break;
                  case (11274): charname += " Spanish (Argentina)"; break;
                  case (11276): charname += " French (Cameroon)"; break;
                  case (12289): charname += " Arabic (Lebanon)"; break;
                  case (12297): charname += " English (Zimbabwe)"; break;
                  case (12298): charname += " Spanish (Ecuador)"; break;
                  case (12300): charname += " French (Cote d'Ivoire)"; break;
                  case (13313): charname += " Arabic (Kuwait)"; break;
                  case (13321): charname += " English (Philippines)"; break;
                  case (13322): charname += " Spanish (Chile)"; break;
                  case (13324): charname += " French (Mali)"; break;
                  case (14337): charname += " Arabic (U.A.E.)"; break;
                  case (14346): charname += " Spanish (Uruguay)"; break;
                  case (15361): charname += " Arabic (Bahrain)"; break;
                  case (15370): charname += " Spanish (Paraguay)"; break;
                  case (16385): charname += " Arabic (Qatar)"; break;
                  case (16394): charname += " Spanish (Bolivia)"; break;
                  case (17418): charname += " Spanish (El Salvador)"; break;
                  case (18442): charname += " Spanish (Honduras)"; break;
                  case (19466): charname += " Spanish (Nicaragua)"; break;
                  case (20490): charname += " Spanish (Puerto Rico)"; break;
                  default: /* do nothing */ break;
                }
              }
              else if ((entryPlatform == 3) && (entryEncoding == 2)) // Windows
              {
                charname = "Windows " + entryEncoding + " " + entryLanguage
                  + " Japanese Shift-JIS"; // our name for this character set
                charset = "Shift_JIS"; // special encoding for Japanese (Japan)
              }
              else if ((entryPlatform == 3) && (entryEncoding == 3)) // Windows
              {
                charname = "Windows " + entryEncoding + " " + entryLanguage
                  + " Simp. Chinese GBK"; // our name for this character set
                charset = "GBK";  // special encoding for Chinese (China)
              }
              else if ((entryPlatform == 3) && (entryEncoding == 4)) // Windows
              {
                charname = "Windows " + entryEncoding + " " + entryLanguage
                  + " Trad. Chinese Big5"; // our name for this character set
                charset = "Big5"; // special encoding for Chinese (Taiwan)
              }
              else if ((entryPlatform == 3) && (entryEncoding == 5)) // Windows
              {
                charname = "Windows " + entryEncoding + " " + entryLanguage
                  + " Korean Wansung"; // our name for this character set
                charset = "EUC-KR"; // special encoding for Korean Wansung
              }
              else if ((entryPlatform == 3) && (entryEncoding == 6)) // Windows
              {
                charname = "Windows " + entryEncoding + " " + entryLanguage
                  + " Korean Johab"; // our name for this character set
                charset = "x-Johab"; // special encoding for Korean Johab
              }
              else                // generic for anything we don't understand
              {
                charname = "Platform " + entryPlatform + " encoding "
                  + entryEncoding + " language " + entryLanguage;
                                  // our name for an unknown character set
                charset = "US-ASCII"; // assume all bytes are encoded in ASCII
              }

              /* Do we have something worth printing? */

              charname += ", " + printName; // append field name to string
              if (entryLength == 0) // does field have a non-empty value?
                putOutput(("      " + charname + " is empty string"), false);
              else
              {
                ramFile.seek(nameTableStrings + entryOffset); // position file
                entryBytes = new byte[entryLength]; // how many bytes to read
                ramFile.read(entryBytes); // grab raw name bytes to convert
                putOutput(("      " + charname + ": "
                  + convertEncodedBytes(charset, entryBytes, relaxFlag)),
                  false);
              }
            }
          } // end of second <for> loop

          if (stopFlag)           // did second <for> loop end early?
            break;                // yes, exit from first <for> loop

          /* One small piece of information is found only in the OS/2 table,
          not in the name table: the font vendor ID.  This ID has exactly four
          bytes and must be interpreted to obtain the full name of the font
          vendor.  We only recognize the major font vendors; for an official
          list, see the

              http://www.microsoft.com/typography/links/VendorList.aspx

          web page.  The OS/2 table is required even for Macintosh fonts. */

          if (fieldFlags[fieldOs2ID] == false) // was OS/2 vendor requested?
          {
            /* Do nothing if the user doesn't want the vendor ID. */
          }
          else if (os2 < 0)       // a missing table is not a problem for us
          {
            putOutput("      OS/2 table is missing.", false);
          }
          else if ((os2TableSize < 68) || (os2TableSize > 200)
            || (fileSize < (os2 + os2TableSize))) // check arbitrary limits
          {
            stopFlag = true;      // stop looking at this file
            stopText = "OS/2 table offset (0x" + Long.toHexString(os2)
              + ") or length (" + os2TableSize + ") is bad.";
            break;                // exit from first <for> loop
          }
          else                    // should be able to read OS/2 vendor ID
          {
            ramFile.seek(os2);    // go to start of OS/2 table
            os2TableVersion = ramFile.readUnsignedShort(); // version number
            entryBytes = new byte[4]; // allocate byte array for vendor ID
            ramFile.seek(os2 + 58); // where vendor ID bytes start in file
            ramFile.read(entryBytes); // grab raw name bytes to convert
            os2VendorID = convertEncodedBytes("ASCII-Z", entryBytes, false);
            os2VendorIdUpper = os2VendorID.toUpperCase(); // for comparisons

            /* Look for the major font vendor IDs.  There are too many to
            include here, and the list keeps changing.  Yes, this would be
            better done as a table, or read from a configuration file. */

            if (os2VendorIdUpper.equals("1ASC"))
              os2VendorName = "Ascender";
            else if (os2VendorIdUpper.equals("ADBE"))
              os2VendorName = "Adobe";
            else if (os2VendorIdUpper.equals("AGFA"))
              os2VendorName = "Agfa Monotype";
            else if (os2VendorIdUpper.equals("ALTS"))
              os2VendorName = "Altsys (Macromedia) Fontographer software";
            else if (os2VendorIdUpper.equals("AMT "))
              os2VendorName = "Agfa Monotype";
            else if (os2VendorIdUpper.equals("APPL"))
              os2VendorName = "Apple";
            else if (os2VendorIdUpper.equals("ARPH"))
              os2VendorName = "Arphic (Taiwan)";
            else if (os2VendorIdUpper.equals("BDFZ"))
              os2VendorName = "Beijing Founder (China)";
            else if (os2VendorIdUpper.equals("BITS"))
              os2VendorName = "Bitstream";
            else if (os2VendorIdUpper.equals("CANT"))
              os2VendorName = "Canada Type";
            else if (os2VendorIdUpper.equals("DYNA"))
              os2VendorName = "DynaComware, DynaFont, DynaLab (Taiwan)";
            else if (os2VendorIdUpper.equals("FWKS"))
              os2VendorName = "Fontworks (Japan)";
            else if (os2VendorIdUpper.equals("GOOD"))
              os2VendorName = "Goodfont (Korea)";
            else if (os2VendorIdUpper.equals("HANY"))
              os2VendorName = "Beijing Hanyi Keyin (China)";
            else if (os2VendorIdUpper.equals("HP  "))
              os2VendorName = "Hewlett-Packard";
            else if (os2VendorIdUpper.equals("HY  "))
              os2VendorName = "HanYang (Korea)";
            else if (os2VendorIdUpper.equals("ITC "))
              os2VendorName = "International Typeface";
            else if (os2VendorIdUpper.equals("LINO"))
              os2VendorName = "Linotype";
            else if (os2VendorIdUpper.equals("MACR"))
              os2VendorName = "Macromedia Fontographer software";
            else if (os2VendorIdUpper.equals("MONO"))
              os2VendorName = "Monotype Imaging";
            else if (os2VendorIdUpper.equals("MRSW"))
              os2VendorName = "Morisawa (Japan)";
            else if (os2VendorIdUpper.equals("MS  "))
              os2VendorName = "Microsoft";
            else if (os2VendorIdUpper.equals("MT  "))
              os2VendorName = "Monotype Imaging";
            else if (os2VendorIdUpper.equals("P22 "))
              os2VendorName = "P22 Type Foundry";
            else if (os2VendorIdUpper.equals("PFED"))
              os2VendorName = "PfaEdit or FontForge software";
            else if (os2VendorIdUpper.equals("PYRS"))
              os2VendorName = "Pyrus FontLab software";
            else if (os2VendorIdUpper.equals("RICO"))
              os2VendorName = "Ricoh (Japan)";
            else if (os2VendorIdUpper.equals("RIX "))
              os2VendorName = "Fontrix (Korea)";
            else if (os2VendorIdUpper.equals("SAND"))
              os2VendorName = "Sandoll Type Bank (Korea)";
            else if (os2VendorIdUpper.equals("URW "))
              os2VendorName = "URW++ Design & Development";
            else if (os2VendorIdUpper.equals("YDI "))
              os2VendorName = "Yoon Design (Korea)";
            else
              os2VendorName = ""; // report nothing for IDs we don't recognize

            /* Print what we found. */

            putOutput(("      OS/2 (" + os2TableVersion + ", " + os2TableSize
              + ") vendor ID is " + ((os2VendorID.length() == 4)
              ? ("\"" + os2VendorID + "\"") : os2VendorID)
              + ((os2VendorName.length() > 0) ? (" for " + os2VendorName) : "")
              + "."), false);
          }
        } // end of first <for> loop
      }

      /* Close the input font file. */

      ramFile.close();            // try to close input file
    }

    /* Catch any file I/O errors, here or in called methods. */

    catch (IOException ioe)
    {
      stopFlag = true;            // stop looking at this file
      stopText = "Can't read from file."; // why we stopped
    }

    /* Print a summary and increment global counters. */

    if (stopFlag)                 // did we stop for a reason?
    {
      totalErrors ++;             // yes, count this file as an error
      if (stopText.equals(CANCEL_TEXT) == false) // don't echo "cancelled"
        putOutput(("   " + stopText), false); // file name already printed
    }
  } // end of checkFile() method


/*
  convertEncodedBytes() method

  The caller gives us the name of a character set and an array of bytes that
  are supposedly encoded in that character set.  We return a string with the
  text converted to standard Java characters (Unicode).  Some adjustments may
  be made to improve the conversion.

  For most strings, such as font names, we want to be very strict about allowed
  characters.  For informational strings like copyright and license, we relax
  our rules and allow such things as newline and tab characters.

  Philip L. Engel has observed that some East Asian font vendors (Chinese,
  Japanese, Korean) insert extra null bytes in name strings, perhaps to make
  each character 16 bits long, whether it normally appears as a single byte or
  in a double-byte shifting pair.
*/
  static String convertEncodedBytes(
    String charset,               // official Java character set name
    byte[] input,                 // an array of encoded bytes
    boolean relaxFlag)            // true if we relax rules for allowed chars
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    byte[] adjust;                // caller's byte array with nulls removed
    int from, to;                 // index variables
    int length;                   // size of byte array or character string
    String result;                // our converted result

    try
    {
      if (hexFlag)                // display all name fields in hexadecimal?
      {
        result = charset + " hex " + formatHexBytes(input);
      }
      else if (charset.equals("ASCII-Z"))
      {
        /* This is not a real character set.  We use this name for converting
        OS/2 vendor IDs from a 4-byte array to a 4-character string.  Printable
        ASCII text is accepted, and null bytes (0x00) are replaced with spaces.
        Any null bytes found are usually filler at the end of names with three
        printable characters or less, but we don't enforce any requirements for
        position or length. */

        buffer = new StringBuffer(); // empty string buffer for our result
        length = input.length;    // count number of original bytes
        result = null;            // mark final string result as not ready
        for (from = 0; from < length; from ++) // look at each original byte
        {
          ch = (char) input[from]; // get one input byte as a character
          if ((ch > 0x1F) && (ch < 0x7F))
            buffer.append(ch);    // accept only printable 7-bit characters
          else if (ch == 0x00)    // and convert null bytes to blank spaces
            buffer.append(' ');
          else                    // usually means an incorrect encoding
          {
            result = charset + " hex " + formatHexBytes(input);
            break;                // exit early from <for> loop
          }
        }
        if (result == null)       // if there was no conversion error
          result = buffer.toString(); // give caller our converted string
      }
      else if (charset.equals("MacRoman"))
      {
        /* MacRoman is officially an 8-bit character set.  Unfortunately, far
        too many East Asian fonts use their local encoding for the MacRoman
        font family name.  To recognize incorrect encodings, MacRoman strings
        are processed as strict US-ASCII if rules are not relaxed, and as real
        MacRoman with newlines if rules are relaxed. */

        if (relaxFlag)            // are we operating under relaxed rules?
        {
          length = input.length;  // count number of caller's original bytes
          adjust = new byte[length]; // allocate new array for removing nulls
          to = 0;                 // put next non-null byte at this index
          for (from = 0; from < length; from ++)
          {
            if ((input[from] != 0x00) && (input[from] != 0x7F)) // null, delete
              adjust[to ++] = input[from]; // keep everything else to convert
          }
          result = new String(adjust, 0, to, charset); // convert, may exception
          length = result.length(); // get resulting string length
          for (from = 0; from < length; from ++) // inspect converted characters
          {
            ch = result.charAt(from); // get one character from converted string
            if ((ch == REPLACE_CHAR) || (Character.isDefined(ch) == false)
              || (Character.isISOControl(ch) && (ch != '\n') && (ch != '\r')
                && (ch != '\t')))
            {
              result = charset + " hex " + formatHexBytes(input);
              break;              // exit early from <for> loop
            }
          }
        }
        else                      // apply strict 7-bit US-ASCII rules
        {
          buffer = new StringBuffer(); // empty string buffer for our result
          length = input.length;  // count number of original bytes
          result = null;          // mark final string result as not ready
          for (from = 0; from < length; from ++) // look at each original byte
          {
            ch = (char) input[from]; // get one input byte as a character
            if ((ch > 0x1F) && (ch < 0x7F))
              buffer.append(ch);  // accept only printable 7-bit characters
            else                  // usually means an incorrect encoding
            {
              result = charset + " hex " + formatHexBytes(input);
              break;              // exit early from <for> loop
            }
          }
          if (result == null)     // if there was no conversion error
            result = buffer.toString(); // give caller our converted string
        }
      }
      else if (charset.equals("US-ASCII"))
      {
        /* US-ASCII allows only printable 7-bit ASCII characters, also known as
        the "plain text" range of most character sets.  Newline characters will
        be accepted if rules are relaxed. */

        buffer = new StringBuffer(); // empty string buffer for our result
        length = input.length;    // count number of original bytes
        result = null;            // mark final string result as not ready
        for (from = 0; from < length; from ++) // look at each original byte
        {
          ch = (char) input[from]; // get one input byte as a character
          if ((ch > 0x1F) && (ch < 0x7F))
            buffer.append(ch);    // accept all printable 7-bit characters
          else if (relaxFlag && ((ch == 0x00) || (ch == 0x7F) || (ch == '\r')))
            { /* ignore null, delete, carriage return if relaxed rules */ }
          else if (relaxFlag && ((ch == '\n') || (ch == '\t')))
            buffer.append(ch);    // accept newline, tab if relaxed rules
          else                    // usually means an incorrect encoding
          {
            result = charset + " hex " + formatHexBytes(input);
            break;                // exit early from <for> loop
          }
        }
        if (result == null)       // if there was no conversion error
          result = buffer.toString(); // give caller our converted string
      }
      else if (charset.equals("UTF-16"))
      {
        /* Blindly convert 16-bit Unicode and inspect the result.  If Unicode
        0xFFFD replacement characters appear, replace our conversion with the
        original bytes in hexadecimal.  You can't remove null bytes (0x00) from
        true Unicode characters, which is why this is a separate case from most
        other character sets (see below). */

        result = new String(input, charset); // convert, may throw exception
        length = result.length(); // get resulting string length
        for (from = 0; from < length; from ++) // inspect converted characters
        {
          ch = result.charAt(from); // get one character from converted string
          if ((ch == REPLACE_CHAR) || (Character.isDefined(ch) == false)
            || (Character.isISOControl(ch) && !(relaxFlag && ((ch == '\n')
              || (ch == '\r') || (ch == '\t')))))
          {
            result = charset + " hex " + formatHexBytes(input);
            break;                // exit early from <for> loop
          }
        }
      }
      else
      {
        /* For all other character sets, remove null bytes.  Then convert to
        standard Java characters (Unicode) using the given encoding.  Inspect
        the result.  If Unicode 0xFFFD replacement characters appear, replace
        our conversion with the original bytes in hexadecimal. */

        length = input.length;    // count number of caller's original bytes
        adjust = new byte[length]; // allocate new array for removing nulls
        to = 0;                   // put next non-null byte at this index
        for (from = 0; from < length; from ++)
        {
          if ((input[from] != 0x00) && (input[from] != 0x7F)) // null, delete
            adjust[to ++] = input[from]; // keep everything else to convert
        }
        result = new String(adjust, 0, to, charset); // convert, may exception
        length = result.length(); // get resulting string length
        for (from = 0; from < length; from ++) // inspect converted characters
        {
          ch = result.charAt(from); // get one character from converted string
          if ((ch == REPLACE_CHAR) || (Character.isDefined(ch) == false)
            || (Character.isISOControl(ch) && !(relaxFlag && ((ch == '\n')
              || (ch == '\r') || (ch == '\t')))))
          {
            result = charset + " hex " + formatHexBytes(input);
                                  // bad character, get hex from original array
            break;                // exit early from <for> loop
          }
        }
      }
    }
    catch (UnsupportedEncodingException uee) // if unknown character set
    {
      result = charset + " hex " + formatHexBytes(input);
    }
    return(result);               // give caller whatever we could find

  } // end of convertEncodedBytes() method


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
    totalErrors = 0;              // total number of files with problems
    totalFiles = 0;               // total number of files found (good or bad)

    openFilesThread = new Thread(new FontNames3User(), "doOpenRunner");
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
      putOutput("", false);       // blank line
      putOutput(("Found " + prettyPlural(totalFiles, "file") + " with "
        + prettyPlural(totalErrors, "error") + "."), true);
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
    String name;                  // our name for the file
    int i;                        // index variable
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
      totalErrors ++;             // count bad file names as errors
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
    putOutput(text, true);        // by default, scroll output lines
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
    System.err.println("  java  FontNames3  [options]  [file or folder names]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -h0 = ignore hidden files or folders (default)");
    System.err.println("  -h1 = -h = process hidden files and folders");
    System.err.println("  -k0 = convert name fields to text characters, if possible (default)");
    System.err.println("  -k1 = -k = report all name fields in hexadecimal only, no text");
    System.err.println("  -n0 to -n" + (FIELD_COUNT - 1) + " = report name fields with given name ID numbers");
    System.err.println("  -n" + fieldUnknown + " = report all name fields with unknown name ID numbers");
    System.err.println("  -n" + fieldOs2ID + " = report OS/2 vendor ID");
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
  buttons, in the context of the main FontNames3 class.
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
    else if (source == hexCheckbox) // report all name fields in hexadecimal
    {
      hexFlag = hexCheckbox.isSelected();
    }
    else if (source == menuButton) // button for selecting name IDs to report
    {
      menuPopup.show(menuButton, 0, menuButton.getHeight());
    }

    /* The following are called when individual checkbox items are changed in
    the menu to select name IDs.  They are not called, directly or indirectly,
    when the "all" or "none" groups are chosen below. */

    else if (source == menuField00) // identical processing for numbered fields
      fieldFlags[ 0] = menuField00.isSelected();
    else if (source == menuField01)
      fieldFlags[ 1] = menuField01.isSelected(); // font family name (ID #1)
    else if (source == menuField02)
      fieldFlags[ 2] = menuField02.isSelected();
    else if (source == menuField03)
      fieldFlags[ 3] = menuField03.isSelected();
    else if (source == menuField04)
      fieldFlags[ 4] = menuField04.isSelected(); // font full name (name ID #4)
    else if (source == menuField05)
      fieldFlags[ 5] = menuField05.isSelected();
    else if (source == menuField06)
      fieldFlags[ 6] = menuField06.isSelected();
    else if (source == menuField07)
      fieldFlags[ 7] = menuField07.isSelected();
    else if (source == menuField08)
      fieldFlags[ 8] = menuField08.isSelected();
    else if (source == menuField09)
      fieldFlags[ 9] = menuField09.isSelected();
    else if (source == menuField10)
      fieldFlags[10] = menuField10.isSelected();
    else if (source == menuField11)
      fieldFlags[11] = menuField11.isSelected();
    else if (source == menuField12)
      fieldFlags[12] = menuField12.isSelected();
    else if (source == menuField13)
      fieldFlags[13] = menuField13.isSelected();
    else if (source == menuField14)
      fieldFlags[14] = menuField14.isSelected();
//  else if (source == menuField15)
//    fieldFlags[15] = menuField15.isSelected(); // reserved (name ID #15)
    else if (source == menuField16)
      fieldFlags[16] = menuField16.isSelected();
    else if (source == menuField17)
      fieldFlags[17] = menuField17.isSelected();
    else if (source == menuField18)
      fieldFlags[18] = menuField18.isSelected();
    else if (source == menuField19)
      fieldFlags[19] = menuField19.isSelected();
    else if (source == menuField20)
      fieldFlags[20] = menuField20.isSelected();
    else if (source == menuFieldOs2ID)
      fieldFlags[fieldOs2ID] = menuFieldOs2ID.isSelected();
    else if (source == menuFieldUnknown)
    {
      fieldFlags[fieldUnknown] = menuFieldUnknown.isSelected();
    }

    /* The following are called when group items ("all" or "none") are chosen
    in the menu to select name IDs.  Note that our calling setSelected() here
    does *not* invoke the action listener, and hence the individual check boxes
    above do not get called twice. */

    else if (source == menuLegalAll) // enable all "legal" name IDs
    {
      fieldFlags[ 0] = true; menuField00.setSelected(true);
      fieldFlags[ 7] = true; menuField07.setSelected(true);
      fieldFlags[13] = true; menuField13.setSelected(true);
      fieldFlags[14] = true; menuField14.setSelected(true);
    }
    else if (source == menuLegalNone) // disable all "legal" name IDs
    {
      fieldFlags[ 0] = false; menuField00.setSelected(false);
      fieldFlags[ 7] = false; menuField07.setSelected(false);
      fieldFlags[13] = false; menuField13.setSelected(false);
      fieldFlags[14] = false; menuField14.setSelected(false);
    }
    else if (source == menuMakerAll) // enable all designer/vendor name IDs
    {
      fieldFlags[ 8] = true; menuField08.setSelected(true);
      fieldFlags[ 9] = true; menuField09.setSelected(true);
      fieldFlags[11] = true; menuField11.setSelected(true);
      fieldFlags[12] = true; menuField12.setSelected(true);
      fieldFlags[fieldOs2ID] = true; menuFieldOs2ID.setSelected(true);
    }
    else if (source == menuMakerNone) // disable all designer/vendor name IDs
    {
      fieldFlags[ 8] = false; menuField08.setSelected(false);
      fieldFlags[ 9] = false; menuField09.setSelected(false);
      fieldFlags[11] = false; menuField11.setSelected(false);
      fieldFlags[12] = false; menuField12.setSelected(false);
      fieldFlags[fieldOs2ID] = false; menuFieldOs2ID.setSelected(false);
    }
    else if (source == menuNamesAll) // enable all detailed name IDs
    {
//    fieldFlags[ 1] = true; menuField01.setSelected(true); // font family name
      fieldFlags[ 2] = true; menuField02.setSelected(true);
      fieldFlags[ 3] = true; menuField03.setSelected(true);
      fieldFlags[ 4] = true; menuField04.setSelected(true); // font full name
      fieldFlags[ 6] = true; menuField06.setSelected(true);
      fieldFlags[16] = true; menuField16.setSelected(true);
      fieldFlags[17] = true; menuField17.setSelected(true);
      fieldFlags[18] = true; menuField18.setSelected(true);
      fieldFlags[20] = true; menuField20.setSelected(true);
    }
    else if (source == menuNamesNone) // disable all detailed name IDs
    {
//    fieldFlags[ 1] = false; menuField01.setSelected(false); // font family name
      fieldFlags[ 2] = false; menuField02.setSelected(false);
      fieldFlags[ 3] = false; menuField03.setSelected(false);
      fieldFlags[ 4] = false; menuField04.setSelected(false); // font full name
      fieldFlags[ 6] = false; menuField06.setSelected(false);
      fieldFlags[16] = false; menuField16.setSelected(false);
      fieldFlags[17] = false; menuField17.setSelected(false);
      fieldFlags[18] = false; menuField18.setSelected(false);
      fieldFlags[20] = false; menuField20.setSelected(false);
    }
    else if (source == menuOtherAll) // enable all miscellaneous name IDs
    {
      fieldFlags[ 5] = true; menuField05.setSelected(true);
      fieldFlags[10] = true; menuField10.setSelected(true);
      fieldFlags[19] = true; menuField19.setSelected(true);
      fieldFlags[fieldUnknown] = true; menuFieldUnknown.setSelected(true);
    }
    else if (source == menuOtherNone) // disable all miscellaneous name IDs
    {
      fieldFlags[ 5] = false; menuField05.setSelected(false);
      fieldFlags[10] = false; menuField10.setSelected(false);
      fieldFlags[19] = false; menuField19.setSelected(false);
      fieldFlags[fieldUnknown] = false; menuFieldUnknown.setSelected(false);
    }
    else if (source == menuPopupAll) // enable all name IDs available to user
    {
      fieldFlags[ 0] = true; menuField00.setSelected(true);
//    fieldFlags[ 1] = true; menuField01.setSelected(true); // font family name
      fieldFlags[ 2] = true; menuField02.setSelected(true);
      fieldFlags[ 3] = true; menuField03.setSelected(true);
      fieldFlags[ 4] = true; menuField04.setSelected(true); // font full name
      fieldFlags[ 5] = true; menuField05.setSelected(true);
      fieldFlags[ 6] = true; menuField06.setSelected(true);
      fieldFlags[ 7] = true; menuField07.setSelected(true);
      fieldFlags[ 8] = true; menuField08.setSelected(true);
      fieldFlags[ 9] = true; menuField09.setSelected(true);
      fieldFlags[10] = true; menuField10.setSelected(true);
      fieldFlags[11] = true; menuField11.setSelected(true);
      fieldFlags[12] = true; menuField12.setSelected(true);
      fieldFlags[13] = true; menuField13.setSelected(true);
      fieldFlags[14] = true; menuField14.setSelected(true);
//    fieldFlags[15] = true; menuField15.setSelected(true); // reserved (#15)
      fieldFlags[16] = true; menuField16.setSelected(true);
      fieldFlags[17] = true; menuField17.setSelected(true);
      fieldFlags[18] = true; menuField18.setSelected(true);
      fieldFlags[19] = true; menuField19.setSelected(true);
      fieldFlags[20] = true; menuField20.setSelected(true);
      fieldFlags[fieldOs2ID] = true; menuFieldOs2ID.setSelected(true);
      fieldFlags[fieldUnknown] = true; menuFieldUnknown.setSelected(true);
    }
    else if (source == menuPopupNone) // disable all name IDs available to user
    {
      fieldFlags[ 0] = false; menuField00.setSelected(false);
//    fieldFlags[ 1] = false; menuField01.setSelected(false); // font family name
      fieldFlags[ 2] = false; menuField02.setSelected(false);
      fieldFlags[ 3] = false; menuField03.setSelected(false);
      fieldFlags[ 4] = false; menuField04.setSelected(false); // font full name
      fieldFlags[ 5] = false; menuField05.setSelected(false);
      fieldFlags[ 6] = false; menuField06.setSelected(false);
      fieldFlags[ 7] = false; menuField07.setSelected(false);
      fieldFlags[ 8] = false; menuField08.setSelected(false);
      fieldFlags[ 9] = false; menuField09.setSelected(false);
      fieldFlags[10] = false; menuField10.setSelected(false);
      fieldFlags[11] = false; menuField11.setSelected(false);
      fieldFlags[12] = false; menuField12.setSelected(false);
      fieldFlags[13] = false; menuField13.setSelected(false);
      fieldFlags[14] = false; menuField14.setSelected(false);
//    fieldFlags[15] = false; menuField15.setSelected(false); // reserved (#15)
      fieldFlags[16] = false; menuField16.setSelected(false);
      fieldFlags[17] = false; menuField17.setSelected(false);
      fieldFlags[18] = false; menuField18.setSelected(false);
      fieldFlags[19] = false; menuField19.setSelected(false);
      fieldFlags[20] = false; menuField20.setSelected(false);
      fieldFlags[fieldOs2ID] = false; menuFieldOs2ID.setSelected(false);
      fieldFlags[fieldUnknown] = false; menuFieldUnknown.setSelected(false);
    }

    /* Continue with non-menu items in alphabetical order. */

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

} // end of FontNames3 class

// ------------------------------------------------------------------------- //

/*
  FontNames3Filter class

  This class limits the files shown in the file open dialog box to font files.
*/

class FontNames3Filter extends javax.swing.filechooser.FileFilter
                implements java.io.FileFilter // not the same as filechooser.*
{
  /* empty constructor */

  public FontNames3Filter() { }

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

} // end of FontNames3Filter class

// ------------------------------------------------------------------------- //

/*
  FontNames3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FontNames3User implements ActionListener, Runnable
{
  /* empty constructor */

  public FontNames3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FontNames3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FontNames3.doOpenRunner();
  }

} // end of FontNames3User class

/* Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License. */
