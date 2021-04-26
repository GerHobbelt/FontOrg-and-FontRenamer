/*
  Compare Folders #3 - Compare Files, Folders, Checksums
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Friday, 2 May 2008
  Java class name: CompareFolders3
  Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 application to compare two folders to determine if all
  files and subfolders are identical.  The folders may be on the same computer,
  on the local network, or they may be represented by checksum files.  Files or
  subfolders that are not the same are reported to the user.  The intention is
  to decide if two different distribution folders have the same contents.  You
  can:

     1. Compare two folders.  One or both folders may be represented by
        checksum files.
     2. Create a checksum file for a folder.  This is the most efficient way of
        remembering the contents of a folder for later comparison.
     3. Update a checksum by assuming that files have not changed if they have
        the same name, date, and size.  This is much faster than creating a new
        checksum for large collections on a local disk drive.  However, it is
        not recommended for files copied over a network or downloaded from the
        internet.
     4. Find duplicate files with the same MD5 or SHA1 checksum.

  Checksum files are used when the original files or folders are not available.
  A checksum file is a plain text file in XML (Extensible Markup Language)
  format with the name, size, and checksums for each file.  Checksums are small
  hexadecimal "signatures" for testing whether or not files have been copied
  correctly, such as over a network.  One person sends a file along with the
  checksum computed on the original computer.  A second person calculates a
  similar checksum for the received file, and if the two checksums agree, then
  the received file is assumed to be correct.  This CompareFolders application
  supports CRC32, MD5, and SHA1 checksums.  It is extremely unlikely that two
  files will have the same MD5 or the same SHA1 checksum and still be different
  (which can't be said about CRC32).  For documentation on XML, see:

      Extensible Markup Language (XML)
      http://en.wikipedia.org/wiki/XML
      http://www.xml.com/
      http://www.w3.org/XML/

  The XML output produced by this program can be read by most XML applications,
  such as those on many internet web sites.  See the FileChecksum Java
  application for an easy way to generate or test checksums for a single file.
  See the FindDupFiles Java application to look for duplicate files based on
  MD5 checksums.  CompareFolders differs from UNIX "md5sum" and "sha1sum" and
  Windows "MD5summer" in that it supports multiple checksums and uses XML
  format for storing checksums in text files.  Other programs for exact file
  comparisons (byte-by-byte) are "comp" on DOS/Windows and "cmp" on UNIX;
  "WinDiff" on Windows will compare folders and subfolders.

  GNU General Public License (GPL)
  --------------------------------
  CompareFolders3 is free software: you can redistribute it and/or modify it
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

      java  CompareFolders3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  CompareFolders3  -s  d:\fonts  d:\temp\checksum.txt  >errors.txt

  The console application will return an exit status of 1 for success, -1 for
  failure, and 0 for unknown.  The graphical interface can be very slow when
  the output text area gets too big, which will happen if thousands of files
  are reported.  For both with over 20,000 files, you may need to set the
  "-Xmx" option on the "java" command to allocate more memory:

      java  -Xmx150M  CompareFolders3

  would give the program a maximum of 150 megabytes (MB) of memory for
  temporary data.  The default is around -Xmx60M or 60 megabytes for Java 1.4
  on Windows 2000.

  Buffer size is one option that can significantly affect speed in some
  situations.  Default sizes are nearly optimal when files are on hard disk
  drives: 64 KB for checksums and 4 MB for comparisons.  (A larger comparison
  size avoids thrashing the disk drive.)  Network shares and CD/DVD drives are
  faster with smaller buffer sizes around 1 KB, and the best size may be
  slightly larger depending upon the speed of your devices.  This is not
  something that a Java program can recognize and compensate for.  If
  performance is slower than you would expect, try changing the "-b" option on
  the command line, and be very careful about judging the results.  Buffer
  sizes are almost always a power of two: 1 KB, 2 KB, 4 KB, 8 KB, etc.  By the
  way, comparing two folders on the same CD/DVD disc is not recommended, due to
  the long latency of "seek" operations on essentially sequential media.

  Restrictions and Limitations
  ----------------------------
  The XML parser used by this program to read checksum files is very simple and
  can not handle the full XML language, as would be supported in optional Java
  1.5 (5.0) and later packages.  Only simple tags (elements) without attributes
  are recognized.  Everything else is either ignored or will generate an error.
  The parser is really only smart enough to read back its own output files.

  File modification time stamps in checksum files can vary depending upon the
  current time zone and rules for daylight saving time (DST).  This may cause
  the "update checksum" action to recompute checksums when in fact nothing has
  changed.  Refreshing checksums occasionally is a good idea, but it would be
  better if the date and time had an invariant form.  The problem has been
  confirmed with Java 1.4 through 6 on Windows 2000/XP and for FAT32 volumes.
  NTFS volumes are not affected.  Both Java and Windows are adjusting for DST:
  Windows 2000/XP uses the current DST offset, and Java tries to be
  historically correct.  Java, of course, gets its information from the
  underlying operating system, after any changes that the OS makes.

  Please avoid slashes (/) or backslashes (\) in file and folder names.  These
  are illegal on Windows but accepted on Macintosh computers.  The Java
  run-time may incorrectly parse these as additional file or folder separators,
  or may substitute them with other characters that obviously won't match the
  original file names.  Slashes have always had a special meaning on UNIX-based
  operating systems, including Linux and Mac OS X.  For maximum compatibility,
  you should avoid all of the following characters: " * / : < > ? \ | and don't
  start a name with "." or a space, and don't end a name with a space.

  Suggestions for New Features
  ----------------------------
  (1) Change command-line parameters for console application to have a keyword
      ("command") as first parameter, followed by one or two file names.  This
      would allow the console application to run all features available in the
      graphical application.  KF, 2007-03-02.
  (2) Allow an option on the "compare folders" action to consider two files to
      be equal if they have the same name, size, date, and time -- without
      reading the contents or computing checksums.  This suggestion was posted
      2009-01-23 in a review on Download.com.  Note that comparing time stamps
      will be subject to the same time zone and DST problem described above.
  (3) There is no specific handling of run-time errors for insufficient memory.
      This happens most often with checksum files, since checksums are read and
      buffered in memory before processing begins.  KF, 2010-01-18.
  (4) Accept and produce checksum files in other formats, such as Linux md5sum
      and sha1sum, even if they contain less information.  KF, 2010-09-11.
  (5) Symbolic links in folders may not be handled properly, depending upon how
      well the operating system and Java run-time environment resolve links on
      behalf of applications.  Poor error messages may be produced ("can't
      compare files with folders").  The same may occur for anything that's not
      a normal file or folder: deleted directory entries, devices, protected
      system files/folders, etc.  Converting abstract file objects into
      canonical form might resolve some stubborn links, but this has not yet
      been tested or demonstrated in any Java implementation, and would have to
      be done at precisely the right time to avoid reporting and/or using the
      wrong file or folder name.  (Think about sorting a folder with ten links
      to files by the same name but in different folders.)  Ignoring "non-file"
      objects is dangerous and may give the false impression that a comparison
      was successful.  Most code here assumes that Java File objects are either
      files, folders, or an error.  KF, 2011-10-30, with help from KS.
  (6) By its general nature, this program can easily consume all resources on a
      computer (CPU, I/O, etc).  A "Pause" button would help.  Checksums could
      be interrupted, saved, and resumed (updated) at a later date, since even
      incomplete checksums are structurally valid.  KF, 2011-10-31.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.security.*;           // MD5 and SHA1 message digests (checksums)
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import java.util.zip.*;           // CRC32 checksums
import javax.swing.*;             // newer Java GUI support

public class CompareFolders3
{
  /* constants */

  static final String[] ACTION_CHOICES = {"compare folders", "create checksum",
    "find duplicates", "update checksum"};
  static final int ACTION_COMPARE_FOLDERS = 0; // index in <ACTION_CHOICES>
  static final int ACTION_CREATE_CHECKSUM = 1;
  static final int ACTION_FIND_DUPLICATES = 2;
  static final int ACTION_UPDATE_CHECKSUM = 3;

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.";
  static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"; // date/time format
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String ENDFILE_TOKEN = "<end of file>"; // token for end-of-file
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final String INDENT_STEP = "   "; // output indent for each XML level
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Compare Files, Folders, Checksums - by: Keith Fenske";
  static final String[] SHOW_CHOICES = {"summary only", "show different",
    "show identical", "show all files"};
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates

  /* Optimal buffer sizes depend upon each computer's hardware and operating
  system.  The following are for Java 1.4 on Windows 2000/XP.  Checksums are
  sequential reads from a single source, and anything else is slower than the
  system size of 64 KB.  Comparisons between two different sources don't show
  much variation in speed.  Comparisons between files/folders on the same hard
  drive benefit from larger buffers to avoid excessive disk seeks; one test
  with 16 MB was almost twice as fast as the 64 to 256 KB range.  No size is
  perfect for all possible scenarios.  Using 16 MB can be twice as *slow* as
  any smaller size for checksums.  Hence, buffer sizes below are different for
  checksums and comparisons.  Sizes are powers of two from 4 KB to 16 MB. */

  static final int BUFFER_CHECKSUM = 0x10000; // checksum buffer size (64 KB)
  static final int BUFFER_COMPARE = 0x400000; // compare buffer size (4 MB)
  static final long BUFFER_REPORT = 1363149; // try status update every 1.3 MB

  /* CompareFolders was written before I started using a global variable called
  <hiddenFlag> and the -h command-line option to control whether or not hidden
  files and folders are processed.  New code added to CompareFolders references
  this global variable, so to be consistent, it is defined here as a constant
  that is always true (always do hidden files and folders).  Don't change this
  constant, unless you are willing to re-examine every line of the old code! */

  static final boolean hiddenFlag = true; // always do hidden files and folders

  /* All file systems have limits on how accurately they store dates and times.
  Don't change file dates when the millisecond difference is too small.  This
  must be at least 2000 ms (2 seconds) for MS-DOS FAT16/FAT32 file systems. */

  static final long MILLI_FUZZ = 2000; // ignore time changes smaller than this

  /* class variables */

  static JComboBox actionDialog;  // graphical choice for program actions
  static int bufferChecksumSize, bufferCompareSize; // default or chosen sizes
  static long bufferReportSize;   // default or chosen status update size
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JCheckBox caseCheckbox;  // graphical option for <caseFlag>
  static boolean caseFlag;        // true if upper/lower case names different
  static boolean consoleFlag;     // true if running as a console application
  static JLabel countDialog;      // running status count of files and folders
  static String countPending;     // will become <countDialog> after delay
  static Thread doStartThread;    // separate thread for doStartButton() method
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static JButton firstFileButton; // click to browse first file or folder
  static JTextField firstFileDialog; // name of first file or folder
  static File firstFileSaved;     // saved Java File object selected by user
  static String firstFileString;  // saved text string with file name and path
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static SimpleDateFormat formatDate; // formats long date/time as numeric text
  static NumberFormat formatPointOne; // formats with one decimal digit
//static boolean hiddenFlag;      // true if we process hidden files or folders
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JTextArea outputText;    // generated report if running as GUI
  static boolean pathSafeFlag;    // true if all files have valid path names
  static int readFileLine;        // current line number for input file
  static int readFilePendingChar; // one pending character, like C's ungetc()
  static boolean readFilePendingFlag; // true if <readFilePendingChar> valid
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JButton saveButton;      // "Save" button for writing output text
  static JCheckBox scrollCheckbox; // graphical option for <scrollFlag>
  static boolean scrollFlag;      // true if we scroll calls to <putOutput>
  static JButton secondFileButton; // click to browse second file or folder
  static JTextField secondFileDialog; // name of second file or folder
  static File secondFileSaved;    // saved Java File object selected by user
  static String secondFileString; // saved text string with file name and path
  static boolean showDiffFlag;    // true if we show different files, errors
  static JComboBox showFileDialog; // graphical choice for <show...Flag>
  static boolean showSameFlag;    // true if we show identical (similar) files
  static JButton startButton;     // "Start" button to begin file processing
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static String systemFileSep;    // file/folder separator for local system
//static String systemNewline;    // newline characters for local system
  static long totalDiffer;        // total number of different files or errors
  static long totalFiles;         // total number of files found (all types)
  static long totalFolders;       // total number of folders and subfolders
  static long totalSame;          // total number of files that are identical
  static long totalSize;          // total number of bytes in all files

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
    Insets inputMargins;          // margins on input text areas
    boolean maximizeFlag;         // true if we maximize our main window
    String osName;                // operating system name according to Java
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    bufferChecksumSize = BUFFER_CHECKSUM; // default buffer size for checksums
    bufferCompareSize = BUFFER_COMPARE; // default buffer size for compares
    bufferReportSize = BUFFER_REPORT; // default status update report size
    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    caseFlag = true;              // uppercase/lowercase distinct in file names
    consoleFlag = false;          // assume no files or folders on command line
    countPending = EMPTY_STATUS;  // begin with no text for <countDialog>
    firstFileSaved = null;        // user has not selected file with GUI dialog
    firstFileString = "";         // no first parameter yet (file/folder name)
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
//  hiddenFlag = true;            // by default, process hidden files, folders
    maximizeFlag = false;         // by default, don't maximize our main window
    recurseFlag = true;           // default for processing folders, subfolders
    scrollFlag = true;            // by default, scroll calls to <putOutput>
    secondFileSaved = null;       // user has not selected file with GUI dialog
    secondFileString = "";        // no second parameter yet (file/folder name)
    showDiffFlag = true;          // by default, show different files, errors
    showSameFlag = false;         // by default, don't show identical files
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    totalDiffer = totalFiles = totalFolders = totalSame = totalSize = 0;
                                  // reset all global file counters
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatDate = new SimpleDateFormat(DATE_FORMAT); // create date/time format

    formatPointOne = NumberFormat.getInstance(); // current locale
    formatPointOne.setGroupingUsed(true); // use commas or digit groups
    formatPointOne.setMaximumFractionDigits(1); // force one decimal digit
    formatPointOne.setMinimumFractionDigits(1);

    /* Some features depend upon the current operating system.  Apple Macintosh
    can have file names with special characters that are illegal in UNIX-style
    path names.  Folders from JFileChooser are subject to a "Hide protected
    operating system files" option in Windows Explorer (Tools menu, Folder
    Options, View tab), while File objects created from path names are not. */

    osName = System.getProperty("os.name"); // get current operating system
    mswinFlag = osName.startsWith("Windows"); // if running Microsoft Windows
    pathSafeFlag = ! osName.startsWith("Mac OS");
                                  // true if all files have valid path names

    /* Get the system's default newline characters for writing output, and the
    local separator for file and folder names. */

    systemFileSep = " / ";        // force spaced-out UNIX style, ignore ...
//  systemFileSep = System.getProperty("file.separator"); // ... local system
//  systemNewline = System.getProperty("line.separator");

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

      else if (word.startsWith("-b") || (mswinFlag && word.startsWith("/b")))
      {
        /* This is an advanced option to set the buffer size when reading
        files.  The default sizes are good for most situations.  Some unusual
        cases may benefit from larger or smaller buffers, such as when reading
        from low-speed devices or devices with long latency (CD/DVD).  Setting
        a size here will use the same value for both the checksum buffer size
        and the comparison buffer size (which differs from the defaults).  We
        limit the overall range of the sizes, but we do not force them to be
        powers of two.  CAREFUL TESTING IS STRONGLY RECOMMENDED. */

        long size = -1;           // default value for buffer size in bytes
        Pattern pattern = Pattern.compile("(\\d{1,9})(|b|k|kb|kib|m|mb|mib)");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          size = Long.parseLong(matcher.group(1)); // numeric part of size
          String suffix = matcher.group(2); // what was given after numbers
          if ((suffix == null) || (suffix.length() == 0) || suffix.equals("b"))
            { /* do nothing: accept number as a size in bytes */ }
          else if (suffix.startsWith("k")) // if "K" or "KB" suffix given
            size *= 0x400;        // multiply by kilobytes
          else                    // otherwise, assume "M" or "MB" suffix
            size *= 0x100000;     // multiply by megabytes
        }
        else                      // bad syntax or too many digits
        {
          size = -1;              // set result to an illegal value
        }

        /* We accept buffer sizes smaller and larger than the documented range,
        so that die-hard experimenters have something to play with.  Java 1.4
        on Windows 2000/XP starts to freeze at sizes bigger than 64 MB (for the
        buffer size in this program), and the standard -Xmx heap size of around
        60 MB allows us only two buffers of 16 MB each. */

        if ((size < 0x100) || (size > 0x4000000)) // 256 bytes to 64 megabytes
        {
          System.err.println("Buffer size must be from 4KB to 16MB: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        bufferChecksumSize = (int) size; // user's choice becomes checksum size
        bufferCompareSize = (int) size; // and choice also becomes compare size
//      bufferReportSize =        // size doesn't change for status updates
      }

      else if (word.equals("-c") || (mswinFlag && word.equals("/c"))
        || word.equals("-c1") || (mswinFlag && word.equals("/c1")))
      {
        caseFlag = true;          // uppercase/lowercase distinct in file names
      }
      else if (word.equals("-c0") || (mswinFlag && word.equals("/c0")))
        caseFlag = false;         // ignore uppercase/lowercase in file names

//    else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
//      || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
//    {
//      hiddenFlag = true;        // process hidden files and folders
//    }
//    else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
//      hiddenFlag = false;       // ignore hidden files or subfolders

      else if (word.equals("-m0") || (mswinFlag && word.equals("/m0")))
      {
        showDiffFlag = false;     // don't show different files
        showSameFlag = false;     // don't show identical files
      }
      else if (word.equals("-m1") || (mswinFlag && word.equals("/m1")))
      {
        showDiffFlag = true;      // yes, show different files
        showSameFlag = false;     // don't show identical files
      }
      else if (word.equals("-m2") || (mswinFlag && word.equals("/m2")))
      {
        showDiffFlag = false;     // don't show different files
        showSameFlag = true;      // yes, show identical files
      }
      else if (word.equals("-m3") || (mswinFlag && word.equals("/m3")))
      {
        showDiffFlag = true;      // yes, show different files
        showSameFlag = true;      // yes, show identical files
      }

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // process files, folders, and subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // process only explicit files, folders

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
        or folder name. */

        consoleFlag = true;       // don't allow GUI methods to be called
        if (firstFileString.length() == 0)
          firstFileString = args[i]; // save first parameter as file/folder
        else if (secondFileString.length() == 0)
          secondFileString = args[i]; // save second parameter as file/folder
        else
        {
          System.err.println("Too many file or folder names on command line: "
            + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }
    }

    /* Run as a console application if file or folder names were given on the
    command line.  Exit to the system with an integer status: +1 for success,
    -1 for failure, and 0 for unknown. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      /* We ignore <cancelFlag> because the user has no way of interrupting us
      at this point (no graphical interface).  Rather than complicating the
      code in this main() method, we call a subroutine to do the processing. */

      System.exit(doConsoleFiles(firstFileString, secondFileString));
                                  // exit from application with this status
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

    /* We set a preferred font for GUI items that we create while building our
    layout below.  The following sets a default font for system items such as
    JFileChooser and JOptionPane dialog boxes.  There is no guarantee that the
    Java run-time environment will pay attention to these requests, or that the
    same attributes will be available in future releases of Java.  To avoid
    damaging the default look-and-feel, set only those attributes that are
    actually needed and do so before GUI items are created.  Passing a Font
    object to UIManager.put() works but can't be changed later, so pass a
    FontUIResource object or even that wrapped in UIDefaults.ProxyLazyValue.

    The following information was obtained from version 1.4.2 source code for
    javax.swing.plaf.basic.BasicLookAndFeel.java in the Sun JDK/SDK. */

    if (false)                    // disable if system fonts look better
//  if (buttonFont != null)       // are we changing away from defaults?
    {
      javax.swing.plaf.FontUIResource uifont = new
        javax.swing.plaf.FontUIResource(buttonFont);

      UIManager.put("Button.font", uifont);
//    UIManager.put("CheckBox.font", uifont);
//    UIManager.put("CheckBoxMenuItem.acceleratorFont", uifont);
//    UIManager.put("CheckBoxMenuItem.font", uifont);
//    UIManager.put("ColorChooser.font", uifont);
      UIManager.put("ComboBox.font", uifont);
//    UIManager.put("EditorPane.font", uifont);
//    UIManager.put("FormattedTextField.font", uifont);
//    UIManager.put("InternalFrame.titleFont", uifont);
      UIManager.put("Label.font", uifont);
      UIManager.put("List.font", uifont);
//    UIManager.put("Menu.acceleratorFont", uifont);
//    UIManager.put("Menu.font", uifont);
//    UIManager.put("MenuBar.font", uifont);
//    UIManager.put("MenuItem.acceleratorFont", uifont);
//    UIManager.put("MenuItem.font", uifont);
//    UIManager.put("OptionPane.buttonFont", uifont);
//    UIManager.put("OptionPane.font", uifont);
//    UIManager.put("OptionPane.messageFont", uifont);
//    UIManager.put("Panel.font", uifont);
//    UIManager.put("PasswordField.font", uifont);
//    UIManager.put("PopupMenu.font", uifont);
//    UIManager.put("ProgressBar.font", uifont);
//    UIManager.put("RadioButton.font", uifont);
//    UIManager.put("RadioButtonMenuItem.acceleratorFont", uifont);
//    UIManager.put("RadioButtonMenuItem.font", uifont);
//    UIManager.put("ScrollPane.font", uifont);
//    UIManager.put("Spinner.font", uifont);
//    UIManager.put("TabbedPane.font", uifont);
//    UIManager.put("Table.font", uifont);
//    UIManager.put("TableHeader.font", uifont);
//    UIManager.put("TextArea.font", uifont);
      UIManager.put("TextField.font", uifont);
//    UIManager.put("TextPane.font", uifont);
//    UIManager.put("TitledBorder.font", uifont);
//    UIManager.put("ToggleButton.font", uifont);
//    UIManager.put("ToolBar.font", uifont);
      UIManager.put("ToolTip.font", uifont);
//    UIManager.put("Tree.font", uifont);
//    UIManager.put("Viewport.font", uifont);
    }

    /* Initialize shared graphical objects. */

    action = new CompareFolders3User(); // create our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser
    inputMargins = new Insets(2, 4, 2, 4); // top, left, bottom, right margins
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
    and hence are only numbered (panel01, panel02, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Create a horizontal panel for the action buttons. */

    JPanel panel02 = new JPanel(new BorderLayout(20, 0));

    actionDialog = new JComboBox(ACTION_CHOICES);
    actionDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) actionDialog.setFont(buttonFont);
    actionDialog.setSelectedIndex(ACTION_COMPARE_FOLDERS); // highlight default
    actionDialog.setToolTipText("Action or feature to be performed.");
    actionDialog.addActionListener(action); // do last so don't fire early
    panel02.add(actionDialog, BorderLayout.WEST);

    JPanel panel03 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    startButton = new JButton("Start");
    startButton.addActionListener(action);
    if (buttonFont != null) startButton.setFont(buttonFont);
    startButton.setMnemonic(KeyEvent.VK_S);
    startButton.setToolTipText("Start finding/opening files.");
    panel03.add(startButton);
    panel03.add(Box.createHorizontalStrut(20));

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setEnabled(false);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding/opening files.");
    panel03.add(cancelButton);
    panel03.add(Box.createHorizontalStrut(20));

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    if (buttonFont != null) saveButton.setFont(buttonFont);
    saveButton.setMnemonic(KeyEvent.VK_O);
    saveButton.setToolTipText("Copy output text to a file.");
    panel03.add(saveButton);
    panel02.add(panel03, BorderLayout.CENTER);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel02.add(exitButton, BorderLayout.EAST);

    panel01.add(panel02);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for the first file name and button. */

    JPanel panel04 = new JPanel(new BorderLayout(10, 0));

    firstFileButton = new JButton("Dummy Button Text...");
    firstFileButton.addActionListener(action);
    if (buttonFont != null) firstFileButton.setFont(buttonFont);
    firstFileButton.setMnemonic(KeyEvent.VK_1);
    firstFileButton.setToolTipText("Browse for first file or folder name.");
    panel04.add(firstFileButton, BorderLayout.WEST);

    firstFileDialog = new JTextField(20);
    if (buttonFont != null) firstFileDialog.setFont(buttonFont);
    firstFileDialog.setMargin(inputMargins);
    firstFileDialog.addActionListener(action); // do last so don't fire early
    panel04.add(firstFileDialog, BorderLayout.CENTER);

    panel01.add(panel04);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for the second file name and button. */

    JPanel panel05 = new JPanel(new BorderLayout(10, 0));

    secondFileButton = new JButton("Dummy Button Text...");
    secondFileButton.addActionListener(action);
    if (buttonFont != null) secondFileButton.setFont(buttonFont);
    secondFileButton.setMnemonic(KeyEvent.VK_2);
    secondFileButton.setToolTipText("Browse for second file or folder name.");
    panel05.add(secondFileButton, BorderLayout.WEST);

    secondFileDialog = new JTextField(20);
    if (buttonFont != null) secondFileDialog.setFont(buttonFont);
    secondFileDialog.setMargin(inputMargins);
    secondFileDialog.addActionListener(action); // do last so don't fire early
    panel05.add(secondFileDialog, BorderLayout.CENTER);

    panel01.add(panel05);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for the options. */

    JPanel panel06 = new JPanel(new BorderLayout(20, 0));

    JPanel panel07 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for output text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel07.add(fontNameDialog);
    panel07.add(Box.createHorizontalStrut(5));

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
    panel07.add(fontSizeDialog);
    panel06.add(panel07, BorderLayout.WEST);

    JPanel panel08 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    showFileDialog = new JComboBox(SHOW_CHOICES);
    showFileDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) showFileDialog.setFont(buttonFont);
    showFileDialog.setSelectedIndex((showDiffFlag ? 1 : 0)
      + (showSameFlag ? 2 : 0));  // highlight default file selection
    showFileDialog.setToolTipText("Select which files to report.");
    showFileDialog.addActionListener(action); // do last so don't fire early
    panel08.add(showFileDialog);
    panel06.add(panel08, BorderLayout.CENTER);

    JPanel panel09 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    caseCheckbox = new JCheckBox("case", caseFlag);
    if (buttonFont != null) caseCheckbox.setFont(buttonFont);
    caseCheckbox.setToolTipText(
      "Select if uppercase, lowercase different in file names.");
    caseCheckbox.addActionListener(action); // do last so don't fire early
    panel09.add(caseCheckbox);
    panel09.add(Box.createHorizontalStrut(5));

    scrollCheckbox = new JCheckBox("scroll", scrollFlag);
    if (buttonFont != null) scrollCheckbox.setFont(buttonFont);
    scrollCheckbox.setToolTipText(
      "Select to scroll displayed text, line by line.");
    scrollCheckbox.addActionListener(action); // do last so don't fire early
    panel09.add(scrollCheckbox);
    panel09.add(Box.createHorizontalStrut(5));

    recurseCheckbox = new JCheckBox("subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText(
      "Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel09.add(recurseCheckbox);
    panel06.add(panel09, BorderLayout.EAST);

    panel01.add(panel06);

    /* Put above boxed options in a panel that is centered horizontally.  Use
    FlowLayout's horizontal gap to add padding on the left and right sides. */

    JPanel panel10 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel10.add(panel01);

    /* Use another BorderLayout for precise control over the margins. */

    JPanel panel11 = new JPanel(new BorderLayout(0, 0));
    panel11.add(Box.createVerticalStrut(11), BorderLayout.NORTH);
    panel11.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
    panel11.add(panel10, BorderLayout.CENTER);
    panel11.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
    panel11.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(18, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
//  outputText.setText("");       // clear output text area

    /* Create an entire panel just for the status message.  Set the margins
    with a second BorderLayout, because a few pixels higher or lower make a
    difference in whether the position of the status text looks correct. */

    JPanel panel12 = new JPanel(new BorderLayout(30, 0));

    statusDialog = new JLabel(statusPending, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    statusDialog.setToolTipText(
      "Running status as files are processed by the Start button.");
    panel12.add(statusDialog, BorderLayout.CENTER);

    countDialog = new JLabel(countPending, JLabel.RIGHT);
    if (buttonFont != null) countDialog.setFont(buttonFont);
    countDialog.setToolTipText("Running count of files and folders.");
    panel12.add(countDialog, BorderLayout.EAST);

    JPanel panel13 = new JPanel(new BorderLayout(0, 0));
    panel13.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
    panel13.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel13.add(panel12, BorderLayout.CENTER);
    panel13.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel13.add(Box.createVerticalStrut(4), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons and
    options above the text area.  Keep text in the center so that it expands
    horizontally and vertically.  Put status message at the bottom, which also
    expands. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel14 = mainFrame.getContentPane(); // where content meets frame
    panel14.setLayout(new BorderLayout(0, 0));
    panel14.add(panel11, BorderLayout.NORTH); // buttons and options
    panel14.add(new JScrollPane(outputText), BorderLayout.CENTER); // text area
    panel14.add(panel13, BorderLayout.SOUTH); // status message

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

    actionDialog.setSelectedIndex(ACTION_COMPARE_FOLDERS);
                                  // set correct button text, enabled/disabled
    firstFileDialog.requestFocusInWindow(); // shift focus to first file name

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  accessDescription() method

  Return a text string that describes our access to a file or folder.  This is
  only used by error messages in the doStartRunner() method.
*/
  static String accessDescription(File givenFile)
  {
    StringBuffer buffer;          // faster than String for multiple appends

    buffer = new StringBuffer();  // allocate empty string buffer for result
    if (givenFile == null) buffer.append("is the <null> object");
    else if (givenFile.exists() == false) buffer.append("does not exist");
    else                          // get more information about file or folder
    {
      buffer.append("is a");      // same for all files and folders
      if (givenFile.isHidden()) buffer.append(" hidden");

      if (givenFile.canRead())    // what access do we have to file/folder?
        if (givenFile.canWrite()) buffer.append(" read-write");
        else buffer.append(" read-only");
      else                        // can't read, but can we write?
        if (givenFile.canWrite()) buffer.append(" write-only");
        else buffer.append(" protected"); // may not be the best word here

      if (givenFile.isDirectory()) buffer.append(" directory/folder");
      else if (givenFile.isFile()) buffer.append(" file");
      else buffer.append(" unknown object");
    }
    return(buffer.toString());    // give caller our converted string

  } // end of accessDescription() method


/*
  calcFileChecksum() method

  Calculate the CRC32, MD5, and SHA1 checksums for a given file, and put those
  checksums into an object provided by the caller.  We look at the <cancelFlag>
  while we are running, and if that flag is true, then we close the file and
  set the checksums to empty strings.

  We always generate all three checksums.  The basic file I/O loop takes about
  35% of the time.  The CRC32 calculation takes about 5% of the time.  The MD5
  takes about 20% and the SHA1 takes about 40%.
*/
  static void calcFileChecksum(
    File givenFile,               // calculate checksums for this File object
    CompareFolders3File result)   // and put resulting checksums here
  {
    byte[] buffer;                // input buffer for reading file
    CRC32 crc32digest;            // object for calculating CRC32 checksum
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    int i;                        // index variable
    FileInputStream inStream;     // input file stream
    MessageDigest md5digest;      // object for calculating MD5 checksum
    MessageDigest sha1digest;     // object for calculating SHA1 checksum
    long sizeDone;                // how much of <fileSize> has been finished
    String sizePrefix, sizeSuffix; // pre-formatted portions of size message
    long sizeUser;                // last <sizeDone> reported to user
    String text;                  // text string in middle of hex conversion

    filePath = givenFile.getPath(); // get name of caller's file, with path
    fileSize = givenFile.length(); // get size of caller's file in bytes
    result.crc32 = "";            // start with empty strings for all checksums
    result.md5 = "";
    result.sha1 = "";

    if (consoleFlag == false)     // only format this message if running as GUI
      setStatusMessage("Checksum " + filePath);

    try
    {
      /* First try to open the user's file, since this may generate an error
      and make all of the following statements meaningless. */

      inStream = new FileInputStream(givenFile);
                                  // open user's file for reading bytes

      /* We should now be able to proceed without errors. */

      buffer = new byte[bufferChecksumSize];
                                  // allocate big/faster input buffer
      crc32digest = new CRC32();  // allocate new object for CRC32 checksum
      md5digest = MessageDigest.getInstance("MD5");
                                  // initialize MD5 message digest
      sha1digest = MessageDigest.getInstance("SHA-1");
                                  // initialize SHA1 message digest
      sizeDone = sizeUser = 0;    // we haven't read anything yet
      sizePrefix = sizeSuffix = null; // don't format size message until needed

      while ((i = inStream.read(buffer, 0, bufferChecksumSize)) > 0)
      {
        if (cancelFlag) break;    // stop if user hit the panic button

        /* Update the checksum calculations. */

        crc32digest.update(buffer, 0, i); // CRC32 checksum
        md5digest.update(buffer, 0, i); // MD5 checksum
        sha1digest.update(buffer, 0, i); // SHA1 checksum

        /* Update the GUI status if this is a big file. */

        sizeDone += i;            // add to number of bytes finished
        if ((consoleFlag == false)
          && ((sizeDone - sizeUser) > bufferReportSize))
        {
          if (sizePrefix == null) // have we formatted the constant portions?
          {
            sizePrefix = "Checksum " + filePath + " - ";
            sizeSuffix = " of " + formatMegabytes(fileSize) + " MB";
          }
          sizeUser = sizeDone;    // remember what we last told the user
          setStatusMessage(sizePrefix + formatMegabytes(sizeDone) + sizeSuffix);
        }
      }
      inStream.close();           // close input file

      /* If we weren't cancelled by the user, then convert the final checksums
      into hexadecimal strings. */

      if (!cancelFlag)            // don't do more work if cancelled by user
      {
        /* Convert the CRC32 checksum to a hexadecimal string.  We must pad
        with leading zeros since the toHexString() method doesn't do this. */

        text = "00000000" + Long.toHexString(crc32digest.getValue());
        result.crc32 = text.substring(text.length() - 8);

        /* Convert the MD5 checksum to a hexadecimal string.  We call another
        method to convert raw bytes to hex, because SHA1 needs the same. */

        result.md5 = formatHexBytes(md5digest.digest());

        /* Convert the SHA1 checksum to a hexadecimal string. */

        result.sha1 = formatHexBytes(sha1digest.digest());
      }
    }
    catch (IOException except)
    {
      putError("Can't read " + filePath + " - " + except.getMessage());
    }
    catch (NoSuchAlgorithmException except)
    {
      putError("Bad checksum algorithm: " + except.getMessage());
    }
  } // end of calcFileChecksum() method


/*
  canonicalFile() method

  In most of this program, we don't care what a user types for a file name, so
  long as it gets accepted by the system.  In a few places, such as in checksum
  files, we much prefer to have the exact or "official" file name.  This method
  attempts to fetch the "canonical" Java File object.  Failing that, it returns
  the caller's File object, which is probably an error-free abstract file name.
*/
  static File canonicalFile(File givenFile)
  {
    File result;                  // our best effort at a canonical File object

    try                           // catch I/O errors during directory search
    {
      result = givenFile.getCanonicalFile(); // do full directory resolution
    }
    catch (IOException ioe)       // if the system couldn't handle file name
    {
      result = givenFile;         // return caller's parameter, without comment
    }
    return(result);               // give caller whatever we could find
  }


/*
  canWriteFile() method

  The caller gives us a Java File object.  We return true if it seems safe to
  write to this file.  That is, if the file doesn't exist and we can create a
  new file, or if the file exists and the user gives permission to replace it.

  This is a GUI method; do not call this method from a console application.
*/
  static boolean canWriteFile(File givenFile)
  {
    boolean result;               // status flag that we return to the caller

    if (givenFile.isDirectory())  // can't write to folders/directories
    {
      putError("");               // blank line
      putError(givenFile.getName()
        + " is a directory or folder; please select a normal file.");
      result = false;             // don't try to open this "file" for writing
    }
    else if (givenFile.isHidden()) // won't write to hidden (protected) files
    {
      putError("");
      putError(givenFile.getName()
        + " is a hidden or protected file; please select a normal file.");
      result = false;
    }
    else if (givenFile.isFile() == false) // are we creating a new file?
    {
      result = true;              // assume we can create new file by this name
    }
    else if (givenFile.canWrite()) // file exists, but can we write to it?
    {
      result = (JOptionPane.showConfirmDialog(mainFrame, (givenFile.getName()
        + " already exists.\nDo you want to replace this with a new file?"))
        == JOptionPane.YES_OPTION);
    }
    else                          // if we can't write to an existing file
    {
      putError("");
      putError(givenFile.getName()
        + " is locked or write protected; can't write to this file.");
      result = false;
    }

    return(result);               // give caller our best guess about writing

  } // end of canWriteFile() method


/*
  cleanChecksum() method

  Return a string that has only the hexadecimal digits '0' to '9' and 'a' to
  'f'.  Uppercase 'A' to 'F' are converted to lowercase 'a' to 'f'.

  Some spacing characters are accepted and removed.  If we find anything that
  we don't like, then an empty string is returned instead.  Of course, if the
  input string is empty, our result will similarly be empty.  The caller must
  decide what empty really means!
*/
  static String cleanChecksum(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = input.length();      // get size of input string in characters
    for (i = 0; i < length; i ++)
    {
      ch = input.charAt(i);       // get one character from input string
      if ((ch == ' ') || (ch == ':')) // ignore spaces and colons
        { /* do nothing */ }
      else if ((ch >= '0') && (ch <= '9'))
        buffer.append(ch);        // accept decimal digit and append to result
      else if ((ch >= 'a') && (ch <= 'f'))
        buffer.append(ch);        // accept lowercase hexadecimal digit
      else if ((ch >= 'A') && (ch <= 'F'))
        buffer.append((char) (ch - 'A' + 'a')); // but convert uppercase hex
      else
        return("");               // return empty string for bad input
    }
    return(buffer.toString());    // give caller our converted string

  } // end of cleanChecksum() method


/*
  compareFolderChecksum() method

  Compare the contents of the first file/folder with the checksums stored in
  the second file.

  The lazy way of doing this would be to generate recursive checksums for the
  first file/folder and then call the compareTwoChecksums() method.  This would
  be easy but not good, because:

  (1) Files may appear in only one folder and calculating their checksums would
      be a waste of time, especially for big files;
  (2) Files may differ in size, in which case checksums are not needed; and
  (3) There might be a long delay while calculating checksums before we start
      comparing files, which would annoy an impatient user.
*/
  static int compareFolderChecksum(
    File firstFile,               // first file or folder
    File secondFile)              // second checksum file (only)
  {
    CompareFolders3File secondChecksum; // second calculated checksums
    int status;                   // exit status for console application

    /* Attempt to parse second file as recursive checksums in XML format. */

    secondChecksum = readChecksumFile(secondFile); // parse checksum file
    if (cancelFlag || (secondChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)

    /* Call our recursive subroutine to compare the file/folder with the
    checksums. */

    compareFolderChecksumRecursive("", firstFile, secondChecksum);
    if (cancelFlag)               // did something go wrong?
      status = EXIT_UNKNOWN;      // any problem means unknown result status
    else
    {
      putError("Found " + prettyPlural(totalDiffer, "difference")
        + " and " + prettyPlural(totalSame, "identical file") + ".");
      if (totalDiffer > 0)        // any errors means return with failure
        status = EXIT_FAILURE;
      else if (totalSame > 0)     // at least one identical means success
        status = EXIT_SUCCESS;
      else                        // zero files means we don't know anything
        status = EXIT_UNKNOWN;
    }
    return(status);               // return whatever status we could find

  } // end of compareFolderChecksum() method

  static void compareFolderChecksumRecursive(
    String pathPrefix,            // path prefix for file names, or "" to start
    File firstFile,               // first file or folder
    CompareFolders3File secondChecksum) // second calculated checksums
  {
    String compareDiffer;         // which checksums or sizes are different
    String compareSame;           // which checksums or sizes are the same
    boolean compareSuccess;       // true if any reliable checksum equals

    CompareFolders3File firstChecksum; // first calculated checksums
    int firstIndex;               // current index into <firstList>
    File[] firstList;             // list of files/subfolders for first folder
    String firstName;             // current name for first file or subfolder

    int secondCount;              // number of elements in <secondList>
    CompareFolders3File secondEntry; // one entry from <secondList>
    int secondIndex;              // current index into <secondList>
    Vector secondList;            // list of files/subfolders if second folder

    /* Begin by ending early if the user has cancelled. */

    if (cancelFlag) return;       // stop if user hit the panic button

    /* Decide if we are comparing two files or two folders. */

    secondList = secondChecksum.list; // null for files, list for folders

    if (firstFile.isDirectory() && (secondList != null))
    {
      /* Compare the contents of two folders.  Both folders should have the
      same files and subfolders, once sorted into similar order. */

      firstIndex = 0;             // start at beginning of the list
      firstList = sortFileList(firstFile.listFiles());
                                  // get sorted contents of first folder
      secondCount = secondList.size(); // get number of elements in list
      secondIndex = 0;
      setStatusMessage("Folder " + pathPrefix + firstFile.getName());

      while ((cancelFlag == false) && ((firstIndex < firstList.length)
       || (secondIndex < secondCount)))
      {
        if (firstIndex >= firstList.length) // at end of first list?
        {
          secondEntry = (CompareFolders3File) secondList.get(secondIndex);
          putSelectDiff(pathPrefix + secondEntry.name
            + ((secondEntry.list != null) ? " (subfolder)" : "")
            + " - only in checksum file");
          secondIndex ++;         // continue stepping through second list
          totalDiffer ++;         // increment number of differences
        }
        else if (secondIndex >= secondCount) // at end of second list?
        {
          firstName = firstList[firstIndex].getName(); // get first name
          putSelectDiff(pathPrefix + firstName
            + ((firstList[firstIndex].isDirectory()) ? " (subfolder)" : "")
            + " - only in file folder");
          firstIndex ++;          // continue stepping through first list
          totalDiffer ++;         // increment number of differences
        }
        else                      // both folders still have files/subfolders
        {
          firstName = firstList[firstIndex].getName(); // get first name
          secondEntry = (CompareFolders3File) secondList.get(secondIndex);
          if ((caseFlag && firstName.equals(secondEntry.name))
            || ((!caseFlag) && firstName.equalsIgnoreCase(secondEntry.name)))
          {
            /* The files or folders have the same name.  Files have null lists;
            folders have non-null lists.  Append to the relative path name if
            we are searching subfolders, but not if we are comparing files. */

            if (firstList[firstIndex].isDirectory() // comparing folders?
              && (secondEntry.list != null))
            {
              if (recurseFlag)    // does user want us to do subfolders?
              {
                setStatusMessage("Folder " + pathPrefix + firstName);
                compareFolderChecksumRecursive((pathPrefix + firstName
                  + systemFileSep), firstList[firstIndex], secondEntry);
              }
              else
                putOutput(pathPrefix + firstName + " - ignoring subfolder");
            }
            else
            {
              compareFolderChecksumRecursive(pathPrefix, firstList[firstIndex],
                secondEntry);
            }
            firstIndex ++;        // continue stepping through first list
            secondIndex ++;       // continue stepping through second list
          }
          else
          {
            /* The file or folder names are different.  Try to figure out why,
            and move forward in the proper sequence.  The checksum sorting key
            will normally already be created, unless in a folder with only one
            file. */

            if (caseFlag && firstName.equalsIgnoreCase(secondEntry.name))
            {
              /* Warn user about having names that differ only in uppercase
              versus lowercase.  Don't increment the error counter. */

              putOutput(pathPrefix + firstName
                + " - uppercase versus lowercase warning");
            }

            if (secondEntry.sortkey == null)
              secondEntry.sortkey = createSortKey((secondEntry.list != null),
                secondEntry.name);
            if (((createSortKey(firstList[firstIndex].isDirectory(),
              firstName)).compareTo(secondEntry.sortkey)) > 0)
            {
              /* First list is ahead of second list. */

              putSelectDiff(pathPrefix + secondEntry.name
                + ((secondEntry.list != null) ? " (subfolder)" : "")
                + " - only in checksum file");
              secondIndex ++;     // continue stepping through second list
              totalDiffer ++;     // increment number of differences
            }
            else
            {
              /* Second list is ahead of first list. */

              putSelectDiff(pathPrefix + firstName
                + ((firstList[firstIndex].isDirectory()) ? " (subfolder)" : "")
                + " - only in file folder");
              firstIndex ++;      // continue stepping through first list
              totalDiffer ++;     // increment number of differences
            }
          } // end of first name equals second name
        }
      } // end of while loop for folder names
    }

    else if (firstFile.isFile() && (secondList == null))
    {
      /* We are comparing two files.  Only if necessary, calculate the checksum
      for the first file and compare it against the second checksum. */

      firstName = firstFile.getName(); // get name of first file
      if ((secondChecksum.size >= 0)
        && (firstFile.length() != secondChecksum.size))
      {
        /* Files are different sizes, no need to create checksum or compare. */

        putSelectDiff(pathPrefix + firstName + " - different size");
        totalDiffer ++;           // increment number of differences
      }
      else if ((secondChecksum.crc32.length() == 0)
        && (secondChecksum.md5.length() == 0)
        && (secondChecksum.sha1.length() == 0))
      {
        /* Second checksum information doesn't actually contain any useful
        checksums, so don't waste time calculating checksum for first file. */

        putSelectDiff(pathPrefix + firstName + " - no reliable comparison");
        totalDiffer ++;           // increment number of differences
      }
      else
      {
        compareDiffer = "";       // append observed results to these strings
        compareSame = "file size"; // already know file size is the same
        compareSuccess = false;   // only declare success for reliable compares
        firstChecksum = new CompareFolders3File(); // just need data structure

        calcFileChecksum(firstFile, firstChecksum); // calculate checksum
        if (!cancelFlag)          // don't do more work if cancelled by user
        {
          /* We know that all checksums are present in <firstChecksum> because
          we calculated it.  We don't know this for <secondChecksum>.  We do
          know that the second checksum has strings, even if they are empty. */

          if (secondChecksum.crc32.length() > 0)
          {
            if (firstChecksum.crc32.equals(secondChecksum.crc32))
            {
              compareSame += ((compareSame.length() > 0) ? ", " : "") + "CRC32";
//            compareSuccess = true; // but should we accept CRC32 as reliable?
            }
            else
              compareDiffer += ((compareDiffer.length() > 0) ? ", " : "")
                + "CRC32";
          }

          if (secondChecksum.md5.length() > 0)
          {
            if (firstChecksum.md5.equals(secondChecksum.md5))
            {
              compareSame += ((compareSame.length() > 0) ? ", " : "") + "MD5";
              compareSuccess = true; // accept MD5 as reliable checksum
            }
            else
              compareDiffer += ((compareDiffer.length() > 0) ? ", " : "")
                + "MD5";
          }

          if (secondChecksum.sha1.length() > 0)
          {
            if (firstChecksum.sha1.equals(secondChecksum.sha1))
            {
              compareSame += ((compareSame.length() > 0) ? ", " : "") + "SHA1";
              compareSuccess = true;  // accept SHA1 as reliable checksum
            }
            else
              compareDiffer += ((compareDiffer.length() > 0) ? ", " : "")
                + "SHA1";
          }

          /* Declare files as different if any of the above are different. */

          if (compareDiffer.length() > 0) // any explicit differences?
          {
            putSelectDiff(pathPrefix + firstName + " - different "
              + compareDiffer
              + ((compareSame.length() > 0) ? (" - same " + compareSame) : ""));
            totalDiffer ++;       // increment number of differences
          }
          else if (compareSuccess) // did we find anything reliable?
          {
            putSelectSame(pathPrefix + firstName + " - same " + compareSame);
            totalSame ++;         // increment number of identical files
          }
          else                    // we don't know nothin'
          {
            putSelectDiff(pathPrefix + firstName + " - no reliable comparison"
              + ((compareSame.length() > 0) ? (" - same " + compareSame) : ""));
            totalDiffer ++;       // increment number of differences
          }
        }
      }
    }

    else
    {
      /* We arrive here if (1) the user gave us one file and one folder; (2) if
      we encounter the same name in two folders, where one name is a file and
      the other name is a folder; or (3) one or both are protected system files
      or folders. */

      putSelectDiff(pathPrefix + firstFile.getName()
        + " - can't compare files with folders or non-file objects");
      totalDiffer ++;             // increment number of differences
    }
  } // end of compareFolderChecksumRecursive() method


/*
  compareTwoChecksums() method

  Compare the checksums stored in the first file with the checksums stored in
  the second file.
*/
  static int compareTwoChecksums(
    File firstFile,               // first checksum file (only)
    File secondFile)              // second checksum file (only)
  {
    CompareFolders3File firstChecksum; // first calculated checksums
    CompareFolders3File secondChecksum; // second calculated checksums
    int status;                   // exit status for console application

    /* Attempt to parse both files as recursive checksums in XML format. */

    firstChecksum = readChecksumFile(firstFile); // parse checksum file
    if (cancelFlag || (firstChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)

    secondChecksum = readChecksumFile(secondFile); // parse checksum file
    if (cancelFlag || (secondChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)

    /* Call our recursive subroutine to compare the checksums. */

    compareTwoChecksumsRecursive("", firstChecksum, secondChecksum);
    if (cancelFlag)               // did something go wrong?
      status = EXIT_UNKNOWN;      // any problem means unknown result status
    else
    {
      putError("Found " + prettyPlural(totalDiffer, "difference")
        + " and " + prettyPlural(totalSame, "identical file") + ".");
      if (totalDiffer > 0)        // any errors means return with failure
        status = EXIT_FAILURE;
      else if (totalSame > 0)     // at least one identical means success
        status = EXIT_SUCCESS;
      else                        // zero files means we don't know anything
        status = EXIT_UNKNOWN;
    }
    return(status);               // return whatever status we could find

  } // end of compareTwoChecksums() method

  static void compareTwoChecksumsRecursive(
    String pathPrefix,            // path prefix for file names, or "" to start
    CompareFolders3File firstChecksum, // first calculated checksums
    CompareFolders3File secondChecksum) // second calculated checksums
  {
    String compareDiffer;         // which checksums or sizes are different
    String compareSame;           // which checksums or sizes are the same
    boolean compareSuccess;       // true if any reliable checksum equals

    int firstCount;               // number of elements in <firstList>
    CompareFolders3File firstEntry; // one entry from <firstList>
    int firstIndex;               // current index into <firstList>
    Vector firstList;             // list of files/subfolders if first folder

    int secondCount;              // number of elements in <secondList>
    CompareFolders3File secondEntry; // one entry from <secondList>
    int secondIndex;              // current index into <secondList>
    Vector secondList;            // list of files/subfolders if second folder

    /* Begin by ending early if the user has cancelled. */

    if (cancelFlag) return;       // stop if user hit the panic button

    /* Decide if we are comparing two files or two folders. */

    firstList = firstChecksum.list; // null for files, list for folders
    secondList = secondChecksum.list;

    if ((firstList != null) && (secondList != null))
    {
      /* Compare the contents of two folders.  Both folders should have the
      same files and subfolders, in the same order.  The lists were sorted when
      they were read from their files, and most sort keys have been created. */

      firstCount = firstList.size(); // get number of elements in list
      firstIndex = 0;             // start at beginning of the list
      secondCount = secondList.size();
      secondIndex = 0;
      setStatusMessage("Folder " + pathPrefix + firstChecksum.name);

      while ((cancelFlag == false) && ((firstIndex < firstCount)
       || (secondIndex < secondCount)))
      {
        if (firstIndex >= firstCount) // at end of first list?
        {
          secondEntry = (CompareFolders3File) secondList.get(secondIndex);
          putSelectDiff(pathPrefix + secondEntry.name
            + ((secondEntry.list != null) ? " (subfolder)" : "")
            + " - only in second checksum");
          secondIndex ++;         // continue stepping through second list
          totalDiffer ++;         // increment number of differences
        }
        else if (secondIndex >= secondCount) // at end of second list?
        {
          firstEntry = (CompareFolders3File) firstList.get(firstIndex);
          putSelectDiff(pathPrefix + firstEntry.name
            + ((firstEntry.list != null) ? " (subfolder)" : "")
            + " - only in first checksum");
          firstIndex ++;          // continue stepping through first list
          totalDiffer ++;         // increment number of differences
        }
        else                      // both folders still have files/subfolders
        {
          firstEntry = (CompareFolders3File) firstList.get(firstIndex);
          secondEntry = (CompareFolders3File) secondList.get(secondIndex);
          if ((caseFlag && firstEntry.name.equals(secondEntry.name))
            || ((!caseFlag)
              && firstEntry.name.equalsIgnoreCase(secondEntry.name)))
          {
            /* The files or folders have the same name.  Files have null lists;
            folders have non-null lists.  Append to the relative path name if
            we are searching subfolders, but not if we are comparing files. */

            if ((firstEntry.list != null) && (secondEntry.list != null))
            {
              if (recurseFlag)    // does user want us to do subfolders?
              {
                setStatusMessage("Folder " + pathPrefix
                  + firstEntry.name);
                compareTwoChecksumsRecursive((pathPrefix + firstEntry.name
                  + systemFileSep), firstEntry, secondEntry);
              }
              else
                putOutput(pathPrefix + firstEntry.name
                  + " - ignoring subfolder");
            }
            else
            {
              compareTwoChecksumsRecursive(pathPrefix, firstEntry,
                secondEntry);
            }
            firstIndex ++;        // continue stepping through first list
            secondIndex ++;       // continue stepping through second list
          }
          else
          {
            /* The file or folder names are different.  Try to figure out why,
            and move forward in the proper sequence.  The checksum sorting keys
            will normally already be created, unless in a folder with only one
            file. */

            if (caseFlag && firstEntry.name.equalsIgnoreCase(secondEntry.name))
            {
              /* Warn user about having names that differ only in uppercase
              versus lowercase.  Don't increment the error counter. */

              putOutput(pathPrefix + firstEntry.name
                + " - uppercase versus lowercase warning");
            }

            if (firstEntry.sortkey == null)
              firstEntry.sortkey = createSortKey((firstEntry.list != null),
                firstEntry.name);
            if (secondEntry.sortkey == null)
              secondEntry.sortkey = createSortKey((secondEntry.list != null),
                secondEntry.name);
            if ((firstEntry.sortkey.compareTo(secondEntry.sortkey)) > 0)
            {
              /* First list is ahead of second list. */

              putSelectDiff(pathPrefix + secondEntry.name
                + ((secondEntry.list != null) ? " (subfolder)" : "")
                + " - only in second checksum");
              secondIndex ++;     // continue stepping through second list
              totalDiffer ++;     // increment number of differences
            }
            else
            {
              /* Second list is ahead of first list. */

              putSelectDiff(pathPrefix + firstEntry.name
                + ((firstEntry.list != null) ? " (subfolder)" : "")
                + " - only in first checksum");
              firstIndex ++;      // continue stepping through first list
              totalDiffer ++;     // increment number of differences
            }
          } // end of first name equals second name
        }
      } // end of while loop for folder names
    }

    else if ((firstList == null) && (secondList == null))
    {
      /* Compare the checksums for two files.  We don't look at the file names,
      because either the user gave us two explicit checksums to compare, or
      else we got here from matching directories. */

      compareDiffer = "";         // append observed results to these strings
      compareSame = "";
      compareSuccess = false;     // only declare success for reliable compares

      if ((firstChecksum.size >= 0) && (secondChecksum.size >= 0))
      {
        /* Both checksums have something specified for the file size.  We don't
        consider file size to be a reliable way of comparing files. */

        if (firstChecksum.size == secondChecksum.size)
          compareSame += ((compareSame.length() > 0) ? ", " : "")
            + "file size";
        else
          compareDiffer += ((compareDiffer.length() > 0) ? ", " : "")
            + "file size";
      }

      if ((firstChecksum.crc32.length() > 0)
        && (secondChecksum.crc32.length() > 0))
      {
        /* Both checksums have a non-empty string for the CRC32.  Since we read
        and parsed both original checksum files, we know that the strings are
        "clean" and safe to compare.  It is an open question whether CRC32 by
        itself should be accepted for comparing the content of files. */

        if (firstChecksum.crc32.equals(secondChecksum.crc32))
        {
          compareSame += ((compareSame.length() > 0) ? ", " : "") + "CRC32";
//        compareSuccess = true;  // but should we accept CRC32 as reliable?
        }
        else
          compareDiffer += ((compareDiffer.length() > 0) ? ", " : "")
            + "CRC32";
      }

      if ((firstChecksum.md5.length() > 0)
        && (secondChecksum.md5.length() > 0))
      {
        /* Both checksums have a non-empty string for the MD5. */

        if (firstChecksum.md5.equals(secondChecksum.md5))
        {
          compareSame += ((compareSame.length() > 0) ? ", " : "") + "MD5";
          compareSuccess = true;  // accept MD5 as reliable checksum
        }
        else
          compareDiffer += ((compareDiffer.length() > 0) ? ", " : "") + "MD5";
      }

      if ((firstChecksum.sha1.length() > 0)
        && (secondChecksum.sha1.length() > 0))
      {
        /* Both checksums have a non-empty string for the SHA1. */

        if (firstChecksum.sha1.equals(secondChecksum.sha1))
        {
          compareSame += ((compareSame.length() > 0) ? ", " : "") + "SHA1";
          compareSuccess = true;  // accept SHA1 as reliable checksum
        }
        else
          compareDiffer += ((compareDiffer.length() > 0) ? ", " : "") + "SHA1";
      }

      /* Declare files as different if any of the above are different. */

      if (compareDiffer.length() > 0) // any explicit differences?
      {
        putSelectDiff(pathPrefix + firstChecksum.name + " - different "
          + compareDiffer
          + ((compareSame.length() > 0) ? (" - same " + compareSame) : ""));
        totalDiffer ++;           // increment number of differences
      }
      else if (compareSuccess)    // did we find anything reliable?
      {
        putSelectSame(pathPrefix + firstChecksum.name + " - same "
          + compareSame);
        totalSame ++;             // increment number of identical files
      }
      else                        // we don't know nothin'
      {
        putSelectDiff(pathPrefix + firstChecksum.name
          + " - no reliable comparison"
          + ((compareSame.length() > 0) ? (" - same " + compareSame) : ""));
        totalDiffer ++;           // increment number of differences
      }
    }

    else
    {
      /* We arrive here if (1) the user gave us one file and one folder, or (2)
      if we encounter the same name in two folders, where one name is a file
      and the other name is a folder. */

      putSelectDiff(pathPrefix + firstChecksum.name
        + " - can't compare files with folders");
      totalDiffer ++;             // increment number of differences
    }
  } // end of compareTwoChecksums() method


/*
  compareTwoFolders() method

  Compare the contents of the first file/folder with the contents of the
  second file/folder.  Files can be compared with files, and folders with
  folders.  (Files can't be compared with folders.)  The comparison is done by
  reading the files.  Checksums are not used.

  The lazy way of doing this would be to generate recursive checksums for both
  files/folders and then call the compareTwoChecksums() method.  This would be
  easy but not good, because:

  (1) Files may appear in only one folder and calculating their checksums would
      be a waste of time, especially for big files;
  (2) Files may differ in size, in which case checksums are not needed; and
  (3) We can stop reading files as soon as we find a difference.  Checksums
      read the whole file before they produce a result.
*/
  static int compareTwoFolders(
    File firstFile,               // first file or folder
    File secondFile)              // second file or folder
  {
    int status;                   // exit status for console application

    compareTwoFoldersRecursive("", firstFile, secondFile);
    if (cancelFlag)               // did something go wrong?
      status = EXIT_UNKNOWN;      // any problem means unknown result status
    else
    {
      putError("Found " + prettyPlural(totalDiffer, "difference")
        + " and " + prettyPlural(totalSame, "identical file") + ".");
      if (totalDiffer > 0)        // any errors means return with failure
        status = EXIT_FAILURE;
      else if (totalSame > 0)     // at least one identical means success
        status = EXIT_SUCCESS;
      else                        // zero files means we don't know anything
        status = EXIT_UNKNOWN;
    }
    return(status);               // return whatever status we could find

  } // end of compareTwoFolders() method

  static void compareTwoFoldersRecursive(
    String pathPrefix,            // path prefix for file names, or "" to start
    File firstFile,               // first file or folder
    File secondFile)              // second file or folder
  {
    long fileSize;                // size of two compared files in bytes

    byte[] firstBuffer;           // big buffer for reading first file
    int firstIndex;               // current index into <firstList>
    File[] firstList;             // list of files/subfolders for first folder
    String firstName;             // current name for first file or subfolder
    FileInputStream firstStream;  // file input stream for first file

    byte[] secondBuffer;          // big buffer for reading second file
    int secondIndex;              // current index into <secondList>
    File[] secondList;            // list of files/subfolders for second folder
    String secondName;            // current name for second file or subfolder
    FileInputStream secondStream; // file input stream for second file

    long sizeDone;                // how much of <fileSize> has been finished
    String sizePrefix, sizeSuffix; // pre-formatted portions of size message
    long sizeUser;                // last <sizeDone> reported to user

    boolean stopFlag;             // local flag to stop compare on difference

    /* Begin by ending early if the user has cancelled. */

    if (cancelFlag) return;       // stop if user hit the panic button

    /* Decide if we are comparing two files or two folders. */

    if (firstFile.isDirectory() && secondFile.isDirectory())
    {
      /* Compare the contents of two folders.  Both folders should have the
      same files and subfolders, in the same order, once we sort the file
      lists. */

      firstIndex = 0;             // start at beginning of the list
      firstList = sortFileList(firstFile.listFiles());
                                  // get sorted contents of first folder
      secondIndex = 0;
      secondList = sortFileList(secondFile.listFiles()); // second contents
      setStatusMessage("Folder " + pathPrefix + firstFile.getName());

      while ((cancelFlag == false) && ((firstIndex < firstList.length)
       || (secondIndex < secondList.length)))
      {
        if (firstIndex >= firstList.length) // at end of first list?
        {
          secondName = secondList[secondIndex].getName(); // get second name
          putSelectDiff(pathPrefix + secondName
            + ((secondList[secondIndex].isDirectory()) ? " (subfolder)" : "")
            + " - only in second folder");
          secondIndex ++;         // continue stepping through second list
          totalDiffer ++;         // increment number of differences
        }
        else if (secondIndex >= secondList.length) // at end of second list?
        {
          firstName = firstList[firstIndex].getName(); // get first name
          putSelectDiff(pathPrefix + firstName
            + ((firstList[firstIndex].isDirectory()) ? " (subfolder)" : "")
            + " - only in first folder");
          firstIndex ++;          // continue stepping through first list
          totalDiffer ++;         // increment number of differences
        }
        else                      // both folders still have files/subfolders
        {
          firstName = firstList[firstIndex].getName(); // get first name
          secondName = secondList[secondIndex].getName(); // get second name
          if ((caseFlag && firstName.equals(secondName))
            || ((!caseFlag) && firstName.equalsIgnoreCase(secondName)))
          {
            /* The files or folders have the same name.  Append to the relative
            path name if we are searching subfolders, but not if we are
            comparing files. */

            if (firstList[firstIndex].isDirectory()
              && secondList[secondIndex].isDirectory())
            {
              if (recurseFlag)    // does user want us to do subfolders?
              {
                setStatusMessage("Folder " + pathPrefix + firstName);
                compareTwoFoldersRecursive((pathPrefix + firstName
                  + systemFileSep), firstList[firstIndex],
                  secondList[secondIndex]);
              }
              else
                putOutput(pathPrefix + firstName + " - ignoring subfolder");
            }
            else
            {
              compareTwoFoldersRecursive(pathPrefix, firstList[firstIndex],
                secondList[secondIndex]);
            }
            firstIndex ++;        // continue stepping through first list
            secondIndex ++;       // continue stepping through second list
          }
          else
          {
            /* The file or folder names are different.  Try to figure out why,
            and move forward in the proper sequence. */

            if (caseFlag && firstName.equalsIgnoreCase(secondName))
            {
              /* Warn user about having names that differ only in uppercase
              versus lowercase.  Don't increment the error counter. */

              putOutput(pathPrefix + firstName
                + " - uppercase versus lowercase warning");
            }

            if ((createSortKey(firstList[firstIndex].isDirectory(),
              firstName)).compareTo(createSortKey(secondList[secondIndex]
              .isDirectory(), secondName)) > 0)
            {
              /* First list is ahead of second list. */

              putSelectDiff(pathPrefix + secondName
                + ((secondList[secondIndex].isDirectory()) ? " (subfolder)" : "")
                + " - only in second folder");
              secondIndex ++;     // continue stepping through second list
              totalDiffer ++;     // increment number of differences
            }
            else
            {
              /* Second list is ahead of first list. */

              putSelectDiff(pathPrefix + firstName
                + ((firstList[firstIndex].isDirectory()) ? " (subfolder)" : "")
                + " - only in first folder");
              firstIndex ++;      // continue stepping through first list
              totalDiffer ++;     // increment number of differences
            }
          } // end of first name equals second name
        }
      } // end of while loop for folder names
    }

    else if (firstFile.isFile() && secondFile.isFile())
    {
      /* Compare the contents of two files.  We don't look at the file names,
      because either the user gave us two explicit files to compare, or else we
      got here from matching directories. */

      firstName = firstFile.getName(); // get name of first file
      setStatusMessage("Reading " + pathPrefix + firstName);
                                  // tell user what we are doing

      if (firstFile.length() != secondFile.length())
      {
        /* Files with different sizes can't be the same.  We don't need to open
        the files. */

        putSelectDiff(pathPrefix + firstName + " - different size");
        totalDiffer ++;           // increment number of differences
      }
      else
      {
        /* Read with big byte buffers, which is much faster than calling read()
        for one byte at a time.  Many buffering strategies are possible here.
        All methods must be compared against a base standard such as 64 KB
        buffers with reads alternating between the first and second files.
        More sophisticated strategies often fail for special cases.  The most
        likely failure is the counter-intuitive result that small buffers can
        be better when reading a single file from CD/DVD, and the preferred
        size depends upon each drive's rotational speed.  However, if someone
        is dumb enough to compare two files or folders on the same CD/DVD, big
        buffers are absolutely necessary! */

        try
        {
          fileSize = firstFile.length(); // get size of both files in bytes
          firstBuffer = new byte[bufferCompareSize];
                                  // allocate big/faster input buffer
          firstStream = new FileInputStream(firstFile); // open first file
          secondBuffer = new byte[bufferCompareSize];
          secondStream = new FileInputStream(secondFile); // open second file
          sizeDone = sizeUser = 0; // we haven't read anything yet
          sizePrefix = sizeSuffix = null; // don't format size message yet
          stopFlag = false;       // not ready to stop yet (no differences)

          while ((cancelFlag == false) && (stopFlag == false))
          {
            firstIndex = firstStream.read(firstBuffer, 0, bufferCompareSize);
            secondIndex = secondStream.read(secondBuffer, 0, bufferCompareSize);

            /* A massive assumption is buried in the next few lines of code,
            that when we read from big files, the amount of data we receive in
            each buffer will be the same if the files are the same.  Java does
            not guarantee this; Java only promises at least one byte and no
            more than the buffer size.  We can't assume that the buffers will
            be full, even if there is more data to be read.  We do assume that
            Java is consistent in its behavior for both reads. */

            if (firstIndex != secondIndex) // same number of bytes?
            {
              putSelectDiff(pathPrefix + firstName + " - different contents");
              stopFlag = true;    // yes, stop comparing
              totalDiffer ++;     // increment number of differences
            }
            else if (firstIndex <= 0) // end of file on both files?
            {
              putSelectSame(pathPrefix + firstName + " - same contents");
              stopFlag = true;    // stop comparing
              totalSame ++;       // increment number of identical files
            }
            else                  // compare all bytes received
            {
              for (int i = 0; i < firstIndex; i ++)
              {
                if (firstBuffer[i] != secondBuffer[i])
                {
                  putSelectDiff(pathPrefix + firstName
                    + " - different contents");
                  stopFlag = true; // stop comparing
                  totalDiffer ++; // increment number of differences
                  break;          // cancel <for> loop
                }
              }

              /* Update the GUI status if this is a big file. */

              sizeDone += firstIndex; // add to number of bytes finished
              if ((stopFlag == false) && (consoleFlag == false)
                && ((sizeDone - sizeUser) > bufferReportSize))
              {
                if (sizePrefix == null) // have we formatted constant portions?
                {
                  sizePrefix = "Reading " + pathPrefix + firstName + " - ";
                  sizeSuffix = " of " + formatMegabytes(fileSize) + " MB";
                }
                sizeUser = sizeDone; // remember what we last told the user
                setStatusMessage(sizePrefix + formatMegabytes(sizeDone)
                  + sizeSuffix);
              }
            }
          }
          firstStream.close();    // close first file
          secondStream.close();   // close second file
        }
        catch (IOException except)
        {
          putOutput(pathPrefix + firstName + " - can't read file(s)");
          totalDiffer ++;         // increment number of differences
          return;                 // done
        }
      }
    }

    else if (firstFile.equals(secondFile)
      || canonicalFile(firstFile).equals(canonicalFile(secondFile)))
    {
      /* Some objects are the same even if we don't know what they are. */

      putSelectSame(pathPrefix + firstFile.getName()
        + " - same non-file object");
      totalSame ++;               // increment number of identical files
    }

    else
    {
      /* We arrive here if (1) the user gave us one file and one folder; (2) if
      we encounter the same name in two folders, where one name is a file and
      the other name is a folder; or (3) one or both are protected system files
      or folders. */

      putSelectDiff(pathPrefix + firstFile.getName()
        + " - can't compare files with folders or non-file objects");
      totalDiffer ++;             // increment number of differences
    }
  } // end of compareTwoFoldersRecursive() method


/*
  createChecksum() method

  Create a new checksum file for a given data file or folder.  See also the
  updateChecksum() method.
*/
  static int createChecksum(
    File firstFile,               // first file or folder
    File secondFile)              // second checksum file (only), or null
  {
    int answer;                   // answer received from user: yes, no, cancel
    CompareFolders3File firstChecksum; // first calculated checksums
    File userFile;                // where user wants to save the results
    boolean writeFlag;            // true while we have something to write

    /* Create a new recursive checksum using the caller's first file. */

    firstChecksum = createUpdateChecksum(false, "", firstFile, null);
                                  // create new checksums for file or folder
    if (cancelFlag || (firstChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)

    /* Catch errors while writing the checksum file, and prompt the user in GUI
    mode, because it may have taken a long time to generate the checksums. */

    if ((consoleFlag == false) && (secondFile == null)) // GUI output only
      outputText.setText("");     // clear output text area
    setStatusMessage(EMPTY_STATUS); // force final counters to appear in status
    userFile = secondFile;        // start writing to second checksum file
    writeFlag = true;             // there is always something to write
    while (writeFlag)             // repeat because output file may be bad
    {
      cancelFlag = false;         // clear any error condition on write
      writeChecksumFile(userFile, firstChecksum, firstFile.getPath());
                                  // write to file or stdout or GUI text area
      if (cancelFlag == false)    // if file was written successfully
        writeFlag = false;        // stop asking user where to save file
      else if (consoleFlag)       // are we running from the command line?
        writeFlag = false;        // yes, don't try again, don't prompt user
      else if (userFile == null)  // are we writing to GUI output text area?
        writeFlag = false;        // yes, don't prompt user if GUI has failed
      else                        // if there was an error while writing file
      {
        answer = JOptionPane.showConfirmDialog(mainFrame,
          ("Sorry, can't write to <" + userFile.getName()
          + ">.\nTry clicking <No> and saving to a new file.\n"
          + "\nReplace existing file with new checksums?"));
        if (answer == JOptionPane.CANCEL_OPTION)
        {
          putOutput("Cancel on replace file question.", true);
          break;                  // stop asking stupid questions
        }
        else if (answer == JOptionPane.YES_OPTION)
          { /* do nothing */ }
        else
        {
          /* Ask the user for an output file name. */

          fileChooser.resetChoosableFileFilters(); // remove any existing filters
          fileChooser.setDialogTitle("Save Checksums as Text File...");
          fileChooser.setFileHidingEnabled(true); // don't show hidden files
          fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fileChooser.setMultiSelectionEnabled(false); // allow only one file
          if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
          {
            putOutput("Cancel on file selection dialog.", true);
            return(EXIT_UNKNOWN); // stop asking stupid questions
          }
          userFile = fileChooser.getSelectedFile();
        }
      }
    }

    /* Tell the user what we found, for consoles and output to files. */

    if (cancelFlag)               // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)
    else if (consoleFlag || (secondFile != null))
    {                             // do something to tell user that we are done
      putError("Checksum has "
        + prettyPlural(firstChecksum.files, "file") + " and "
        + prettyPlural(firstChecksum.folders, "subfolder") + " with "
        + prettyPlural(firstChecksum.size, "byte")
        + ((firstChecksum.name.length() > 0) ? (" for "
        + firstChecksum.name) : "."));
    }
    return(EXIT_SUCCESS);         // tell caller that we were successful

  } // end of createChecksum() method


/*
  createUpdateChecksum() method

  Create a new recursive checksum object given a file or folder.  This method
  is common to both creating a new checksum and updating an old checksum.  The
  difference is in whether or not we are given a second parameter that has a
  recursive checksum of previously known data.

  Updating occurs if <secondChecksum> is not null, or if <updateFlag> is true.
  Normally, a non-null checksum is sufficient, but when there is a mismatch
  between files in a folder and files in the checksum, the caller may not give
  us a checksum even though updating is taking place.

  Be very careful about which messages get written with <putError> and which
  messages get written with <putOutput> during the creation of a new checksum,
  because when running in console mode, <putError> goes to standard error and
  <putOutput> goes to standard output ... and the new checksum file also goes
  to standard output.  Updating is always a GUI action, and isn't a problem.

  We return a <CompareFolders3File> object, unless the <cancelFlag> is set, in
  which case we return a <null> object.
*/
  static CompareFolders3File createUpdateChecksum(
    boolean updateFlag,           // decides which counters and status messages
    String pathPrefix,            // path prefix for file names, or "" to start
    File firstFile,               // first file or folder
    CompareFolders3File secondChecksum) // second calculated checksums, or null
  {
    boolean changeFlag;           // true if old and new files are different
    int firstIndex;               // current index into <firstList>
    File[] firstList;             // list of files/subfolders for first folder
    String firstName;             // current name for first file or subfolder
    CompareFolders3File next;     // information about one file or subfolder
    long oldDiffer;               // saved value of <totalDiffer> for directory
    long oldSame;                 // saved value of <totalSame> for directory
    String pathName;              // relative path name for this file or folder
    CompareFolders3File result;   // our recursive result
    int secondCount;              // number of elements in <secondList>
    CompareFolders3File secondEntry; // one entry from <secondList>
    int secondIndex;              // current index into <secondList>
    Vector secondList;            // list of files/subfolders if second folder

    /* Begin by ending early if the user has cancelled. */

    if (cancelFlag) return(null); // stop if user hit the panic button

    /* Assume that we will return a result and create an empty copy of one of
    our file checksum objects. */

    result = new CompareFolders3File(); // start with an empty data object
    result.name = firstFile.getName(); // get file name (without the path)
    pathName = pathPrefix + result.name; // name that we report to user

    /* If this is a folder, then get a list of all files or subfolders inside
    this folder.  Process all files.  Do subfolders only if the recursion flag
    is turned on.  Be nice and sort the list before we start recursion, so that
    everything appears in order. */

    if (firstFile.isDirectory())
    {
      if (updateFlag)             // updates are quick, only need status text
        setStatusMessage("Folder " + firstFile.getPath());
      else                        // creation is much slower, write log text
        putError("Searching folder " + firstFile.getPath());

      firstIndex = 0;             // start at beginning of the list
      firstList = sortFileList(firstFile.listFiles());
                                  // get sorted contents of first folder
      oldDiffer = totalDiffer;    // update wants to know folders that change
      oldSame = totalSame;        // so save local copies at start of directory
      result.list = new Vector(); // create a new list for folder contents
      result.size = 0;            // start folder with a known size of zero

      /* Unlike the case below for files, when searching folders, we only use
      old checksum information as a tree to traverse during recursion. */

      if (secondChecksum == null) // normal creation if no old data given
      {
        secondList = null;        // indicate that we have nothing extra
        secondCount = secondIndex = 0; // just to keep compiler happy
      }
      else if (secondChecksum.list == null) // if old data is not for a folder
      {
        secondList = null;        // indicate that we have nothing extra
        secondCount = secondIndex = 0; // just to keep compiler happy
        totalDiffer ++;           // increment number of differences
        putSelectDiff(pathName + " - can't compare files with folders");
      }
      else                        // we have been given secondary data
      {
        secondList = secondChecksum.list; // traverse this list when searching
        secondCount = secondList.size(); // get number of elements in list
        secondIndex = 0;          // start from first element in list
      }

      /* Search through the first folder, using secondary data as available. */

      while ((cancelFlag == false) && ((firstIndex < firstList.length)
       || (secondIndex < secondCount)))
      {
        if (cancelFlag) break;    // stop if user hit the panic button

        /* If we have finished the first list, then we are looping just to
        identify missing/deleted files or folders in the secondary list. */

        if (firstIndex >= firstList.length) // at end of first list?
        {
          secondEntry = (CompareFolders3File) secondList.get(secondIndex);
          putSelectDiff(pathPrefix + secondEntry.name
            + ((secondEntry.list != null) ? " - folder deleted"
            : " - file deleted"));
          secondIndex ++;         // continue stepping through second list
          totalDiffer ++;         // increment number of differences
          continue;               // go back to start of <while> loop
        }

        /* Syncronize names in both lists, if there is a secondary list. */

        firstName = firstList[firstIndex].getName(); // get first name
        if ((secondList == null) || (secondIndex >= secondCount))
        {
          secondEntry = null;     // no secondary data for names or recursion
          if (updateFlag)         // don't count when creating new checksum
          {
            putSelectDiff(pathPrefix + firstName
              + ((firstList[firstIndex].isDirectory()) ? " - new folder"
              : " - new file"));
            totalDiffer ++;       // increment number of differences
          }
        }
        else                      // if there is a secondary checksum
        {
          secondEntry = (CompareFolders3File) secondList.get(secondIndex);
          if (firstName.equals(secondEntry.name) == false)
          {
            /* The file or folder names are different.  Try to figure out why,
            and move forward in the proper sequence. */

            if (secondEntry.sortkey == null)
              secondEntry.sortkey = createSortKey((secondEntry.list != null),
                secondEntry.name);
            if (((createSortKey(firstList[firstIndex].isDirectory(),
              firstName)).compareTo(secondEntry.sortkey)) > 0)
            {
              /* First list is ahead of second list. */

              putSelectDiff(pathPrefix + secondEntry.name
                + ((secondEntry.list != null) ? " - folder deleted"
                : " - file deleted"));
              secondIndex ++;     // continue stepping through second list
              totalDiffer ++;     // increment number of differences
              continue;           // go back to start of <while> loop
            }
            else
            {
              /* Second list is ahead of first list. */

              putSelectDiff(pathPrefix + firstName
                + ((firstList[firstIndex].isDirectory()) ? " - new folder"
                : " - new file"));
              secondEntry = null; // no secondary entry when recursing
              totalDiffer ++;     // increment number of differences

              /* Fall through to process this directory entry. */
            }
          }
        }

        /* Recursively call ourself to handle this directory entry. */

        if (firstList[firstIndex].isDirectory()) // maybe do folders
        {
          if (recurseFlag)        // only do subfolders if recursion is true
          {
            next = createUpdateChecksum(updateFlag, (pathPrefix + firstName
              + systemFileSep), firstList[firstIndex], secondEntry);
            if (next != null)     // might be cancelled, might be invalid
            {
              result.files += next.files; // accumulate subfolder's files
              result.folders += 1 + next.folders;
                                  // accumulate subfolder and its subfolders
              result.list.add(next); // add subfolder information to our list
              result.size += next.size; // add subfolder size to folder total
            }
          }
          else
            putError(pathPrefix + firstName + " - ignoring subfolder");
        }
        else if (firstList[firstIndex].isFile()) // always do files
        {
          next = createUpdateChecksum(updateFlag, pathPrefix,
            firstList[firstIndex], secondEntry);
          if (next != null)       // might be cancelled, might be invalid
          {
            result.files ++;      // increment number of files found
            result.list.add(next); // add file information to our list
            result.size += next.size; // add file size to folder total
          }
        }
        else
          putError(pathPrefix + firstName + " - unknown directory entry");

        /* Increment the index to the next entry in our search list. */

        firstIndex ++;            // continue stepping through first list
        if (secondEntry != null)  // if a matching secondary entry was found
          secondIndex ++;         // continue stepping through second list
      }

      /* Count this folder in the totals only if there were no errors.  Unlike
      other summaries written with <putError>, these summaries are written with
      <putOutput> because they occur after each folder and are not final. */

      if (cancelFlag == false)
      {
        if (updateFlag == false)  // when creating completely new checksums
        {
          totalFolders ++;        // increment total number of folders (global)
          putError("Found " + prettyPlural(result.files, "file") + " and "
            + prettyPlural(result.folders, "subfolder") + " with "
            + prettyPlural(result.size, "byte") + " in " + firstFile.getPath());
        }
        else if ((showDiffFlag && (totalDiffer > oldDiffer)) // if <updateFlag>
          || (showSameFlag && (totalSame > oldSame)))
        {
          putOutput("Found "
            + prettyPlural((totalDiffer - oldDiffer), "difference") + " and "
            + prettyPlural((totalSame - oldSame), "identical file") + " in "
            + firstFile.getPath());
        }
      }
    }

    /* If this is a file, then compute the checksums.  We use old data, if
    given, to estimate when a file has changed.  We don't compare file names;
    that is done during directory searches.  The order of the checks below is
    for speed: simplest and fastest checks first. */

    else if (firstFile.isFile())
    {
      changeFlag = false;         // assume that no checksum is necessary
      try                         // may cause a parsing exception
      {
        if (secondChecksum == null) // is there any old data to compare to?
        {
          changeFlag = true;      // no, force new checksum calculation
        }
        else if (secondChecksum.list != null) // if old data is not for a file
        {
          changeFlag = true;      // no, force new checksum calculation
          totalDiffer ++;         // increment number of differences
          putSelectDiff(pathName + " - can't compare files with folders");
        }
        else if ((secondChecksum.crc32 == null)
          || (secondChecksum.crc32.length() == 0))
        {
          changeFlag = true;      // force new checksum calculation
          totalDiffer ++;         // increment number of differences
          putSelectDiff(pathName + " - missing CRC32 checksum");
        }
        else if ((secondChecksum.md5 == null)
          || (secondChecksum.md5.length() == 0))
        {
          changeFlag = true;      // force new checksum calculation
          totalDiffer ++;         // increment number of differences
          putSelectDiff(pathName + " - missing MD5 checksum");
        }
        else if ((secondChecksum.sha1 == null)
          || (secondChecksum.sha1.length() == 0))
        {
          changeFlag = true;      // force new checksum calculation
          totalDiffer ++;         // increment number of differences
          putSelectDiff(pathName + " - missing SHA1 checksum");
        }
        else if ((secondChecksum.size < 0)
          || (secondChecksum.size != firstFile.length()))
        {
          changeFlag = true;      // force new checksum calculation
          totalDiffer ++;         // increment number of differences
          putSelectDiff(pathName + " - different size");
        }
        else if ((secondChecksum.date == null)
          || (secondChecksum.date.length() == 0)
          || (Math.abs(formatDate.parse(secondChecksum.date).getTime() -
          firstFile.lastModified()) >= MILLI_FUZZ))
        {
          changeFlag = true;      // force new checksum calculation
          totalDiffer ++;         // increment number of differences
          putSelectDiff(pathName + " - different date or time");
        }
        else
        {
          totalSame ++;           // increment number of identical files
          putSelectSame(pathName + " - same");
        }
      }
      catch (ParseException pe)   // checksum file has bad date/time format
      {
        changeFlag = true;        // force new checksum calculation
        totalDiffer ++;           // increment number of differences
        putSelectDiff(pathName + " - can't parse date <" + secondChecksum.date
          + ">");
      }

      /* Copy old checksums if there is no reason to believe that they have
      changed. */

      result.date = formatDate.format(new Date(firstFile.lastModified()));
                                  // file date and time in readable format
      result.files = 1;           // this is a file, so count it as one file
      result.size = firstFile.length(); // remember file size in bytes
      if (changeFlag)             // has there been a change that we noticed?
        calcFileChecksum(firstFile, result); // yes, calculate new checksums
      else
      {
        result.crc32 = secondChecksum.crc32; // no change, copy old checksums
        result.md5 = secondChecksum.md5;
        result.sha1 = secondChecksum.sha1;
      }

      /* Count this file in the totals only if there were no errors. */

      if ((cancelFlag == false) && (updateFlag == false))
      {
        totalFiles ++;            // increment total number of files (global)
        totalSize += result.size; // add file size to total bytes (global)
      }
    }

    /* If this is neither a file nor a folder, then ignore it. */

    else
    {
      putError("Unknown file or folder: " + firstFile.getPath());
      result = null;              // throw away newly created object
    }

    /* Clean up and return to the caller. */

    if (cancelFlag)               // did user hit the panic button?
      result = null;              // yes, invalidate any/all work that we did

    return(result);               // return whatever we created to the caller

  } // end of createUpdateChecksum() method


/*
  createSortKey() method

  Given a file or subfolder name, return a unique sorting key that puts the
  file/subfolder in the correct order when the keys are compared as a plain
  Unicode strings, while sorting the contents of a single folder.  (Keys may
  not be valid if comparing two files or subfolders from different folders.)

  The following keys have been attempted:

  (1) Return the file or folder name unchanged, which defaults to Unicode
      binary string order (all uppercase before lowercase before accented
      letters, etc).  This is too confusing except for Linux/UNIX hackers.

  (2) Convert the file or folder name to a consistent case (such as lowercase),
      and then append the original name for systems that consider uppercase and
      lowercase to be distinct (i.e., other than Windows).  This is close to
      English dictionary order, but is not correct for foreign languages with
      accents or non-Roman characters.

  (3) Begin the sorting key with a flag to put files ("1") before folders
      ("2"), then the file or folder name in lowercase, and end with the
      original file or folder name.  In this method, code elsewhere that
      detects the condition where you "can't compare files with folders" is
      unlikely to be invoked (that is, when a file object has the same name
      but different attributes in two folders being compared).

  (4) The most international solution would be to use a local instance of the
      Collator class and generate CollationKey objects.  Unfortunately, while
      local collators may be good at their own language, they tend to be poor
      with characters from other languages, often defaulting to Unicode string
      order.  File names can be in any language, so you prefer a sorting order
      that is consistent across all languages.
*/
  static String createSortKey(
    boolean folderflag,           // false for files, true for folders
    String filename)              // file or folder name
  {
    /* Method (1) above: return file name unchanged. */

//  return(filename);

    /* Method (2) above: lowercase plus original case. */

//  return(filename.toLowerCase() + " " + filename);

    /* Method (3) above: files before folders, then lowercase + original. */

    StringBuffer buffer;          // faster than String for multiple appends
    buffer = new StringBuffer();  // allocate empty string buffer for result
    if (folderflag)               // if this object is a directory/folder
      buffer.append("2 ");        // then folders come after files
    else                          // else not a folder, so assume a file
      buffer.append("1 ");        // and files come before folders
    buffer.append(filename.toLowerCase()); // consistent case in lowercase
    buffer.append(" ");           // delimiter between lowercase and original
    buffer.append(filename);      // original file name in original case
    return(buffer.toString());    // give caller our converted string
  }


/*
  doActionSelection() method

  This GUI method is called when the user changes the combo box with the list
  of possible actions.  We rearrange the buttons and text fields to match what
  the selected action supports.
*/
  static void doActionSelection()
  {
    outputText.setText("");       // clear output text area
    switch(actionDialog.getSelectedIndex())
    {
      case (ACTION_COMPARE_FOLDERS):  // compare two folders
        caseCheckbox.setEnabled(true);
        firstFileButton.setText("First Folder or Checksum...");
        recurseCheckbox.setEnabled(true);
        scrollCheckbox.setEnabled(true);
        secondFileButton.setText("Second Folder or Checksum...");
        showFileDialog.setEnabled(true);
        putOutput("Compare two folders to determine if all files and subfolders", false);
        putOutput("are identical.  Folders may be on the same computer, on the", false);
        putOutput("local network, or they may be represented by checksum files.", false);
        putOutput("Checksum files are used when the original folders are not", false);
        putOutput("available.  A checksum file is a plain text file in XML", false);
        putOutput("(Extensible Markup Language) format with the name, size, and", false);
        putOutput("checksums for each file.  Checksums are small hexadecimal", false);
        putOutput("\"signatures\" for testing whether or not files have been", false);
        putOutput("copied correctly, such as over a network.", false);
        putOutput("", false);
        putOutput("Select an action to perform with the top-left button.", false);
        putOutput("Browse for files or folders, or type the full path names.", false);
        putOutput("Click the \"Start\" button to begin comparing.  (Click the", false);
        putOutput("\"Cancel\" button to stop.)", false);
        putOutput("", false);
        putOutput(COPYRIGHT_NOTICE, false);
        break;

      case (ACTION_CREATE_CHECKSUM): // create checksums for given file/folder
        caseCheckbox.setEnabled(false);
        firstFileButton.setText("Read File or Folder...");
        recurseCheckbox.setEnabled(true);
        scrollCheckbox.setEnabled(false);
        secondFileButton.setText("Write Checksum File...");
        showFileDialog.setEnabled(false);
        putOutput("Create a checksum file for a given folder (or one data", false);
        putOutput("file).  Put the folder as the first file name.  Output will", false);
        putOutput("be written to the second file name as XML text.", false);
        break;

      case (ACTION_FIND_DUPLICATES): // find duplicate files, checksums folders
        caseCheckbox.setEnabled(false);
        firstFileButton.setText("Trusted Folder or Checksum...");
        recurseCheckbox.setEnabled(false);
        scrollCheckbox.setEnabled(true);
        secondFileButton.setText("Unknown Folder or Checksum...");
        showFileDialog.setEnabled(true);
        putOutput("To find duplicate files (equal, identical, same) or unique", false);
        putOutput("files (different) inside a single checksum or folder, put", false);
        putOutput("the checksum or folder as the first file name.  Don't put a", false);
        putOutput("second name.  Select an option for which files to report.", false);
        putOutput("", false);
        putOutput("To find duplicate or unique files using two checksums, a", false);
        putOutput("checksum and a folder, or two folders, put the known good or", false);
        putOutput("\"trusted\" checksum or folder as the first file name.  Put", false);
        putOutput("the unknown checksum or folder as the second file name.", false);
        putOutput("(Duplicate files inside the \"trusted\" folder are ignored.)", false);
        break;

      case (ACTION_UPDATE_CHECKSUM): // update checksum with new folder data
        caseCheckbox.setEnabled(false);
        firstFileButton.setText("Read File or Folder...");
        recurseCheckbox.setEnabled(true);
        scrollCheckbox.setEnabled(true);
        secondFileButton.setText("Old Checksum File...");
        showFileDialog.setEnabled(true);
        putOutput("Update a checksum file by scanning a given folder (or one", false);
        putOutput("data file) and assuming that files with the same name, size,", false);
        putOutput("and date have not changed.", false);
        break;

      default:                    // invalid action
        putError("Invalid action index <" + actionDialog.getSelectedIndex()
          + "> in doActionSelection for <"
          + actionDialog.getSelectedItem() + ">.");
        break;
    }
    outputText.select(0, 0);      // force scroll to beginning of text

  } // end of doActionSelection() method


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
    putOutput("Cancelled by user.", true); // write message and scroll output
  }


/*
  doConsoleFiles() method

  When running as a console application, the user has given us one or two file
  or folder names.  Depending upon how many names were given, and whether they
  are files or folders, we call different routines to do the real processing.
  Given one file or folder, we always generate new checksums.  Given two files
  or folders, we do either a comparison or an update.  The syntax is confusing
  and would be better with a command keyword before the file or folder names.

  Files are assumed to be text files with checksum information.  Folders are
  assumed to contain real files and subfolders.
*/
  static int doConsoleFiles(String firstName, String secondName)
  {
    CompareFolders3File firstChecksum; // first calculated checksums
    File firstFile;               // Java File object for <firstName>
    CompareFolders3File secondChecksum; // second calculated checksums
    File secondFile;              // Java File object for <secondName>
    int status;                   // exit status for console application

    /* If only one file or folder name was given, then we always open that file
    or folder and calculate checksums.  Output goes on standard output. */

    if (secondName.length() == 0)
    {
      firstFile = canonicalFile(new File(firstName));
                                  // turn file name into usable File object
      status = createChecksum(firstFile, null); // write checksum on stdout
    }

    /* If two file or folder names were given, then we do either a comparison
    or an update.  Files must contain checksum information in XML format. */

    else
    {
      firstFile = canonicalFile(new File(firstName));
                                  // turn file name into usable File object
      secondFile = canonicalFile(new File(secondName));

      if (firstFile.isDirectory() && secondFile.isDirectory())
      {
        status = compareTwoFolders(firstFile, secondFile);
      }
      else if (firstFile.isDirectory() && secondFile.isFile())
      {
        status = compareFolderChecksum(firstFile, secondFile);
                                  // correct folder and checksum file order
      }
      else if (firstFile.isFile() && secondFile.isDirectory())
      {
//      status = compareFolderChecksum(secondFile, firstFile);  // command line compare checksum
        status = updateChecksum(secondFile, firstFile);         // command line update checksum
                                  // reverse folder and checksum file order
      }
      else if (firstFile.isFile() && secondFile.isFile())
      {
        status = compareTwoChecksums(firstFile, secondFile);
      }
      else
      {
        /* One or both of the input file names are bad. */

        System.err.println("Can't open one or both input files.");
        System.err.println("First file name is: " + firstName);
        System.err.println("Second file name is: " + secondName);
        showHelp();               // show help summary
        status = EXIT_UNKNOWN;    // any problem means unknown result status
      }
    }

    /* You will notice that the console application is missing some features
    found in the graphical interface.  We don't calculate checksums and write
    them to a named file, for example.  The user can do that by redirecting
    standard output.  The number of files given as parameters completely
    decides between creating checksums and comparing files and/or checksums. */

    return(status);               // return whatever status we could find

  } // end of doConsoleFiles() method


/*
  doFirstFileButton() method

  Browse for the first file or folder, and save the full file name with the
  path.  We don't know at this point what the user will be asking us to do with
  the file or folder, so we accept any single file or folder (no multiple
  selections).  If the user cancels the file selection dialog box, then the
  current file name text is not changed.

  It is not always possible to convert a Java File object into a path name, and
  later convert this path name back into a matching File object.  Slashes (/)
  in a Macintosh file/folder name, for example, will be replaced with colons
  (:) by the Java run-time environment when the path name is constructed.
*/
  static void doFirstFileButton()
  {
    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Select First File or Folder...");
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
    {
      firstFileSaved = fileChooser.getSelectedFile(); // correct Java object
      firstFileString = firstFileSaved.getPath(); // possibly correct path
      firstFileDialog.setText(firstFileString); // save full path in dialog
    }
    return;                       // return to caller, our work is done
  }


/*
  doSecondFileButton() method

  Similar to doFirstFileButton(), browse for the second file or folder, and
  save the full file name with the path.  See additional notes above for the
  doFirstFileButton() method.
*/
  static void doSecondFileButton()
  {
    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Select Second File or Folder...");
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
    {
      secondFileSaved = fileChooser.getSelectedFile(); // correct Java object
      secondFileString = secondFileSaved.getPath(); // possibly correct path
      secondFileDialog.setText(secondFileString); // save full path in dialog
    }
    return;                       // return to caller, our work is done
  }


/*
  doStartButton() method

  The user clicked the "Start" button to start processing something or other.
  We won't really know what we're doing until we check all the options, file
  names, etc.  Then we run in a separate thread that can be cancelled, and
  won't interfere with the main GUI thread.
*/
  static void doStartButton()
  {
    /* Reset the global file counters.  Not all counters are used by each
    action.  Comparisons use <totalDiffer> and <totalSame>.  Checksum creation
    uses <totalFiles>, <totalFolders>, and <totalSize>.  There is no guarantee
    that any particular action uses any counters at all! */

    totalDiffer = totalFiles = totalFolders = totalSame = totalSize = 0;

    /* Disable the "Start" button until we are done, and enable a "Cancel"
    button in case our secondary thread runs for a long time and the user
    panics. */

    actionDialog.setEnabled(false); // suspend changes to current action
    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    firstFileButton.setEnabled(false); // suspend changes to first file
    firstFileDialog.setEnabled(false);
    outputText.setText("");       // clear output text area
    secondFileButton.setEnabled(false); // suspend changes to second file
    secondFileDialog.setEnabled(false);
    startButton.setEnabled(false); // suspend "Start" until we are done

    setStatusMessage(EMPTY_STATUS); // clear status message and file counters
    statusTimer.start();          // start updating the status message

    doStartThread = new Thread(new CompareFolders3User(), "doStartRunner");
    doStartThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    doStartThread.start();        // run separate thread to open files, report

  } // end of doStartButton() method


/*
  doStartRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files or folders in the
  context of the "main" class.  By doing all the heavy-duty work in a separate
  thread, we won't stall the main thread that runs the graphical interface, and
  we allow the user to cancel the work if it takes too long.
*/
  static void doStartRunner()
  {
    boolean errorFlag;            // true if user's files/folders incorrect
    File firstFile;               // Java File object for first file/folder
    File secondFile;              // Java File object for second file/folder

    /* Start checking the caller's file or folder names.  Use original Java
    File objects returned by the file selection dialog box, when not all File
    objects can be represented by path names in plain text characters (POSIX
    compliance). */

    errorFlag = false;            // assume that user will supply good input

    if ((pathSafeFlag == false)   // if not all files have valid path names
      && (firstFileSaved != null) // and file was selected by GUI dialog box
      && (firstFileDialog.getText().equals(firstFileString))) // not edited
    {
      firstFile = firstFileSaved; // then use that exact Java File object
    }
    else                          // create new File object from path name
    {
      firstFileSaved = null;      // don't need saved File object anymore
      firstFileString = firstFileDialog.getText().trim(); // clean up input
      if (firstFileString.length() > 0) // create File object if name given
        firstFile = canonicalFile(new File(firstFileString));
      else                        // invalidate File object if name missing
        firstFile = null;
    }

    if ((pathSafeFlag == false)   // if not all files have valid path names
      && (secondFileSaved != null) // and file was selected by GUI dialog box
      && (secondFileDialog.getText().equals(secondFileString))) // not edited
    {
      secondFile = secondFileSaved; // then use that exact Java File object
    }
    else                          // create new File object from path name
    {
      secondFileSaved = null;     // don't need saved File object anymore
      secondFileString = secondFileDialog.getText().trim(); // clean up input
      if (secondFileString.length() > 0) // create File object if name given
        secondFile = canonicalFile(new File(secondFileString));
      else                        // invalidate File object if name missing
        secondFile = null;
    }

    /* Process the request differently depending upon what action the user has
    chosen.  We carefully consider cases in the following order: a <null> File
    object created above to be missing, a File object for something that does
    not exist, a File object for a real directory (folder), a File object for a
    real file, and finally a File object for something that exists but isn't a
    file or folder. */

    try                           // catch most "out of memory" errors
    {
      switch(actionDialog.getSelectedIndex())
      {
        /* Compare two folders or checksum files. */

        case (ACTION_COMPARE_FOLDERS):
          if ((firstFile == null) || (firstFile.exists() == false)
            || (secondFile == null) || (secondFile.exists() == false))
          {
            errorFlag = true;     // always need both files or folders
          }
          else if (firstFile.isDirectory() && secondFile.isDirectory())
          {
            compareTwoFolders(firstFile, secondFile);
          }
          else if (firstFile.isDirectory() && secondFile.isFile())
          {
            compareFolderChecksum(firstFile, secondFile); // normal order
          }
          else if (firstFile.isFile() && secondFile.isDirectory())
          {
            compareFolderChecksum(secondFile, firstFile); // reverse order
          }
          else if (firstFile.isFile() && secondFile.isFile())
          {
            compareTwoChecksums(firstFile, secondFile);
          }
          else
            errorFlag = true;     // give the user some help, with a warning
          break;

        /* Create a checksum file for a given file or folder. */

        case (ACTION_CREATE_CHECKSUM):
          if ((firstFile == null) || (firstFile.exists() == false))
          {
            errorFlag = true;     // always need a first file or folder
          }
          else if (secondFile == null)
          {
            createChecksum(firstFile, null); // write checksum in GUI text area
          }
          else if (canWriteFile(secondFile))
          {
            createChecksum(firstFile, secondFile); // write checksum to file
          }
          else
            errorFlag = true;     // give the user some help, with a warning
          break;

        /* Compare a known or "trusted" checksum file/folder with an "unknown"
        checksum file/folder, to find either duplicate files (that is, the
        same) or unique files (different).  There are actually six situations:

           1. single folder
           2. trusted folder versus unknown folder
           3. trusted folder versus unknown checksum file
           4. single checksum file
           5. trusted checksum file versus unknown folder
           6. trusted checksum file versus unknown checksum file

        All duplicate file methods scan recursively, independent of the value
        of the <recurseFlag> global variable.  Otherwise, would the flag apply
        to the first checksum/folder, the second checksum/folder, or both? */

        case (ACTION_FIND_DUPLICATES):
          if (firstFile == null)  // omit first file means single second file
          {
            if ((secondFile == null) || (secondFile.exists() == false))
            {
              errorFlag = true;   // need second file/folder if no first given
            }
            else if (secondFile.isDirectory())
            {
              findDupFoldOne(secondFile); // find all duplicates in one folder
            }
            else if (secondFile.isFile())
            {
              findDupCheckOne(secondFile); // find all duplicates in one checksum
            }
            else
              errorFlag = true;   // give the user some help, with a warning
          }
          else if (firstFile.exists() == false)
          {
            errorFlag = true;     // any given first file must already exist
          }
          else if (firstFile.isDirectory())
          {
            if (secondFile == null) // omit second file means single first file
            {
              findDupFoldOne(firstFile); // find all duplicates in one folder
            }
            else if (secondFile.exists() == false)
            {
              errorFlag = true;   // any given second file must already exist
            }
            else if (secondFile.isDirectory())
            {
              findDupFoldTwo(firstFile, secondFile);
                                  // trusted folder versus unknown folder
            }
            else if (secondFile.isFile())
            {
              findDupFoldCheck(firstFile, secondFile);
                                  // trusted folder versus unknown checksum
            }
            else
              errorFlag = true;   // give the user some help, with a warning
          }
          else if (firstFile.isFile())
          {
            if (secondFile == null) // omit second file means single first file
            {
              findDupCheckOne(firstFile); // find all duplicates in one checksum
            }
            else if (secondFile.exists() == false)
            {
              errorFlag = true;   // any given second file must already exist
            }
            else if (secondFile.isDirectory())
            {
              findDupCheckFold(firstFile, secondFile);
                                  // trusted checksum versus unknown folder
            }
            else if (secondFile.isFile())
            {
              findDupCheckTwo(firstFile, secondFile);
                                  // trusted checksum versus unknown checksum
            }
            else
              errorFlag = true;   // give the user some help, with a warning
          }
          else
            errorFlag = true;     // give the user some help, with a warning
          break;

        /* Update a checksum file by rescanning a file or folder. */

        case (ACTION_UPDATE_CHECKSUM):
          if ((firstFile == null) || (firstFile.exists() == false)
            || (secondFile == null) || (secondFile.exists() == false))
          {
            errorFlag = true;     // always need both files or folders
          }
          else if ((firstFile.isDirectory() || firstFile.isFile())
            && secondFile.isFile())
          {
            updateChecksum(firstFile, secondFile); // update first into second
          }
          else
            errorFlag = true;     // give the user some help, with a warning
          break;

        /* The good old final option: none of the above. */

        default:
          putError("Invalid action index <" + actionDialog.getSelectedIndex()
            + "> in doStartRunner for <"
            + actionDialog.getSelectedItem() + ">.");
          break;
      }

      /* If the user's input wasn't valid for the action selected, then repeat
      our general help for that action. */

      if (errorFlag)              // was there a problem with the user's input?
      {
        doActionSelection();      // call someone else to do the real work
        putError("");             // blank line
        putError("Your file or folder names do not match what is required for");
        putError("the selected action.  The following information may help:");

        /* Show the user what we found for files and/or folders, in case this
        is not what the user was expecting, and hence is the cause of the
        error. */

        if (firstFile == null)
          putError("First file/folder is missing.");
        else
        {
          putError("First file/folder name = <" + firstFile.getName() + ">");
          putError("First file/folder path = <" + firstFile.getPath() + ">");
          putError("First file/folder " + accessDescription(firstFile) + ".");
        }

        if (secondFile == null)
          putError("Second file/folder is missing.");
        else
        {
          putError("Second file/folder name = <" + secondFile.getName() + ">");
          putError("Second file/folder path = <" + secondFile.getPath() + ">");
          putError("Second file/folder " + accessDescription(secondFile) + ".");
        }
      }
    }

    /* When there is not enough memory for our own data structures above, we
    can usually notify the user.  However, if the problem is in a GUI element
    (such as the output text area being too big), then a GUI alert may fail.
    Some called methods generate "out of memory" errors that we don't catch but
    which generally lead to other errors that we do catch. */

    catch (OutOfMemoryError oome) // for this thread only, not the GUI thread
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Not enough memory to complete your request.\nPlease close this program, then try increasing\nthe Java heap size with the -Xmx option on the\nJava command line.");
    }

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again.  Clear the status message at the bottom of the
    screen. */

    actionDialog.setEnabled(true); // enable changes to current action
    cancelButton.setEnabled(false); // disable "Cancel" button
    firstFileButton.setEnabled(true); // enable changes to first file
    firstFileDialog.setEnabled(true);
    secondFileButton.setEnabled(true); // enable changes to second file
    secondFileDialog.setEnabled(true);
    startButton.setEnabled(true); // enable "Start" button

    statusTimer.stop();           // stop updating the status message by timer
    setStatusMessage(EMPTY_STATUS); // and clear any previous status message

  } // end of doStartRunner() method


/*
  findDupCheckFold() method

  Given a known good checksum file, find all duplicate or unique files inside a
  second folder.

  All duplicate file methods scan recursively, independent of the value of the
  <recurseFlag> global variable.
*/
  static int findDupCheckFold(
    File firstFile,               // first checksum file (only)
    File secondFile)              // second folder (only)
  {
    Vector orderList = new Vector(); // start with an empty linear order
    TreeMap sizeList = new TreeMap(); // start with an empty size mapping

    CompareFolders3File firstChecksum = readChecksumFile(firstFile);
    if (cancelFlag || (firstChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)
    findDupScanChecksum(sizeList, null, "", firstChecksum);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button
    firstChecksum = null;         // release memory for these linked objects

    findDupScanFolder(null, orderList, "", secondFile);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button

    return(findDupCompare(sizeList, orderList)); // compare checksum, folder

  } // end of findDupCheckFold() method


/*
  findDupCheckOne() method

  Find all duplicate or unique files inside one of our file checksum objects.

  All duplicate file methods scan recursively, independent of the value of the
  <recurseFlag> global variable.
*/
  static int findDupCheckOne(
    File firstFile)               // first checksum file (only)
  {
    Vector orderList = new Vector(); // start with an empty linear order
    TreeMap sizeList = new TreeMap(); // start with an empty size mapping

    CompareFolders3File firstChecksum = readChecksumFile(firstFile);
    if (cancelFlag || (firstChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)
    findDupScanChecksum(sizeList, orderList, "", firstChecksum);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button
    firstChecksum = null;         // release memory for these linked objects

    return(findDupCompare(sizeList, orderList)); // compare checksum to itself

  } // end of findDupCheckOne() method


/*
  findDupCheckTwo() method

  Given a known good checksum file, find all duplicate or unique files inside a
  second checksum file.

  All duplicate file methods scan recursively, independent of the value of the
  <recurseFlag> global variable.
*/
  static int findDupCheckTwo(
    File firstFile,               // first checksum file (only)
    File secondFile)              // second checksum file (only)
  {
    Vector orderList = new Vector(); // start with an empty linear order
    TreeMap sizeList = new TreeMap(); // start with an empty size mapping

    CompareFolders3File firstChecksum = readChecksumFile(firstFile);
    if (cancelFlag || (firstChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)
    findDupScanChecksum(sizeList, null, "", firstChecksum);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button
    firstChecksum = null;         // release memory for these linked objects

    CompareFolders3File secondChecksum = readChecksumFile(secondFile);
    if (cancelFlag || (secondChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)
    findDupScanChecksum(null, orderList, "", secondChecksum);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button
    secondChecksum = null;        // release memory for these linked objects

    return(findDupCompare(sizeList, orderList)); // compare checksum, checksum

  } // end of findDupCheckTwo() method


/*
  findDupCompare() method

  This is the one-and-only duplicate file finder, after the other initial
  methods have converted a file directory (folder) or checksum data to a
  common list format.

  The caller gives us two lists.  The first list is indexed by file size, a
  necessary (but not sufficient) condition for two files being equal.  The
  second list is a linear order.  For each file in the second list, in order,
  we find the number of identical files in the first list, that are not the
  same File object, of course.  Then if the file from the second list is not
  already in the first list, we add it for later comparisons.
*/
  static int findDupCompare(
    TreeMap sizeList,             // mapping of file sizes to File objects
    Vector orderList)             // sorted list of files in linear order
  {
    CompareFolders3File orderEntry; // current item from <orderList>
    boolean orderFound;           // true if this exact object in <sizeList>
    int orderIndex;               // current index into <orderList>
    int orderLength;              // number of items in <orderList>
    boolean sameFound;            // true if a distinct duplicate is found
    String sameText;              // message string for first duplicate match
    CompareFolders3File sizeEntry; // current item from <sizeVector>
    int sizeIndex;                // current index of into <sizeVector>
    Long sizeKey;                 // file size converted to an object
    int sizeLength;               // number of items in <sizeVector>
    Vector sizeVector;            // list of files having the same size

    /* Loop through all items in the ordered list. */

    setStatusMessage("Comparing file sizes and checksums...");
    orderLength = orderList.size(); // get number of items in ordered list
    for (orderIndex = 0; orderIndex < orderLength; orderIndex ++)
    {
      if (cancelFlag) break;      // stop if user hit the panic button
      orderEntry = (CompareFolders3File) orderList.get(orderIndex);
      if (orderEntry.size <= 0)   // ignore empty files
      {
        putOutput(orderEntry.name + " - ignoring zero-length (empty) file");
      }
      else
      {
        /* Count how many files have the same checksum (that is, are equal). */

        orderFound = false;       // assume that <orderEntry> not in <sizeList>
        sameFound = false;        // assume that no files are equal
        sameText = null;          // no message yet about first duplicate match
        sizeKey = new Long(orderEntry.size); // create index into size list
        if (sizeList.containsKey(sizeKey) == false) // is there a size entry?
          sizeList.put(sizeKey, new Vector()); // no, start with empty vector
        sizeVector = (Vector) sizeList.get(sizeKey); // files with same size
        sizeLength = sizeVector.size(); // get number of known files this size

        /* Loop through all known files with the same size. */

        for (sizeIndex = 0; sizeIndex < sizeLength; sizeIndex ++)
        {
          if (cancelFlag) break;  // stop if user hit the panic button
          sizeEntry = (CompareFolders3File) sizeVector.get(sizeIndex);
          if ((orderEntry == sizeEntry) // exactly the same Java data object?
            || ((orderEntry.object != null) && (sizeEntry.object != null)
            && orderEntry.object.equals(sizeEntry.object))) // same real file?
          {
            /* If we were always comparing files with files (never checksums),
            then it would be good to call equals() for the Java File objects to
            see if they are for the same underlying file.  Unfortunately, since
            we can't do something similar with checksums, then folder-vs-folder
            duplicate searches would yield inconsistent results compared to
            checksum-vs-folder and checksum-vs-checksum. */

            orderFound = true;    // can't compare a file against itself
          }
          else
          {
            /* Do we need to compute checksums for <orderEntry>? */

            if ((cancelFlag == false) && (orderEntry.object != null)
              && ((orderEntry.md5 == null) || (orderEntry.md5.length() == 0)
              || (orderEntry.sha1 == null) || (orderEntry.sha1.length() == 0)))
            {
              calcFileChecksum(orderEntry.object, orderEntry);
            }

            /* Do we need to compute checksums for <sizeEntry>? */

            if ((cancelFlag == false) && (sizeEntry.object != null)
              && ((sizeEntry.md5 == null) || (sizeEntry.md5.length() == 0)
              || (sizeEntry.sha1 == null) || (sizeEntry.sha1.length() == 0)))
            {
              calcFileChecksum(sizeEntry.object, sizeEntry);
            }

            /* Compare checksums, assuming that SHA1 is better than MD5. */

            if (cancelFlag)
              { /* do nothing */ }
            else if ((orderEntry.sha1 != null) && (orderEntry.sha1.length() > 0)
              && (sizeEntry.sha1 != null) && (sizeEntry.sha1.length() > 0))
            {
              if (orderEntry.sha1.equals(sizeEntry.sha1))
              {
                sameFound = true; // two distinct files have same SHA1 checksum
                if (sameText == null) // create a message for first duplicate
                  sameText = "same SHA1 as " + sizeEntry.name;
              }
            }
            else if ((orderEntry.md5 != null) && (orderEntry.md5.length() > 0)
              && (sizeEntry.md5 != null) && (sizeEntry.md5.length() > 0))
            {
              if (orderEntry.md5.equals(sizeEntry.md5))
              {
                sameFound = true; // two distinct files have same MD5 checksum
                if (sameText == null) // create a message for first duplicate
                  sameText = "same MD5 as " + sizeEntry.name;
              }
            }
          }
          if (sameFound)          // did we find a duplicate (matching) file?
            break;                // yes, no need to continue checking others
        }

        /* Use <sameFound> to decide if this <orderEntry> is a duplicate. */

        if (cancelFlag)
          { /* do nothing */ }
        else if (sameFound)       // if our <orderEntry> is a duplicate
        {
          totalSame ++;           // increment total number of duplicates
          putSelectSame(orderEntry.name + " - " + sameText);
        }
        else                      // if our <orderEntry> is unique (so far)
        {
          totalDiffer ++;         // increment total number of unique files
          putSelectDiff(orderEntry.name + " - unique (no duplicate found)");
        }

        /* Insert the current <orderEntry> into <sizeList> if it is not already
        found in that list.  This prevents multiple copies of the same file in
        an "unknown" folder from being added later to a "trusted" folder. */

        if (cancelFlag)
          { /* do nothing */ }
        else if (orderFound == false) // was order entry found in size list?
          ((Vector) sizeList.get(sizeKey)).add(orderEntry);
                                  // no, pretend order entry is a known file
      }
    }

    /* Tell the user what we found, even if we were cancelled. */

    putError("Found " + prettyPlural(totalSame, "duplicate") + " and "
      + prettyPlural(totalDiffer, "unique file") + ".");
    return((int) totalSame);      // return the number of duplicate files

  } // end of findDupCompare() method


/*
  findDupFoldCheck() method

  Given a known good folder, find all duplicate or unique files inside a second
  checksum file.

  All duplicate file methods scan recursively, independent of the value of the
  <recurseFlag> global variable.
*/
  static int findDupFoldCheck(
    File firstFile,               // first folder (only)
    File secondFile)              // second checksum file (only)
  {
    Vector orderList = new Vector(); // start with an empty linear order
    TreeMap sizeList = new TreeMap(); // start with an empty size mapping

    findDupScanFolder(sizeList, null, "", firstFile);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button

    CompareFolders3File secondChecksum = readChecksumFile(secondFile);
    if (cancelFlag || (secondChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)
    findDupScanChecksum(null, orderList, "", secondChecksum);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button
    secondChecksum = null;        // release memory for these linked objects

    return(findDupCompare(sizeList, orderList)); // compare folder, checksum

  } // end of findDupFoldCheck() method


/*
  findDupFoldOne() method

  Find all duplicate or unique files inside one folder and its subfolders.

  All duplicate file methods scan recursively, independent of the value of the
  <recurseFlag> global variable.
*/
  static int findDupFoldOne(
    File firstFile)               // first folder (only)
  {
    Vector orderList = new Vector(); // start with an empty linear order
    TreeMap sizeList = new TreeMap(); // start with an empty size mapping

    findDupScanFolder(sizeList, orderList, "", firstFile);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button

    return(findDupCompare(sizeList, orderList)); // compare folder to itself

  } // end of findDupFoldOne() method


/*
  findDupFoldTwo() method

  Given a known good folder, find all duplicate or unique files inside a second
  folder.

  All duplicate file methods scan recursively, independent of the value of the
  <recurseFlag> global variable.
*/
  static int findDupFoldTwo(
    File firstFile,               // first folder (only)
    File secondFile)              // second folder (only)
  {
    Vector orderList = new Vector(); // start with an empty linear order
    TreeMap sizeList = new TreeMap(); // start with an empty size mapping

    findDupScanFolder(sizeList, null, "", firstFile);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button

    findDupScanFolder(null, orderList, "", secondFile);
    if (cancelFlag) return(EXIT_UNKNOWN); // stop if user hit the panic button

    return(findDupCompare(sizeList, orderList)); // compare folder, folder

  } // end of findDupFoldTwo() method


/*
  findDupScanChecksum() method

  Recursively scan CompareFolders3File objects to create up to two lists.  One
  list has file sizes indexed to vectors of CompareFolders3File objects with
  the same size.  Another list is a vector of CompareFolders3File objects in
  the sorted (linear) order that they were found.  Both lists are optional.

  findDupScanChecksum() and findDupScanFolder() generate lists with identical
  structure; however, the contents differ in what information is available and
  what information needs to be generated (regenerated) upon a comparison.
*/
  static void findDupScanChecksum(
    TreeMap sizeList,             // mapping of file sizes to File objects
    Vector orderList,             // sorted list of files in linear order
    String pathPrefix,            // path prefix for file names, or "" to start
    CompareFolders3File givenEntry) // caller gives us one file checksum object
  {
    int i;                        // index variable
    int length;                   // total number of elements in list
    CompareFolders3File newentry; // checksum object for one file or folder
    Long sizekey;                 // file size converted to an object

    if (cancelFlag) return;       // stop if user hit the panic button

    if (givenEntry.list != null)  // is this checksum object a folder?
    {
      setStatusMessage("Scanning " + givenEntry.name); // show name, no path
      length = givenEntry.list.size(); // get total number of elements in list
      for (i = 0; i < length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        newentry = (CompareFolders3File) givenEntry.list.get(i);
        if (newentry.list != null) // is this entry for a folder?
          findDupScanChecksum(sizeList, orderList, (pathPrefix + newentry.name
            + systemFileSep), newentry); // yes, do subfolder
        else                      // entry is for a regular file
          findDupScanChecksum(sizeList, orderList, pathPrefix, newentry);
      }
    }
    else                          // this checksum object must be a file
    {
      newentry = new CompareFolders3File(); // create new file checksum object
      newentry.name = pathPrefix + givenEntry.name; // new relative file name
      newentry.md5 = givenEntry.md5; // copy only those fields that we need
      newentry.sha1 = givenEntry.sha1;
      newentry.size = givenEntry.size;

      if (sizeList != null)       // add to this indexed list, if given
      {
        sizekey = new Long(newentry.size); // get file size as an object
        if (sizeList.containsKey(sizekey) == false) // map entry for this size?
          sizeList.put(sizekey, new Vector()); // no, add empty list for size
        ((Vector) sizeList.get(sizekey)).add(newentry); // append new entry
      }

      if (orderList != null)      // add entry to ordered list, if given
        orderList.add(newentry);  // append all files in our sorted order
    }
  } // end of findDupScanChecksum() method


/*
  findDupScanFolder() method

  Recursively scan a file directory (folder) to create up to two lists.  One
  list has file sizes indexed to vectors of CompareFolders3File objects that
  have the same size.  Another list is a vector of CompareFolders3File objects
  in the sorted (linear) order that they were found.  Both lists are optional.

  findDupScanChecksum() and findDupScanFolder() generate lists with identical
  structure; however, the contents differ in what information is available and
  what information needs to be generated (regenerated) upon a comparison.
*/
  static void findDupScanFolder(
    TreeMap sizeList,             // mapping of file sizes to File objects
    Vector orderList,             // sorted list of files in linear order
    String pathPrefix,            // path prefix for file names, or "" to start
    File givenFile)               // caller gives us one file or folder
  {
    File[] contents;              // contents if <givenFile> is a folder
    int i;                        // index variable
    CompareFolders3File newentry; // checksum object for one file or folder
    File next;                    // next File object from <contents>
    Long sizekey;                 // file size converted to an object

    if (cancelFlag) return;       // stop if user hit the panic button

    if (givenFile.isDirectory())  // is this a folder?
    {
      setStatusMessage("Scanning " + givenFile.getPath()); // show folder name
      contents = sortFileList(givenFile.listFiles()); // no filter, but sorted
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
          { /* Silently ignore hidden files and folders. */ }
        else if (next.isDirectory()) // is this entry for a folder?
          findDupScanFolder(sizeList, orderList, (pathPrefix + next.getName()
            + systemFileSep), next); // yes, do subfolder
        else if (next.isFile())   // entry is for a regular file
          findDupScanFolder(sizeList, orderList, pathPrefix, next);
        else
          { /* Silently ignore unknown directory entries. */ }
      }
    }
    else if (givenFile.isFile())  // is this a file?
    {
      newentry = new CompareFolders3File(); // create new file checksum object
      newentry.name = pathPrefix + givenFile.getName(); // relative file name
      newentry.object = givenFile; // remember original Java File object
      newentry.size = givenFile.length(); // size of file in bytes

      if (sizeList != null)       // add to this indexed list, if given
      {
        sizekey = new Long(newentry.size); // get file size as an object
        if (sizeList.containsKey(sizekey) == false) // map entry for this size?
          sizeList.put(sizekey, new Vector()); // no, add empty list for size
        ((Vector) sizeList.get(sizekey)).add(newentry); // append new entry
      }

      if (orderList != null)      // add entry to ordered list, if given
        orderList.add(newentry);  // append all files in our sorted order
    }
    else
    {
      /* Silently ignore anything we can't identify as a file or folder. */
    }
  } // end of findDupScanFolder() method


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
  makeCharReference() method

  This method converts a string parameter to plain text (ASCII) in such a way
  that any characters we consider unprintable are replaced by XML character
  references in decimal such as &#8225; (double dagger).  See:

      http://www.w3.org/TR/REC-xml/#sec-references

  For path separators in file names, the caller should have already replaced
  backslashes (\) by forward slashes (/), because backslashes are interpreted
  as currency symbols in some character sets.  Hence, we consider backslashes
  to be special characters.  Some systems like Java use backslashes as their
  escape character.

  Leading, trailing, and multiple spaces are treated as unprintable characters,
  to prevent them from getting trimmed or compressed.  Leading and trailing
  spaces are only known to happen in file/folder names on Macintosh computers;
  multiple internal spaces can happen on any computer system.  Other rules for
  spaces are not recognized and/or enforced, such as Windows' dislike of spaces
  after the final "dot" (.) in a file name and before the file name extension.

  This method does not assume a maximum binary value for character codes, other
  than being able to convert a <char> to an <int>.
*/
  static String makeCharReference(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int count;                    // number of consecutive plain-text spaces
    int i;                        // index variable
    int length;                   // size of input string in characters
    final String SPACE = "&#32;"; // replacement for unprintable spaces

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = input.length();      // get size of input string in characters

    i = 0;                        // index of first input character
    while ((i < length) && (input.charAt(i) == ' '))
    {                             // remove all leading plain-text spaces
      buffer.append(SPACE);       // and treat as unprintable characters
      i ++;                       // continue with next character
    }

    count = 0;                    // no consecutive spaces found yet
    while (i < length)            // look at all remaining characters
    {
      ch = input.charAt(i ++);    // get one character from input string
      if (ch == ' ')              // is this an internal or trailing space?
        count ++;                 // yes, count number of consecutive spaces
      else                        // no, character is not a plain-text space
      {
        if (count == 1) buffer.append(' '); // if exactly one pending space
        else while (count -- > 0) buffer.append(SPACE); // multiple spaces
        count = 0;                // all pending spaces are now done

        if ((ch <= 0x20)          // control codes and spaces are unprintable
          || (ch >= 0x7F)         // anything past plain text is unprintable
          || (ch == '&')          // ampersand is character escape delimiter
          || (ch == '<')          // less and greater than are XML delimiters
          || (ch == '>')
          || (ch == '\\'))        // backslash is often a currency symbol
        {
          buffer.append("&#");    // start of XML decimal character reference
          buffer.append(String.valueOf((int) ch)); // decimal character number
          buffer.append(';');     // end of XML decimal character reference
        }
        else
          buffer.append(ch);      // append one character unchanged to result
      }
    }

    while (count -- > 0) buffer.append(SPACE); // unprintable trailing spaces

    return(buffer.toString());    // give caller our converted string

  } // end of makeCharReference() method


/*
  parseCharReference() method

  This method parses XML character references in a string and converts them to
  Unicode text.  The standard five character entity references are supported:

      &amp;   &   ampersand
      &apos;  '   apostrophe
      &gt;    >   greater than
      &lt;    <   less than
      &quot;  "   quotation mark

  Numeric character references are supported in both decimal and hexadecimal:
  &#8225; and &#x2021; are both the double dagger symbol.  See:

      http://www.w3.org/TR/REC-xml/#sec-references

  The input string is usually in plain text (ASCII), but this is not assumed or
  required.  Since Java currently limits characters to 16 bits, any character
  references that exceed 0xFFFF are not converted and are copied to the result
  unchanged.  (Java 1.5 will have better support for extended character sets.)
*/
  static String parseCharReference(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int digits;                   // number of digits found in reference
    boolean hexflag;              // true if reference is hexadecimal
    int i;                        // index variable
    int length;                   // size of input string in characters
    final int maxCharValue = 0xFFFF; // maximum binary value of a character
    int start;                    // index for start of character reference
    int value;                    // numeric value of character reference

    buffer = new StringBuffer();  // allocate empty string buffer for result
    i = 0;                        // start input string at the beginning
    length = input.length();      // get size of input string in characters
    while (i < length)            // do the whole input string
    {
      ch = input.charAt(i);       // get one character from input string
      if (ch == '&')              // we only recognize this escape code
      {
        start = i;                // remember where character reference begins
        i ++;                     // index of next character
        if (i >= length)          // is there really another character?
        {
          buffer.append(input.substring(start, i)); // copy malformed syntax
        }
        else if (input.startsWith("amp;", i)) // character entity reference
        {
          buffer.append('&');     // ampersand
          i += 4;                 // skip over remainder of this reference
        }
        else if (input.startsWith("apos;", i)) // character entity reference
        {
          buffer.append('\'');    // apostrophe
          i += 5;                 // skip over remainder of this reference
        }
        else if (input.startsWith("gt;", i)) // character entity reference
        {
          buffer.append('>');     // greater than
          i += 3;                 // skip over remainder of this reference
        }
        else if (input.startsWith("lt;", i)) // character entity reference
        {
          buffer.append('<');     // less than
          i += 3;                 // skip over remainder of this reference
        }
        else if (input.startsWith("quot;", i)) // character entity reference
        {
          buffer.append('"');     // quotation mark
          i += 5;                 // skip over remainder of this reference
        }

        else if ((ch = input.charAt(i++)) != '#') // next character must be #
        {
          i --;                   // "put back" <ch> because might start new &
          buffer.append(input.substring(start, i)); // copy malformed syntax
        }
        else                      // start of numeric reference
        {
          digits = 0;             // no digits found yet in numeric reference
          hexflag = false;        // assume that number is in decimal
          value = 0;              // start with zero for the numeric value
          while ((digits >= 0) && (i < length) && (value <= maxCharValue))
                                  // loop through remaining characters
          {
            ch = input.charAt(i++); // get one character from input string
            if ((digits == 0) && (!hexflag) && ((ch == 'X') || (ch == 'x')))
            {
              hexflag = true;     // now parsing in hexadecimal
            }
            else if ((!hexflag) && (ch >= '0') && (ch <= '9')) // decimal digit?
            {
              digits ++;          // one more digit found
              value = (value * 10) + (ch - '0'); // add decimal to total value
            }
            else if (hexflag && (ch >= '0') && (ch <= '9')) // hex digit?
            {
              digits ++;          // one more digit found
              value = (value << 4) + (ch - '0'); // add hex to total value
            }
            else if (hexflag && (ch >= 'A') && (ch <= 'F')) // hex digit?
            {
              digits ++;          // one more digit found
              value = (value << 4) + (ch - 'A' + 10); // add hex to total value
            }
            else if (hexflag && (ch >= 'a') && (ch <= 'f')) // hex digit?
            {
              digits ++;          // one more digit found
              value = (value << 4) + (ch - 'a' + 10); // add hex to total value
            }
            else if ((digits > 0) && (ch == ';')) // end of numeric reference?
            {
              digits = -1;        // use <digits> to flag successful finish
            }
            else                  // not an acceptable character
            {
              i --;               // "put back" <ch> because might start new &
              break;              // exit from <while> loop
            }
          }

          /* The number of digits gets set to negative if we properly finished
          the character reference. */

          if ((digits < 0) && (value <= maxCharValue)) // successful finish?
            buffer.append((char) value); // yes, convert binary value to char
          else                    // reference was not terminated properly
            buffer.append(input.substring(start, i)); // copy malformed syntax
        }
      }
      else                        // not inside a character reference
      {
        buffer.append(ch);        // append this character to result string
        i ++;                     // index of next character in input string
      }
    }
    return(buffer.toString());    // give caller our converted string

  } // end of parseCharReference() method


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
    putOutput(text, scrollFlag);  // allow user to set default scroll behavior
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
  putSelective(), putSelectDiff(), putSelectSame() methods

  These are convenience methods that call putOutput() if a selection flag is
  true for "report different files" or "report identical (similar) files".
*/
  static void putSelective(boolean flag, String text)
  {
    if (flag) putOutput(text);    // write output only if selection flag true
  }

  static void putSelectDiff(String text)
  {
    putSelective(showDiffFlag, text); // only if we show differences, errors
  }

  static void putSelectSame(String text)
  {
    putSelective(showSameFlag, text); // only if show similar (identical) files
  }


/*
  readChecksumChar() method

  Read the next character from the input file, as an integer.  If there is a
  pending lookahead character from last time, return that instead.  Return the
  standard integer value of -1 when we reach the end of the file.

  A space is substituted for tabs, and a newline for line feeds, so that the
  caller only has to deal with two types of white space characters.
*/
  static int readChecksumChar(BufferedReader input)
    throws IOException            // comes from calling read()
  {
    int result;                   // our resulting character as an integer

    if (cancelFlag) return(-1);   // stop if user hit the panic button

    /* Get previous (pending) lookahead character, if there is/was one.
    Otherwise, drop into the read loop by assuming a null byte (0x00). */

    if (readFilePendingFlag == false) // if nothing saved from before
      result = 0x00;              // use null character to enter while loop
    else if (readFilePendingChar >= 0) // was previous a real character?
    {
      result = readFilePendingChar; // yes, use that previous character
      readFilePendingFlag = false; // don't use this character again
    }
    else                          // previous was actually end-of-file
    {
      result = readFilePendingChar; // yes, use previous end-of-file
      readFilePendingFlag = true; // repeat end-of-file if called again
    }

    /* Throw away all ASCII control codes except those we recognize.  This
    removes many nuisance characters. */

    while ((result >= 0) && (result <= 0x1F) && (result != '\n'))
    {
      result = input.read();      // read one input character from file
      if (result == '\n')         // DOS/Windows line feed or UNIX newline
        readFileLine ++;          // count newlines to report input line number
      else if (result == '\r')    // carriage return in DOS/Windows
        result = '\n';            // substitute, but don't count as a newline
      else if (result == '\t')    // ASCII tab character
        result = ' ';             // substitute with a space
    }
    return(result);               // give caller whatever we could find

  } // end of readChecksumChar() method


/*
  readChecksumFile() method

  Read a plain text file in XML format with checksum information for one file
  or one folder (possibly recursive).  Return one of our file-or-folder objects
  if successful.  Upon error, return the <null> object, including when the user
  clicks the "Cancel" button.

  This XML parser is very simple and only accepts files similar to what the
  writeChecksumFile() method generates for output.
*/
  static CompareFolders3File readChecksumFile(File givenFile)
  {
    String endToken;              // one input token (should be end-of-file)
    BufferedReader input;         // where we read our input, or <null>
    CompareFolders3File result;   // our recursive result

    if (cancelFlag) return(null); // stop if user hit the panic button
    result = null;                // default to no result
    try
    {
      input = new BufferedReader(new FileReader(givenFile));
                                  // try to open input file, use buffered I/O
      readFileLine = 1;           // first input line number is one
      readFilePendingFlag = false; // no pending lookahead character
      result = readChecksumRecurse(input, 0, "", null);
                                  // read one file or one recursive folder

      if ((!cancelFlag) && (result != null)
        && ((endToken = readChecksumToken(input)) != ENDFILE_TOKEN))
      {
        /* There is more input in the file than we expected. */

        cancelFlag = true;
        putError("Only one root file or folder allowed at line "
          + formatComma.format(readFileLine) + ": " + endToken);
        result = null;
      }

      if ((result != null) && (result.name.length() == 0))
        result.name = givenFile.getPath(); // default name is full file name

      input.close();              // try to close the file
    }
    catch (IOException except)
    {
      putError("Can't read " + givenFile.getPath() + " - "
        + except.getMessage());
      cancelFlag = true;          // try to abort any processing
    }

    /* Clean up and return to the caller. */

    if (cancelFlag)               // did user hit the panic button?
      result = null;              // yes, invalidate any/all work that we did

    return(result);               // return whatever we created to the caller

  } // end of readChecksumFile() method


/*
  readChecksumRecurse() method

  This is a recursive method called by the readChecksumFile() method to read
  one complete XML description for a file or for a folder (which may contain
  subfolders).  If successful, we return one of our file-or-folder objects.  If
  there are any problems, we stop reading and return the <null> object instead.
  The caller should check our result and also the <cancelFlag> variable.

  The parsing is very simple.  Correct input will be read correctly.  Many
  errors will be caught.  Some incorrect syntax will be accepted without
  comment, or mangled until it is acceptable.
*/
  static CompareFolders3File readChecksumRecurse(
    BufferedReader input,         // where we read input from
    int level,                    // subfolder level (reliable, accurate)
    String pathPrefix,            // path prefix for file names (unreliable)
    String oldToken)              // starting token from caller, or null
    throws IOException            // comes from calling readChecksumToken()
  {
    String first, second, third, fourth; // up to four input tokens
    CompareFolders3File newfile;  // for adding file or subfolder to folder
    CompareFolders3File result;   // our recursive result

    if (cancelFlag) return(null); // stop if user hit the panic button
    result = new CompareFolders3File(); // start with an empty data object

    /* The only tokens we are expecting are <file>, <folder>, or comments.
    Comments are removed by the readChecksumToken() method.  We have strings
    for the next four tokens.  For example:

        first = <file>
        second = <name>
        third = README.TXT
        fourth = </name>

    Don't reuse a variable for an earlier token, or else the <while> loops will
    either fail or repeat endlessly. */

    first = (oldToken != null) ? oldToken : readChecksumToken(input);
    if (first.equals("<file>"))
    {
      /* Read one complete file description. */

      result.files = 1;           // one file is one file (and counts as one)
      second = readChecksumToken(input); // get next input token
      while ((cancelFlag == false) && (second.equals("</file>") == false))
      {
        if (second.equals("<name>")) // file name, must be given
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing name for <file> group at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else
          {
            result.name = parseCharReference(third); // save file name
            fourth = readChecksumToken(input); // get next input token
            if (fourth.equals("</name>") == false)
            {
              cancelFlag = true;
              putError("Missing </name> after file name at line "
                + formatComma.format(readFileLine) + ": " + fourth);
            }
          }
        }
        else if (second.equals("<date>")) // file date and time, may be null
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals("</date>"))
          {
            result.date = "";     // null input becomes empty date string
          }
          else if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing date/time after file <date> at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else                    // we have a non-empty string
          {
            result.date = third;  // accept anything for the date and time
            fourth = readChecksumToken(input); // get next input token
            if (fourth.equals("</date>") == false)
            {
              cancelFlag = true;
              putError("Missing </date> after file date/time at line "
                + formatComma.format(readFileLine) + ": " + fourth);
            }
          }
        }
        else if (second.equals("<size>")) // file size, may be null
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals("</size>"))
          {
            result.size = -1;     // null input becomes unknown file size
          }
          else if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing number after file <size> at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else
          {
            try                   // number format may be bad
            {
              result.size = Long.parseLong(third); // save file size
            }
            catch (NumberFormatException except)
            {
              cancelFlag = true;
              putError("Invalid number after file <size> at line "
                + formatComma.format(readFileLine) + ": " + third);
            }
            if (!cancelFlag)      // if we are still alive and kicking
            {
              fourth = readChecksumToken(input); // get next input token
              if (fourth.equals("</size>") == false)
              {
                cancelFlag = true;
                putError("Missing </size> after file size at line "
                  + formatComma.format(readFileLine) + ": " + fourth);
              }
            }
          }
        }
        else if (second.equals("<crc32>")) // CRC32 checksum, may be null
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals("</crc32>"))
          {
            result.crc32 = "";    // null input becomes empty checksum
          }
          else if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing checksum after file <crc32> at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else                    // we have input that should be a checksum
          {
            result.crc32 = cleanChecksum(third); // clean and test string
            if (result.crc32.length() == 0) // was the string bad hex?
            {
              cancelFlag = true;
              putError("Invalid hex checksum after file <crc32> at line "
                + formatComma.format(readFileLine) + ": " + third);
            }

            if (!cancelFlag)      // if we are still alive and kicking
            {
              fourth = readChecksumToken(input); // get next input token
              if (fourth.equals("</crc32>") == false)
              {
                cancelFlag = true;
                putError("Missing </crc32> after CRC32 checksum at line "
                  + formatComma.format(readFileLine) + ": " + fourth);
              }
            }
          }
        }
        else if (second.equals("<md5>")) // MD5 checksum, may be null
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals("</md5>"))
          {
            result.md5 = "";      // null input becomes empty checksum
          }
          else if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing checksum after file <md5> at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else                    // we have input that should be a checksum
          {
            result.md5 = cleanChecksum(third); // clean and test string
            if (result.md5.length() == 0) // was the string bad hex?
            {
              cancelFlag = true;
              putError("Invalid hex checksum after file <md5> at line "
                + formatComma.format(readFileLine) + ": " + third);
            }

            if (!cancelFlag)      // if we are still alive and kicking
            {
              fourth = readChecksumToken(input); // get next input token
              if (fourth.equals("</md5>") == false)
              {
                cancelFlag = true;
                putError("Missing </md5> after MD5 checksum at line "
                  + formatComma.format(readFileLine) + ": " + fourth);
              }
            }
          }
        }
        else if (second.equals("<sha1>")) // SHA1 checksum, may be null
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals("</sha1>"))
          {
            result.sha1 = "";     // null input becomes empty checksum
          }
          else if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing checksum after file <sha1> at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else                    // we have input that should be a checksum
          {
            result.sha1 = cleanChecksum(third); // clean and test string
            if (result.sha1.length() == 0) // was the string bad hex?
            {
              cancelFlag = true;
              putError("Invalid hex checksum after file <sha1> at line "
                + formatComma.format(readFileLine) + ": " + third);
            }

            if (!cancelFlag)      // if we are still alive and kicking
            {
              fourth = readChecksumToken(input); // get next input token
              if (fourth.equals("</sha1>") == false)
              {
                cancelFlag = true;
                putError("Missing </sha1> after SHA1 checksum at line "
                  + formatComma.format(readFileLine) + ": " + fourth);
              }
            }
          }
        }
        else
        {
          cancelFlag = true;
          putError("Unexpected input during <file> group at line "
            + formatComma.format(readFileLine) + ": " + second);
        }

        /* Get the next starting tag inside a <file> group, if we haven't
        been cancelled by the user. */

        if (!cancelFlag)
          second = readChecksumToken(input); // get next input token
      }

      /* Finish the file grouping.  Check if the fields are valid. */

      if (cancelFlag)             // did user hit the panic button?
      {
        /* Then do nothing more. */
      }
      else if (result.name.length() == 0) // files must have a name
      {
        cancelFlag = true;
        putError("Missing name for <file> group at line "
          + formatComma.format(readFileLine) + ": " + second);
      }
    } // end of file grouping

    else if (first.equals("<folder>"))
    {
      /* Read one complete folder description, which may include subfolders. */

      result.list = new Vector();   // create a new list for folder contents
      result.size = 0;              // start folder with byte size of zero

      second = readChecksumToken(input); // get next input token
      while ((cancelFlag == false) && (second.equals("</folder>") == false))
      {
        if (second.equals("<file>")) // start new file grouping
        {
          /* We pass a relative path prefix to the called routine, but because
          XML tags can be received in almost any order, there is no guarantee
          that we have set this folder's name yet.  Don't trust the path prefix
          for anything except informational messages. */

          newfile = readChecksumRecurse(input, (level + 1), (pathPrefix
            + ((result.name.length() > 0) ? (result.name + systemFileSep)
            : "")), second);      // parse one file
          if (newfile != null)    // do nothing more after an errror
          {
            result.files ++;      // add this file to our file count
            result.list.add(newfile); // add File object to our list
            result.size += (newfile.size > 0) ? newfile.size : 0;
                                  // add file's size (if any) to our total
          }
        }
        else if (second.equals("<folder>")) // start new subfolder grouping
        {
          newfile = readChecksumRecurse(input, (level + 1), (pathPrefix
            + ((result.name.length() > 0) ? (result.name + systemFileSep)
            : "")), second);      // parse one subfolder recursively
          if (newfile != null)    // do nothing more after an errror
          {
            result.files += newfile.files; // accumulate subfolder's files
            result.folders += 1 + newfile.folders;
                                  // accumulate subfolder and its subfolders
            result.list.add(newfile); // add subfolder object to our list
            result.size += newfile.size; // add subfolder size to our total
          }
        }
        else if (second.equals("<name>")) // assign name to this folder
        {
          third = readChecksumToken(input); // get next input token
          if (third.equals(ENDFILE_TOKEN) || (third.charAt(0) == '<'))
          {
            cancelFlag = true;
            putError("Missing name for <folder> group at line "
              + formatComma.format(readFileLine) + ": " + third);
          }
          else
          {
            result.name = parseCharReference(third); // save folder name
            fourth = readChecksumToken(input); // get next input token
            if (fourth.equals("</name>") == false)
            {
              cancelFlag = true;
              putError("Missing </name> after folder name at line "
                + formatComma.format(readFileLine) + ": " + fourth);
            }
          }
        }
        else
        {
          cancelFlag = true;        // throw everything away on syntax error
          putError("Unexpected input during <folder> group at line "
            + formatComma.format(readFileLine) + ": " + second);
        }

        /* Get the next starting tag inside a <folder> group, if we haven't been
        cancelled by the user. */

        if (!cancelFlag)
          second = readChecksumToken(input); // get next input token
      }

      /* Finish the folder grouping.  Check if the fields are valid. */

      if (cancelFlag)             // did user hit the panic button?
      {
        /* Then do nothing more. */
      }
      else if ((level > 0) && (result.name.length() == 0)) // folder has name?
      {
        cancelFlag = true;
        putError("Missing subfolder name for <folder> group at line "
          + formatComma.format(readFileLine) + ": " + second);
      }
      else
      {
        Collections.sort(result.list); // sort contents of this folder
        if ((pathPrefix.length() + result.name.length()) > 0)
          setStatusMessage("Parsing " + pathPrefix + result.name);
      }
    } // end of folder grouping

    else
    {
      /* Unexpected input or premature end-of-file. */

      cancelFlag = true;          // throw everything away on syntax error
      putError("Unexpected input or premature end-of-file at line "
        + formatComma.format(readFileLine) + ": " + first);
    }

    /* Clean up and return to the caller. */

    if (cancelFlag)               // did user hit the panic button?
      result = null;              // yes, invalidate any/all work that we did

    return(result);               // give caller whatever we could find

  } // end of readChecksumRecurse() method


/*
  readChecksumToken() method

  Return the next complete token in the XML file as a string with unnecessary
  white space removed.  This token may be a group starting tag such as <file>
  or a group ending tag such as </file> or a data string.  We throw away
  comments and other white space.  We indicate the end of the file with a
  special token called ENDFILE_TOKEN, which must be a non-null string that can
  not occur normally.

  This method does not assign meaning to the tokens or the order in which they
  appear.  That is the caller's responsibility.
*/
  static String readChecksumToken(BufferedReader input)
    throws IOException            // comes from calling readChecksumChar()
  {
    StringBuffer buffer;          // faster than String for multiple appends
    int ch;                       // one input character as an integer or -1
    String result;                // our resulting String token for the caller

    buffer = new StringBuffer();  // allocate empty string buffer for result
    result = null;                // repeat until we find a token, because ...
    while (result == null)        // ... we may need to ignore comments
    {
      if (cancelFlag) break;      // stop if user hit the panic button

      /* Ignore leading white space. */

      ch = readChecksumChar(input); // get first character or end-of-file
      while ((ch >= 0) && Character.isWhitespace((char) ch))
        ch = readChecksumChar(input); // throw away white space and get more

      /* The first non-white character determines whether we are looking at
      data, a grouping tag, or a comment. */

      buffer.setLength(0);        // throw away any previous string contents
      if (ch < 0)                 // end of file?
      {
        result = ENDFILE_TOKEN;   // yes, tell the user that the game is over
      }
      else if (ch == '<')         // both comments and tags start with this
      {
        /* This is either the start of a grouping tag or a comment.  To suit
        our limited needs, we compress all non-white characters until we hit
        '>' or the end of the line or the end of the file. */

        buffer.append((char) ch); // put leading '<' character into result
        ch = readChecksumChar(input); // get next character or end-of-file
        while ((ch >= 0) && (ch != '\n') && (ch != '>'))
        {
          if (ch != ' ')          // remove all blank spaces and tabs
            buffer.append(Character.toLowerCase((char) ch)); // keep lowercase
          ch = readChecksumChar(input); // get another character or end-of-file
        }

        if (ch == '>')            // was there a proper ending?
          buffer.append((char) ch); // put trailing '>' character into result
        else                      // no, was end-of-file or newline
        {
          readFilePendingChar = ch; // remember the character that stopped us
          readFilePendingFlag = true; // flag pending character as valid data
        }

        if ((buffer.length() > 1) && (((ch = (int) buffer.charAt(1)) == '!')
          || (ch == '?')))        // might be comment or declaration
        {
          result = null;          // invalidate token and look for next
        }
        else
          result = buffer.toString(); // convert buffer back to normal string
      }
      else
      {
        /* This is the start of plain data.  In proper XML, it would continue
        until the group ending tag.  Being a lazy parser, we go until '<' or
        the end of the line or the end of the file.  Leading and trailing
        spaces are removed. */

        buffer.append((char) ch); // put first character into result
        ch = readChecksumChar(input); // get next character or end-of-file
        while ((ch >= 0) && (ch != '\n') && (ch != '<'))
        {
          buffer.append((char) ch); // put this character into result
          ch = readChecksumChar(input); // get another character or end-of-file
        }
        readFilePendingChar = ch; // remember the character that stopped us
        readFilePendingFlag = true; // flag pending character as valid data
        result = buffer.toString().trim(); // trim leading and trailing spaces
      }
    }

    /* Clean up and return to the caller. */

    if (cancelFlag)               // did user hit the panic button?
      result = ENDFILE_TOKEN;     // yes, invalidate any/all work that we did

    return(result);               // give caller whatever we could find

  } // end of readChecksumToken() method


/*
  saveOutputText() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void saveOutputText()
  {
    File outputFile;              // user's selected output file
    FileWriter outputStream;      // our output stream for user's file

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      outputFile = fileChooser.getSelectedFile();
                                  // get user's selected output file
      if (canWriteFile(outputFile)) // if writing this file seems safe
      {
        outputStream = new FileWriter(outputFile);
                                  // try to open output file
        outputText.write(outputStream); // couldn't be much easier for writing!
        outputStream.close();     // try to close output file
      }
    }
    catch (IOException ioe)
    {
      putError("Can't write to text file: " + ioe.getMessage());
    }
  } // end of saveOutputText() method


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
    StringBuffer buffer;          // faster than String for multiple appends
    boolean empty;                // fast flag to decide if <buffer> is empty

    /* The caller spent time formatting a status message.  However, console
    applications don't show these messages, so we just throw them away. */

    if (consoleFlag)              // are we running as a console application?
      return;                     // yes, console doesn't show running status

    /* In addition to the regular status message, there is a counter dialog in
    the bottom right-hand corner with running totals for the number of files.
    Not all counters are used by each action, so the order here is somewhat
    arbitrary.  We build up the message in a StringBuffer and later copy it to
    <countPending>, since the text is displayed in a separate non-synchronized
    timer thread, and we need to minimize the amount of time where the text is
    incomplete or inconsistent. */

    buffer = new StringBuffer();  // allocate empty string buffer for result
    empty = true;                 // keep this fast flag for deciding if empty
    if (totalDiffer > 0)          // how many files are different or in error?
    {
      if (!empty) buffer.append(INDENT_STEP); // delimiter between counters
      buffer.append(formatComma.format(totalDiffer) + " differ");
      empty = false;
    }
    if (totalFiles > 0)           // how many files have we found?
    {
      if (!empty) buffer.append(INDENT_STEP); // delimiter between counters
      buffer.append(prettyPlural(totalFiles, "file"));
      empty = false;
    }
//  if (totalFolders > 0)         // how many folders or subfolders?
//  {
//    if (!empty) buffer.append(INDENT_STEP); // delimiter between counters
//    buffer.append(prettyPlural(totalFolders, "folder"));
//    empty = false;
//  }
    if (totalSame > 0)            // how many files are identical?
    {
      if (!empty) buffer.append(INDENT_STEP); // delimiter between counters
      buffer.append(formatComma.format(totalSame) + " same");
      empty = false;
    }
    if (totalSize > 0)            // how many bytes for all files?
    {
      if (!empty) buffer.append(INDENT_STEP); // delimiter between counters
      buffer.append(prettyPlural(totalSize, "byte"));
      empty = false;
    }
    if (empty)                    // is the counter dialog still empty?
      buffer.append(EMPTY_STATUS); // yes, use special string when no message

    /* Set the status message and/or counter dialog. */

    countPending = buffer.toString(); // convert buffer back to normal string
    statusPending = text;         // always save caller's status message

    if (statusTimer.isRunning())  // are we updating on a timed basis?
      return;                     // yes, wait for timer to kick in and update

    countDialog.setText(countPending); // show the file counter now
    statusDialog.setText(statusPending); // show the status message now

  } // end of setStatusMessage() method


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
    System.err.println("Create CRC32, MD5, SHA1 checksums on standard output.  Syntax is:");
    System.err.println("  java  CompareFolders3  [options]  fileOrFolderName");
    System.err.println("  java  CompareFolders3  [options]  fileOrFolderName  >checksumFile");
    System.err.println();
    System.err.println("Compare folder contents or checksums from file.  Syntax is:");
    System.err.println("  java  CompareFolders3  [options]  firstChecksumFile  secondChecksumFile");
    System.err.println("  java  CompareFolders3  [options]  firstFolderName  secondFolderName");
    System.err.println("  java  CompareFolders3  [options]  folderName  checksumFile");
    System.err.println();
    System.err.println("Update checksum file with new folder contents.  Syntax is:");   // command line update checksum
    System.err.println("  java  CompareFolders3  [options]  checksumFile  folderName"); // command line update checksum
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -b# = buffer size from 4KB to 16MB for reading files.  Please accept the");
    System.err.println("      default sizes except in unusual situations after careful testing.");
    System.err.println("  -c0 = ignore uppercase/lowercase in file names (not recommended)");
    System.err.println("  -c1 = -c = uppercase/lowercase different in file names (default)");
    System.err.println("  -m0 = show only summary messages");
    System.err.println("  -m1 = show different files only (default)");
    System.err.println("  -m2 = show identical (equal) files only");
    System.err.println("  -m3 = show all files");
    System.err.println("  -s0 = process selected files and folders only, no subfolders");
    System.err.println("  -s1 = -s = process files, folders, and subfolders (default)");
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
  subfolders to appear in order.  All sorting keys are generated by the
  createSortKey() method so that the order is the same no matter where sorting
  is done in this program.

  Keep this code simple and fast.  Otherwise, users will see long delays when
  sorting large directories.  The same applies to the createSortKey() method.

  The caller's parameter may be <null> and this may happen if the caller asks
  File.listFiles() for the contents of a protected system directory.  All calls
  to listFiles() in this program are wrapped inside a call to us, so we replace
  a null parameter with an empty array as our result.
*/
  static File[] sortFileList(File[] input)
  {
    File entry;                   // one File object from input array
    int i;                        // index variable
    TreeMap list;                 // temporary sorted list of files, folders
    File[] result;                // our result as an array of File objects
    int size;                     // number of entries if input is not null

    if (input == null)            // were we given a null pointer?
      result = new File[0];       // yes, replace with an empty array
    else if (input.length < 2)    // don't sort lists with zero or one element
      result = input;             // just copy input array as result array
    else
    {
      /* First, create a sorted list with our choice of index keys and the File
      objects as data. */

      list = new TreeMap();       // create empty sorted list with keys
      size = input.length;        // get total number of input entries
      for (i = 0; i < size; i ++) // for each File object in input array
      {
        entry = input[i];         // get and re-use same indexed File object
        list.put(createSortKey(entry.isDirectory(), entry.getName()), entry);
                                  // put this file or folder into sorted list
      }

      /* Second, now that the TreeMap object has done all the hard work of
      sorting, pull the File objects from the list in order as determined by
      the sort keys that we created. */

      result = (File[]) list.values().toArray(new File[0]);
    }
    return(result);               // give caller whatever we could find

  } // end of sortFileList() method


/*
  updateChecksum() method

  Update a checksum file by (1) reading the file, (2) comparing against a data
  file or folder, (3) revising entries for files with a different name, size,
  or date, and (4) writing the updated checksum back into the original file.

  See also the createChecksum() method.
*/
  static int updateChecksum(
    File firstFile,               // first data file or folder
    File secondFile)              // second checksum file (only)
  {
    int answer;                   // answer received from user: yes, no, cancel
    String error;                 // error message from writing to a file
    CompareFolders3File firstChecksum; // first calculated checksums
    CompareFolders3File secondChecksum; // second calculated checksums
    String summary;               // summary of different and same files
    File userFile;                // where user wants to save the results
    boolean writeFlag;            // true while we have something to write

    /* Attempt to parse second file as recursive checksums in XML format. */

    secondChecksum = readChecksumFile(secondFile); // parse checksum file
    if (cancelFlag || (secondChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)

    /* Create a new recursive checksum using the caller's first file and the
    secondary checksum data that we just read the second file. */

    firstChecksum = createUpdateChecksum(true, "", firstFile, secondChecksum);
    if (cancelFlag || (firstChecksum == null)) // was there a problem?
      return(EXIT_UNKNOWN);       // yes, do nothing more (error printed)

    /* Save the new checksum file according to the user's wishes. */

    error = "";                   // assume that there are no file I/O errors
    setStatusMessage(EMPTY_STATUS); // force final counters to appear in status
    summary = prettyPlural(totalDiffer, "difference") + " and "
      + prettyPlural(totalSame, "identical file");
    putError("Checksum has " + summary + "."); // final summary, scroll output
    userFile = secondFile;        // start writing to second checksum file
    writeFlag = (consoleFlag == false) || (totalDiffer > 0);
                                  // command line does nothing if no changes
    while (writeFlag)             // repeat because output file may be bad
    {
      /* Tell the user how many changes were found and ask if they want to save
      the new checksum file. */

      if (consoleFlag)            // are we running from the command line?
        answer = JOptionPane.YES_OPTION; // replace file without prompting
      else
        answer = JOptionPane.showConfirmDialog(mainFrame, (error + "Found "
          + summary + ".\nReplace existing file with new checksums?"));

      if (answer == JOptionPane.CANCEL_OPTION)
      {
        putOutput("Cancel on replace file question.", true);
        break;                    // stop asking stupid questions
      }
      else if (answer == JOptionPane.YES_OPTION)
        { /* do nothing */ }
      else
      {
        /* Ask the user for an output file name. */

        fileChooser.resetChoosableFileFilters(); // remove any existing filters
        fileChooser.setDialogTitle("Save Checksums as Text File...");
        fileChooser.setFileHidingEnabled(true); // don't show hidden files
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false); // allow only one file
        if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
        {
          putOutput("Cancel on file selection dialog.", true);
          return(EXIT_UNKNOWN);   // stop asking stupid questions
        }
        userFile = fileChooser.getSelectedFile();
      }

      /* Try to write the results to the user's chosen file.  Ask again if
      something goes wrong. */

      cancelFlag = false;         // clear any error condition on write
      writeChecksumFile(userFile, firstChecksum, firstFile.getPath());
                                  // try writing to user's chosen file
      if (cancelFlag == false)    // if file was written successfully
        writeFlag = false;        // stop asking user where to save file
      else if (consoleFlag)       // are we running from the command line?
        writeFlag = false;        // yes, don't try again, don't prompt user
      else                        // if there was an error while writing file
      {
        error = "Sorry, can't write to <" + userFile.getName()
          + ">.\nTry clicking <No> and saving to a new file.\n\n";
      }
    }
    return((int) totalDiffer);    // return the number of changed files/folders

  } // end of updateChecksum() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main CompareFolders3 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == actionDialog)   // user's choice for what action to perform
    {
      doActionSelection();        // call someone else to do the real work
    }
    else if (source == cancelButton) // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if (source == caseCheckbox) // uppercase/lowercase in file names
    {
      caseFlag = caseCheckbox.isSelected();
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == firstFileButton) // browse for first file or folder
    {
      doFirstFileButton();        // find name of first file or folder
//    secondFileDialog.requestFocusInWindow(); // shift focus to second name
    }
    else if (source == firstFileDialog) // user pressed Enter in text area
    {
      secondFileDialog.requestFocusInWindow(); // shift focus to second name
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
    else if (source == recurseCheckbox) // recursion for folders, subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == saveButton) // "Save Output" button
    {
      saveOutputText();           // write output text area to a file
    }
    else if (source == scrollCheckbox) // scroll calls to <putOutput>
    {
      scrollFlag = scrollCheckbox.isSelected();
    }
    else if (source == secondFileButton) // browse for second file or folder
    {
      doSecondFileButton();       // find name of second file or folder
//    startButton.requestFocusInWindow(); // shift focus to Start button
    }
    else if (source == secondFileDialog) // user pressed Enter in text area
    {
      startButton.requestFocusInWindow(); // shift focus to Start button
    }
    else if (source == showFileDialog) // select which files to display
    {
      switch(showFileDialog.getSelectedIndex())
      {
        case (0): showDiffFlag = false; showSameFlag = false; break;
        case (1): showDiffFlag = true; showSameFlag = false; break;
        case (2): showDiffFlag = false; showSameFlag = true; break;
        case (3): showDiffFlag = true; showSameFlag = true; break;
        default:
          putError("Invalid file selection index <"
            + showFileDialog.getSelectedIndex() + "> in userButton for <"
            + showFileDialog.getSelectedItem() + ">.");
          break;
      }
    }
    else if (source == startButton) // "Start" button
    {
      doStartButton();            // start opening files or folders
    }
    else if (source == statusTimer) // update timer for status message text
    {
      if (countPending.equals(countDialog.getText()) == false)
        countDialog.setText(countPending); // new counter, update the display
      if (statusPending.equals(statusDialog.getText()) == false)
        statusDialog.setText(statusPending); // new status, update the display
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method


/*
  writeChecksumFile() method

  Given a possibly recursive file-or-folder object, write indented XML output
  with the details.  We supply the initial XML declaration and a comment with
  the current date and time.
*/
  static void writeChecksumFile(
    File givenFile,               // file to write, or <null> for stdout
    CompareFolders3File fileinfo, // one or more files or folders
    String source)                // original data source, or empty string
  {
    BufferedWriter output;        // where we write our output, or <null>

    if (cancelFlag) return;       // stop if user hit the panic button
    try
    {
      if (givenFile == null)      // are we to write on a specific file?
        output = null;            // no, write to standard output
      else                        // yes, open file as chosen by user
        output = new BufferedWriter(new FileWriter(givenFile));

      writeCheckSumLine(output, "<?xml version=\"1.0\"?>");
      writeCheckSumLine(output, "<!-- checksums created "
        + formatDate.format(new Date()) + " -->");
      writeCheckSumLine(output,
        "<!-- by the CompareFolders3 Java application -->");
      writeCheckSumLine(output,
        "<!-- http://www.psc-consulting.ca/fenske/ -->");
      if (source.length() > 0)    // was there an original source given?
        writeCheckSumLine(output, "<!-- original source: " + source + " -->");
      writeChecksumRecurse(output, "", fileinfo);
      if (fileinfo.list != null)  // include summary comment for folders only
        writeCheckSumLine(output, "<!-- "
          + prettyPlural(fileinfo.files, "file") + " and "
          + prettyPlural(fileinfo.folders, "subfolder") + " with "
          + prettyPlural(fileinfo.size, "byte") + " -->");

      if (output != null)         // should we close the output file?
        output.close();           // yes, try to close the file
    }
    catch (IOException except)
    {
      putError("Can't write to file: " + except.getMessage());
      cancelFlag = true;          // try to abort any processing
    }
  } // end of writeChecksumFile() method


/*
  writeCheckSumLine() method

  Write exactly one line of text to a file, the console's standard output, or
  the output text area in the graphical interface.  We supply the newline
  character, not the caller.
*/
  static void writeCheckSumLine(
    BufferedWriter output,        // where we write our output, or <null>
    String text)                  // one line of text to be written
    throws IOException            // comes from calling write()
  {
    if (cancelFlag) return;       // stop if user hit the panic button
    if (output != null)           // is there an output file?
    {
      output.write(text);         // yes, write to file with newline
      output.newLine();
    }
    else if (consoleFlag)         // no output file, running as console app?
      System.out.println(text);   // yes, use console's standard output
    else
      putOutput(text);            // put into GUI's output text area

  } // end of writeCheckSumLine() method


/*
  writeChecksumRecurse() method

  Recursively write XML information about files and folders, adjusting the left
  indent as we go deeper.
*/
  static void writeChecksumRecurse(
    BufferedWriter output,        // where we write our output, or <null>
    String indent,                // current blank space at start of line
    CompareFolders3File fileinfo) // one or more files or folders
    throws IOException            // comes from calling writeCheckSumLine()
  {
    int i;                        // index variable
    int length;                   // total number of elements in list
    String nextindent;            // avoid repeating <indent + INDENT_STEP>

    if (cancelFlag) return;       // stop if user hit the panic button
    nextindent = indent + INDENT_STEP; // do this once, not each time we use it
    if (fileinfo == null)
    {
      /* Ignore null objects: just return to caller. */
    }
    else if (fileinfo.list == null) // is this a file?
    {
      writeCheckSumLine(output, (indent + "<file>")); // start XML grouping
      writeCheckSumLine(output, (nextindent + "<name>"
        + makeCharReference(fileinfo.name) + "</name>"));
      if (fileinfo.date.length() > 0) // optional file date and time
        writeCheckSumLine(output, (nextindent + "<date>" + fileinfo.date
          + "</date>"));
      if (fileinfo.size >= 0)         // optional file size in bytes
        writeCheckSumLine(output, (nextindent + "<size>" + fileinfo.size
          + "</size>"));
      if (fileinfo.crc32.length() > 0) // optional CRC32 checksum
        writeCheckSumLine(output, (nextindent + "<crc32>" + fileinfo.crc32
          + "</crc32>"));
      if (fileinfo.md5.length() > 0) // optional MD5 checksum
        writeCheckSumLine(output, (nextindent + "<md5>" + fileinfo.md5
          + "</md5>"));
      if (fileinfo.sha1.length() > 0) // optional SHA1 checksum
        writeCheckSumLine(output, (nextindent + "<sha1>" + fileinfo.sha1
          + "</sha1>"));
      writeCheckSumLine(output, (indent + "</file>")); // end XML grouping
    }
    else                          // must be a folder
    {
      writeCheckSumLine(output, (indent + "<folder>")); // start XML grouping
      if (fileinfo.name.length() > 0) // was there a folder name?
      {
        writeCheckSumLine(output, (nextindent + "<name>"
          + makeCharReference(fileinfo.name) + "</name>"));
      }
      length = fileinfo.list.size(); // get number of elements in this list
      for (i = 0; i < length; i ++) // print each element in list
      {
        if (cancelFlag) break;    // stop if user hit the panic button
        writeChecksumRecurse(output, nextindent, (CompareFolders3File)
          fileinfo.list.get(i));
      }
      writeCheckSumLine(output, (indent + "</folder>")); // end XML grouping
    }
  } // end of writeChecksumRecurse() method

} // end of CompareFolders3 class

// ------------------------------------------------------------------------- //

/*
  CompareFolders3File class

  A data structure to hold information about one file or folder.  For a file,
  the <list> variable should be <null>.  For a folder, <list> should be a valid
  Vector object, with one element per file or subfolder.  An empty folder would
  thus be a Vector object with zero elements, as distinguished from a file that
  has the <null> object.
*/

class CompareFolders3File implements Comparable
{
  /* class variables */

  String name;                    // relative file name from initial folder
  String date;                    // file date and time (unknown format)
  long size;                      // size of file or folder in bytes, or -1
  String sortkey;                 // unique sorting key for file or folder

  int files;                      // total files in this folder and subfolders
  int folders;                    // total subfolders in this folder/subfolders
  Vector list;                    // for folders: list of files or subfolders
  File object;                    // original Java File object, if necessary

  String crc32;                   // CRC32 checksum
  String md5;                     // MD5 checksum
  String sha1;                    // SHA1 checksum

  /* constructor (no arguments) */

  public CompareFolders3File()
  {
    this.name = "";               // default to empty file name
    this.date = "";               // default to empty file date and time
    this.size = -1;               // mark file size as unknown, less than empty
    this.sortkey = null;          // mark sorting key as missing, not empty

    this.files = 0;               // default to no files in folder + subfolders
    this.folders = 0;             // default to no folders in folder/subfolders
    this.list = null;             // default to empty folder contents
    this.object = null;           // only used when finding duplicate files

    this.crc32 = "";              // default to empty CRC32 checksum
    this.md5 = "";                // default to empty MD5 checksum
    this.sha1 = "";               // default to empty SHA1 checksum
  }

  /* Make all objects of this type comparable, so that we can sort a list of
  files and subfolders contained in a folder.  All sorting keys are generated
  by the createSortKey() method so that the order is the same no matter where
  sorting is done in this program.  Note that this comparison only makes sense
  for two files or subfolders contained in the same folder. */

  public int compareTo(Object otherObject)
  {
    CompareFolders3File other = (CompareFolders3File) otherObject;

    if (other.sortkey == null)    // create sorting keys if necessary
      other.sortkey = CompareFolders3.createSortKey((other.list != null),
        other.name);
    if (this.sortkey == null)
      this.sortkey = CompareFolders3.createSortKey((this.list != null),
        this.name);
    return(this.sortkey.compareTo(other.sortkey));
  }

} // end of CompareFolders3File class

// ------------------------------------------------------------------------- //

/*
  CompareFolders3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class CompareFolders3User implements ActionListener, Runnable
{
  /* empty constructor */

  public CompareFolders3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    CompareFolders3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run() { CompareFolders3.doStartRunner(); }

} // end of CompareFolders3User class

/* Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License. */
