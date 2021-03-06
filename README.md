# Hypernomicon

Hypernomicon is a personal database application for keeping track of information like theories, debates, arguments, concepts, etc. that might be used by people such as philosophers or others doing academic research.

Hypernomicon is perfect for: Anyone who works in a field (professionals, hobbyists, and students alike) where they have to keep track of several (or more) of the following:
 * Terminology
 * Concepts
 * Theoretical questions/Debates (hierarchically organized)
 * Theories (hierarchically organized)
 * Positions (hierarchically organized)
 * Arguments for theories/positions (and counterarguments)
 * Authors (and information about them such as website, affiliation, etc.)
 * The works authors have authored
 * Sources and authors associated with arguments, theories, positions, and debates
 * PDF files associated with such sources and works (including the multiple works an edited volume might contain, and ability to jump to the PDF page of a particular paper)
 * Personal notes associated with any of the above
 * Notes taken during talks, meetings, seminars
 * Any other files or folders associated with any of the above (including notes)
 * Ability to manage (rename, move, etc.) files and folders while not losing associations with any of the above information
 * Associations between works and entries in your bibliography manager (currently integrates with Zotero; Mendeley in the future)

Hypernomicon keeps track of all of the above in a highly structured, thoroughly indexed and user friendly relational database, and automatically generates semantic hyperlinks between all of them so that you are constantly informed of ways all of your information is related that you had not realized.

## Getting started ##

The best way to get started with Hypernomicon is by downloading and installing on your developer machine the latest
[Hypernomicon release](http://hypernomicon.org/download).

Or you can clone this repository and build from source (see below).

Either way, you will also need some data. You can download a starter database [here](http://jasonwinning.com/starter_db.zip).

## Issues and Contributions ##

Issues can be reported to the [Issue tracker](https://github.com/jasonwinning/hypernomicon/issues/)

Contributions can be submitted via [Pull requests](https://github.com/jasonwinning/hypernomicon/pulls/)

## Building Hypernomicon ##

### Prerequisites

* A recent version of [Java 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html) for building 'master' branch
* A recent version of [Git](https://git-scm.com/downloads)
* [Maven](https://maven.apache.org/download.cgi) version 3.0.5 or greater

### How to build Hypernomicon ###

Use Git to get a copy of the source code onto your computer.

`$ git clone git@github.com:jasonwinning/hypernomicon.git`

On the project's root, run:

`mvn clean package`

It will create an executable jar under `target/hypernomicon-$version.jar`.


## Authors

* **Jason Winning** - *Original design and development* - [website](http://jasonwinning.org)

See also the list of [contributors](https://github.com/jasonwinning/hypernomicon/contributors) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](http://htmlpreview.github.com/?https://github.com/jasonwinning/hypernomicon/blob/master/LICENSE.html) file for details

## Acknowledgements

* Icons:

  * [FatCow](http://www.fatcow.com/free-icons)
  * [Fugue](http://p.yusukekamiyamane.com/)

* Third-party libraries used:
  * Apache [Commons](https://commons.apache.org/), [PDFBox](https://pdfbox.apache.org/), [Tika](https://tika.apache.org/), [HttpClient](https://hc.apache.org/httpcomponents-client-ga/)
  * [Guava](https://github.com/google/guava)
  * [JxBrowser](https://www.teamdev.com/jxbrowser)
  * [PDF.js](https://mozilla.github.io/pdf.js/)
  * [jsoup](https://jsoup.org/)
  * [jQuery](https://jquery.com/)
  * [ICU4J](http://site.icu-project.org/home)
  * [ControlsFX](http://fxexperience.com/controlsfx/)
  * [JSON.simple](https://code.google.com/archive/p/json-simple/)
  * [ScribeJava](https://github.com/scribejava/scribejava)
  * [XMP Toolkit for Java](https://www.adobe.com/devnet/xmp.html)
  * [Mammoth .docx to HTML converter](https://github.com/mwilliamson/java-mammoth)
  * [JBibTex](https://github.com/jbibtex/jbibtex)
  * [JIntellitype](https://code.google.com/archive/p/jintellitype/)
  * [highlight](http://johannburkard.de/blog/programming/javascript/highlight-javascript-text-higlighting-jquery-plugin.html)
