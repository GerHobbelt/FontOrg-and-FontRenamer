/*
  File Redate Rename #1 - Change File Directory Names or Modification Dates
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Monday, 14 April 2008
  Java class name: FileDateName1
  Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 graphical (GUI) application to rename multiple files or to
  change their directory dates.  The contents of the files are not changed.
  File names may be converted to all lowercase ("a happy dog.txt"), to all
  uppercase ("A HAPPY DOG.TXT"), or to title case ("A Happy Dog.txt").
  Leading, trailing, and multiple spaces may be removed.  Folders and
  subfolders may be searched recursively.  Changes may be applied to files
  only, both files and folders, or only the folders.  Hidden and read-only
  files won't be renamed or redated unless an option is given on the command
  line, and the action is permitted by both Java and the operating system.

  One word of caution: there is no "undo" feature.  Once you change a file date
  or name, the only way to restore the original date or name is to change the
  file date or name again.  Practice on copies of your files before you blindly
  apply this program to large folders.  You may also turn on the "simulate"
  option to see what would be changed, without actually making the changes.

  GNU General Public License (GPL)
  --------------------------------
  FileDateName1 is free software: you can redistribute it and/or modify it
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
  The Java command line may contain options for including hidden or read-only
  files, searching subfolders, and the size of the display font.  See the "-?"
  option for a help summary:

      java  FileDateName1  -?

  For information on Java regular expressions and back references, please see
  the following web page:

      http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.

  Restrictions and Limitations
  ----------------------------
  Daylight saving time (DST) is not properly accounted for on Microsoft Windows
  when setting times in a period of the year opposite to the current DST rules.
  Java is setting the correct time; Windows is being too helpful by adjusting
  the clock, and the effect varies with the underlying file system (FAT32,
  NTFS, etc).

  Suggestions for New Features
  ----------------------------
  (1) Add an "undo" feature.  KF, 2008-07-20.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // panel borders and decorations
import javax.swing.event.*;       // document listener for text fields

public class FileDateName1
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String GMT_ZONE = "GMT+00:00"; // name for GMT time zone
  static final int MAX_YEAR = 2099; // maximum year that we accept from user
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final int MIN_YEAR = 1981; // minimum year that we accept from user
  static final String PROGRAM_TITLE =
    "Change File Directory Names or Modification Dates - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available

  /* All file systems have limits on how accurately they store dates and times.
  Don't change file dates when the millisecond difference is too small.  This
  must be at least 2000 ms (2 seconds) for MS-DOS FAT16/FAT32 file systems. */

  static final long MILLI_FUZZ = 2000; // ignore time changes smaller than this

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JCheckBox caseCheckbox;  // graphical option for <caseFlag>
  static boolean caseFlag;        // true if uppercase/lowercase is significant
  static int changeCount;         // number of files or folders changed
  static boolean changeDateFlag;  // true if we change file modification dates
  static JRadioButton changeDateGmt, changeDateLocal, changeDateNone,
    changeDeleteSpace, changeFilesFolders, changeFilesOnly, changeFoldersOnly,
    changeNameCustom, changeNameLower, changeNameNone, changeNameReplace,
    changeNameTitle, changeNameUpper, changeTrimSpace; // radio button options
  static TreeMap convertMap;      // conversion map for file name characters
  static JButton customButton;    // graphical button for special requests
  static boolean customChangeFlag; // true if special requests for file name
  static boolean deleteSpaceFlag; // true if we remove all spaces from names
  static JButton exitButton;      // "Exit" button for ending this application
  static int failCount;           // number of failures to change files/folders
  static boolean fileChangeFlag;  // true if we change file objects
  static JFileChooser fileChooser; // asks for input and output file names
  static int fileCount;           // total number of files found
  static boolean folderChangeFlag; // true if we change folder objects
  static int folderCount;         // total number of folders found
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static NumberFormat formatTwo;  // formats with two integer digits
  static Calendar gmtCalendar;    // for setting dates/times in GMT time zone
  static SimpleDateFormat gmtDateFormat; // format date/time in GMT time zone
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static Calendar localCalendar;  // for setting dates/times in local time zone
  static SimpleDateFormat localDateFormat; // format date in local time zone
  static boolean lowerChangeFlag; // true if we change file names to lowercase
  static JFrame mainFrame;        // this application's GUI window
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report while opening files
  static boolean readonlyFlag;    // true if we try to change read-only files
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we search folders and subfolders
  static JCheckBox regexCheckbox; // graphical option for <regexFlag>
  static boolean regexFlag;       // true if replace uses regular expressions
  static boolean replaceFlag;     // true if we search and replace name text
  static JTextField replaceNewDialog, replaceOldDialog; // name search and replace
  static String replaceNewString; // replacement string for <replaceOldPattern>
  static Pattern replaceOldPattern; // compiled regular expression for searching
  static boolean showCustomOptions; // true to show custom renaming options
  static JCheckBox simulateCheckbox; // graphical option for <simulateFlag>
  static boolean simulateFlag;    // true if we only show what would change
  static boolean titleChangeFlag; // true if we change file names to title case
  static boolean trimSpaceFlag;   // true if we remove extra spaces from names
  static boolean upperChangeFlag; // true if we change file names to uppercase
  static Calendar userCalendar;   // date/time calendar selected by user
  static SimpleDateFormat userDateFormat; // date/time format selected by user
  static JTextField userDay, userHour, userMinute, userMonth, userSecond,
    userYear;                     // input fields for numeric date and time
  static long userGmtMillis;      // user's date/time in Java GMT milliseconds

/*
  main() method

  We run as a graphical application only.  Set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    Insets textMargins;           // input margins: top, left, bottom, right
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    caseFlag = true;              // by default, uppercase lowercase different
    convertMap = new TreeMap();   // create empty mapping for char conversions
    hiddenFlag = false;           // by default, don't process hidden files
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    readonlyFlag = false;         // by default, don't change read-only files
    recurseFlag = false;          // by default, don't search subfolders
    regexFlag = false;            // by default, don't use regular expressions
    showCustomOptions = false;    // set true to show custom renaming options
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize calendars for the GMT time zone and the local time zone. */

    TimeZone gmtZone = TimeZone.getTimeZone(GMT_ZONE);
    gmtCalendar = Calendar.getInstance(gmtZone); // GMT zone, default locale
    gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");
    gmtDateFormat.setTimeZone(gmtZone);
    localCalendar = Calendar.getInstance(); // default time zone and locale
    localDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatTwo = NumberFormat.getInstance();
    formatTwo.setMinimumIntegerDigits(2); // two digits, leading zero

    /* Check command-line parameters for options. */

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

      else if (word.equals("-c") || (mswinFlag && word.equals("/c"))
        || word.equals("-c1") || (mswinFlag && word.equals("/c1")))
      {
        caseFlag = true;          // uppercase and lowercase are different
      }
      else if (word.equals("-c0") || (mswinFlag && word.equals("/c0")))
        caseFlag = false;         // consider uppercase and lowercase equal

      else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
      {
        hiddenFlag = true;        // process hidden files and folders
      }
      else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
        hiddenFlag = false;       // ignore hidden files or subfolders

      else if (word.equals("-m") || (mswinFlag && word.equals("/m"))
        || word.equals("-m1") || (mswinFlag && word.equals("/m1")))
      {
        showCustomOptions = true; // let user see custom renaming options
      }
      else if (word.equals("-m0") || (mswinFlag && word.equals("/m0")))
        showCustomOptions = false; // don't show custom renaming options

      else if (word.equals("-r") || (mswinFlag && word.equals("/r"))
        || word.equals("-r1") || (mswinFlag && word.equals("/r1")))
      {
        readonlyFlag = true;      // change read-only files if permitted
      }
      else if (word.equals("-r0") || (mswinFlag && word.equals("/r0")))
        readonlyFlag = false;     // don't try to change read-only files

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

      else                        // parameter is not a recognized option
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(-1);          // exit application after printing help
      }
    }

    /* Open the graphical user interface (GUI).  The standard Java style is the
    most reliable, but you can switch to something closer to the local system,
    if you want. */

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

    action = new FileDateName1User(); // create our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser
    textMargins = new Insets(1, 3, 2, 3); // top, left, bottom, right

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel01, panel02, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Options for changing the file directory name. */

    JPanel panel11 = new JPanel();
    panel11.setBorder(BorderFactory.createTitledBorder(null,
      " Change File Directory Name ", TitledBorder.DEFAULT_JUSTIFICATION,
      TitledBorder.DEFAULT_POSITION, buttonFont));
    panel11.setLayout(new BoxLayout(panel11, BoxLayout.Y_AXIS));

    JPanel panel12 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    ButtonGroup group01 = new ButtonGroup();
    changeNameNone = new JRadioButton("no change");
    if (buttonFont != null) changeNameNone.setFont(buttonFont);
    changeNameNone.setToolTipText("Do not change file directory name.");
    group01.add(changeNameNone);
    panel12.add(changeNameNone);
    changeTrimSpace = new JRadioButton("remove extra spaces");
    if (buttonFont != null) changeTrimSpace.setFont(buttonFont);
    changeTrimSpace.setToolTipText(
      "Select to remove leading, trailing, multiple spaces.");
    group01.add(changeTrimSpace);
    panel12.add(changeTrimSpace);
    changeDeleteSpace = new JRadioButton("delete all spaces");
    if (buttonFont != null) changeDeleteSpace.setFont(buttonFont);
    changeDeleteSpace.setToolTipText(
      "Select to remove all white space from names.");
    group01.add(changeDeleteSpace);
    panel12.add(changeDeleteSpace);
    panel11.add(panel12);

    JPanel panel13 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    changeNameLower = new JRadioButton("convert to lowercase");
    if (buttonFont != null) changeNameLower.setFont(buttonFont);
    changeNameLower.setToolTipText(
      "Change file name to all lowercase (little letters).");
    group01.add(changeNameLower);
    panel13.add(changeNameLower);
    changeNameUpper = new JRadioButton("UPPERCASE");
    if (buttonFont != null) changeNameUpper.setFont(buttonFont);
    changeNameUpper.setToolTipText(
      "Change file name to all uppercase (capital letters).");
    group01.add(changeNameUpper);
    panel13.add(changeNameUpper);
    changeNameTitle = new JRadioButton("Title Case");
    if (buttonFont != null) changeNameTitle.setFont(buttonFont);
    changeNameTitle.setToolTipText(
      "Change file name to title case (capitalize words).");
    group01.add(changeNameTitle);
    panel13.add(changeNameTitle);
    panel11.add(panel13);

    JPanel panel14 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    changeNameReplace = new JRadioButton("replace");
    changeNameReplace.addActionListener(action);
    if (buttonFont != null) changeNameReplace.setFont(buttonFont);
    changeNameReplace.setToolTipText(
      "Match string (or regular expression), replace with new text.");
    group01.add(changeNameReplace);
    panel14.add(changeNameReplace);
    replaceOldDialog = new JTextField(10);
    replaceOldDialog.addFocusListener((FocusListener) action);
    replaceOldDialog.getDocument().addDocumentListener((DocumentListener)
      action);
    if (buttonFont != null) replaceOldDialog.setFont(buttonFont);
    replaceOldDialog.setMargin(textMargins);
    panel14.add(replaceOldDialog);
    JLabel label01 = new JLabel("with");
    if (buttonFont != null) label01.setFont(buttonFont);
    panel14.add(label01);
    replaceNewDialog = new JTextField(10);
    replaceNewDialog.addFocusListener((FocusListener) action);
    replaceNewDialog.getDocument().addDocumentListener((DocumentListener)
      action);
    if (buttonFont != null) replaceNewDialog.setFont(buttonFont);
    replaceNewDialog.setMargin(textMargins);
    panel14.add(replaceNewDialog);
    panel11.add(panel14);

    JPanel panel15 = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 0));
    JLabel label02 = new JLabel("where replace uses: ");
    if (buttonFont != null) label02.setFont(buttonFont);
    panel15.add(label02);
    caseCheckbox = new JCheckBox("exact case", caseFlag);
    if (buttonFont != null) caseCheckbox.setFont(buttonFont);
    caseCheckbox.setToolTipText(
      "Select if uppercase, lowercase are different.");
    panel15.add(caseCheckbox);
    regexCheckbox = new JCheckBox("regular expression", regexFlag);
    if (buttonFont != null) regexCheckbox.setFont(buttonFont);
    regexCheckbox.setToolTipText(
      "Select if replace uses Java regular expression.");
    panel15.add(regexCheckbox);
    panel11.add(panel15);         // some users don't want to see these options

    JPanel panel16 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    changeNameCustom = new JRadioButton(
      "convert characters using table from file");
    if (buttonFont != null) changeNameCustom.setFont(buttonFont);
    changeNameCustom.setToolTipText("Translate characters in file name.");
    group01.add(changeNameCustom);
    panel16.add(changeNameCustom);
    customButton = new JButton("Read Table...");
    customButton.addActionListener(action);
    if (buttonFont != null) customButton.setFont(buttonFont);
    customButton.setMnemonic(KeyEvent.VK_T);
    customButton.setToolTipText("Read character conversion table from file.");
    panel16.add(customButton);
    if (showCustomOptions) panel11.add(panel16); // also needs customFilename()

    changeNameNone.setSelected(true); // set default choice (radio button)
    panel01.add(panel11);
    panel01.add(Box.createVerticalStrut(5)); // space between panels

    /* Options for changing the file modification date. */

    JPanel panel21 = new JPanel();
    panel21.setBorder(BorderFactory.createTitledBorder(null,
      " Change File Modification Date ", TitledBorder.DEFAULT_JUSTIFICATION,
      TitledBorder.DEFAULT_POSITION, buttonFont));
    panel21.setLayout(new BoxLayout(panel21, BoxLayout.Y_AXIS));

    JPanel panel22 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    ButtonGroup group02 = new ButtonGroup();
    changeDateNone = new JRadioButton("no change");
    if (buttonFont != null) changeDateNone.setFont(buttonFont);
    changeDateNone.setToolTipText("Do not change file modification date.");
    group02.add(changeDateNone);
    panel22.add(changeDateNone);
    changeDateLocal = new JRadioButton("set local time");
    changeDateLocal.addActionListener(action);
    if (buttonFont != null) changeDateLocal.setFont(buttonFont);
    changeDateLocal.setToolTipText("Following date is local time zone.");
    group02.add(changeDateLocal);
    panel22.add(changeDateLocal);
    changeDateGmt = new JRadioButton("GMT time zone");
    changeDateGmt.addActionListener(action);
    if (buttonFont != null) changeDateGmt.setFont(buttonFont);
    changeDateGmt.setToolTipText("Following date is GMT time zone.");
    group02.add(changeDateGmt);
    panel22.add(changeDateGmt);
    changeDateNone.setSelected(true); // set default choice (radio button)
    panel21.add(panel22);

    JPanel panel23 = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    JLabel label03 = new JLabel("year (4 digits) - month - day  ");
    if (buttonFont != null) label03.setFont(buttonFont);
    panel23.add(label03);
    userYear = new JTextField(4);
    userYear.addFocusListener((FocusListener) action);
    if (buttonFont != null) userYear.setFont(buttonFont);
    userYear.setMargin(textMargins);
    panel23.add(userYear);
    JLabel label04 = new JLabel("-"); // numeric date separator
    if (buttonFont != null) label04.setFont(buttonFont);
    panel23.add(label04);
    userMonth = new JTextField(2);
    userMonth.addFocusListener((FocusListener) action);
    if (buttonFont != null) userMonth.setFont(buttonFont);
    userMonth.setMargin(textMargins);
    panel23.add(userMonth);
    JLabel label05 = new JLabel("-");
    if (buttonFont != null) label05.setFont(buttonFont);
    panel23.add(label05);
    userDay = new JTextField(2);
    userDay.addFocusListener((FocusListener) action);
    if (buttonFont != null) userDay.setFont(buttonFont);
    userDay.setMargin(textMargins);
    panel23.add(userDay);
    panel21.add(panel23);

    JPanel panel24 = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    JLabel label06 = new JLabel("hour (0 to 23) : minute : second  ");
    if (buttonFont != null) label06.setFont(buttonFont);
    panel24.add(label06);
    userHour = new JTextField(2);
    userHour.addFocusListener((FocusListener) action);
    if (buttonFont != null) userHour.setFont(buttonFont);
    userHour.setMargin(textMargins);
    panel24.add(userHour);
    JLabel label07 = new JLabel(":"); // numeric time separator
    if (buttonFont != null) label07.setFont(buttonFont);
    panel24.add(label07);
    userMinute = new JTextField(2);
    userMinute.addFocusListener((FocusListener) action);
    if (buttonFont != null) userMinute.setFont(buttonFont);
    userMinute.setMargin(textMargins);
    panel24.add(userMinute);
    JLabel label08 = new JLabel(":");
    if (buttonFont != null) label08.setFont(buttonFont);
    panel24.add(label08);
    userSecond = new JTextField(2);
    userSecond.addFocusListener((FocusListener) action);
    if (buttonFont != null) userSecond.setFont(buttonFont);
    userSecond.setMargin(textMargins);
    panel24.add(userSecond);
    panel21.add(panel24);

    panel01.add(panel21);
    panel01.add(Box.createVerticalStrut(5)); // space between panels

    /* Miscellaneous options. */

    JPanel panel31 = new JPanel();
    panel31.setBorder(BorderFactory.createTitledBorder(null,
      " Search Options ", TitledBorder.DEFAULT_JUSTIFICATION,
      TitledBorder.DEFAULT_POSITION, buttonFont));
    panel31.setLayout(new BoxLayout(panel31, BoxLayout.Y_AXIS));

    JPanel panel32 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    JLabel label09 = new JLabel("change: ");
    if (buttonFont != null) label09.setFont(buttonFont);
    panel32.add(label09);
    ButtonGroup group03 = new ButtonGroup();
    changeFilesOnly = new JRadioButton("files only");
    if (buttonFont != null) changeFilesOnly.setFont(buttonFont);
    changeFilesOnly.setToolTipText("Only change files, no folders.");
    group03.add(changeFilesOnly);
    panel32.add(changeFilesOnly);
    changeFilesFolders = new JRadioButton("files and folders");
    if (buttonFont != null) changeFilesFolders.setFont(buttonFont);
    changeFilesFolders.setToolTipText("Change all files and folders.");
    group03.add(changeFilesFolders);
    panel32.add(changeFilesFolders);
    changeFoldersOnly = new JRadioButton("folders only");
    if (buttonFont != null) changeFoldersOnly.setFont(buttonFont);
    changeFoldersOnly.setToolTipText("Only change folders, no files.");
    group03.add(changeFoldersOnly);
    panel32.add(changeFoldersOnly);
    changeFilesOnly.setSelected(true); // set default choice (radio button)
    panel31.add(panel32);

    JPanel panel33 = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 0));
    recurseCheckbox = new JCheckBox("search folders, subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    panel33.add(recurseCheckbox);
    simulateCheckbox = new JCheckBox("simulate changes", false);
    if (buttonFont != null) simulateCheckbox.setFont(buttonFont);
    simulateCheckbox.setToolTipText("Select to scan without making changes.");
    panel33.add(simulateCheckbox);
    panel31.add(panel33);

    panel01.add(panel31);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for the action buttons. */

    JPanel panel41 = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));

    openButton = new JButton("Open File/Folder...");
    openButton.addActionListener(action);
    if (buttonFont != null) openButton.setFont(buttonFont);
    openButton.setMnemonic(KeyEvent.VK_O);
    openButton.setToolTipText("Start finding/opening files.");
    panel41.add(openButton);

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding/opening files.");
    panel41.add(cancelButton);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel41.add(exitButton);

    panel01.add(panel41);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(10, 40);
    outputText.setEditable(false); // user can't change this text area
    if (buttonFont != null) outputText.setFont(buttonFont);
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setText(
      "\nChange file directory names or file modification dates."
      + "\nChoose your options; then open files or folders to search.\n\n"
      + COPYRIGHT_NOTICE + "\n");

    /* Combine buttons/options with output text.  Let the text area expand and
    contract with the window size. */

    JPanel panel42 = new JPanel(new BorderLayout(0, 0));
    panel42.add(panel01, BorderLayout.NORTH); // buttons and options
    panel42.add(new JScrollPane(outputText), BorderLayout.CENTER); // text area

    /* Create the main window frame for this application.  We supply our own
    margins using the edges of the frame's border layout. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel43 = mainFrame.getContentPane(); // where content meets frame
    panel43.setLayout(new BorderLayout(0, 0));
    panel43.add(Box.createVerticalStrut(10), BorderLayout.NORTH); // top margin
    panel43.add(Box.createHorizontalStrut(5), BorderLayout.WEST); // left
    panel43.add(panel42, BorderLayout.CENTER); // actual content in center
    panel43.add(Box.createHorizontalStrut(5), BorderLayout.EAST); // right
    panel43.add(Box.createVerticalStrut(5), BorderLayout.SOUTH); // bottom

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Supply default values for the numeric date and time, and start listening
    to any changes. */

    userYear.setText(String.valueOf(localCalendar.get(Calendar.YEAR)));
    userYear.getDocument().addDocumentListener((DocumentListener) action);
    userMonth.setText(formatTwo.format(1 + localCalendar.get(Calendar.MONTH)));
    userMonth.getDocument().addDocumentListener((DocumentListener) action);
    userDay.setText(formatTwo.format(localCalendar.get(Calendar.DAY_OF_MONTH)));
    userDay.getDocument().addDocumentListener((DocumentListener) action);
    userHour.setText("12");       // default time is today at noon
    userHour.getDocument().addDocumentListener((DocumentListener) action);
    userMinute.setText("00");
    userMinute.getDocument().addDocumentListener((DocumentListener) action);
    userSecond.setText("00");
    userSecond.getDocument().addDocumentListener((DocumentListener) action);

    /* Let the graphical interface run the application now. */

    openButton.requestFocusInWindow(); // give keyboard focus to "Open" button

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  convertPlainReplace() method

  Convert plain text into a replacement string that can be used with the
  Matcher.appendReplacement() method.  See the following references:

  http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Matcher.html
  http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html
*/
  static String convertPlainReplace(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get size of caller's string in characters
    for (i = 0; i < length; i ++) // do all characters in caller's string
    {
      ch = text.charAt(i);        // get one character from caller's string
      if (ch == '$')              // only one special during replacement
        buffer.append('\\');      // escape this special character
      buffer.append(ch);          // and append the original character
    }
    return(buffer.toString());    // give caller our converted string

  } // end of convertPlainReplace() method


/*
  convertPlainSearch() method

  Convert plain text into an equivalent regular expression.  This allows us to
  search for plain text with the same algorithm as regular expressions -- and
  isn't much slower.
*/
  static String convertPlainSearch(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get size of caller's string in characters
    for (i = 0; i < length; i ++) // do all characters in caller's string
    {
      ch = text.charAt(i);        // get one character from caller's string
      if ((ch == '$') || (ch == '(') || (ch == ')') || (ch == '*')
        || (ch == '+') || (ch == '.') || (ch == '?') || (ch == '[')
        || (ch == '\\') || (ch == ']') || (ch == '^') || (ch == '{')
        || (ch == '|') || (ch == '}'))
      {
        buffer.append('\\');      // escape this special character
      }
      buffer.append(ch);          // and append the original character
    }
    return(buffer.toString());    // give caller our converted string

  } // end of convertPlainSearch() method


/*
  customFilename() method

  To make it easy to add one special request for renaming files, there is a
  "custom" option that is normally hidden.  Changes should be implemented in
  this method, and additional buttons or options may be added to the "custom"
  line in the GUI layout.

  The example allows each character in a file name to be converted to a
  different sequence of zero or more characters, or for the original characters
  to remain unchanged.  Similar, more sophisticated code can be obtained from
  the "Plain Text" Java application.
*/
  static String customFilename(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters
    String replace;               // replacement string for one character

    /* Convert (translate) characters, according to user's configuration. */

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get size of input string in characters
    for (i = 0; i < length; i ++)
    {
      ch = text.charAt(i);        // get one character from input string
      replace = (String) convertMap.get(new Integer(ch));
      if (replace == null)        // is there a translation for this character?
        buffer.append(ch);        // no, use the original character
      else
        buffer.append(replace);   // yes, use the character's translated string
    }
    return(buffer.toString());    // give caller our converted string

  } // end of customFilename() method


/*
  deleteSpaces() method

  Remove all white space from a string: blanks, tabs, newlines, and anything
  that Unicode considers to be a spacing character.
*/
  static String deleteSpaces(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from caller's string
    int i;                        // index variable
    int length;                   // size of caller's string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get size of caller's string in characters
    for (i = 0; i < length; i ++) // do all characters in caller's string
    {
      ch = text.charAt(i);        // get one character from caller's string
      if (Character.isWhitespace(ch) == false) // ignore all Unicode spaces
        buffer.append(ch);        // append printable character to the result
    }
    return(buffer.toString());    // give caller our converted string

  } // end of deleteSpaces() method


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
    putOutput("Cancelled by user.", true); // print message and scroll
  }


/*
  doCustomButton() method

  This method is invoked when the user clicks on the <customButton> object.  It
  only has meaning when associated with the customFilename() method.

  The example loads a character-by-character conversion table from a text file
  chosen by the user.  As mentioned in comments for customFilename(), more
  sophisticated code can be obtained from the "Plain Text" Java application.
*/
  static void doCustomButton()
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input line
    Pattern convertPattern;       // compiled regular expression
    int i;                        // index variable
    BufferedReader inputFile;     // input character stream from text file
    int length;                   // size of a string in characters
    Matcher matcher;              // pattern matcher for regular expression
    String text;                  // one input line from file, or otherwise
    String word;                  // first command word on input line

    /* Ask the user for an input file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle(
      "Read Conversion Table in ASCII or UTF-8 Format...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box

    /* Open and read lines from the configuration data file. */

    buffer = new StringBuffer();  // re-use same string buffer for each line
    convertMap = new TreeMap();   // create empty mapping for char conversions
    convertPattern = Pattern.compile(
      "\\s*[Uu]\\+([0-9A-Fa-f]+)\\s*=\\s*\"([^\"]*)\"(?:\\s+#.*)?\\s*");

    try                           // catch specific and general I/O errors
    {
      inputFile = new BufferedReader(new InputStreamReader(new
        FileInputStream(fileChooser.getSelectedFile()), "UTF-8"));
                                  // open and read UTF-8 encoded text file
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
          /* Ignore "accept" commands from a "Font Rename" (Java) configuration
          file, so that we can use the same file with the same conversion table
          for this application too. */
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
                putOutput("Duplicate character conversion: " + text);
              else
                convertMap.put(key, matcher.group(2)); // save substitution
            }
            else                  // character number can't be Unicode
              putOutput("Invalid character convert value: " + text);
          }
          else
            putOutput("Invalid character convert syntax: " + text);
        }
        else if (word.length() > 0)
          putOutput("Unknown configuration command: " + text);
        else if ((i < length) && (text.charAt(i) != '#'))
          putOutput("Invalid configuration comment: " + text);
      }
      inputFile.close();          // try to close input file
    }

    catch (IOException ioe)       // for all file I/O errors
    {
      putOutput("Can't read from text file: " + ioe.getMessage());
    }

    /* Tell the user what we found. */

    putOutput(("Read " + prettyPlural(convertMap.size(), "entry", "entries")
      + " for the character conversion table."), true); // with scroll
    if (convertMap.size() > 0)    // were any conversion entries defined?
      changeNameCustom.setSelected(true); // set "custom" as renaming choice

  } // end of doCustomButton() method


/*
  doOpenButton() method

  Allow the user to select one or more files or folders for processing.
*/
  static void doOpenButton()
  {
    /* Eliminate the nonsensical case where nothing is to be changed. */

    if (changeDateNone.isSelected() && changeNameNone.isSelected()
      && (simulateCheckbox.isSelected() == false)
      && (JOptionPane.showConfirmDialog(mainFrame,
        "Do you really want to scan for files\nwithout changing dates or names?")
        != JOptionPane.YES_OPTION))
    {
      return;                     // user was mistaken, so do nothing
    }

    /* Decide how to change file names, if names will be changed. */

    caseFlag = caseCheckbox.isSelected();
    customChangeFlag = changeNameCustom.isSelected();
    deleteSpaceFlag = changeDeleteSpace.isSelected();
    lowerChangeFlag = changeNameLower.isSelected();
    regexFlag = regexCheckbox.isSelected();
    replaceFlag = changeNameReplace.isSelected();
    titleChangeFlag = changeNameTitle.isSelected();
    trimSpaceFlag = changeTrimSpace.isSelected();
    upperChangeFlag = changeNameUpper.isSelected();

    /* Decide if we change file objects, folder objects, or both. */

    fileChangeFlag = changeFilesFolders.isSelected()
      || changeFilesOnly.isSelected();
    folderChangeFlag = changeFilesFolders.isSelected()
      || changeFoldersOnly.isSelected();
    recurseFlag = recurseCheckbox.isSelected();
    simulateFlag = simulateCheckbox.isSelected();

    /* Convert the user's search-and-replace strings, as necessary. */

    if (changeNameReplace.isSelected() == false) // using this feature?
    {
      replaceNewString = null;    // no, disable replacement string
      replaceOldPattern = null;   // and disable search pattern
    }
    else if (replaceOldDialog.getText().length() == 0)
    {
      JOptionPane.showMessageDialog(mainFrame, (regexFlag
        ? "Search regular expression can not be empty."
        : "Search string can not be empty."));
      return;                     // user was mistaken, so do nothing
    }
    else try
    {
      replaceNewString = replaceNewDialog.getText();
      String search = replaceOldDialog.getText(); // assume regular expressions
      if (regexFlag == false)     // if using plain text search and replace
      {
        replaceNewString = convertPlainReplace(replaceNewString);
        search = convertPlainSearch(search); // get equivalent expressions
      }
      replaceOldPattern = Pattern.compile(search, (caseFlag ? 0
        : Pattern.CASE_INSENSITIVE));
    }
    catch (PatternSyntaxException pse) // syntax error for regular expression
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Unable to compile regular expression:\n" + pse.getMessage());
      return;                     // user was mistaken, so do nothing
    }

    /* Convert the user's time stamp, if file dates will be changed. */

    changeDateFlag = changeDateNone.isSelected() == false;
    if (changeDateFlag)           // are we changing file dates/times?
    {
      int year, month, day, hour, minute, second; // date and time fields
      try                         // try to parse user's input as integers
      {
        year = Integer.parseInt(userYear.getText());
        month = Integer.parseInt(userMonth.getText());
        day = Integer.parseInt(userDay.getText());
        hour = Integer.parseInt(userHour.getText());
        minute = Integer.parseInt(userMinute.getText());
        second = Integer.parseInt(userSecond.getText());
      }
      catch (NumberFormatException nfe) // if not a number or bad syntax
      {
        year = month = day = hour = minute = second = -1; // mark as invalid
      }

      /* Check approximate range of date and time values.  MS-DOS file systems
      (FAT16, FAT32) start in 1980, making 1981 the safest minimum year. */

      if ((year < MIN_YEAR) || (year > MAX_YEAR) || (month < 1) || (month > 12)
        || (day < 1) || (day > 31) || (hour < 0) || (hour > 23)
        || (minute < 0) || (minute > 59) || (second < 0) || (second > 59))
      {
        JOptionPane.showMessageDialog(mainFrame,
          "Date has 4-digit year from " + MIN_YEAR + " to " + MAX_YEAR
          + ".\nTime has 24-hour clock from 0 to 23 hours.\nExample: 1999-12-31 23:59:59");
        return;                   // don't accept incorrect dates
      }

      /* Convert numeric date and time to Java's GMT milliseconds. */

      if (changeDateGmt.isSelected()) // which time zone are we using?
      {
        userCalendar = gmtCalendar; // set calendar to GMT time zone
        userDateFormat = gmtDateFormat; // with date/time formatting to match
      }
      else
      {
        userCalendar = localCalendar; // set calendar to local time zone
        userDateFormat = localDateFormat; // with date/time formatting to match
      }
      userCalendar.clear();       // clear all fields, including initial date
      userCalendar.set(year, (month - 1), day, hour, minute, second);
      if (day != userCalendar.get(Calendar.DAY_OF_MONTH))
      {
        JOptionPane.showMessageDialog(mainFrame,
          ("That date would actually be " + userCalendar.get(Calendar.YEAR)
          + "-" + formatTwo.format(1 + userCalendar.get(Calendar.MONTH)) + "-"
          + formatTwo.format(userCalendar.get(Calendar.DAY_OF_MONTH)) + "."));
        return;                   // don't accept incorrect dates
      }
      userGmtMillis = userCalendar.getTimeInMillis(); // get GMT milliseconds

      /* Microsoft Windows 2000/XP adjusts all file dates and times using the
      current rules for daylight saving time, no matter which rules should be
      applied at that actual date and time.  Correcting for this assumption is
      almost impossible because both Java and Windows think they are in charge
      of time zones and DST.  The following code only does local times, and is
      quite likely to break if either the JRE or Windows changes.  In fact, I
      can't decide which is worse: this code or no code at all. */

      if (mswinFlag && (changeDateGmt.isSelected() == false))
      {                           // do we correct for Windows assumptions?
        TimeZone tz = localCalendar.getTimeZone(); // get local time zone
        userGmtMillis += tz.getOffset(userGmtMillis) // adjust for difference
          - tz.getOffset((new Date()).getTime()); // in daylight saving time
      }
    }
    else
      userGmtMillis = -1;         // invalidate the user's date and time

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
    changeCount = failCount = fileCount = folderCount = 0; // nothing found yet
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area

    openFilesThread = new Thread(new FileDateName1User(), "doOpenRunner");
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
    valid file names. */

    for (i = 0; i < openFileList.length; i ++)
    {
      if (cancelFlag) break;      // exit from <for> loop if user cancelled
      processFileOrFolder(openFileList[i]); // process this file or folder
    }

    /* Print a summary and scroll the output, even if we were cancelled. */

    putOutput(("Found " + prettyPlural(fileCount, "file") + " and "
      + prettyPlural(folderCount, "folder") + " with "
      + prettyPlural(changeCount, "change") + " and "
      + prettyPlural(failCount, "failure") + "."), true);

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button
    openButton.setEnabled(true);  // enable "Open" button

  } // end of doOpenRunner() method


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
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  Search all files.  Get folder contents and process each file
  found, doing subfolders only if the <recurseFlag> is true.
*/
  static void processFileOrFolder(File givenFile)
  {
    File canon;                   // full directory resolution of <givenFile>
    File[] contents;              // contents if <givenFile> is a folder
    String filename;              // caller's file name only, without path
    String filepath;              // name of caller's file, including path
    int i;                        // index variable
    File newfile;                 // renamed file object for <givenFile>
    String newname;               // new file or folder name
    File next;                    // next File object from <contents>
    long oldstamp;                // existing file modification date and time

    if (cancelFlag) return;       // stop if user hit the panic button

    /* Go through some trouble to get the exact file name.  The name that the
    user types on a command line or dialog box may be in all lowercase, for
    example, when the real file name is in mixed case ("arial.ttf" instead of
    "Arial.ttf").  This happens on Windows, where case is not important in file
    names, but will be for us later when we compare old and new names. */

    try                           // catch I/O errors during directory search
    {
      canon = givenFile.getCanonicalFile(); // do full directory resolution
      filename = canon.getName(); // get the exact file name
      filepath = canon.getPath(); // get file name with path
    }
    catch (IOException ioe)       // if the system couldn't handle file name
    {
      filename = givenFile.getName(); // accept abstract file name (no errors)
      filepath = givenFile.getPath(); // accept abstract file path (no errors)
    }

    /* Decide what kind of File object this is, if it's even real!  We process
    all files/folders given to us, no matter whether they are hidden or not.
    It's only when we look at subfolders that we pay attention to <hiddenFlag>
    and <recurseFlag>. */

    if (givenFile.isDirectory())  // is this a folder?
    {
      folderCount ++;             // found one more folder (directory)
      if (recurseFlag || (folderChangeFlag == false)) // search subfolders?
      {
        putOutput("Searching folder " + filepath);
        contents = sortFileList(givenFile.listFiles()); // sorted, no filter
        for (i = 0; i < contents.length; i ++) // for each file in order
        {
          if (cancelFlag) return; // stop if user hit the panic button
          next = contents[i];     // get next File object from <contents>
          if (next.isHidden() && (hiddenFlag == false))
          {
            putOutput(selectName(next.getName(), next.getPath())
              + " - ignoring hidden file or folder");
          }
          else if (next.isDirectory())
          {
            if (recurseFlag)      // should we look at subfolders?
              processFileOrFolder(next); // yes, search this subfolder
            else
              putOutput(selectName(next.getName(), next.getPath())
                + " - ignoring subfolder");
          }
          else if (next.isFile())
          {
            processFileOrFolder(next); // do all files (that exist)
          }
          else
          {
            /* File or folder does not exist.  Ignore it without comment. */
          }
        }
      }
      if (folderChangeFlag == false) // should we change this folder?
        return;                   // no, nothing more to do
    }
    else if (givenFile.isFile())  // is this a file?
    {
      fileCount ++;               // found one more file
      if (fileChangeFlag == false) // should we change this file?
        return;                   // no, nothing more to do
    }
    else                          // not a file, not a folder: doesn't exist
    {
      putOutput(selectName(filename, filepath) + " - not a file or folder");
      return;                     // nothing more to do
    }

    /* We have a genuine file or folder that is to be changed.  One nice thing
    about Java is that we don't need to know which it is: file or folder. */

    if (changeDateFlag)           // are we changing file modification dates?
    {
      oldstamp = givenFile.lastModified(); // get current file date and time
      if (Math.abs(oldstamp - userGmtMillis) < MILLI_FUZZ)
      {
        /* Existing time stamp is too close to change.  Say nothing. */
      }
      else if ((readonlyFlag == false) && (givenFile.canWrite() == false))
      {
        failCount ++;             // count this as one more failure
        putOutput(selectName(filename, filepath)
          + " - can't change date/time on read-only files");
      }
      else if (simulateFlag)      // scan files without making changes?
      {
        putOutput(selectName(filename, filepath) + " - simulate redate from "
          + userDateFormat.format(new Date(oldstamp)));
      }
      else if (givenFile.setLastModified(userGmtMillis))
      {
        changeCount ++;           // count this as a successful change
        putOutput(selectName(filename, filepath) + " - changed date/time from "
          + userDateFormat.format(new Date(oldstamp)));
      }
      else                        // error from setLastModified() method
      {
        failCount ++;             // count this as one more failure
        putOutput(selectName(filename, filepath)
          + " - failed to change date/time: file open or locked");
      }
    }

    /* Although some of the file name changes could be combined, we only do one
    change on each scan, to remove any question about which change gets done in
    what order. */

    newname = filename;           // assume no changes to file/folder name
    if (customChangeFlag)         // has something special been requested?
      newname = customFilename(newname); // yes, do customized name change
    else if (deleteSpaceFlag)     // do we remove all spaces from name?
      newname = deleteSpaces(newname); // yes, remove all white space
    else if (lowerChangeFlag)     // change file/folder name to lowercase?
      newname = newname.toLowerCase(); // yes, convert old name
    else if (replaceFlag)         // search and replace text in file name?
      newname = replaceFilename(newname); // yes, edit old file to make new
    else if (titleChangeFlag)     // change file/folder name to title case?
      newname = titleCase(newname); // yes, convert old name
    else if (trimSpaceFlag)       // do we remove extra spaces from name?
      newname = trimFilename(newname); // remove leading, trailing, multiples
    else if (upperChangeFlag)     // change file/folder name to uppercase?
      newname = newname.toUpperCase(); // yes, convert old name

    if (filename.equals(newname) == false) // is there anything to change?
    {
      newfile = new File(givenFile.getParent(), newname); // name that we want
      if (newfile.exists() && (givenFile.equals(newfile) == false))
      {
        failCount ++;             // count this as one more failure
        putOutput(selectName(filename, filepath) + " - another file has name <"
          + newname + ">");
      }
      else if ((readonlyFlag == false) && (givenFile.canWrite() == false))
      {
        failCount ++;             // count this as one more failure
        putOutput(selectName(filename, filepath)
          + " - can't change name on read-only files");
      }
      else if (simulateFlag)      // scan files without making changes?
      {
        putOutput(selectName(filename, filepath) + " - simulate rename to <"
          + newname + ">");
      }
      else if (givenFile.renameTo(newfile)) // try to rename file/folder
      {
        changeCount ++;           // count this as a successful change
        putOutput(selectName(filename, filepath) + " - changed name to <"
          + newname + ">");
      }
      else                        // error from renameTo() method
      {
        failCount ++;             // count this as one more failure
        putOutput(selectName(filename, filepath) + " - failed to rename as <"
          + newname + ">");
      }
    }
  } // end of processFileOrFolder() method


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
    if (mainFrame == null)        // during setup, there is no GUI window
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      if (scroll)                 // does caller want us to scroll?
        outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  replaceFilename() method

  Search for and replace patterns/strings in a file name, using previously
  created global variables for the old and new strings.  When the flag is set
  for regular expressions, the replacement string can have "$n" notation for
  substitutions from previously matched groups ("back references").  See the
  following references:

  http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Matcher.html
  http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html
*/
  static String replaceFilename(String text)
  {
    StringBuffer buffer = new StringBuffer(); // needed for append() calls
    Matcher matcher = replaceOldPattern.matcher(text); // search caller's text
    while (matcher.find())        // repeat search as many times as necessary
    {
      try                         // try to replace match with substitutions
      {
        matcher.appendReplacement(buffer, replaceNewString);
      }
      catch (IndexOutOfBoundsException ioobe) // illegal back reference number
      {
        putOutput(text + " - illegal back reference: " + replaceNewString);
        return(text);             // return caller's file name without changes
      }
    }
    matcher.appendTail(buffer);   // add unmatched remainder of file name
    return(buffer.toString());    // convert back to something caller can use
  }


/*
  selectName() method

  We normally report files by their file name without the path.  Root folders
  have no "file name" and we must report the full path name instead.
*/
  static String selectName(String filename, String pathname)
  {
    return((filename.length() > 0) ? filename : pathname);
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
    System.err.println("This is a graphical application.  You may give options on the command line:");
    System.err.println();
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -c0 = uppercase, lowercase same in file name search/replace");
    System.err.println("  -c1 = -c = uppercase, lowercase different in file names (default)");
    System.err.println("  -h0 = ignore hidden files or folders (default)");
    System.err.println("  -h1 = -h = process hidden files and folders");
    System.err.println("  -m0 = don't show custom renaming options (default)");
    System.err.println("  -m1 = -m = show custom renaming, usually character conversion");
    System.err.println("  -r0 = don't try to change read-only files (default)");
    System.err.println("  -r1 = -r = change read-only files if permitted by system");
    System.err.println("  -s0 = do only given files or folders, no subfolders (default)");
    System.err.println("  -s1 = -s = process files, folders, and subfolders");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
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
  titleCase() method

  Convert a string to "title case" where the first letter of each word is
  capitalized.  Words are separated by spaces or hyphens (-).
*/
  static String titleCase(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one input character
    int i;                        // index variable
    int length;                   // number of input characters
    boolean upper;                // true if next character will be uppercase

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get number of input characters
    upper = true;                 // first word is always capitalized
    for (i = 0; i < length; i ++) // do all input characters
    {
      ch = text.charAt(i);        // get one input character
      if (Character.isWhitespace(ch) || (ch == '-') || (ch == '_'))
      {                           // recognize these as word delimiters
        buffer.append(ch);        // copy delimiter unchanged
        upper = true;             // next letter will be uppercase
      }
      else if (upper)             // should we leave this as uppercase?
      {
        buffer.append(Character.toUpperCase(ch)); // append as uppercase
        upper = false;            // next character will be lowercase
      }
      else
        buffer.append(Character.toLowerCase(ch)); // append as lowercase
    }
    return(buffer.toString());    // give caller our converted string

  } // end of titleCase() method


/*
  trimFilename() method

  Trim leading, trailing, and multiple spaces from a file name.  We do this in
  two parts: once for the root name (before the dot or period) and once for the
  file name extension/type (after the dot/period).  We obviously assume that
  the last dot/period in a file name is for an extension/type.  If a file is
  simply named "Mr. Roberts" with no extension, then the trimming will produce
  "Mr.Roberts" as a result.
*/
  static String trimFilename(String filename)
  {
    int i;                        // index variable
    String result;                // our converted string

    i = filename.lastIndexOf('.'); // index of last period (dot), or else -1
    if (i >= 0)                   // if a period (dot) was found in the name
    {
      result = trimString(filename.substring(0, i))
        + "." + trimString(filename.substring(i + 1));
    }
    else                          // no period (dot), so only one piece
      result = trimString(filename);

    return(result);               // give caller our converted string

  } // end of trimFilename() method


/*
  trimString() method

  Remove leading, trailing, and repeated spaces from a string.  There will be
  exactly one space between any two "words" in the result.
*/
  static String trimString(String text)
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
//    if (ch == ' ')              // be very specific about what a space is
      if (Character.isWhitespace(ch)) // general search for any Unicode spaces
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

  } // end of trimString() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main FileDateName1 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if ((source == changeDateGmt) || (source == changeDateLocal))
    {
      userYear.requestFocusInWindow(); // give keyboard focus to year value
    }
    else if (source == changeNameReplace) // search-and-replace name change
    {
      replaceOldDialog.requestFocusInWindow(); // give keyboard to search value
    }
    else if (source == customButton) // button name and meaning tends to vary
    {
      doCustomButton();           // graphical button for special requests
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == openButton) // "Open" button for files or folders
    {
      doOpenButton();             // open files or folders for processing
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method


/*
  userDocument() method

  This method is called by our action listener when the user changes a text
  field that has a document listener.  In this application, we don't really
  care what the exact change is; we are more interested in switching radio
  buttons to match which fields are edited.
*/
  static void userDocument(javax.swing.text.Document source)
  {
    if ((source == replaceNewDialog.getDocument()) // file name search/replace
      || (source == replaceOldDialog.getDocument()))
    {
      if (changeNameReplace.isSelected() == false) // if replace not selected
        changeNameReplace.setSelected(true); // then editing forces selection
    }
    else if ((source == userDay.getDocument()) // numeric date and time
      || (source == userHour.getDocument())
      || (source == userMinute.getDocument())
      || (source == userMonth.getDocument())
      || (source == userSecond.getDocument())
      || (source == userYear.getDocument()))
    {
      if (changeDateNone.isSelected()) // if no time zone has been selected
        changeDateLocal.setSelected(true); // then select the local time zone
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userDocument(): unknown document object.");
                                  // should never happen, so write on console
    }
  } // end of userDocument() method

} // end of FileDateName1 class

// ------------------------------------------------------------------------- //

/*
  FileDateName1User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FileDateName1User implements ActionListener, DocumentListener,
  FocusListener, Runnable
{
  /* empty constructor */

  public FileDateName1User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FileDateName1.userButton(event);
  }

  /* document listeners for changes to text fields */

  public void changedUpdate(DocumentEvent event)
  {
    FileDateName1.userDocument(event.getDocument()); // notify document owner
  }

  public void insertUpdate(DocumentEvent event)
  {
    FileDateName1.userDocument(event.getDocument());
  }

  public void removeUpdate(DocumentEvent event)
  {
    FileDateName1.userDocument(event.getDocument());
  }

  /* focus listeners for text fields (mostly date and time) */

  public void focusGained(FocusEvent event)
  {
    ((javax.swing.text.JTextComponent) event.getSource()).selectAll();
  }

  public void focusLost(FocusEvent event) { }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FileDateName1.doOpenRunner();
  }

} // end of FileDateName1User class

/* Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License. */
