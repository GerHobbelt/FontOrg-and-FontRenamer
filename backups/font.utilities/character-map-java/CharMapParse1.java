/*
  Character Map Parse #1 - Quick-and-Dirty Extraction of UNIDATA/Blocks.txt
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Monday, 12 January 2009
  Java class name: CharMapParse1
  Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License.

  This is a quick-and-dirty Java 1.4 console application to parse the data file
  downloaded from:

      http://www.unicode.org/Public/UNIDATA/Blocks.txt

  to obtain Unicode "block" or range names.  Output is formatted as source code
  to be inserted into the captionGet() method of the CharMap4 Java application.
  Some manual editing will be required on the first line ("if" instead of "else
  if").  This program assumes that block names do not contain backslashes (\),
  control codes, escape sequences, quotation marks ("), or any other characters
  that would cause syntax errors in the generated source code.

  CharMapParse1 should be run once each time the Unicode standard is revised.
  This source file should only be distributed with the source for CharMap4.
  General users have no need for the CharMapParse1 application.  THIS CODE IS
  UGLY AND SHOULD *NOT* BE USED AS THE BASIS FOR ANY OTHER PROGRAMS.

  GNU General Public License (GPL)
  --------------------------------
  CharMapParse1 is free software: you can redistribute it and/or modify it
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
import java.util.regex.*;         // regular expressions

public class CharMapParse1
{
  public static void main(String[] args)
  {
    BufferedReader input;         // input character stream
    String line;                  // one line of text from input file
    Matcher matcher;              // pattern matcher for <pattern>
    PrintWriter output;           // output character stream
    Pattern pattern;              // compiled regular expression

    try                           // catch specific and general I/O errors
    {
      input = new BufferedReader(new FileReader("Blocks.txt"));
      output = new PrintWriter(new BufferedWriter(new FileWriter(
        "parsed-blocks.txt")));
      pattern = Pattern.compile(
        "^\\s*([0-9A-Fa-f]+)\\s*\\.\\.\\s*([0-9A-Fa-f]+)\\s*;\\s*(\\S.*\\S)\\s*$");
      while ((line = input.readLine()) != null)
      {
        matcher = pattern.matcher(line); // attempt to match
        if (matcher.find())       // if the search pattern is found
        {
          output.println("      else if ((value >= 0x"
            + matcher.group(1).toUpperCase() + ") && (value <= 0x"
            + matcher.group(2).toUpperCase() + "))");
          output.println("        caption = \"" + matcher.group(3) + "\";");
        }
      }
      input.close();              // try to close input file
      output.close();             // try to close output file
    }
    catch (IOException ioe)       // all other I/O errors
    {
      System.err.println("File I/O error: " + ioe.getMessage());
    }
  } // end of main() method

} // end of CharMapParse1 class

/* Copyright (c) 2009 by Keith Fenske.  Released under GNU Public License. */
