# JavaSeed - An API for FDSN SEED data

### About

Java SEED is an API that began its life in 2001 in an effort to support applications, like PDCC, that were to make use of the FDSN standard SEED format (fdsn.org) for seismic datasets.  A comprehensive, object-oriented application interface was needed to work with SEED data and convert to and from other file formats.

Originally called FISSURES SEED, JavaSeed is the product of this endeavor, and is used in whole or in part by a number of applications in the seismic community over these many years.

JavaSeed in essence follows the Builder Design Pattern in its overall class design.  You have a Director that knows one format, and a Builder that knows how to construct another format.  JavaSeed has an Import side and an Export side, each of which are independent Builder modules.

This approach was taken because one of the inherent requirements for JavaSeed is the ability to convert to and from other data formats.  The Builder pattern is well suited to satisfying this requirement because you only have to write one side of the conversion chain to support a new format and you have standard interfaces to develop the new module to.

Another requirement for JavaSeed is the ability to retain the data it reads in a malleable and persistent form to support editing, viewing, merging, and sorting of the imported data, as opposed to just pushing it out to a new file or data stream.  In an object-oriented language like Java, it is natural to represent the data in memory as objects, and JavaSeed collects these objects in a Container for ready referencing.

In adopting an object format that could conceivably accommodate most known seismic data formats, following the extensively documented SEED Blockette design made perfect sense.  SEED is designed as an exchange format that is both flexible and modular, and covers most if not all types of information that would need to be delivered in a seismic data volume.  Its modularity makes it an easy match to object-oriented design, and so the Blockette object serves as the central data format for all operations in JavaSeed. 

To provide proof of concept for the JavaSeed API, a skeletal application was written that would drive all of the Java class modules to perform front to back operations, such as reading and writing SEED volumes, converting to other file formats, and so on.  Called Jseedr, it consists of a simple command line interface with flagged parameters to direct the operations.  Jseedr allows users to easily access JavaSeed’s most basic functionality to perform typical operations.  More advanced applications tap into various parts of JavaSeed to take advantage of even more functionality.

JavaSeed continues to see use and development at IRIS, even as IRIS is officially moving away from the SEED format for metadata.  Others, such as Yazan@iris are diligently maintaining this API and making it available on GitHub.

### Introductory Slides

Included is a PDF of slides from 2005, where the philosophy and general use patterns for JavaSeed are described.

