/*
  Character Map Parse #5 - Quick-and-Dirty Extraction of Unicode Data
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Tuesday, 13 January 2009
  Java class name: CharMapParse5
  Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License.

  This is a quick-and-dirty Java 1.4 console application to parse two data
  files with Unicode character numbers (hexadecimal) and names.  These files
  are subject to change each time the Unicode standard is revised:

   1. UnicodeData.txt (standard character names)
      from: http://www.unicode.org/Public/UNIDATA/UnicodeData.txt

   2. Unihan.txt (Chinese-Japanese-Korean ideographs)
      from: http://www.unicode.org/Public/UNIDATA/Unihan.zip

  Output is formatted as plain text for the CharMap4.txt data file used by the
  CharMap4 Java application.  Some manual editing will be required:

   1. The data goes after the explanatory comments in the CharMap4.txt file.

   2. Control codes (U+0000 to U+001F and U+007F to U+009F) will have the wrong
      descriptions.  You should use the existing descriptions that were created
      by hand.  Search for "<" and ">" in the output file.

   3. There will be spurious entries for the first and last characters in large
      Unicode blocks (ranges).  Again, search for "<" and ">".

   4. Some descriptions will have incorrect capitalization.  In particular, you
      may find "Apl" instead of the correct "APL" (U+2336 to U+2395), and "Cjk"
      instead of the correct "CJK" (U+2E80 to U+31E3).

   5. Some "CJK compatibility ideographs" from U+FA0E to U+FA29 have default
      names in UnicodeData.txt that are not replaced by better information in
      the Unihan.txt file.

  This program replaces three separate, smaller programs: CharMapParse2 (basic
  character names), CharMapParse3 (CJK ideographs), and CharMapParse4 (Korean
  Hangul syllables).  Those smaller programs produced correct data, but merging
  the Unicode blocks was time consuming.  The only nice features that have been
  lost are the comments at the beginning and end of each large Unicode block.

  This source file should only be distributed with the source for CharMap4.
  General users have no need for the CharMapParse5 application.  THIS CODE IS
  UGLY AND SHOULD *NOT* BE USED AS THE BASIS FOR ANY OTHER PROGRAMS.  Much of
  the string handling is inefficient and would be better with StringBuffers.

  GNU General Public License (GPL)
  --------------------------------
  CharMapParse5 is free software: you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by the Free
  Software Foundation, either version 3 of the License or (at your option) any
  later version.  This program is distributed in the hope that it will be
  useful, but WITHOUT ANY WARRANTY, without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
  Public License for more details.

  You should have received a copy of the GNU General Public License along with
  this program.  If not, see the http://www.gnu.org/licenses/ web page.
*/

import java.io.*;                 // standard I/O
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions

public class CharMapParse5
{
  /* constants */

  static final String EMPTY = ""; // the empty string

  /* The following string arrays are used when converting Unicode character
  numbers for Korean Hangul syllables to caption strings.  Please refer to
  these on-line references:

      http://en.wikipedia.org/wiki/Hangul
      http://en.wikipedia.org/wiki/Korean_language_and_computers
      http://en.wikipedia.org/wiki/Korean_romanization
      http://en.wikipedia.org/wiki/Revised_Romanization_of_Korean

  The Unicode assignments for Hangul are so regular that it is unnecessary to
  store 11,172 separate caption strings for 0xAC00 to 0xD7A3.  This becomes
  especially true when you realize that only 2,350 of those Hangul syllables
  are in common use! */

  static final String[] HANGUL_NAME_INITIAL = {"Kiyeok", "Ssangkiyeok",
    "Nieun", "Tikeut", "Ssangtikeut", "Rieul", "Mieum", "Pieup", "Ssangpieup",
    "Sios", "Ssangsios", "Ieung", "Cieuc", "Ssangcieuc", "Chieuch", "Khieukh",
    "Thieuth", "Phieuph", "Hieuh"};
  static final String[] HANGUL_NAME_MEDIAL = {"A", "Ae", "Ya", "Yae", "Eo",
    "E", "Yeo", "Ye", "O", "Wa", "Wae", "Oe", "Yo", "U", "Weo", "We", "Wi",
    "Yu", "Eu", "Yi", "I"};
  static final String[] HANGUL_NAME_FINAL = {"", "Kiyeok", "Ssangkiyeok",
    "Kiyeok-Sios", "Nieun", "Nieun-Cieuc", "Nieun-Hieuh", "Tikeut", "Rieul",
    "Rieul-Kiyeok", "Rieul-Mieum", "Rieul-Pieup", "Rieul-Sios",
    "Rieul-Thieuth", "Rieul-Phieuph", "Rieul-Hieuh", "Mieum", "Pieup",
    "Pieup-Sios", "Sios", "Ssangsios", "Ieung", "Cieuc", "Chieuch", "Khieukh",
    "Thieuth", "Phieuph", "Hieuh"};
  static final String[] HANGUL_SOUND_INITIAL = {"G", "KK", "N", "D", "TT", "R",
    "M", "B", "PP", "S", "SS", "", "J", "JJ", "CH", "K", "T", "P", "H"};
  static final String[] HANGUL_SOUND_MEDIAL = {"A", "AE", "YA", "YAE", "EO",
    "E", "YEO", "YE", "O", "WA", "WAE", "OE", "YO", "U", "WO", "WE", "WI",
    "YU", "EU", "UI", "I"};
  static final String[] HANGUL_SOUND_FINAL = {"", "K", "KK", "KS", "N", "NJ",
    "NH", "T", "L", "LK", "LM", "LP", "LS", "LT", "LP", "LH", "M", "P", "PS",
    "S", "SS", "NG", "J", "CH", "K", "T", "P", "H"};

  /* class variables */

  static TreeMap captionMap;      // mapping of character numbers to captions
  static String codeNumber;       // Unicode notation for current character
  static Pattern codePattern;     // compiled expression for Unicode notation
  static Pattern linePattern;     // compiled expression for CJK input line
  static Vector wordList;         // list of interesting words for each U+nnnn
  static Pattern wordPattern;     // compiled expression for alphanumeric words

  /* main program */

  public static void main(String[] args)
  {
    try                           // catch specific and general I/O errors
    {
      /* Start with an empty mapping from Unicode character numbers (as Integer
      objects) to caption text (as String objects). */

      captionMap = new TreeMap(); // start without any caption strings

      /* Build up the list of known characters, allowing later characters to
      replace earlier characters. */

      uniBegin();                 // parse standard character names
      hanBegin();                 // generate Korean Hangul syllables
      cjkBegin();                 // add or replace CJK ideographs

      /* Extract all known characters and write an output line for each. */

      PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(
        "parsed-unidata.txt")));
      Set keyList = captionMap.keySet(); // sorted list of character numbers
      Iterator keyIter = keyList.iterator(); // get iterator for those keys
      while (keyIter.hasNext())   // any more character numbers?
      {
        Integer charObj = (Integer) keyIter.next(); // next character as object
        int charNum = charObj.intValue(); // next character as number
        String charText = (String) captionMap.get(charObj); // caption text
        output.println(unicodeNotation(charNum) + " = " + charText);
      }
      output.close();             // try to close output file
    }
    catch (IOException ioe)       // all other I/O errors
    {
      System.err.println("File I/O error: " + ioe.getMessage());
    }
  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  cjkBegin() method

  Extract pronunciation and variant notations for Chinese-Japanese-Korean (CJK)
  ideographs from the Unihan.txt data file.
*/
  static void cjkBegin() throws IOException
  {
    BufferedReader input;         // input character stream
    String line;                  // one line of text from input file
    Matcher matcher;              // pattern matcher for <pattern>

    /* Open the input file. */

    input = new BufferedReader(new InputStreamReader(new FileInputStream(
      "Unihan.txt"), "UTF-8"));

    /* Compile regular expressions once only. */

    codePattern = Pattern.compile("U\\+[0-9A-Fa-f]{1,6}");
    linePattern = Pattern.compile("^\\s*U\\+([0-9A-Fa-f]{1,6})\\s+([^\\s]+)\\s+(.+)$");
    wordPattern = Pattern.compile("[0-9A-Z\u00DCa-z\u00FC]+");
                                  // kMandarin has <Letter U With Diaeresis>

    /* Start without a known character name or interesting word list. */

    codeNumber = EMPTY;           // no Unicode character notation yet
    wordList = new Vector();      // clear word list for next character

    /* Try to add each input line to current word list. */

    while ((line = input.readLine()) != null)
    {
      matcher = linePattern.matcher(line); // attempt to match input line
      if (matcher.find())         // if the general search pattern is found
      {
        String hex = matcher.group(1).toUpperCase(); // hex character code
        if (codeNumber.equals(hex) == false) // have we changed hex codes?
        {
          cjkFlush();             // output data for previous hexadecimal code
          codeNumber = hex;       // and remember the new hex code
        }
        String field = matcher.group(2);
        String value = matcher.group(3);
        if (field.equals("kCantonese"))
          cjkNewWords(value, "C:");
        else if (field.equals("kCompatibilityVariant"))
          cjkNewCodes(value);
        else if (field.equals("kJapaneseKun"))
          cjkNewWords(value, "J:");
        else if (field.equals("kJapaneseOn"))
          cjkNewWords(value, "S:");
        else if (field.equals("kKorean"))
          cjkNewWords(value, "K:");
        else if (field.equals("kMandarin"))
          cjkNewWords(value, "M:");
        else if (field.equals("kSemanticVariant"))
          cjkNewCodes(value);
        else if (field.equals("kSimplifiedVariant"))
          cjkNewCodes(value);
        else if (field.equals("kSpecializedSemanticVariant"))
          cjkNewCodes(value);
        else if (field.equals("kTraditionalVariant"))
          cjkNewCodes(value);
        else if (field.equals("kZVariant"))
          cjkNewCodes(value);
      }
    }
    cjkFlush();                   // print final character code, if any
    input.close();                // try to close input file

  } // end of cjkBegin() method


/*
  cjkFlush() method

  Write any pending data for the previous hexadecimal character code.
*/
  static void cjkFlush()
  {
    if (codeNumber.equals(EMPTY) == false) // only if there is a character code
    {
      int size = wordList.size(); // number of words in interesting word list
      if (size > 0)               // do nothing if there are no words to print
      {
        Collections.sort(wordList); // sort list of words as Unicode strings
        String tag = EMPTY;       // no language tag found yet
        String text = EMPTY;      // begin line
        for (int i = 0; i < size; i ++) // do each word in word list
        {
          if (i > 0)              // insert space for second and later word
            text += ' ';          // put the delimiter before the word

          String word = (String) wordList.get(i); // get one interesting word
          if (word.substring(0, 2).equals(tag))
            text += word.substring(2); // don't repeat language tag
          else
            text += word;         // first time we've seen this language tag

          tag = (word.charAt(1) == '+') ? EMPTY : word.substring(0, 2);
        }
        putCaption(Integer.parseInt(codeNumber, 16), text); // hex char number
      }
    }
    wordList = new Vector();      // clear word list for next character

  } // end of cjkFlush() method


/*
  cjkNewCodes() method

  Given a string, add each Unicode notation in that string to our list of
  interesting words.
*/
  static void cjkNewCodes(String text)
  {
    Matcher matcher = codePattern.matcher(text); // start looking for words
    while (matcher.find())        // for all words that we find
      cjkNewEntry(matcher.group().toUpperCase()); // insert found word in list
  }


/*
  cjkNewEntry() method

  Common method called by cjkNewCodes() and cjkNewWords() to insert one word
  into our list of interesting words.
*/
  static void cjkNewEntry(String text)
  {
    int length = text.length();   // get number of characters in caller's text
    String word = "";             // start to copy caller's text to our word
    for (int i = 0; i < length; i ++) // for each character in caller's text
    {
      char ch = text.charAt(i);   // get one character from caller's text
      if (ch == '\u00DC')         // Latin Capital Letter U With Diaeresis?
        ch = 'U';                 // replace with regular uppercase letter
      else if (ch == '\u00FC')    // Latin Small Letter U With Diaeresis?
        ch = 'u';                 // replace with regular lowercase letter
      word += ch;                 // append changed or original character
    }
    if (wordList.contains(word) == false) // is this word already in the list?
      wordList.add(word);         // add only new words, no duplicates
  }


/*
  cjkNewWords() method

  Given a string, add each alphanumeric word in that string to our list of
  interesting words.
*/
  static void cjkNewWords(String text, String tag)
  {
    Matcher matcher = wordPattern.matcher(text); // start looking for words
    while (matcher.find())        // for all words that we find
    {
      String word = matcher.group(); // need to format word in title case
      cjkNewEntry(tag + word.substring(0, 1).toUpperCase()
        + word.substring(1).toLowerCase()); // insert found word in list
    }
  }


/*
  hanBegin() method

  Generate Korean Hangul syllables and sounds similar to the data file:

  http://www.iana.org/domains/idn-tables/tables/kr_ko-kr_1.0.html
  linked from: http://www.iana.org/domains/idn-tables/
  was (c.2008): http://www.iana.org/assignments/idn/kr-korean.html
*/
  static void hanBegin()
  {
    String caption;               // generated caption string
    int value;                    // Unicode character number

    /* The following code was obtained from the CharMap4 Java application, with
    minimal changes (to verify that the code works as expected). */

    for (value = 0xAC00; value <= 0xD7A3; value ++) // Korean Hangul range
    {
      int first = value - 0xAC00; // set zero point for following calculation
      int third = first % 28;     // index of "final" phonetic piece
      first = first / 28;         // remove value of final piece
      int second = first % 21;    // index of "medial" phonetic piece
      first = first / 21;         // index of "initial" phonetic piece

      caption = "Hangul Syllable " + HANGUL_NAME_INITIAL[first] + " "
        + HANGUL_NAME_MEDIAL[second] + " " + HANGUL_NAME_FINAL[third];
      caption = caption.trim();   // remove any unused third piece
      String sound = HANGUL_SOUND_INITIAL[first]
        + HANGUL_SOUND_MEDIAL[second] + HANGUL_SOUND_FINAL[third];
      caption += " (" + sound.charAt(0) + sound.substring(1).toLowerCase()
        + ")";                    // first "letter" may be from second piece

      putCaption(value, caption); // save character's constructed caption
    }
  } // end of hanBegin() method


/*
  putCaption() method

  Save caption text (string) corresponding to a character value.  Use a common
  method for this simple operation, so that everyone does it the same way.
*/
  static void putCaption(int value, String text)
  {
    captionMap.put(new Integer(value), text);
  }


/*
  titleCase() method

  Return a string with everything in lowercase except the first letter of each
  word.  Unicode standard names are all uppercase by definition.
*/
  static String titleCase(String input)
  {
    char ch;                      // one input character
    int i;                        // index variable
    int length;                   // number of input characters
    String result;                // our converted string
    boolean upper;                // true if next character will be uppercase

    length = input.length();      // get number of input characters
    result = "";                  // start with an empty string
    upper = true;                 // first word is always capitalized
    for (i = 0; i < length; i ++) // do all input characters
    {
      ch = input.charAt(i);       // get one input character
      if ((ch == ' ') || (ch == '-')) // check for word delimiters
      {
        result += ch;             // copy delimiter unchanged
        upper = true;             // next letter will be uppercase
      }
      else if (upper)             // should we leave this as uppercase?
      {
        result += ch;             // yes, append original character to result
        upper = false;            // next character will be lowercase
      }
      else
        result += Character.toLowerCase(ch); // append as lowercase
    }
    return(result);               // give caller our converted string

  } // end of titleCase() method


/*
  uniBegin() method

  Extract standard Unicode character names from the UnicodeData.txt data file.
*/
  static void uniBegin() throws IOException
  {
    BufferedReader input;         // input character stream
    String line;                  // one line of text from input file
    Matcher matcher;              // pattern matcher for <pattern>
    Pattern pattern;              // compiled regular expression

    input = new BufferedReader(new FileReader("UnicodeData.txt"));
    pattern = Pattern.compile(    // regular expression for lines we want
      "^\\s*([0-9A-Fa-f]{1,6})\\s*;\\s*(\\S[^;]*\\S)\\s*;");
    while ((line = input.readLine()) != null)
    {
      matcher = pattern.matcher(line); // attempt to match
      if (matcher.find())         // if the search pattern is found
      {
        putCaption(Integer.parseInt(matcher.group(1), 16), // hex char number
          titleCase(matcher.group(2))); // caption text for character
      }
    }
    input.close();                // try to close input file

  } // end of uniBegin() method


/*
  unicodeNotation() method

  Given an integer, return the Unicode "U+nnnn" notation for that character
  number.
*/
  static String unicodeNotation(int value)
  {
    String result;                // our converted result

    result = Integer.toHexString(value).toUpperCase(); // convert binary to hex
    if (result.length() < 4)      // must have at least four digits
      result = "0000".substring(result.length()) + result;
    result = "U+" + result;       // insert the "U+" prefix

    return(result);               // give caller our converted string
  }

} // end of CharMapParse5 class

/* Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License. */
