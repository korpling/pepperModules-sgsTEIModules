![SaltNPepper project](./gh-site/img/SaltNPepper_logo2010.png)
# pepperModules-sgsTEIModules
This project provides an importer to support the TEI-subset used in the sgs corpus (see http://sociolab.phil-fak.uni-koeln.de/25788.html?&L=1) for the linguistic converter framework Pepper (see https://u.hu-berlin.de/saltnpepper).

Pepper is a pluggable framework to convert a variety of linguistic formats (like [TigerXML](http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/TIGERSearch/doc/html/TigerXML.html), the [EXMARaLDA format](http://www.exmaralda.org/), [PAULA](http://www.sfb632.uni-potsdam.de/paula.html) etc.) into each other. Furthermore Pepper uses Salt (see https://github.com/korpling/salt), the graph-based meta model for linguistic data, which acts as an intermediate model to reduce the number of mappings to be implemented. That means converting data from a format _A_ to format _B_ consists of two steps. First the data is mapped from format _A_ to Salt and second from Salt to format _B_. This detour reduces the number of Pepper modules from _n<sup>2</sup>-n_ (in the case of a direct mapping) to _2n_ to handle a number of n formats.

![n:n mappings via SaltNPepper](./gh-site/img/puzzle.png)

In Pepper there are three different types of modules:
* importers (to map a format _A_ to a Salt model)
* manipulators (to map a Salt model to a Salt model, e.g. to add additional annotations, to rename things to merge data etc.)
* exporters (to map a Salt model to a format _B_).

For a simple Pepper workflow you need at least one importer and one exporter.

## Requirements
Since the here provided module is a plugin for Pepper, you need an instance of the Pepper framework. If you do not already have a running Pepper instance, click on the link below and download the latest stable version (not a SNAPSHOT):

> Note:
> Pepper is a Java based program, therefore you need to have at least Java 7 (JRE or JDK) on your system. You can download Java from https://www.oracle.com/java/index.html or http://openjdk.java.net/ .


## Install module
If this Pepper module is not yet contained in your Pepper distribution, you can easily install it. Just open a command line and enter one of the following program calls:

**Windows**
```
pepperStart.bat 
```

**Linux/Unix**
```
bash pepperStart.sh 
```

Then type in command *is* and the path from where to install the module:
```
pepper> update de.hu_berlin.german.korpling.saltnpepper::pepperModules-pepperModules-sgsTEIModules::https://korpling.german.hu-berlin.de/maven2/
```

## Usage
To use this module in your Pepper workflow, put the following lines into the workflow description file. Note the fixed order of xml elements in the workflow description file: &lt;importer/>, &lt;manipulator/>, &lt;exporter/>. 
A detailed description of the Pepper workflow can be found on the [Pepper project site](https://u.hu-berlin.de/saltnpepper). 

### a) Identify the module by name

```xml
<importer name="SgsTEIImporter" path="PATH_TO_CORPUS"/>
```

### b) Identify the module by formats
```xml
<importer formatName="xml" formatVersion="1.0" path="PATH_TO_CORPUS"/>
```

### c) Use properties
```xml
<importer name="SgsTEIImporter" path="PATH_TO_CORPUS">
  <property key="PROPERTY_NAME">PROPERTY_VALUE</property>
</importer>
```

## Contribute
Since this Pepper module is under a free license, please feel free to fork it from github and improve the module. If you even think that others can benefit from your improvements, don't hesitate to make a pull request, so that your changes can be merged.
If you have found any bugs, or have some feature request, please open an issue on github. If you need any help, please write an e-mail to saltnpepper@lists.hu-berlin.de .

## Funders
This project has been funded by the [department of corpus linguistics and morphology](https://www.linguistik.hu-berlin.de/institut/professuren/korpuslinguistik/) of the Humboldt-Universität zu Berlin, the Institut national de recherche en informatique et en automatique ([INRIA](www.inria.fr/en/)) and the [Sonderforschungsbereich 632](https://www.sfb632.uni-potsdam.de/en/). 
This module has been funded by the [Romanisches Seminar](http://romanistik.phil-fak.uni-koeln.de/26768.html) of the University of Cologne.

## License
  Copyright 2009 Humboldt-Universität zu Berlin, INRIA.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.


# <a name="details">SgsTEIImporter</a>

sgs uses a subset of the TEI standard. The module imports the primary data to multiple segmentations, including a diplomatic, a normed, a syntactic layer and a layer for pauses.
Since sgs features dialog data, the segmentation layers will be prefixed by the speaker name separated by "_". The syntactic token layer is the foundation of the syntactic trees
and can feature empty tokens. Also the morphological annotations will be assigned to the syntactic tokens. Further the module imports annotated referring expressions and discourse
entities.

## Properties

The following table contains an overview of all usable
properties to customize the behaviour of this Pepper module. The following section contains a close
description to each single property and describes the resulting differences in the mapping to the Salt
model.

|Name of property                  |Type of property |optional/mandatory |default value|
|----------------------------------|-----------------|-------------------|-------------|
|dipl.name		                   |String           | optional          |"dipl"       |
|norm.name		                   |String           | optional          |"norm"       |
|pause.name		                   |String           | optional          |"pause"      |
|syn.name		                   |String           | optional          |"syn"        |
|syn.fallback.anno                 |String           | optional          |"lemma"      |
|ignore.unknown.features           |Boolean			 | optional			 | false	   |
|empty.text.value				   |String			 | optional			 |"∅"		   |


### dipl.name

The provided string will be used as name suffix for the diplomatic token layer.

### norm.name

The provided string will be used as name suffix for the normed token layer.

### pause.name

The provided string will be used as name suffix for the pause token layer.

### syn.name

The provided string will be used as name suffix for the syntactic token layer.

### syn.fallback.anno

The given annotation name will be used, when a single norm token overlaps multiple syntactic token (subtokenization) and a simple copying of the norm token's text value
is not useful. Instead, on real subtokens the fallback annotation will be used as text value. For example:

The single token "à propos du" (see corpus / format documentation) will carry two syntactic subtokens "{à propos de}" and "{le}" derived from their lemma annotation.

### ignore.unknown.features

This property defines how to behave in case an unknown feature is observed. Given the property is true, the unknown feature will be ignored, if false an error will be raised.
In any case only features will be imported that are known to the importer as defined in `SgsTEIDictionary`.

### empty.text.value

This property defines the text value for empty tokens on the syntactic token level.
