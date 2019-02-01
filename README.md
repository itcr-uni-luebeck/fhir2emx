<!----------------------------------------------------------------------------------
# Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
# Noemi Deppenwiese, Hannes Ulrich
#------------------------------------------------------------------------------ -->

# FHIR2EMX
FHIR2EMX is a webbased Tool to convert HL7 FHIR Questionnaires (STU 3) into MOLGENIS EMX Tables.

HL7 FHIR (https://www.hl7.org/fhir/) is an emerging standard for exchanging health care data. For more information about FHIR Questionnaires and FHIR in general visit https://www.hl7.org/fhir/questionnaire.html.

MOLGENIS (http://molgenis.github.io/) is an open-source tool for managing scientific data. Is is available on github: https://github.com/molgenis/molgenis.

FHIR2EMX can be used to automatically transfer the information contained in FHIR Questionnaires, e.g. from clinical trials, to MOLGENIS. This tool only works with metadata (questions), not with data (answers, that would be FHIR QuestionnaireResponse). It will produce an empty (only column names) sheet for an entity that represents your questionnaire and corresponding entities, packages and attributes sheet. The attributes of the generated entity correspond to your questionnaires items.
Because of EMX naming restrictions the use of FHIR URLs as identifiers is sadly not possible, and your new entity will be called something like A2345346. Its description in the entity table will contain the original FHIR URL (the value of the Questionnaire.url field).

Some Information from FHIR Questionnaires, e.g. title, status or item.enableWhen can currently not be represented in EMX. However, FHIR2EMX will try to preserve as much information as possible from your Questionnaires items. It will for example try to convert item.code fields into tags. This only works for codes that are valid EMX identifiers (see "Rules for technical names" in https://molgenis.gitbooks.io/molgenis/content/user_documentation/ref-emx.html). Otherwise, a warning message (yellow) will be displayed along with the generated tables.

Items of type "display" will be skipped.

If your Questionnaire references FHIR ValueSets, FHIR2EMX will try to resolve the reference and convert the ValueSet into its own entity. This only works for ValueSets containing an expansion, the elements of the expansion become instances of the ValueSet entity. This entity is then referenced by the attribute generated from the item that referenced to the ValueSet in the questionnaire.


Build using the Spark Web Framework (http://sparkjava.com/) and HAPI FHIR (http://hapifhir.io/).


## Installation
Build using maven and run. Application starting point is server.Main. Jetty uses Port 4567 by default.

## Usage
Questionnaires can be loaded by providing an FHIR SERVER URL and an corresponding Questionnaire ID. The genereted EMX Tables can either be downloaded as zipped *.tsv file or directly uploaded to your MOLGENIS Instance via GUI.

 API Documentation is available at [host]/fhir2emx/api, GET [host]/fhir2emx/ will return initial GUI.
 Enter the Base-URL of the FHIR-Server that hosts your target Questionnaire, for example the HAPI FHIR test server (https://fhirtest.uhn.ca/baseDstu3). In the field below, enter the ID of your Questionnaire. For example, the Questionnaire available at https://fhirtest.uhn.ca/baseDstu3/Questionnaire/144829/_history/1 has the id 144829. You can view your Questionnaire in raw JSON on the next page. If you want to check that all names generated during the conversion are still available on your MOLGENIS, fill out the login form. You can turn tag-generation on or off depending on your MOLGENIS Version (older ones don't support tags). If you only want to generate the tables without connection to MOLGENIS, use the button on the right. The next page will show your results along with a "download tables" button. If you choose to connect to MOLGENIS in the previous step you will also see an "upload" button which will upload the generated tables to your MOLGENIS.

## Project Structure
```
project
│   README.md
│   pom.xml
└─── src
   └───  main
        │
        └───  java
                └───  converter
                		└───  QuestionnaireConverter.java
                		└───  Issue.java
                		└───  ConversionOutcome.java
                └───  emxModel
                		└───  EMX.java
                		└───  ...
                └───  server
                		└───  Main.java
                		└───  FHIRResourceFetcher.java
                		└───  FileHandler.java
                		└───  FileMutex.java
                		└───  OutdatedFilesRemover.java
                		└───  MOLGENISServerConnector.java
                		└───  JsonTransformer.java
                		└───  TableResult.java
                └───  JsonModels
                		└───  Errors.java
                		└───  IncomingRequest.java
                		└───  Login.java
                		└───  LoginResponse.java
        └───  resources
        			└───  public
        					└───  fhir2emx
        						└───  tables
        							└───  [EMX Files will be saved here]
        						└───  css
        							└───  ...
        						└───  images
        							└───  ...
        						└───  js
        							└───  ...
        						└───  apiDoku.html
        			└───  spark
        					└───  ...
        			logback.xml
       └───  webapp
               └───  WEB-INF
                      └───  web.xml
```
## Citation
```
@conference {1029,
	title = {Entwicklung eines Tools zum Konvertieren von HL7 FHIR Questionnaires in das MOLGENIS EMX Format},
	booktitle = {GMDS},
	year = {2017},
	doi = {10.3205/17gmds148},
	url = {https://www.egms.de/static/en/meetings/gmds2017/17gmds148.shtml},
	author = {Deppenwiese, Noemi and Ulrich, Hannes and Wrage, Jan-Hinrich and Kock-Schoppenhauer, Ann-Kristen and Ingenerf, Josef}
}
```
