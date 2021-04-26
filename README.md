# FontOrg and FontRenamer

copy of **FontOrg** (Al Jones, a.k.a. *`//al`*) and **FontRenamer** (Philip M. Engel, a.k.a. *RedEar*) font tools as posted in yonder days on NNTP *alt.binaries.fonts*.

---

![image](https://user-images.githubusercontent.com/402462/116137857-656f4280-a6d4-11eb-8b09-4f30ec037868.png)
![image](https://user-images.githubusercontent.com/402462/116137975-8a63b580-a6d4-11eb-84db-8a9704826894.png)
![image](https://user-images.githubusercontent.com/402462/116138001-93ed1d80-a6d4-11eb-8d27-2d9e315396a1.png)

---

## How do I grab this?

Two ways to achive the same:

1. click the green **Code** button above, you'll see a dropdown. Pick "Download ZIP"
2. [click on darker "Releases" title (middle-right on the page; when you hover over it with your mousee, that black title will turn blue to indicate that's a link!)](https://github.com/GerHobbelt/FontOrg-and-FontRenamer/releases). A new page shows up. There you can download **everything** in this repo as (zip) or (tar.gz) archive file by clicking on the links below the **the-whole-shabang** tag.



## What is this?

These tools were posted regularly on alt.bin.fonts by the authors and are very useful tools to organize your TTF/OTF/Type-1 fonts.

FontRenamer does the obvious: renaming the files to a sensible filename (which includes Font Family, etc. as extracted from the font file itseelf),
while FontOrg can organize any set of font files in a directory tree of your choice.

FontOrg was available on http://fontorg.us/ for a while but that website has expired long ago. Its [author's site](http://aljones.us/) is still up, but covers a *quite different subject*: family genealogy. 

AFAICT was FontRenamer never published *officially* outside the NNTP channel, though you may look around the net and may dig up a *still older* copy on some freeware/shareware site(s). Make sure to include the name of the author (Philip M. Engel) in your search or you'll hit several other **unrelated** tools!

These tools are made available here for my personal use and may help others dig up these oldies.

No sourcecode available, alas.

FontOrg v3 still runs fine on Windows 10/64 over here. YMMV.

FontRenamer v2 seems slightly more finicky r4e stability on Windows 10/64, even while it's produced in .NET 2.0, which should still be okay 🤔   Anyway, here again: YMMV.

## Notes

- the Designer and other text files provided with this instance of FontOrg is my (*probably patched*) copy from 2015 AD.
- Older versions are stored in the `backups` directory. Try them and see which works if the latest if giving you trouble.
- At times Al would post updates for the Designer.txt and other text files, which serve as driving databases for FontOrg and can be edited in any decent text eeditor, ee.g. [Notepad++](https://notepad-plus-plus.org/) or [Sublime](https://www.sublimetext.com/)
- Nothing of note has been posted since 2014/2015 (I haven't watched *very* closely 🕵️😥); the stuff in this repo came off old drives and I've been looking for this a couple of years ago and today once again: nothing useful or trustworthy pops up on the Net. So here's what I got in cold storage. 😄 The tools are still relevant with the free fonts out there nowadays that are nice but also quite disorganized - at least to my tastes.
- Enjoy. Please *do* apply patience as the tools are old and may need a bit of assistance to get going on your box. Once they do, they'll be pretty fast as they have not changed but your hardware certainly *will have changed*. 😆🐎


---

## Re: FontRenamer

From: redear <redear@nowhere.com.invalid>  
Subject: UPDATE - RedEar's Font Renamer - FontRenamer307.zip (0/1)  
Date: Mon, 09 Feb 2015 18:28:11 -0600  
Newsgroups: alt.binaries.fonts  


Existing Users:

This is a minor update and bug fix for RedEar's Font Renamer. All users of Font Renamer should install this update, particularly if you are running in a multi-monitor environment.

This release, version 3.0.7, contains the following fixes:

- Corrected the processing that keeps the main window fully visible so that it works properly in a multi-monitor environment

- Corrected the processing of relative directory references on the command line

- Added Windows 8.1 and Windows 10 to processing that identifies OS version for error log 

- Changed the Exit button image on the tool bar I also added a comment to the Easter Egg, although to my knowledge nobody has ever found the egg.

New Users:

Please read the "Getting Started" sections in the help file before using the program!

RedEar's Font Renamer is a Windows utility program that consistently and automatically changes a font's external file name to match the font's internal name. Thus, for example, when looking at a renamed file in Windows Explorer, you will see "Chaparral MM.pfb" rather than "xtr_____.pfb", a much more comprehensible name. In other words, you get a meaningful file name, not a cryptic file name (which in most cases was created under the outdated 8.3 file naming conventions).

Because of individual whims or variations in the ways older automated renamers handle characters that are invalid in a file name, many fonts in the public domain are circulating with different names. A font renamer allows you to bring consistency to your font collection and in the process identify possible duplicates. (Note that there do exist many font files that have the same internal name but are in fact different fonts with different glyphs. Often, however, files with the same internal name are in fact the same font.)

Font Renamer robustly renames all types of font files and related files. It has extensive options to control the specific internal name that is used for renaming, to handle invalid characters, and to fine tune the renaming process. However, the default settings are recommended for most users.

Font Renamer optionally checks that a font can actually be installed by your operating system and can flag fonts that cannot.Font Renamer does not change the contents of font files nor does it fix bad fonts.

Note that you must have Microsoft .NET Framework 2.0 (the necessary run-time files) installed. See the "Before You Begin" section in the help file for information on obtaining the Framework if you have not yet installed it.

If you are running a different operating system, you should look at FontRename, one of several Java-based utilities by Keith Fenske






