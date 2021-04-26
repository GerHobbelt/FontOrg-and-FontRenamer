/*
  Show Character Sets #2 - Show Available Java Character Set Encodings
  Written by: Keith Fenske, http://www.psc-consulting.ca/fenske/
  Monday, 15 December 2008
  Java class name: ShowCharSets2
  Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License.

  This is a Java 1.4 console application to print a complete list of installed
  character set names and aliases for the current computer and version of Java.

  GNU General Public License (GPL)
  --------------------------------
  ShowCharSets2 is free software: you can redistribute it and/or modify it
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
import java.nio.charset.*;        // character sets
import java.util.*;               // calendars, dates, lists, maps, vectors

public class ShowCharSets2
{
  public static void main(String[] args)
  {
    /* introduction */

    System.out.println();         // blank line
    System.out.println("Available Java Character Set Encodings");
    System.out.println("Version " + System.getProperty("java.version")
      + " from " + System.getProperty("java.vendor"));
    System.out.println(System.getProperty("os.name") + " ("
      + System.getProperty("os.version") + ") "
      + System.getProperty("os.arch"));
    System.out.println((new Date()).toString());
    System.out.println();

    /* character sets */

    SortedMap csmap = Charset.availableCharsets();
    Set keylist = csmap.keySet();
    Iterator keyiter = keylist.iterator();
    while (keyiter.hasNext())
    {
      String keyname = (String) keyiter.next();
      System.out.println("canonical charset name <" + keyname + ">");
      Charset cs = (Charset) csmap.get(keyname);
      Set aliases = cs.aliases();
      Iterator aliter = aliases.iterator();
      while (aliter.hasNext())
      {
        System.out.println("   alias <" + ((String) aliter.next()) + ">");
      }
    }

    /* conclusion */

    System.out.println();
    System.out.println("[end]");

  } // end of main() method

} // end of ShowCharSets2 class

/* Copyright (c) 2008 by Keith Fenske.  Released under GNU Public License. */
