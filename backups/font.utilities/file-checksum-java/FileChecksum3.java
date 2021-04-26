/*
  File Checksum #3 - Compute CRC32, MD5, SHA1 File Checksums
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Thursday, 30 October 2008
  Java class name: FileChecksum3
  Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 application to compute common checksums for files: CRC32,
  MD5, and SHA1.  Checksums are small hexadecimal "signatures" for testing
  whether or not files have been copied correctly, such as over a network.  One
  person sends a file along with the checksum computed on the original
  computer.  A second person calculates a similar checksum for the received
  file, and if the two checksums agree, then the received file is assumed to be
  correct.  MD5 is more reliable than and preferred over the older and simpler
  CRC32.  Many web sites provide MD5 signatures for their downloads; use this
  program to verify files that you download.

  See the CompareFolders application for comparing two folders to determine if
  all files and subfolders are identical.  See the FindDupFiles application to
  look for duplicate files based on MD5 checksums.

  GNU General Public License (GPL)
  --------------------------------
  FileChecksum3 is free software: you can redistribute it and/or modify it
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
  The Java command line may contain parameters for a file name and optional
  checksums.  If no parameters are given on the command line, then this program
  runs as a graphical or "GUI" application with the usual dialog boxes and
  windows.  See the "-?" option for a help summary:

      java  FileChecksum3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If parameters are given on the command line, then
  this program runs as a console application without a graphical interface.
  The first parameter must be a file name.  Checksums are calculated for that
  file.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  FileChecksum3  README.TXT  >output.txt

  This will calculate checksums for a file named "README.TXT".  Standard output
  will be written to a file named "output.txt".  The second and following
  parameters, if given, must be hexadecimal checksums.  For example:

      java  FileChecksum3  README.TXT  d36952838c47c701745293e1a16333f3

  Second and following parameters are compared against the generated checksums
  (CRC32, MD5, SHA1).  If each parameter matches a checksum, then the result is
  considered successful.  The console application will return an exit status of
  1 for success, -1 for failure, and 0 for unknown.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.security.*;           // MD5 and SHA1 message digests (checksums)
import java.text.*;               // number formatting
import java.util.regex.*;         // regular expressions
import java.util.zip.*;           // CRC32 checksums
import javax.swing.*;             // newer Java GUI support

public class FileChecksum3
{
  /* constants */

  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String HELLO_TEXT =
    "Open a file to compute checksums, or compare against known checksum.";
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Compute CRC32, MD5, SHA1 File Checksums - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 400; // 0.400 seconds between timed updates
  static final String WAIT_TEXT =
    "Calculating checksums.  Please wait or click the Cancel button.";

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JLabel compareLabel;     // dialog label for comparison text from user
  static JTextField compareText;  // text field for comparison text from user
  static boolean consoleFlag;     // true if running as a console application
  static JButton copyCrc32Button; // "Copy CRC32" checksum button
  static JButton copyMd5Button;   // "Copy MD5" checksum button
  static JButton copySha1Button;  // "Copy SHA1" checksum button
  static JLabel crc32Label;       // dialog label for CRC32 checksum
  static String crc32String;      // calculated CRC32 checksum
  static JTextField crc32Text;    // graphical text box for <crc32String>
  static JButton exitButton;      // "Exit" button
  static JFileChooser fileChooser; // asks for input and output file names
  static JButton filenameButton;  // button for input file name
  static JTextField filenameText; // text field for input file name
  static JLabel filesizeLabel;    // dialog label for file size in bytes
  static String filesizeString;   // formatted size of file in bytes
  static JTextField filesizeText; // graphical text box for <filesizeString>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static NumberFormat formatPointOne; // formats with one decimal digit
  static JLabel legalNotice;      // boring legal notice about copyright, etc
  static JFrame mainFrame;        // this application's window
  static JLabel md5Label;         // dialog label for MD5 checksum
  static String md5String;        // calculated MD5 checksum
  static JTextField md5Text;      // graphical text box for <md5String>
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static File openFileObject;     // file to be opened in a separate thread
  static Thread openFileThread;   // separate thread for openFile() method
  static JButton pasteCompareButton; // button for pasting comparison text
  static JProgressBar progressBar; // graphical display needed for big files
  static javax.swing.Timer progressTimer; // timer for updating progress text
  static JLabel sha1Label;        // dialog label for SHA1 checksum
  static String sha1String;       // calculated SHA1 checksum
  static JTextField sha1Text;     // graphical text box for <sha1String>
  static long sizeDone;           // how much of <sizeTotal> has been finished
  static String sizeSuffix;       // pre-formatted portion of size message
  static long sizeTotal;          // total number of bytes in current file
  static java.applet.AudioClip soundsBad; // sound if checksums do not agree
  static JButton startButton;     // "Start" button
  static JLabel statusText;       // text area for displaying status messages

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Color buttonColor, labelColor, statusColor, textColor; // GUI colors
    Font buttonFont, labelFont, statusFont, textFont; // GUI font elements
    int exitStatus;               // exit status for console application
    int gapSize;                  // basis for pixel gap between GUI elements
    GridBagConstraints gbc;       // reuse the same constraint object
    File givenFile;               // calculate checksums for this file object
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    Insets textMargins;           // margins for input and output text areas
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonColor = labelColor = statusColor = textColor = null;
                                  // by default, no custom colors or fonts
    buttonFont = labelFont = statusFont = textFont = null;
    cancelFlag = false;           // don't cancel unless user complains
    consoleFlag = false;          // assume no parameters on command line
    crc32String = "";             // set CRC32 checksum to empty string
    exitStatus = EXIT_SUCCESS;    // assume success for console application
    filesizeString = "";          // set formatted file size to empty string
    maximizeFlag = false;         // by default, don't maximize our main window
    md5String = "";               // set MD5 checksum to empty string
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    sha1String = "";              // set SHA1 checksum to empty string
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
    as an option is assumed to be a file name or checksum. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore null parameters, which are more common that you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
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
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonColor = Color.BLACK; // buttons and text should be black
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        labelColor = new Color(102, 102, 102); // reduce labels to medium gray
        labelFont = buttonFont;   // no need for anything different
        statusColor = new Color(51, 51, 51); // reduce status to charcoal gray
        statusFont = new Font(SYSTEM_FONT, Font.BOLD, size); // status message
        textColor = buttonColor;  // no need for anything different
        textFont = buttonFont;    // no need for anything different
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
        name or checksum.  We ignore <cancelFlag> because the user has no way
        of interrupting us at this point (no graphical interface). */

        if (!consoleFlag)         // first non-option parameter is a file name
        {
          consoleFlag = true;     // don't allow GUI methods to be called
          givenFile = new File(args[i]); // convert name to Java File object
          if (givenFile.isFile() == false) // if parameter is not a real file
          {
            System.err.println("File not found: " + args[i]);
            showHelp();           // show help summary
            System.exit(EXIT_FAILURE); // exit application after printing help
          }
          System.out.println("     file name: " + givenFile.getName());
          System.out.println("    file bytes: "
            + formatComma.format(givenFile.length()));
                                  // show file name, size before start checksum
          calcFileChecksum(givenFile); // calculate checksums (may be slow)
          if (cancelFlag)         // did something go wrong?
            System.exit(EXIT_FAILURE); // exit from application with status
          System.out.println("CRC32 checksum: " + crc32String);
          System.out.println("  MD5 checksum: " + md5String);
          System.out.println(" SHA1 checksum: " + sha1String);
        }
        else                      // second and later non-options are checksums
        {
          if (compareChecksum(cleanChecksum(args[i])) != EXIT_SUCCESS)
            exitStatus = EXIT_FAILURE; // one failure means application fails
        }
      }
    }

    /* If running as a console application, exit to the system with an integer
    status for success or failure. */

    if (consoleFlag)              // was a file name given?
      System.exit(exitStatus);    // exit from application with status

    /* There was no file name on the command line.  Open the graphical user
    interface (GUI).  We don't need to be inside an if-then-else construct here
    because the console application called System.exit() above.  The standard
    Java interface style is the most reliable, but you can switch to something
    closer to the local system, if you want. */

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

    action = new FileChecksum3User(); // create our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser
    progressTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update progress text on clock ticks only
    textMargins = new Insets(3, 5, 3, 5); // top, left, bottom, right

    try { soundsBad = java.applet.Applet.newAudioClip(new java.net.URL(
      "file:FileChecksum3.au")); } // play sound if checksums do not agree
    catch (java.net.MalformedURLException mue) { soundsBad = null; }

    /* We allow a tremendous range for the GUI font size, so it only makes
    sense to adjust spacing of the layout to match.  This is necessary for
    vertical spacing; constant horizontal spacing is acceptable for tested
    font sizes from 10 to 30 points.  This effectively uses a font size in
    points as a display size in pixels.  To be more accurate, we could call
    getFontMetrics() after a JButton or other Component is defined. */

    if (buttonFont != null)       // if defined, use the button font size
      gapSize = buttonFont.getSize(); // ... to set the basic pixel gap
    else                          // otherwise, default Java look-and-feel
      gapSize = 12;               // ... has this approximate font size

    /* Put everything into one "grid bag" layout.  Most of this code is just
    plain ugly.  There isn't much chance of understanding it unless you read
    the documentation for GridBagLayout ... if you can understand that! */

    JPanel panel1 = new JPanel(new GridBagLayout()); // create grid bag layout
    gbc = new GridBagConstraints(); // modify and reuse these constraints

    /* First layout line has status with informational messages. */

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    statusText = new JLabel(HELLO_TEXT, JLabel.CENTER);
    if (statusFont != null) statusText.setFont(statusFont);
    if (statusColor != null) statusText.setForeground(statusColor);
    panel1.add(statusText, gbc);
    panel1.add(Box.createVerticalStrut((int) (1.5 * gapSize)), gbc);

    /* Second line has the file name with two types of "open" buttons. */

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    filenameButton = new JButton("File Name...");
    filenameButton.addActionListener(action);
    filenameButton.setEnabled(true);
    if (buttonFont != null) filenameButton.setFont(buttonFont);
    if (buttonColor != null) filenameButton.setForeground(buttonColor);
    filenameButton.setMnemonic(KeyEvent.VK_F);
    filenameButton.setToolTipText("Find a file for checksumming.");
    panel1.add(filenameButton, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    filenameText = new JTextField("", 20);
    filenameText.addActionListener(action); // listen if Enter key pushed
    filenameText.setEditable(true); // enter file name, or click Open button
    filenameText.setEnabled(true);
    if (textFont != null) filenameText.setFont(textFont);
    if (textColor != null) filenameText.setForeground(textColor);
    filenameText.setMargin(textMargins);
    panel1.add(filenameText, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    startButton = new JButton("Start");
    startButton.addActionListener(action);
    startButton.setEnabled(true);
    if (buttonFont != null) startButton.setFont(buttonFont);
    if (buttonColor != null) startButton.setForeground(buttonColor);
    startButton.setMnemonic(KeyEvent.VK_S);
    startButton.setToolTipText("Open named file for checksumming.");
    panel1.add(startButton, gbc);
    panel1.add(Box.createVerticalStrut((int) (2.0 * gapSize)), gbc);

    /* Third line has the file size, the CRC32 checksum, and the CRC's "Copy"
    button. */

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    filesizeLabel = new JLabel("File size (bytes):");
    if (labelFont != null) filesizeLabel.setFont(labelFont);
    if (labelColor != null) filesizeLabel.setForeground(labelColor);
    panel1.add(filesizeLabel, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.5 * gapSize)), gbc);

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    filesizeText = new JTextField("", 11);
    filesizeText.setEditable(false); // user can't change this field
    if (textFont != null) filesizeText.setFont(textFont);
    if (textColor != null) filesizeText.setForeground(textColor);
    filesizeText.setMargin(textMargins);
    filesizeText.setText(filesizeString);
    panel2.add(filesizeText);
    panel2.add(Box.createHorizontalStrut((int) (2.0 * gapSize)));

    crc32Label = new JLabel("CRC32:");
    if (labelFont != null) crc32Label.setFont(labelFont);
    if (labelColor != null) crc32Label.setForeground(labelColor);
    panel2.add(crc32Label);
    panel2.add(Box.createHorizontalStrut((int) (0.5 * gapSize)));

    crc32Text = new JTextField("", 7);
    crc32Text.setEditable(false); // user can't change this field
    if (textFont != null) crc32Text.setFont(textFont);
    if (textColor != null) crc32Text.setForeground(textColor);
    crc32Text.setMargin(textMargins);
    crc32Text.setText(crc32String);
    panel2.add(crc32Text);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    panel1.add(panel2, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copyCrc32Button = new JButton("Copy CRC32");
    copyCrc32Button.addActionListener(action);
    if (buttonFont != null) copyCrc32Button.setFont(buttonFont);
    if (buttonColor != null) copyCrc32Button.setForeground(buttonColor);
    copyCrc32Button.setMnemonic(KeyEvent.VK_R);
    copyCrc32Button.setToolTipText("Copy CRC32 checksum to clipboard.");
    panel1.add(copyCrc32Button, gbc);
    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    /* Fourth line has the MD5 checksum and its "Copy" button. */

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    md5Label = new JLabel("MD5 checksum:");
    if (labelFont != null) md5Label.setFont(labelFont);
    if (labelColor != null) md5Label.setForeground(labelColor);
    panel1.add(md5Label, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    md5Text = new JTextField("", 24);
    md5Text.setEditable(false);   // user can't change this field
    if (textFont != null) md5Text.setFont(textFont);
    if (textColor != null) md5Text.setForeground(textColor);
    md5Text.setMargin(textMargins);
    md5Text.setText(md5String);
    panel1.add(md5Text, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copyMd5Button = new JButton("Copy MD5");
    copyMd5Button.addActionListener(action);
    if (buttonFont != null) copyMd5Button.setFont(buttonFont);
    if (buttonColor != null) copyMd5Button.setForeground(buttonColor);
    copyMd5Button.setMnemonic(KeyEvent.VK_M);
    copyMd5Button.setToolTipText("Copy MD5 checksum to clipboard.");
    panel1.add(copyMd5Button, gbc);
    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    /* Fifth line has the SHA1 checksum and its "Copy" button. */

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    sha1Label = new JLabel("SHA1 checksum:");
    if (labelFont != null) sha1Label.setFont(labelFont);
    if (labelColor != null) sha1Label.setForeground(labelColor);
    panel1.add(sha1Label, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    sha1Text = new JTextField("", 29);
    sha1Text.setEditable(false);  // user can't change this field
    if (textFont != null) sha1Text.setFont(textFont);
    if (textColor != null) sha1Text.setForeground(textColor);
    sha1Text.setMargin(textMargins);
    sha1Text.setText(sha1String);
    panel1.add(sha1Text, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copySha1Button = new JButton("Copy SHA1");
    copySha1Button.addActionListener(action);
    if (buttonFont != null) copySha1Button.setFont(buttonFont);
    if (buttonColor != null) copySha1Button.setForeground(buttonColor);
    copySha1Button.setMnemonic(KeyEvent.VK_H);
    copySha1Button.setToolTipText("Copy SHA1 checksum to clipboard.");
    panel1.add(copySha1Button, gbc);
    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    /* Sixth line has a comparison field where the user can enter a checksum to
    compare against our calculated checksums. */

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    compareLabel = new JLabel("Compare against:");
    if (labelFont != null) compareLabel.setFont(labelFont);
    if (labelColor != null) compareLabel.setForeground(labelColor);
    panel1.add(compareLabel, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    compareText = new JTextField("", 20);
    compareText.addActionListener(action); // listen if Enter key pushed
    compareText.setEditable(true); // user can put anything he/she wants here
    if (textFont != null) compareText.setFont(textFont);
    if (textColor != null) compareText.setForeground(textColor);
    compareText.setMargin(textMargins);
    panel1.add(compareText, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    pasteCompareButton = new JButton("Paste");
    pasteCompareButton.addActionListener(action);
    if (buttonFont != null) pasteCompareButton.setFont(buttonFont);
    if (buttonColor != null) pasteCompareButton.setForeground(buttonColor);
    pasteCompareButton.setMnemonic(KeyEvent.VK_P);
    pasteCompareButton.setToolTipText("Paste checksum for comparison.");
    panel1.add(pasteCompareButton, gbc);
    panel1.add(Box.createVerticalStrut((int) (2.0 * gapSize)), gbc);

    /* Seventh line has the "Cancel" button, a progress bar, and the standard
    "Exit" button. */

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    if (buttonColor != null) cancelButton.setForeground(buttonColor);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop checking/opening files.");
    panel1.add(cancelButton, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    progressBar = new JProgressBar(0, 100);
    if (textFont != null) progressBar.setFont(textFont);
    progressBar.setString("");
    progressBar.setStringPainted(true);
    progressBar.setValue(0);
    panel1.add(progressBar, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    if (buttonColor != null) exitButton.setForeground(buttonColor);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel1.add(exitButton, gbc);
    panel1.add(Box.createVerticalStrut((int) (1.5 * gapSize)), gbc);

    /* Last line is our copyright notice in subdued gray text. */

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    legalNotice = new JLabel(COPYRIGHT_NOTICE, JLabel.CENTER);
    if (labelFont != null) legalNotice.setFont(labelFont);
    if (labelColor != null) legalNotice.setForeground(labelColor);
    panel1.add(legalNotice, gbc);

    /* The layout in a grid bag goes strange if there isn't enough space.  Box
    the grid bag inside a flow layout to center it horizontally and stop
    expansion, then inside a plain box to center it vertically. */

    JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
    panel3.add(panel1);           // put grid bag in a fancy horizontal box

    Box panel4 = Box.createVerticalBox(); // create a basic vertical box
    panel4.add(Box.createGlue()); // stretch to the top
    panel4.add(Box.createVerticalStrut(30)); // top margin
    panel4.add(panel3);           // put boxed grid bag in center
    panel4.add(Box.createVerticalStrut(30)); // bottom margin
//  panel4.add(Box.createGlue()); // stretch to bottom (assumed by layout)

    /* Create the main window frame for this application. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel5 = mainFrame.getContentPane(); // where content meets frame
    panel5.setLayout(new BorderLayout(0, 0));
    panel5.add(panel4, BorderLayout.CENTER); // just the boxed grid bag layout

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
  calcFileChecksum() method

  Calculate the CRC32, MD5, and SHA1 checksums for a given file.  We watch the
  <cancelFlag> while we are running, and if that flag is true, then we close
  the file and set the checksums to empty strings.

  This method should only be called from a console application or from inside a
  separate thread started by the openFile() method.

  We always generate all three checksums.  The basic file I/O loop takes about
  35% of the time.  The CRC32 calculation takes about 5% of the time.  The MD5
  takes about 20% and the SHA1 takes about 40%.
*/
  static void calcFileChecksum(File givenFile)
  {
    byte[] buffer;                // input buffer for reading file
    CRC32 crc32digest;            // object for calculating CRC32 checksum
    int i;                        // index variable
    FileInputStream input;        // input file stream
    MessageDigest md5digest;      // object for calculating MD5 checksum
    String rawtext;               // text string in middle of hex conversion
    MessageDigest sha1digest;     // object for calculating SHA1 checksum

    /* Clear global checksum strings, and displayed text if GUI application. */

    clearChecksums();             // clear all checksum fields, file size, etc
    sizeDone = 0;                 // how much of <sizeTotal> has been finished
    sizeSuffix = null;            // no pre-formatted text for progress bar
    sizeTotal = givenFile.length(); // get total size of user's file in bytes
    filesizeString = formatComma.format(sizeTotal); // format and save size
    if (!consoleFlag)             // displayed text, status timer only if GUI
    {
      filesizeText.setText(filesizeString); // show size before start checksums
      progressTimer.start();      // start updating the progress bar and text
    }

    /* Try to open the user's file, since this may generate an error and make
    all other statements meaningless. */

    try
    {
      input = new FileInputStream(givenFile);
                                  // open user's file for reading bytes

      /* We should now be able to proceed without errors. */

      buffer = new byte[BUFFER_SIZE]; // allocate big/faster input buffer
      crc32digest = new CRC32();  // allocate new object for CRC32 checksum
      md5digest = MessageDigest.getInstance("MD5");
                                  // initialize MD5 message digest
      sha1digest = MessageDigest.getInstance("SHA-1");
                                  // initialize SHA1 message digest

      while ((i = input.read(buffer, 0, BUFFER_SIZE)) > 0)
      {
        if (cancelFlag) break;    // stop if user hit the panic button

        /* Update the checksum calculations. */

        crc32digest.update(buffer, 0, i); // CRC32 checksum
        md5digest.update(buffer, 0, i); // MD5 checksum
        sha1digest.update(buffer, 0, i); // SHA1 checksum
        sizeDone += i;            // add to number of bytes finished
      }
      input.close();              // close input file

      /* If we weren't cancelled by the user, then convert the final checksums
      into hexadecimal strings. */

      if (!cancelFlag)            // don't do more work if cancelled by user
      {
        /* Convert the CRC32 checksum to a hexadecimal string.  We must pad
        with leading zeros since the toHexString() method doesn't do this. */

        rawtext = "00000000" + Long.toHexString(crc32digest.getValue());
        crc32String = rawtext.substring(rawtext.length() - 8);

        /* Convert the MD5 checksum to a hexadecimal string.  We call another
        method to convert raw bytes to hex, because SHA1 needs the same. */

        md5String = formatHexBytes(md5digest.digest());

        /* Convert the SHA1 checksum to a hexadecimal string. */

        sha1String = formatHexBytes(sha1digest.digest());

        /* Force the progress bar to one hundred percent. */

        if (!consoleFlag)
        {
          progressTimer.stop();   // stop updating the progress text by timer
          progressBar.setString("100 %"); // final text label
          progressBar.setValue(100); // final position
        }
      }
    }
    catch (IOException except)
    {
      if (consoleFlag)
        System.err.println("Can't read from file: " + except.getMessage());
      else
        statusText.setText("Unable to open or read from selected file.");
      cancelFlag = true;          // tell caller that we cancelled
    }
    catch (NoSuchAlgorithmException except)
    {
      if (consoleFlag)
        System.err.println("Bad checksum algorithm: " + except.getMessage());
      else
        statusText.setText("Internal error: unsupported checksum algorithm.");
      cancelFlag = true;          // tell caller that we cancelled
    }

    /* If running as a graphical application, copy the checksum strings into
    text boxes visible to the user.  Some may be empty if there was a problem
    above (cancelled or error). */

    if (!consoleFlag)
    {
      crc32Text.setText(crc32String);
      md5Text.setText(md5String);
      progressTimer.stop();       // stop updating the progress text by timer
      sha1Text.setText(sha1String);
    }
  } // end of calcFileChecksum() method


/*
  cleanChecksum() method

  Do some mild cleaning up for a string that is supposed to be a hexadecimal
  checksum: remove spaces, some punctuation, and convert proper hex digits to
  lowercase.  Anything else is left untouched.  Bad input is left in the string
  so that later comparisons with valid checksums will fail.
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
      if ((ch == ' ') || (ch == ',') || (ch == '.') || (ch == ':'))
        { /* do nothing: ignore selected punctuation */ }
      else if ((ch >= '0') && (ch <= '9'))
        buffer.append(ch);        // accept decimal digit and append to result
      else if ((ch >= 'a') && (ch <= 'f'))
        buffer.append(ch);        // accept lowercase hexadecimal digit
      else if ((ch >= 'A') && (ch <= 'F'))
        buffer.append((char) (ch - 'A' + 'a')); // but convert uppercase hex
      else
        buffer.append(ch);        // don't change so that comparison will fail
    }
    return(buffer.toString());    // give caller our converted string

  } // end of cleanChecksum() method


/*
  clearChecksums() method

  Clear all checksum text fields to empty strings, and clear the progress bar.
*/
  static void clearChecksums()
  {
    crc32String = "";             // set CRC32 checksum to empty string
    filesizeString = "";          // set formatted file size to empty string
    md5String = "";               // set MD5 checksum to empty string
    sha1String = "";              // set SHA1 checksum to empty string

    if (!consoleFlag)             // if running as graphical application
    {
      crc32Text.setText(crc32String); // copy strings to visible text boxes
      filesizeText.setText(filesizeString);
      md5Text.setText(md5String);
      progressBar.setString("");  // remove any text label from progress bar
      progressBar.setValue(0);    // reset progress bar to beginning (empty)
      sha1Text.setText(sha1String);
    }
  } // end of clearChecksums() method


/*
  compareChecksum() method

  Check if a given string matches any of the computed checksums, and print or
  set the status message to reflect the result.  We don't change or clean up
  the caller's string in any way.  We do return an integer status, if the
  caller wants to check.
*/
  static int compareChecksum(String given)
  {
    int status;                   // the status that we return

    status = EXIT_UNKNOWN;        // assume that string does not match

    if ((given.length() > 0) && (crc32String.length() > 0)) // anything to do?
    {
      /* It shouldn't be possible for one string to match more than one of the
      CRC32, MD5, and SHA1 checksums because they have different lengths. */

      if (given.equals(crc32String)) // match for CRC32 checksum?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the CRC32 checksum.");
        else
          statusText.setText("Successfully matched the CRC32 checksum.");
      }
      else if (given.equals(md5String)) // match for MD5 checksum?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the MD5 checksum.");
        else
          statusText.setText("Successfully matched the MD5 checksum.");
      }
      else if (given.equals(sha1String)) // match for SHA1 checksum?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the SHA1 checksum.");
        else
          statusText.setText("Successfully matched the SHA1 checksum.");
      }
      else if ((!consoleFlag) && (startButton.isEnabled() == false))
      {
        statusText.setText(WAIT_TEXT); // tell impatient user to wait
//      status = EXIT_UNKNOWN;    // repeat default status of "know nothing"
      }
      else
      {
        /* The comparison failed, and not because we are otherwise busy
        calculating a new checksum. */

        status = EXIT_FAILURE;    // doesn't match, isn't pending, etc
        if (consoleFlag)
          System.out.println("Supplied checksum <" + given
            + "> does not match calculated checksums.");
        else
        {
          statusText.setText(
            "Supplied checksum does not match calculated checksums.");
          if (soundsBad != null)  // sound file may not have loaded properly
            soundsBad.play();     // play sound if checksums do not agree
        }
      }
    }
    else if (!consoleFlag)        // nothing given, but running graphical?
    {
      if (startButton.isEnabled())
        statusText.setText(HELLO_TEXT); // return to welcome status message
      else
        statusText.setText(WAIT_TEXT); // tell impatient user to wait
    }
    return(status);               // return the indicated status to caller

  } // end of compareChecksum() method


/*
  doCancelButton() method

  This method is called while we are opening files if the user wants to end the
  processing early, perhaps because it is taking too long.  We must cleanly
  terminate any secondary threads.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    statusText.setText("Checksum calculation cancelled by user.");
  }


/*
  doFilenameButton() method

  Open a dialog box to browse for a file.  Then open that file and calculate
  the checksums.
*/
  static void doFilenameButton()
  {
    File givenFile;               // user's selected file

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Browse or Open File...");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
    {
      givenFile = fileChooser.getSelectedFile(); // get selected file
      filenameText.setText(givenFile.getPath()); // save file name in text box
      openFile(givenFile);        // and open that file for checksumming
    }
  } // end of doFilenameButton() method


/*
  doFilenameEnter() method

  The user typed something into the file name text area.  Inspect the contents,
  and if it looks like a valid file name, then open the file to calculate the
  checksums.
*/
  static void doFilenameEnter()
  {
    String filename;              // entered file name after some cleaning up

    filename = filenameText.getText().trim(); // remove leading/trailing spaces
    if (filename.length() == 0)   // was anything actually entered?
    {
      clearChecksums();           // nothing entered, clear all checksum fields
      filenameText.setText("");   // and clear file name text field too
      statusText.setText(HELLO_TEXT); // return to welcome status message
    }
    else
    {
      filenameText.setText(filename); // reset to trimmed (cleaned) file name
      openFile(new File(filename)); // assume string is a file name and open
    }
  } // end of doFilenameEnter() method


/*
  doStartButton() method

  The only purpose for the "Start" button is when people enter a file name,
  without using the "File Name" button, and without pressing the Enter key.
*/
  static void doStartButton()
  {
    doFilenameEnter();            // gotta love modular programming!
  }


/*
  doTimer() method

  Update the progress bar when the GUI timer is activated.  We rely on several
  global variables initialized and updated by the calcFileChecksum() method.
*/
  static void doTimer()
  {
    if (sizeTotal > 0)            // none of this makes sense for empty files
    {
      int percent = (int) (((double) sizeDone) * 100.0 / ((double) sizeTotal));
      progressBar.setValue(percent); // always update progress bar

      if (sizeTotal > 99999999)   // one format of progress text for big files
      {
        if (sizeSuffix == null)   // have we formatted the total file size?
          sizeSuffix = " of " + formatMegabytes(sizeTotal) + " MB";
        progressBar.setString(formatMegabytes(sizeDone) + sizeSuffix);
      }
      else if (sizeTotal > 999999) // another format for medium-sized files
      {
        progressBar.setString(percent + " %"); // show only percent complete
      }
    }
  } // end of doTimer() method


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
  openFile() method

  The caller gives us a File object that may or may not be a valid file.  Try
  to open this file and calculate the checksums.  Since this may take a long
  time for a big file, we do the heavy processing in a separate thread and have
  a "Cancel" button to terminate processing early.
*/
  static void openFile(File givenFile)
  {
    openFileObject = givenFile;   // save caller's parameter as global variable

    /* Disable the "Start" button until we are done, and enable a "Cancel"
    button in case our secondary thread runs for a long time and the user
    panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    filenameButton.setEnabled(false); // suspend browsing for input files
    filenameText.setEnabled(false); // suspend entering of file name text
    startButton.setEnabled(false); // suspend "Start" button until we are done
    statusText.setText(WAIT_TEXT); // tell user to wait or cancel

    openFileThread = new Thread(new FileChecksum3User(), "openFileRunner");
    openFileThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    openFileThread.start();       // run separate thread to open files, report

  } // end of openFile() method


/*
  openFileRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void openFileRunner()
  {
    /* Call a common routine for calculating the checksums. */

    calcFileChecksum(openFileObject);

    /* We are done the dirty work, so turn off the "Cancel" button and allow
    the user to click the "Start" button again. */

    cancelButton.setEnabled(false);
    filenameButton.setEnabled(true); // resume browsing for input files
    filenameText.setEnabled(true); // resume entering of file name text
    startButton.setEnabled(true);

    /* If we weren't cancelled, then compare any checksum supplied by the user
    with the calculated checksums.  We do this after enabling the regular
    buttons, so that compareChecksum() can set the appropriate status message.
    */

    if (!cancelFlag)
      compareChecksum(compareText.getText());

  } // end of openFileRunner() method


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
    System.err.println("To run as a console application, first parameter must be a file name.  Second");
    System.err.println("and following parameters are optional checksums to be tested against calculated");
    System.err.println("checksums for the given file.  Output may be redirected with the \">\" operator.");
    System.err.println();
    System.err.println("    java  FileChecksum3  filename  [checksums]");
    System.err.println();
    System.err.println("To run as a graphical application, don't put a file name on the command line:");
    System.err.println();
    System.err.println("    java  FileChecksum3  [options]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
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
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main FileChecksum3 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop calculating current checksum
    }
    else if (source == compareText) // user typed text and pressed Enter
    {
      compareText.setText(cleanChecksum(compareText.getText())); // clean up
      compareChecksum(compareText.getText()); // compare with calculated
    }
    else if (source == copyCrc32Button) // copy CRC32 checksum to clipboard
    {
      crc32Text.selectAll();      // select all characters in text field
      crc32Text.copy();           // and copy those characters to the clipboard
    }
    else if (source == copyMd5Button) // copy MD5 checksum to clipboard
    {
      md5Text.selectAll();        // select all characters in text field
      md5Text.copy();             // and copy those characters to the clipboard
    }
    else if (source == copySha1Button) // copy SHA1 checksum to clipboard
    {
      sha1Text.selectAll();       // select all characters in text field
      sha1Text.copy();            // and copy those characters to the clipboard
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == filenameButton) // "File Name" button
    {
      doFilenameButton();         // browse or select input file
    }
    else if (source == filenameText) // user typed text and pressed Enter
    {
      doFilenameEnter();          // inspect file name field and open file
    }
    else if (source == pasteCompareButton) // paste from clipboard and compare
    {
      compareText.setText("");    // clear any existing comparison text
      compareText.paste();        // paste clipboard into our comparison text
      compareText.setText(cleanChecksum(compareText.getText())); // clean up
      compareChecksum(compareText.getText()); // compare with calculated
    }
    else if (source == progressTimer) // update progress text on clock ticks
    {
      doTimer();                  // recalculate megabytes or percent done
    }
    else if (source == startButton) // "Start" button
    {
      doStartButton();            // start calculating checksum for named file
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of FileChecksum3 class

// ------------------------------------------------------------------------- //

/*
  FileChecksum3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FileChecksum3User implements ActionListener, Runnable
{
  /* empty constructor */

  public FileChecksum3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FileChecksum3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FileChecksum3.openFileRunner();
  }

} // end of FileChecksum3User class

/* Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License. */
