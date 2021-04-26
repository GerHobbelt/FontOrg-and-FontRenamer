/*
  Font Redate #3 - Change Directory Dates for Font Files
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Sunday, 16 September 2007
  Java class name: FontRedate3
  Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 application to read internal dates from OpenType,
  PostScript, or TrueType font files, and to change the file modification dates
  to match.  The file contents are not changed, only the date in the system
  file directory.  Internal font dates may or may not be accurate; many fonts
  are created and later edited without setting the date properly.  Times may
  differ from what you expect depending upon your time zone, and will usually
  agree with what the Microsoft Font Properties Extension shows in its Version
  tab for OTF/TTC/TTF files, plus or minus daylight saving time.  Dates can not
  and will not be changed for read-only files, which is a restriction imposed
  by the Java run-time, not the operating system.

  OpenType fonts (*.OTF), TrueType fonts (*.TTF), and TrueType collections
  (*.TTC) actually have very similar internal formats and are processed
  together.  All three of these use a single file for each font.  PostScript
  fonts can have multiple files (*.AFM, *.INF, *.PFB, *.PFM, etc).  For
  PostScript, an attempt is made to extract the internal date from *.PFA or
  *.PFB files, and if that works, then the file directory dates are changed for
  all files with the same root name and one of the alternate file types.  This
  naming convention does not apply to font files on an Apple Macintosh, so use
  caution with Mac fonts!

  For more information on the internal format of font files, start with the
  following on-line references:

      Microsoft TrueType Font Properties Extension
        http://www.microsoft.com/typography/TrueTypeProperty21.mspx

      The OpenType Font File
        http://www.microsoft.com/typography/otspec/otff.htm
        http://www.microsoft.com/typography/otspec/head.htm

      The PostScript Font Format
        http://partners.adobe.com/public/developer/font/index.html
        http://partners.adobe.com/public/developer/opentype/index_font_formats.html

      The TrueType Font File
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6.html
        http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6head.html

  PostScript fonts have largely been replaced by OpenType fonts.  This program
  makes a limited effort to extract the creation date from PostScript fonts,
  because the date is a text field with an arbitrary format.  The expected
  format is the UNIX style of "Fri Mar 28 22:03:48 1997" as used by Adobe and
  most major font foundries.  Other numeric styles will also be accepted.

  GNU General Public License (GPL)
  --------------------------------
  FontRedate3 is free software: you can redistribute it and/or modify it under
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

      java  FontRedate3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  FontRedate3  -s  d:\fonts  >report.txt

  The console application will return an exit status equal to the number of
  files whose dates have been successfully changed.  The graphical interface
  can be very slow when the output text area gets too big, which will happen if
  thousands of files are reported.

  Restrictions and Limitations
  ----------------------------
  A 32-bit unsigned binary integer can hold OpenType or TrueType dates in
  seconds from January 1904 to February 2040.  FontRedate accepts 33 bits for
  March 2176, then restricts new file dates to the range from January 1981
  until the same time tomorrow.  The default action is to interpret all dates
  in the local time zone, so that time stamps shown by the operating system are
  more consistent from machine to machine.  This is particularly important to
  people who collect fonts and exchange archive files in RAR or ZIP format.
  The -g option on the command line will force dates to be in the standard GMT
  time zone.

  Not all font files are correctly structured.  Before reporting an error in
  this program, make sure that the error isn't in the font file.  Select the
  highest message level for more detailed information about a particular file
  (the -m4 option on the command line, or the graphical "show all files,
  details" option).  For example, if a folder contains both a *.PFA file and a
  *.PFB file for the same PostScript font, and the PFA file has a different
  internal date than the PFB file, then the file dates will be changed twice
  each time this program is run: once to satisfy the PFA file, then once to
  satisfy the PFB file.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class FontRedate3
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License.";
  static final long DATE_MASK = 0x00000001FFFFFFFFL;
                                  // from 1904 to 2176 in TrueType seconds
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final String GMT_ZONE = "GMT+00:00"; // name for GMT time zone
  static final long INT_MASK = 0x00000000FFFFFFFFL;
                                  // logical mask for one int as long value
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String[] MONTH_NAMES = {"January", "February", "March", "April",
    "May", "June", "July", "August", "September", "October", "November",
    "December"};                  // English names for months #1 to #12
  static final String PROGRAM_TITLE =
    "Change Directory Dates for Font Files - by: Keith Fenske";
  static final String[] REPORT_CHOICES = {"only summary, errors",
    "successful changes", "failures to change", "all files, summary",
    "all files, details"};        // descriptions for <reportIndex> values
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates

  /* All file systems have limits on how accurately they store dates and times.
  Don't change file dates when the millisecond difference is too small.  This
  must be at least 2000 ms (2 seconds) for MS-DOS FAT16/FAT32 file systems. */

  static final long MILLI_FUZZ = 2000; // ignore time changes smaller than this

  /* To mark a Java date/time as invalid, we use a special value of -1.  There
  aren't any digital font files dated before 1970 (the zero of the Java
  calendar), and date/time strings are precise only to the nearest second,
  whereas a value of -1 would require millisecond precision. */

  static final long NO_DATE = -1; // no font file has this internal Java date

  /* PostScript fonts have multiple files, generally two or three for each
  font.  Once you find the date in a PFA/PFB file, you need to redate files
  with the same root name and different suffixes (extensions or "file types").
  We have one list for most systems, and a second list in lowercase only for
  Microsoft Windows.  Note that the processUnknownFile() method has a third
  list built into its if-then-else code. */

  static final String[] PS_ALTERNATES = {"AFM", "afm", "CFG", "cfg", "INF",
    "inf", "MMM", "mmm", "PFA", "pfa", "PFB", "pfb", "PFM", "pfm"};
                                  // uppercase and lowercase
  static final String[] PS_LOWERCASE = {"afm", "cfg", "inf", "mmm", "pfa",
    "pfb", "pfm"};                // lowercase only (Windows)

  /* class variables */

  static JLabel ambigDate;        // extracted date from font file
  static JLabel ambigName;        // name of font file and descriptive text
  static JRadioButton[] ambigOptions; // choices for ambiguous date format
  static Box ambigPanel;          // contents of dialog box for ambiguous dates
  static int ambigPrevious;       // previously selected <ambigOptions> index
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static int changeCount;         // number of file dates changed
  static boolean consoleFlag;     // true if running as a console application
  static Pattern datePattern1, datePattern2; // pre-compiled for parsing dates
  static long defaultDateMillis;  // default Java date/time in milliseconds
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
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static SimpleDateFormat formatGmtDate; // always formats in GMT time zone
  static SimpleDateFormat formatUserDate; // formats date/time in user's zone
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static long ignoreMillis;       // ignore time differences less than this
  static JFrame mainFrame;        // this application's window if GUI
  static long maximumJavaDate;    // maximum Java date/time in milliseconds
  static long maximumTruetype;    // maximum TrueType date/time in seconds
  static long minimumJavaDate;    // minimum Java date/time in milliseconds
  static long minimumTruetype;    // minimum TrueType date/time in seconds
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static Calendar ourCalendar;    // for setting times as local or as GMT
  static JTextArea outputText;    // generated report if running as GUI
  static String[] postscriptTypes; // file types (suffixes) for PostScript
  static JCheckBox promptCheckbox; // graphical option for <promptFlag>
  static boolean promptFlag;      // true if pop-up dialogs allowed in GUI
  static boolean readonlyFlag;    // true if we try to change read-only files
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JComboBox reportDialog;  // graphical option for <reportIndex>
  static int reportIndex;         // user's selection from <REPORT_CHOICES>
  static JButton saveButton;      // "Save" button for writing output text
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static TimeZone timeZone;       // for setting times as local or as GMT
  static long truetypeOffset;     // millisecond offset from 1904 to 1970

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

    ambigPrevious = -1;           // no previous index for ambiguous dates
    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    changeCount = fileCount = folderCount = 0; // no files or folders found yet
    consoleFlag = false;          // assume no files or folders on command line
    datePattern1 = datePattern2 = null; // compile later for parsing dates
    defaultDateMillis = NO_DATE;  // no default Java date/time in milliseconds
    fontFilter = new FontRedate3Filter(); // create our shared file filter
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hiddenFlag = false;           // by default, don't process hidden files
    ignoreMillis = (1 * 60 * 60 * 1000) + MILLI_FUZZ; // ignore time < 1 hour
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    promptFlag = false;           // by default, don't allow pop-up dialogs
    readonlyFlag = false;         // by default, don't change read-only files
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

    /* Set the minimum and maximum date/time stamps that we will allow for
    changing files.  These are calculated as long integers in milliseconds
    since 00:00:00 GMT on 1 January 1970.  The MS-DOS FAT file system can't
    handle dates before 1980.  OpenType/TrueType font files have long integers
    in seconds since 00:00:00 GMT on 1 January 1904. */

    formatGmtDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");
    formatGmtDate.setTimeZone(TimeZone.getTimeZone(GMT_ZONE));
    formatUserDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    ourCalendar = Calendar.getInstance(); // default time zone and locale
    timeZone = TimeZone.getDefault(); // by default, use local time zone

    ourCalendar.clear(Calendar.MILLISECOND); // clear so divisible by 1000
    ourCalendar.add(Calendar.DATE, 1); // add one day to current calendar
    maximumJavaDate = ourCalendar.getTimeInMillis(); // same time tomorrow
    minimumJavaDate = convertYmdMilli(1981, 1, 1, 0, 0, 0); // January 1981
    truetypeOffset = Math.abs(convertYmdMilli(1970, 1, 1, 0, 0, 0)
      - convertYmdMilli(1904, 1, 1, 0, 0, 0)); // milliseconds 1904 to 1970
    maximumTruetype = (maximumJavaDate + truetypeOffset) / 1000;
    minimumTruetype = (minimumJavaDate + truetypeOffset) / 1000;

    /* We have to do more work with PostScript file name extensions on systems
    where uppercase and lowercase are distinct.  Avoid this extra work -- and
    duplicate messages -- on Windows where case is ignored in file names. */

    if (mswinFlag)                // if running on Microsoft Windows
      postscriptTypes = PS_LOWERCASE; // look only for lowercase file types
    else                          // but for all other operating systems
      postscriptTypes = PS_ALTERNATES; // look in both uppercase and lowercase

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

      else if (word.startsWith("-f") || (mswinFlag && word.startsWith("/f")))
      {
        /* This is an undocumented option to supply a default date and time,
        parsed as a TrueType GMT binary date in hexadecimal or as a PostScript
        date in text.  The value saved is sensitive to the -g option, and isn't
        re-evaluated later if the time zone changes.  This is helpful in batch
        files (scripts) where many fonts are being unpacked, and you need to
        recognize which files have valid internal dates.  Put -m4 before -f to
        test date/time strings from the command line. */

        Pattern pattern = Pattern.compile("([0-9A-Fa-f]{1,10})"); // syntax
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // is this a TrueType date in hexadecimal?
          defaultDateMillis = (Long.parseLong(matcher.group(1), 16) * 1000)
            - truetypeOffset;     // hex 1904 seconds to Java 1970 millis
        else                      // not TrueType hex, must be PostScript text
          defaultDateMillis = parsePostscriptDate("Default Date",
            "Default Date", args[i].substring(2)); // PostScript to Java date

        if ((defaultDateMillis == NO_DATE) // is this a valid date/time?
          || (defaultDateMillis < minimumJavaDate)
          || (defaultDateMillis > maximumJavaDate))
        {
          System.err.println("Invalid default date or time: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
        printDebug("Default date is " + formatMilliGmt(defaultDateMillis)
          + " or " + formatMilliUser(defaultDateMillis) + ".");
      }

      else if (word.equals("-g") || (mswinFlag && word.equals("/g"))
        || word.equals("-g1") || (mswinFlag && word.equals("/g1")))
      {
        timeZone = TimeZone.getTimeZone(GMT_ZONE); // assume GMT date/time
        formatUserDate.setTimeZone(timeZone); // display in GMT time zone
        ourCalendar.setTimeZone(timeZone); // convert/parse in GMT time zone
      }
      else if (word.equals("-g0") || (mswinFlag && word.equals("/g0")))
      {
        timeZone = TimeZone.getDefault(); // assume local date/time
        formatUserDate.setTimeZone(timeZone); // display in local time zone
        ourCalendar.setTimeZone(timeZone); // convert/parse in local time zone
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

      else if (word.equals("-p") || (mswinFlag && word.equals("/p"))
        || word.equals("-p1") || (mswinFlag && word.equals("/p1")))
      {
        promptFlag = true;        // allow pop-up dialogs in GUI
      }
      else if (word.equals("-p0") || (mswinFlag && word.equals("/p0")))
        promptFlag = false;       // don't allow pop-up dialogs

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

      else if (word.startsWith("-t") || (mswinFlag && word.startsWith("/t")))
      {
        /* This option is followed by an unsigned number of days, hours,
        minutes, or seconds.  Any time difference less than this will be
        ignored.  For minutes or longer we add MILLI_FUZZ; for seconds,
        MILLI_FUZZ is the minimum. */

        Pattern pattern = Pattern.compile("(\\d{1,9})([dhms]?)"); // syntax
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if number followed by optional time unit
        {
          ignoreMillis = Long.parseLong(matcher.group(1)); // no exceptions
          String timesuffix = matcher.group(2); // time letter or empty
          if (timesuffix.equals("d")) // if days
            ignoreMillis = (ignoreMillis * 86400000) + MILLI_FUZZ;
          else if (timesuffix.equals("m")) // if minutes
            ignoreMillis = (ignoreMillis * 60000) + MILLI_FUZZ;
          else if (timesuffix.equals("s")) // if seconds
            ignoreMillis = ignoreMillis * 1000;
          else                    // assume hours
            ignoreMillis = (ignoreMillis * 3600000) + MILLI_FUZZ;
        }
        else                      // bad syntax or too many digits
          ignoreMillis = -1;      // force an error with illegal value

        if (ignoreMillis < MILLI_FUZZ) // no need to enforce a maximum here
        {
          System.err.println("Invalid time difference to ignore: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

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

        consoleFlag = true;       // don't allow GUI methods to be called
        processFileOrFolder(new File(args[i]));
      }
    }

    /* If running as a console application, print a summary of what we found
    and/or changed.  Exit to the system with an integer status that has the
    number of files whose dates were successfully changed. */

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

    action = new FontRedate3User(); // create our shared action listener
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
      "\nChange file directory dates for OpenType, PostScript, and TrueType"
      + "\nfont files based on their internal font dates.\n"
      + "\nChoose your options; then open files or folders to search.\n\n"
      + COPYRIGHT_NOTICE + "\n\n");

    /* Create an entire panel just for the status message.  We do this so that
    we have some control over the margins.  Put the status text in the middle
    of a BorderLayout so that it expands with the window size. */

    JPanel panel5 = new JPanel(new BorderLayout(30, 0));

    statusDialog = new JLabel(EMPTY_STATUS, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    statusDialog.setToolTipText(
      "Running status as files are processed by the Open button.");
    panel5.add(statusDialog, BorderLayout.CENTER);

    promptCheckbox = new JCheckBox("enable annoying pop-up", promptFlag);
    if (buttonFont != null) promptCheckbox.setFont(buttonFont);
    promptCheckbox.setToolTipText(
      "Select to prompt about ambiguous PostScript dates.");
    promptCheckbox.addActionListener(action); // do last so don't fire early
    panel5.add(promptCheckbox, BorderLayout.EAST);

    JPanel panel6 = new JPanel(new BorderLayout(0, 0));
    panel6.add(Box.createVerticalStrut(2), BorderLayout.NORTH);
    panel6.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel6.add(panel5, BorderLayout.CENTER);
    panel6.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel6.add(Box.createVerticalStrut(1), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons and
    options above the text area.  Keep text in the center so that it expands
    horizontally and vertically.  Put status message at the bottom, which also
    expands. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel7 = mainFrame.getContentPane(); // where content meets frame
    panel7.setLayout(new BorderLayout(0, 0));
    panel7.add(panel4, BorderLayout.NORTH); // buttons and options
    panel7.add(new JScrollPane(outputText), BorderLayout.CENTER); // text area
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

    /* Create the contents of a dialog box that we may use later to prompt the
    user about ambiguous PostScript dates.  We don't display the dialog now. */

    ambigOptions = new JRadioButton[3]; // allocate space for radio buttons
    ambigOptions[0] = new JRadioButton("day-month-year (30 September 1999)");
    if (buttonFont != null) ambigOptions[0].setFont(buttonFont);
    ambigOptions[0].setMnemonic(KeyEvent.VK_D);
    ambigOptions[1] = new JRadioButton("month-day-year (30 September 1999)");
    if (buttonFont != null) ambigOptions[1].setFont(buttonFont);
    ambigOptions[1].setMnemonic(KeyEvent.VK_M);
    ambigOptions[2] = new JRadioButton("year-month-day (30 September 1999)");
    if (buttonFont != null) ambigOptions[2].setFont(buttonFont);
    ambigOptions[2].setMnemonic(KeyEvent.VK_E); // VK_Y is used by "Yes" button

    ButtonGroup group1 = new ButtonGroup(); // allow only one radio button
    group1.add(ambigOptions[0]);
    group1.add(ambigOptions[1]);
    group1.add(ambigOptions[2]);

    JPanel panel8 = new JPanel();
    panel8.setLayout(new BoxLayout(panel8, BoxLayout.Y_AXIS));
    panel8.add(Box.createVerticalStrut(10)); // extra space at panel top
    ambigName = new JLabel("insert PostScript file name here");
    if (buttonFont != null) ambigName.setFont(buttonFont);
    panel8.add(ambigName);
    panel8.add(Box.createVerticalStrut(10));
    ambigDate = new JLabel("insert extracted PostScript date here");
    if (buttonFont != null) ambigDate.setFont(buttonFont);
    panel8.add(ambigDate);
    panel8.add(Box.createVerticalStrut(10));
    JLabel label1 = new JLabel("Please choose the correct date format:");
    if (buttonFont != null) label1.setFont(buttonFont);
    panel8.add(label1);
    panel8.add(Box.createVerticalStrut(10));
    panel8.add(ambigOptions[0]);
    panel8.add(ambigOptions[1]);
    panel8.add(ambigOptions[2]);
    panel8.add(Box.createVerticalStrut(10));
    JLabel label2 = new JLabel("Do you want to change the date for this file?");
    if (buttonFont != null) label2.setFont(buttonFont);
    panel8.add(label2);
    panel8.add(Box.createVerticalStrut(10));
    ambigPanel = new Box(BoxLayout.X_AXIS);
    ambigPanel.add(Box.createHorizontalStrut(10)); // extra space on left
    ambigPanel.add(panel8);
    ambigPanel.add(Box.createHorizontalStrut(10)); // extra space on right

    /* Let the graphical interface run the application now. */

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  changeDate() method

  Change the last-modified date and time for a given file, if it does not
  already match the caller's date/time stamp.  If the <rootName> parameter is
  not null, then we actually change all files in the same folder that have the
  root file name plus one of the file types in <suffixList>.
*/
  static void changeDate(
    File givenFile,               // original File object
    long newStamp,                // new Java date/time in GMT milliseconds
    String rootName,              // root file name or <null>
    String[] suffixList)          // list of file types or <null>
  {
    long delta;                   // millisecond difference between old and new
    String filePath;              // name of caller's file, including path
    int i;                        // index variable
    long newMillis;               // value of <newStamp> after DST correction
    long oldMillis;               // value of <oldStamp> after DST correction
    long oldStamp;                // old date/time stamp from file directory
    File parent;                  // parent folder of <givenFile>

    /* Print a debugging trace if the user wants detailed information. */

    if (cancelFlag) return;       // stop if user hit the panic button
    filePath = givenFile.getPath(); // get name of caller's file, with path
    printDebug(filePath + " - changeDate called, newStamp = " + newStamp
      + ", rootName = " + ((rootName == null) ? "null" : ("<" + rootName
      + ">")));

    /* Figure out if the new date and time are meaningful, and if the file
    directory really needs to be changed.  Java isn't completely consistent
    about file dates/times, and the underlying file system has limits on
    accuracy, so it's necessary to accept a margin of error. */

    if (givenFile.isFile() == false) // root + suffix file name may not exist
    {
      printDebug(filePath + " - constructed file name does not exist");
                                  // don't want printFailure here
    }
    else if (newStamp <= 0)       // were we unable to find the date/time?
    {
      printFailure(filePath + " - missing or invalid internal date");
    }
    else if ((newStamp < minimumJavaDate) || (newStamp > maximumJavaDate))
    {
      printFailure(filePath + " - internal font date out of range: "
        + formatMilliUser(newStamp));
    }
    else if (rootName == null)    // if this is a single file to be changed
    {
      /* Microsoft Windows 2000/XP adjusts all file dates and times using the
      current rules for daylight saving time, no matter which rules should be
      applied at that actual date and time.  Correcting for this assumption is
      almost impossible because both Java and Windows think they are in charge
      of time zones and DST.  The following code is quite likely to break if
      either the JRE or Windows changes.  DST offsets are always zero for the
      GMT time zone, so adding or subtracting has no effect.  Hence, we ignore
      the problem with Windows DST correction when using the GMT time zone. */

      newMillis = newStamp;       // assume caller needs no DST correction
      oldStamp = givenFile.lastModified(); // get date/time from file directory
      oldMillis = oldStamp;       // assume directory needs no DST correction
      if (mswinFlag)              // only if running on Microsoft Windows
      {
        long today = timeZone.getOffset(System.currentTimeMillis());
        newMillis += timeZone.getOffset(newMillis) - today; // to Windows
        oldMillis -= timeZone.getOffset(oldMillis) - today; // from Windows
      }
      delta = Math.abs(newMillis - oldStamp); // difference in milliseconds
      if (delta < ignoreMillis)   // is the change too small to effect?
      {
        printSummary(filePath + " - no change from "
          + formatMilliUser(oldMillis) + ((delta > 0) ? (", difference is "
          + (delta / 1000.0) + " seconds") : ""));
      }
      else                        // the difference is significant
      {
        if ((readonlyFlag == false) && (givenFile.canWrite() == false))
        {
          printFailure(filePath + " - can't change read-only file to "
            + formatMilliUser(newStamp));
        }
        else if (givenFile.setLastModified(newMillis)) // try to set new date
        {
          changeCount ++;         // count successful date/time changes
          printChange(filePath + " - changed to " + formatMilliUser(newStamp)
            + ", was " + formatMilliUser(oldMillis));
        }
        else                      // above attempt failed to set new date/time
        {
          printFailure(filePath + " - failed to change from "
            + formatMilliUser(oldMillis) + " to " + formatMilliUser(newStamp));
        }
      }
    }
    else                          // do all files with same root file name
    {
      printSummary(filePath + " - checking alternate file types");
      parent = givenFile.getParentFile(); // get parent folder for given file
      for (i = 0; i < suffixList.length; i ++) // for each known file type
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        changeDate((new File(parent, (rootName + "." + suffixList[i]))),
          newStamp, null, null);  // call ourself for each possibility
      }
    }
  } // end of changeDate() method


/*
  convertSmallYear() method

  Convert two-digit years to something from 1970 to 2069.  Don't change numbers
  outside of the range 00 to 99.
*/
  static int convertSmallYear(int year)
  {
    int result;                   // resulting year, after conversion

    result = year;                // assume that there is no change
    if ((year >= 00) && (year <= 69)) // 00 to 69 becomes 2000 to 2069
      result = year + 2000;
    else if ((year >= 70) && (year <= 99)) // 70 to 99 becomes 1970 to 1999
      result = year + 1900;
    return(result);               // give caller whatever we could find
  }


/*
  convertYmdMilli() method

  Convert year-month-day hour-minute-second to a Java date/time in milliseconds
  in the GMT time zone.  The caller's date/time is assumed to be in the current
  time zone, as determined by the -g option.  Note that Java months are from 0
  to 11, not from 1 to 12 as the caller expects.

  This method is not thread-safe because it uses a common Calendar object.
*/
  static long convertYmdMilli(int year, int month, int day, int hour,
    int minute, int second)
  {
    ourCalendar.clear();          // clear all fields, including initial date
    ourCalendar.set(year, (month - 1), day, hour, minute, second);
    return(ourCalendar.getTimeInMillis()); // return GMT milliseconds
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
    changeCount = fileCount = folderCount = 0; // no files or folders found yet
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    setStatusMessage(EMPTY_STATUS); // clear status message at bottom of window
    statusTimer.start();          // start updating the status message

    openFilesThread = new Thread(new FontRedate3User(), "doOpenRunner");
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
  formatMilliGmt(), formatMilliUser() methods

  Convert a millisecond Java date/time to a string.  The date/time stamp must
  already be in the GMT time zone, and we can bypass the <ourCalendar> object.
*/
  static String formatMilliGmt(long timestamp) // display in GMT time zone
  {
    return(formatGmtDate.format(new Date(timestamp)));
  }

  static String formatMilliUser(long timestamp) // display in user's time zone
  {
    return(formatUserDate.format(new Date(timestamp)));
  }


/*
  parsePostscriptDate() method

  Convert a PostScript date/time string into a long date/time stamp in the
  magic millisecond style beloved by Java.  If we can't parse the date/time,
  print an error message and return <NO_DATE>.
*/
  static long parsePostscriptDate(
    String fileName,              // name of caller's file, without path
    String filePath,              // name of caller's file, including path
    String text)                  // date and time string to be parsed
  {
    int date1, date2, date3;      // raw numbers obtained while parsing dates
    int day, month, year;         // numbers for known items in date string
    int hour, minute, second;     // numbers for known items in time string
    Matcher matcher;              // pattern matcher
    int option;                   // option chosen by user in dialog box
    long result;                  // resulting date/time stamp in milliseconds
    String tag;                   // time tag, usually "AM" or "PM" or zone

    result = NO_DATE;             // assume failure, mark as invalid

    /* The standard format is the UNIX style of "Fri Mar 28 22:03:48 1997", as
    used by Adobe and most font foundries.  Some older URW fonts are missing
    the time of day, for which we assume 12 o'clock noon, because that's the
    hour that is most likely to remain in the same day after applying a time
    zone offset.  We ignore the weekday, although it could be used to test the
    rest of the date. */

    if (datePattern1 == null)     // compile pattern only once
    {
      datePattern1 = Pattern.compile(
        "^\\s*\\w+\\.?,?\\s+(\\w+)\\.?\\s+(\\d{1,2}),?(?:\\s+(\\d{1,2})[.:](\\d{1,2})(?:[.:](\\d{1,2}))?,?)?\\s+(\\d{1,4})\\.?\\s*$");
    }
    matcher = datePattern1.matcher(text); // try match input text to pattern
    if (matcher.matches())        // if this text matches our generic pattern
    {
      /* Convert parsed fields to numbers.  The time is optional. */

      tag = matcher.group(1).toLowerCase(); // abbreviation or name for month
      day = Integer.parseInt(matcher.group(2)); // numeric day of the month
      year = convertSmallYear(Integer.parseInt(matcher.group(6)));

      if (matcher.group(3) == null) // number of hours is optional
        hour = 12;                // noon is safest with shifting time zones
      else
        hour = Integer.parseInt(matcher.group(3));

      if (matcher.group(4) == null) // number of minutes is optional
        minute = 0;
      else
        minute = Integer.parseInt(matcher.group(4));

      if (matcher.group(5) == null) // number of seconds is optional
        second = 0;
      else
        second = Integer.parseInt(matcher.group(5));

      /* Convert month abbreviation or name to a number from 1 to 12. */

      if (tag.startsWith("jan")) month = 1; // matches "jan", "january", etc
      else if (tag.startsWith("feb")) month = 2;
      else if (tag.startsWith("mar")) month = 3;
      else if (tag.startsWith("apr")) month = 4;
      else if (tag.startsWith("may")) month = 5;
      else if (tag.startsWith("jun")) month = 6;
      else if (tag.startsWith("jul")) month = 7;
      else if (tag.startsWith("aug")) month = 8;
      else if (tag.startsWith("sep")) month = 9;
      else if (tag.startsWith("oct")) month = 10;
      else if (tag.startsWith("nov")) month = 11;
      else if (tag.startsWith("dec")) month = 12;
      else month = -1;            // failed to identify the month

      /* Accept numbers that are within an appropriate range.  We don't check
      that the dates are actually valid, i.e., February 31st is okay by us. */

      if ((year >= 1900) && (year <= 2099)
        && (month >= 1) && (month <= 12)
        && (day >= 1) && (day <= 31)
        && (hour >= 0) && (hour <= 23)
        && (minute >= 0) && (minute <= 59)
        && (second >= 0) && (second <= 59))
      {
        result = convertYmdMilli(year, month, day, hour, minute, second);
        printDebug(filePath + " - converted UNIX date is "
          + formatMilliUser(result));
      }
      else
      {
        printDebug(filePath + " - invalid UNIX date <" + text + ">");
      }

      /* Return result now, even if it's failure, because we don't want anybody
      else trying to scan this particular date/time format. */

      return(result);             // give caller whatever we could find
    }

    /* We scan simple numeric dates ourself, because Java allows ambiguous
    month-day pairs according to the current user's locale, while font files
    may have been obtained from anywhere.  Look for variations on the format
    "12/31/97 at 07:23:56 AM" with the time being optional.  Consider these
    orderings for dates: year-month-day, month-day-year, day-month-year. */

    if (datePattern2 == null)     // compile pattern only once
    {
      datePattern2 = Pattern.compile(
        "^\\s*(\\d{1,4})(?:(?:\\s*[-./:]\\s*)|(?:\\s+))(\\d{1,4})(?:(?:\\s*[-./:]\\s*)|(?:\\s+))(\\d{1,4})(?:(?:\\s+[Aa][Tt])?\\s+(\\d{1,2})[.:](\\d{1,2})(?:[.:](\\d{1,2}))?(?:\\s+((?:[Aa][Mm])|(?:[Pp][Mm])|(?:[Uu][Hh][Rr])))?)?\\s*$");
    }
    matcher = datePattern2.matcher(text); // try match input text to pattern
    if (matcher.matches())        // if this text matches our generic pattern
    {
      date1 = Integer.parseInt(matcher.group(1)); // don't know which of these
      date2 = Integer.parseInt(matcher.group(2)); // ... is the year, month, or
      date3 = Integer.parseInt(matcher.group(3)); // ... day yet

      if (matcher.group(4) == null) // number of hours is optional
        hour = 12;                // noon is safest with shifting time zones
      else
        hour = Integer.parseInt(matcher.group(4));

      if (matcher.group(5) == null) // number of minutes is optional
        minute = 0;
      else
        minute = Integer.parseInt(matcher.group(5));

      if (matcher.group(6) == null) // number of seconds is optional
        second = 0;
      else
        second = Integer.parseInt(matcher.group(6));

      if (matcher.group(7) == null) // AM/PM indicator is optional
        tag = "";
      else
        tag = matcher.group(7).toUpperCase();

      /* Try to decide which of the three date numbers are the year, the month,
      and the day.  We only want "safe" answers, no guessing.  The full range
      for each field will be tested later. */

      year = month = day = -1;    // assume failure, mark as invalid
      if ((date1 == date2) && (date2 == date3))
      {
        year = month = day = date1; // no confusion if all numbers are same
      }
      else if ((date1 == 0) || (date1 > 31)) // always year-month-day order,
      {                           // ... never year-day-month order
        day = date3; month = date2; year = date1;
      }
      else if (date2 > 12)        // only happens for month-day-year
      {
        day = date2; month = date1; year = date3;
      }
      else if ((date3 == 0) || (date3 > 31)) // third number must be year
      {
        if (date1 == date2)       // no confusion if month and day are same
        {
          day = month = date1; year = date3;
        }
        else if ((date1 <= 12) && (date2 > 12)) // must be month-day-year
        {
          day = date2; month = date1; year = date3;
        }
        else if ((date1 > 12) && (date2 <= 12)) // must be day-month-year
        {
          day = date1; month = date2; year = date3;
        }
      }

      /* If the date is ambiguous, then ask the user for help, if allowed. */

      if (((year < 0) || (month < 0) || (day < 0)) && (mainFrame != null)
        && (promptFlag == true))
      {
        ambigName.setText("File <" + fileName
          + "> has an ambiguous PostScript date:");
        ambigDate.setText("     " + text);  // repeat date string given to us

        /* Set up which date orders are possible, and hence have their radio
        buttons enabled. */

        if ((date1 >= 1) && (date1 <= 31) && (date2 >= 1) && (date2 <= 12))
        {
          ambigOptions[0].setEnabled(true);
          ambigOptions[0].setSelected(ambigPrevious == 0);
          ambigOptions[0].setText("day-month-year (" + date1 + " "
            + MONTH_NAMES[date2 - 1] + " " + convertSmallYear(date3) + ")");
        }
        else
        {
          ambigOptions[0].setEnabled(false);
          ambigOptions[0].setSelected(false);
          ambigOptions[0].setText("day-month-year order not possible");
        }

        if ((date1 >= 1) && (date1 <= 12) && (date2 >= 1) && (date2 <= 31))
        {
          ambigOptions[1].setEnabled(true);
          ambigOptions[1].setSelected(ambigPrevious == 1);
          ambigOptions[1].setText("month-day-year (" + date2 + " "
            + MONTH_NAMES[date1 - 1] + " " + convertSmallYear(date3) + ")");
        }
        else
        {
          ambigOptions[1].setEnabled(false);
          ambigOptions[1].setSelected(false);
          ambigOptions[1].setText("month-day-year order not possible");
        }

        if ((date2 >= 1) && (date2 <= 12) && (date3 >= 1) && (date3 <= 31))
        {
          ambigOptions[2].setEnabled(true);
          ambigOptions[2].setSelected(ambigPrevious == 2);
          ambigOptions[2].setText("year-month-day (" + date3 + " "
            + MONTH_NAMES[date2 - 1] + " " + convertSmallYear(date1) + ")");
        }
        else
        {
          ambigOptions[2].setEnabled(false);
          ambigOptions[2].setSelected(false);
          ambigOptions[2].setText("year-month-day order not possible");
        }

        /* Ask the user what he/she thinks.  Note that the user may close the
        dialog box without selecting anything. */

        option = JOptionPane.showConfirmDialog(mainFrame, ambigPanel,
          "Ambiguous PostScript Date Found", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) // user click dialog "Cancel"?
        {
          cancelFlag = true;      // act as if user clicked *our* Cancel button
          printAlways("Cancelled by user at ambiguous dialog.", true);
          return(result);         // return with default result (failure)
        }
        else if (option == JOptionPane.YES_OPTION)
        {
          /* The user clicked the "Yes" button, but did he/she select one of
          the options?  This program does not force any of the enabled options
          to be selected, because we try to keep the previous selection as the
          default, even if the previous option is not currently enabled. */

          if (ambigOptions[0].isEnabled() && ambigOptions[0].isSelected())
          {
            day = date1; month = date2; year = date3; // day-month-year
            ambigPrevious = 0;    // remember as default option for next time
          }
          else if (ambigOptions[1].isEnabled() && ambigOptions[1].isSelected())
          {
            day = date2; month = date1; year = date3; // month-day-year
            ambigPrevious = 1;    // remember as default option for next time
          }
          else if (ambigOptions[2].isEnabled() && ambigOptions[2].isSelected())
          {
            day = date3; month = date2; year = date1; // year-month-day
            ambigPrevious = 2;    // remember as default option for next time
          }
        }
      }

      /* If the date is still ambiguous, then print an error message and return
      to the caller. */

      if ((year < 0) || (month < 0) || (day < 0))
      {
        printDebug(filePath + " - ambiguous year-month-day <" + text + ">");
        return(result);           // return with default result (failure)
      }

      /* Adjust two-digit years to be between 1970 and 2069. */

      year = convertSmallYear(year); // safely ignores negative numbers

      /* Adjust the hour for the AM/PM tag. */

      if (tag.equals("AM"))       // is this an "AM" tag (before noon)?
      {
        if (hour <= 0)            // "zero" hour not allowed with AM/PM
          hour = -1;
        else if (hour <= 11)      // hours from 1 to 11 are normal
          { /* do nothing */ }
        else if (hour == 12)      // hour 12 is zero hour in 24-hour clock
          hour = 0;
        else                      // anything past 12 is not allowed
          hour = -1;
      }
      else if (tag.equals("PM"))  // is this a "PM" tag (after noon)?
      {
        if (hour <= 0)            // "zero" hour not allowed with AM/PM
          hour = -1;
        else if (hour <= 11)      // shift hours from 1 to 11 to 24-hour clock
          hour += 12;
        else if (hour == 12)      // hour number 12 is correct on 24-hour clock
          { /* do nothing */ }
        else                      // anything past 12 is not allowed
          hour = -1;
      }
      else                        // assume 24-hour clock for all other tags
        { /* do nothing */ }

      /* Accept numbers that are within an appropriate range. */

      if ((year >= 1900) && (year <= 2099)
        && (month >= 1) && (month <= 12)
        && (day >= 1) && (day <= 31)
        && (hour >= 0) && (hour <= 23)
        && (minute >= 0) && (minute <= 59)
        && (second >= 0) && (second <= 59))
      {
        result = convertYmdMilli(year, month, day, hour, minute, second);
        printDebug(filePath + " - converted numeric date is "
          + formatMilliUser(result));
      }
      else
      {
        printDebug(filePath + " - invalid numeric date <" + text + ">");
      }
      return(result);             // give caller whatever we could find
    }

    /* None of the rules above were satisfied, so return the failure result. */

    printDebug(filePath + " - can't parse PostScript date <" + text + ">");
    return(result);

  } // end of parsePostscriptDate() method


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

  We can't test <consoleFlag> to decide if we are writing on standard output
  because our -f option may call parsePostscriptDate() before a file or folder
  name is found on the command line -- that is, before we know if we will be
  running as a console or graphical application.
*/
  static void printAlways(String text)
  {
    printAlways(text, promptFlag); // don't scroll unless prompting enabled
  }

  static void printAlways(String text, boolean scroll)
  {
    if (mainFrame == null)        // are we running as a console application?
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
          processUnknownFile(next); // figure out what to do with this file
        }
        else                      // file directory has an invalid entry
        {
          printSummary(next.getPath() + " - not a file or folder");
        }
      }
    }
    else if (givenFile.isFile())  // we do want to look at normal files
    {
      processUnknownFile(givenFile); // figure out what to do with this file
    }
    else                          // user gave bad file or folder name
    {
      printAlways(givenFile.getPath() + " - not a file or folder");
    }
  } // end of processFileOrFolder() method


/*
  processPostScript() method

  The caller gives us a Java File object that should be a PostScript *.PFA
  (ASCII) or *.PFB (binary) font file.  The part we want ("CreationDate") is
  in plain text for both file types.
*/
  static void processPostScript(
    File givenFile,               // original File object
    String rootName)              // root file name
  {
    int bufChar;                  // input character from <bufFile> or -1
    BufferedReader bufFile;       // file stream for reading font file
    String fileName;              // name of caller's file, without path
    String filePath;              // name of caller's file, including path
    long javaDate;                // milliseconds since midnight 1 January 1970
    String postscriptDate;        // date string found in font file
    final char[] searchChars = {'%', '%', 'C', 'r', 'e', 'a', 't', 'i', 'o',
      'n', 'D', 'a', 't', 'e', ':'}; // string that appears before date ...
                                  // ... with all spaces and tabs removed
    int searchIndex;              // current index into <searchChars>

    /* Open the file, look for a creation date, parse the date. */

    if (cancelFlag) return;       // stop if user hit the panic button
    fileName = givenFile.getName(); // get name of caller's file, no path
    filePath = givenFile.getPath(); // get name of caller's file, with path
    printDebug(filePath + " - processPostScript called, rootName = <"
      + rootName + ">");

    try                           // catch file I/O errors
    {
      bufFile = new BufferedReader(new FileReader(givenFile));

      /* Rather than parsing the whole file as proper PostScript, we search for
      a specific string and extract all following text until the next newline
      character or control code.  Crude but fast and effective. */

      postscriptDate = null;      // assume failure, mark as invalid
      searchIndex = 0;            // start from beginning of search string
      do
      {
        bufChar = bufFile.read(); // get one character or -1 for end-of-file
        if (bufChar < 0)          // have we reached end of file?
          { /* do nothing */ }
        else if ((bufChar == ' ') || (bufChar == '\t')) // ignore spaces
          { /* do nothing */ }
        else if (bufChar == searchChars[searchIndex])
          searchIndex ++;         // found one more character in string
        else if (bufChar == searchChars[0])
          searchIndex = 1;        // mismatch, but can restart successfully
        else
          searchIndex = 0;        // mismatch, start again with nothing found
      } while ((cancelFlag == false) && (bufChar >= 0)
        && (searchIndex < searchChars.length));

      if (cancelFlag)             // continue only if no errors so far
        { /* do nothing */ }
      else if (bufChar < 0)       // end-of-file means search failed
      {
        printDebug(filePath + " - no PostScript "
          + (String.valueOf(searchChars)) + " found");
      }
      else                        // grab all text until end-of-line
      {
        StringBuffer buffer = new StringBuffer(); // empty string result
        while (((bufChar = bufFile.read()) >= ' ') && (bufChar < 0x7F))
          buffer.append((char) bufChar); // append character to date string
        postscriptDate = buffer.toString().trim(); // remove extra spaces
        printDebug(filePath + " - PostScript date is <" + postscriptDate
          + ">");
      }

      /* Close the input font file before trying to change the date. */

      bufFile.close();            // try to close input file
      if (cancelFlag) return;     // stop if user hit the panic button

      /* Parse the extracted date string and change the file date(s). */

      if (postscriptDate != null) // did we find a date string?
        javaDate = parsePostscriptDate(fileName, filePath, postscriptDate);
      else                        // there was no date string in file
        javaDate = NO_DATE;       // use same value as if parsing failed
      if (cancelFlag) return;     // stop if user hit the panic button

      if ((javaDate != NO_DATE) && (javaDate >= minimumJavaDate)
        && (javaDate <= maximumJavaDate)) // is this a valid date/time?
      {
        changeDate(givenFile, javaDate, rootName, postscriptTypes);
      }
      else if (defaultDateMillis != NO_DATE) // is there a default date/time?
      {
        javaDate = defaultDateMillis; // use default, no range checking here
        printDebug(filePath + " - no date or out of range, using default "
          + formatMilliUser(javaDate)); // display default in user's time zone
        changeDate(givenFile, javaDate, rootName, postscriptTypes);
      }
      else                        // no date found in file, no default given
      {
        printFailure(filePath + " - can't find a valid date");
      }
    }

    /* Catch any file I/O errors, here or in called methods. */

    catch (IOException ioe)
    {
      printFailure(filePath + " - can't read file");
    }
  } // end of processPostScript() method


/*
  processTrueType() method

  The caller gives us a Java File object that should be an OpenType (*.OTF) or
  TrueType (*.TTC, *.TTF) font file.  The part we want (the "modified" field)
  is in the "head" table.  While *.TTC files may have multiple "head" tables,
  we only look at the first one.
*/
  static void processTrueType(File givenFile)
  {
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of the file in bytes
    long headTable;               // location of "head" table in file
    int i;                        // index variable
    long javaDate;                // milliseconds since midnight 1 January 1970
    int numTables;                // number of table directory entries
    long offsetTable;             // location of (first) Offset Table in file
    RandomAccessFile ramFile;     // file stream for reading font file
    long signature;               // signature bytes from beginning of file
    long truetypeCreated, truetypeModified; // raw 64-bit binary font dates
    long truetypeDate;            // seconds since midnight on 1 January 1904

    /* Open the file, index through the tables, and extract the binary date. */

    if (cancelFlag) return;       // stop if user hit the panic button
    filePath = givenFile.getPath(); // get name of caller's file, with path
    fileSize = givenFile.length(); // get size of caller's file in bytes
    printDebug(filePath + " - processTrueType called");

    try                           // catch file I/O errors
    {
      ramFile = new RandomAccessFile(givenFile, "r"); // open file for reading

      /* Figure out what type of file this is, if it's even a font file! */

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
          || (signature == 0x4F54544FL) // "OTTO" for OpenType
          || (signature == 0x74727565L)) // "true" for Macintosh TrueType
        {
          /* This is an OpenType or TrueType file containing a single font. */

          offsetTable = 0;        // conveniently located at start of file
        }
        else if (signature == 0x74746366L) // "ttcf" for TrueType collection
        {
          /* This is a TrueType collection containing at least one font.  Well,
          we assume that there is at least one internal font! */

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

      /* Find the "head" table, if any. */

      headTable = -1;             // assume failure, mark as invalid
      if ((cancelFlag == false) && (offsetTable >= 0)) // if no errors so far
      {
        printDebug(filePath + " - Offset Table is at 0x"
          + Long.toHexString(offsetTable));

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
              if ((((long) ramFile.readInt()) & INT_MASK) == 0x68656164L)
                                  // is this entry for the "head" table?
              {
                ramFile.skipBytes(4); // skip checksum
                headTable = ((long) ramFile.readInt()) & INT_MASK;
                break;            // exit early from <for> loop
              }
              else                // no, continue looking
                ramFile.skipBytes(12); // skip remainder of table entry
            }

            if (headTable > 0)    // did we find the "head" table?
            {
              printDebug(filePath + " - \"head\" table is at 0x"
                + Long.toHexString(headTable));
            }
            else
            {
              printDebug(filePath + " - \"head\" table not found");
            }
          }
        }
      }

      /* Get the "created" and "modified" dates/times from the "head" table,
      which are 64-bit integers with the number of seconds since 00:00:00 on
      1st January 1904, with no specified time zone except maybe GMT.  Only the
      low-order 32 bits are used for dates from 1904 to 2040, and some old font
      editors are (were) known for leaving garbage or "flags" in the high-order
      32 bits.  Letraset put 32-bit dates in the high-order half, and with zero
      in the low-order half. */

      truetypeDate = NO_DATE;     // assume failure, mark as invalid
      if ((cancelFlag == false) && (headTable > 0)) // only if no errors so far
      {
        if (fileSize < (headTable + 36)) // file big enough for "head" table?
        {
          printDebug(filePath + " - file too small for \"head\" table");
        }
        else
        {
          /* Get the raw binary dates/times as 64-bit integers. */

          ramFile.seek(headTable + 20); // location of "created" date/time
          truetypeCreated = ramFile.readLong(); // get 64-bit created seconds
          truetypeModified = ramFile.readLong(); // get 64-bit modified seconds
          printDebug(filePath + " - created 0x"
            + Long.toHexString(truetypeCreated) + ", modified 0x"
            + Long.toHexString(truetypeModified));

          /* Hopefully, the modified date is good.  Repair as necessary. */

          if (truetypeModified != 0) // prefer modified date over created date
          {
            if (truetypeModified == (truetypeModified & DATE_MASK))
            {
              truetypeDate = truetypeModified; // normal and correct case
            }
            else if ((truetypeModified < 0)
              && ((truetypeModified & INT_MASK) == 0))
            {
              /* Letraset mistake of putting a 32-bit date in the high-order
              end instead of the low-order.  Starting in 1972, such a 32-bit
              date will force a 64-bit signed integer to be negative. */

              truetypeDate = truetypeModified >>> 32;
              printDebug(filePath + " - shifting right as 0x"
                + Long.toHexString(truetypeDate));
            }
            else if (((truetypeModified & INT_MASK) != 0) // low-order non-zero
              && ((truetypeCreated & INT_MASK) == (truetypeModified & INT_MASK)))
            {
              /* Altsys Fontographer mistake of leaving random garbage in the
              high-order 32 bits, while putting a valid date in the low-order.
              To be safe, we require that both dates have the same 32-bit
              non-zero value in the low-order portion. */

              truetypeDate = truetypeModified & INT_MASK; // remove high-order
              printDebug(filePath + " - truncating date to 0x"
                + Long.toHexString(truetypeDate));
            }
            else
            {
              printDebug(filePath + " - can't repair modified date");
            }
          }
          else if (truetypeCreated != 0) // already know truetypeModified = 0
          {
            printDebug(filePath + " - no modified date, using created date");
            if (truetypeCreated == (truetypeCreated & DATE_MASK))
            {
              truetypeDate = truetypeCreated; // accept created if no modified
            }
            else if ((truetypeCreated < 0) // the Letraset mistake
              && ((truetypeCreated & INT_MASK) == 0))
            {
              truetypeDate = truetypeCreated >>> 32;
              printDebug(filePath + " - shifting right as 0x"
                + Long.toHexString(truetypeDate));
            }
            /* Can't do Altsys Fontographer check since modified is zero. */
            else
            {
              printDebug(filePath + " - can't repair created date");
            }
          }
          else                    // truetypeCreated = truetypeModified = 0
          {
            printDebug(filePath + " - created and modified dates are zero");
          }
        }
      }

      /* Close the input font file before trying to change the date. */

      ramFile.close();            // try to close input file
      if (cancelFlag) return;     // stop if user hit the panic button

      /* Show the raw binary date and the equivalent TrueType date, formatted
      without checking range or validity.  If the program does not correctly
      change dates, according to the user's expectations, this information is
      essential for deciding what went wrong. */

      if (truetypeDate != NO_DATE) // show raw binary date, ignore range
        printDebug(filePath + " - binary date 0x"
          + Long.toHexString(truetypeDate) + " is "
          + formatMilliGmt((truetypeDate * 1000) - truetypeOffset));

      /* Convert the TrueType date (in seconds since 1904) to a Java date (in
      milliseconds since 1970).  We don't need to use <ourCalendar> or the
      user's local <timeZone> here, because binary TrueType dates are (by
      definition) already in the standard GMT time zone.  However, people who
      upload and download fonts in archive files prefer to have the same local
      time no matter what zone they may be in, and the only way of getting a
      consistent local date/time is to misinterpret the GMT date/time as being
      local.  In effect, we cancel the time zone offset. */

      if ((truetypeDate >= 0x1C000000L) && (truetypeDate <= 0x3F000000L))
      {
        /* Another Altsys Fontographer and/or Altsys Metamorphosis mistake of
        putting dates as seconds since 1970 instead of 1904.  If converted by
        official TrueType rules, this would have years in the 1920s or 1930s.
        The <if> range above is roughly November 1984 (1918) to June 2003
        (1937), at which point one hopes that old Altsys software has been
        retired!  We assume the time zone is still GMT.  The same mistake
        happens in later years, mostly as a result of poor conversion from
        PostScript to TrueType; those dates unreliable for our purposes. */

        javaDate = truetypeDate * 1000; // don't need offset, already 1970+
        printDebug(filePath + " - correcting 0x"
          + Long.toHexString(truetypeDate) + " relative to 1970, not 1904");
        printDebug(filePath + " - corrected date is "
          + formatMilliGmt(javaDate)); // display binary date as correct GMT
        javaDate -= timeZone.getOffset(javaDate); // reverse time zone effect
        changeDate(givenFile, javaDate, null, null); // compare, maybe change
      }
      else if ((truetypeDate != NO_DATE) && (truetypeDate >= minimumTruetype)
        && (truetypeDate <= maximumTruetype)) // is this a valid date/time?
      {
        javaDate = (truetypeDate * 1000) - truetypeOffset; // both in GMT zone
        javaDate -= timeZone.getOffset(javaDate); // reverse time zone effect
        changeDate(givenFile, javaDate, null, null); // compare, maybe change
      }
      else if (defaultDateMillis != NO_DATE) // is there a default date/time?
      {
        javaDate = defaultDateMillis; // use default, no range checking here
        printDebug(filePath + " - no date or out of range, using default "
          + formatMilliUser(javaDate)); // display default in user's time zone
        changeDate(givenFile, javaDate, null, null); // compare, maybe change
      }
      else                        // no date found in file, no default given
      {
        printFailure(filePath + " - can't find a valid date");
      }
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
  static void processUnknownFile(File givenFile)
  {
    String fileName;              // name of caller's file (root + dot + type)
    String filePath;              // name of caller's file, including path
    String fileRoot;              // only the part before the last period (dot)
    String fileType;              // only the part after the last period (dot)
    int i;                        // index variable

    if (cancelFlag) return;       // stop if user hit the panic button
    fileCount ++;                 // found one more file, of unknown type
    fileName = givenFile.getName(); // get name of caller's file, no path
    filePath = givenFile.getPath(); // get name of caller's file, with path
    setStatusMessage("Reading file " + filePath);

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

    if (fileType.equals("otf")    // OpenType single font
      || fileType.equals("ttc")   // TrueType collection
      || fileType.equals("ttf"))  // TrueType single font
    {
      processTrueType(givenFile); // handle as generic OpenType or TrueType
    }
    else if (fileType.equals("pfa") || fileType.equals("pfb"))
    {
      processPostScript(givenFile, fileRoot); // PostScript ASCII or binary
    }
    else if (fileType.equals("afm") // can't be processed until PFA/PFB found
      || fileType.equals("cfg")
      || fileType.equals("inf")
      || fileType.equals("mmm")
      || fileType.equals("pfm"))
    {
      printSummary(filePath + " - was or will be done with PFA/PFB file");
    }
    else                          // file type not recognized as a font
    {
      printSummary(filePath + " - not a recognized font file type <"
        + fileType.toUpperCase() + ">");
    }
  } // end of processUnknownFile() method


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
    if (mainFrame == null)        // are we running as a console application?
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
    System.err.println("  java  FontRedate3  [options]  file or folder names");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -g0 = convert/display dates in the local time zone (default)");
    System.err.println("  -g1 = -g = convert/display dates in standard GMT time zone");
    System.err.println("  -h0 = ignore hidden files or folders (default)");
    System.err.println("  -h1 = -h = process hidden files and folders");
    System.err.println("  -m0 = show only program summary, critical errors");
    System.err.println("  -m1 = show files with successful changes (default)");
    System.err.println("  -m2 = show only files that couldn't be changed");
    System.err.println("  -m3 = show all files, with summary for each file");
    System.err.println("  -m4 = show all files, with details for each file");
    System.err.println("  -p0 = don't prompt about ambiguous PostScript dates (default)");
    System.err.println("  -p1 = -p = allow pop-up dialog for ambiguous PostScript dates");
    System.err.println("  -r0 = don't try to change read-only files (default)");
    System.err.println("  -r1 = -r = change read-only files if permitted by system");
    System.err.println("  -s0 = do only given files or folders, no subfolders (default)");
    System.err.println("  -s1 = -s = process files, folders, and subfolders");
    System.err.println("  -t# = ignore time differences less than # hours; default -t1");
    System.err.println("  -t#s = seconds, -t#m = minutes, -t#h = hours, -t#d = days");
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
  buttons, in the context of the main FontRedate3 class.
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
    else if (source == promptCheckbox) // prompt ambiguous PostScript dates
    {
      promptFlag = promptCheckbox.isSelected();
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

} // end of FontRedate3 class

// ------------------------------------------------------------------------- //

/*
  FontRedate3Filter class

  This class limits the files shown in the file open dialog box to font files.
*/

class FontRedate3Filter extends javax.swing.filechooser.FileFilter
                implements java.io.FileFilter // not the same as filechooser.*
{
  /* empty constructor */

  public FontRedate3Filter() { }

  /* file filter: accept files of given types */

  public boolean accept(File givenFile)
  {
    String name = givenFile.getName().toLowerCase(); // get name of file
    if (givenFile.isDirectory()   // allow user to navigate directories
      || name.endsWith(".otf")    // accept this list of file types
      || name.endsWith(".pfa")
      || name.endsWith(".pfb")
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
    return("OpenType, PostScript, TrueType font files");
  }

} // end of FontRedate3Filter class

// ------------------------------------------------------------------------- //

/*
  FontRedate3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FontRedate3User implements ActionListener, Runnable
{
  /* empty constructor */

  public FontRedate3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FontRedate3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FontRedate3.doOpenRunner();
  }

} // end of FontRedate3User class

/* Copyright (c) 2007 by Keith Fenske.  Released under GNU Public License. */
