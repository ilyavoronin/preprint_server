Configure dependencies that require manual installation
------------------------------
Follow [this](https://grobid.readthedocs.io/en/latest/Install-Grobid/#build-grobid-with-gradle) instructions to download and install Grobid. Grobid is used to process PDF files. Skip this step if you only want to build validation database.

Building validation database
----------------------------

### Downloading data to index
1. Download latest crossref metadata dump from archive.org. The latest for now is available [here](https://archive.org/download/crossref_doi_dump_201909) (~45GB)
2. Download latest data from Semantic Scholar. Follow instructions from [here](http://s2-public-api-prod.us-west-2.elasticbeanstalk.com/corpus/download) (~115GB)


### Build
```sh
./gradlew -p validation_db build
```

### Run data indexing
Copy `validationDBconfig.properties` from `validation_db` folder to `~/.preprint_server/`. Specify the following parameters:
* validation_db_path -- path where to store database
* crossref_path_to_file -- path to the file with CrossRef data
* semsch_path_to_files -- path to the folder with Semantic Scholar data  


#### 1. Indexing CrossRef data
##### Run from the project folder:
```sh
java -jar validation_db/build/libs/validation-1.0-all.jar crossref
```
The following options are available:
* filter-duplicates -- publication with doi, that already exists in the database, won't be stored (defautlt: false)
* start-from -- the number of first record to store(used for resuming)

##### For example:
```sh
java -jar validation_db/build/libs/validation-1.0-all.jar crossref --filter-duplicates=false --start-from=0
```

#### 2. Indexing Semantic Scholar data
##### Run from the project folder:
```sh
java -jar validation_db/build/libs/validation-1.0-all.jar semsch
```
The following options are available:
* filter-duplicates -- publication with doi, that already exists in the database, won't be stored (defautlt: true)
* with-doi -- store only records that have doi  

The number of the last processed archive with SemanticScholar data is saved in file `START.txt` and on the next launch processing will start from the archive number specified in this file

##### For example:
```sh
java -jar validation_db/build/libs/validation-1.0-all.jar semsch --filter-duplicates=true --with-doi=true
```

Downloading and processing old arxiv publications
-------------------------------------------------

### Bulid
```sh
./gradlew -p arxiv-s3 build
```

### Run data collector
Copy `ArxivS3config.properties` from `arxiv-s3` folder to `~/.preprint_server/`. Specify the following parameters:
* arxiv_pdf_path -- path where to store downloaded archives
* neo4j_url, neo4j_port, neo4j_user, neo4j_password


##### Run from the project folder:
```sh
java -jar arxiv-s3/build/libs/arxiv-s3-1.0-all.jar
```
The following options are available:
* download-only -- only downoload arcives without processing
* ref-extractor -- reference extractor to use. Valid arguments 'c' for CustomReferenceExtractor, 'g' for GrobidReferenceExtractor (default argument: 'c')
* validators -- list of validators to use. Valid arguments 'l' for LocalValidator, 'c' for CrossRefValidator, 'a' for ArxivValidator (defalut argument: 'la')
* mpd -- maximum parallel downloads(default argument: '10')
* threads -- maximum number of threads to use when extracting references(default argument: '-1', number of threads equal to the number of CPU cores will be used)

##### For example:
```sh
java -jar arxiv-s3/build/libs/arxiv-s3-1.0-all.jar --download-only --ref-extractor=c --validators=la --mpd=16 --threads=8
```

Download and process new arxiv publications
-------------------------------------------

### Build
```sh
./gradlew -p core build
```

### Run data collector
Copy `config.properties` from `core` folder to `~/.preprint_server/`. Specify the following parameters:
* grobid_home -- path to Grobid home folder
* neo4j_url, neo4j_port, neo4j_user, neo4j_password
* email -- specify email if you want to use CrossRefValidator. This email will only be used in requests to CrossRef api(in `mailto` parameter of the request)


##### Run from the project folder:
```sh
java -jar core/build/libs/collector-1.0-all.jar --from=YYYY-MM-DD
```
The following options are available:
* from -- the date to begin from in format `YYYY-MM-DD`(required)
* validators -- list of validators to use. Valid arguments 'l' for LocalValidator, 'c' for CrossRefValidator, 'a' for ArxivValidator (defalut argument: 'ca')

##### For example:
```sh
java -jar core/build/libs/collector-1.0-all.jar --from=2020-05-30 --validators=la
```

Benchmarking Extractors
--------------------------
Prerequisite: download and install Anaconda: https://docs.anaconda.com/anaconda/install/

1. Launch `benchmarkExtractors.kt` with desired `FILES_FOLDER` in order to test implemented extractors on files from
the folder. A `benchmark.csv` file will be produced in the `BENCHMARKS_FOLDER`.
2. Launch Jupyter notebook from the project folder:

   ```
   jupyter notebook
   ```

3. Open `benchmarks/benchmark.ipynb` and click `Cell > Run All` to get summary and visualizations of the comparison.
   
Benchmarking results
---------------------

The `CustomReferenceExtractor`(Grobid combinded with some heuristics) and `GrobidReferenceExtractor`(just Grobid wrapped in ReferenceExtractor interface) was tested on a set of 6228 preprints from `arxiv.org`. The best value to evaluate quality is the number of validated references, because validated references is used to create connections in the neo4j graph. Validation is finding one of the publication unique identifiers. In this case DOI or arxiv id. The validation was done with `LocalValidator` and `ArxivValidator`

| reference extractor      | average references | average validated | average time(ms) | processed files | total files |
|--------------------------|--------------------|-------------------|------------------|-----------------|-------------|
| CustomReferenceExtractor | 27.992             | 15.698            | 457              | 6082            | 6228        |
| GrobidReferenceExtractor | 28.238             | 15.707            | 640              | 6077            | 6228        |

Despite the fact that Grobid validated a little bit more references in average, when comparing validated references for each file separately I got slightly different results. `CustomReferenceExtractor` parsed 244 validated references that `GrobidReferenceExtractor` didn't parse. While `GrobidReferenceExtractor` parsed only 82 validated references that `CustomReferenceExtractor` didn't parse. This may happened because Grobid parsed reference several times(for example parsed main information and arxiv id as two different references). The advantage in the average amount of extracted references could be explained by the fact that Grobid parses a little bit more 'garbage'(= not references). But anyway this numbers are insignificant compared to the total amount of extracted references. The only major difference between two extractors is average time and as we can see `CustomReferenceExtractor` is faster