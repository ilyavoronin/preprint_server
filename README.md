Downloading and processing old arxiv publications
-------------------------------------------------

## Downloading data to index
1. Download latest crossref metadata dump from archive.org. The latest for now is available [here](https://archive.org/download/crossref_doi_dump_201909) (~45GB)
2. Download latest data from Semantic Scholar. Follow instructions from [here](http://s2-public-api-prod.us-west-2.elasticbeanstalk.com/corpus/download) (~115GB)

## Bulid data collector
```sh
./gradlew -p arxiv-s3 build
```

## Run data collector
Copy `validationDBconfig.properties` from `validation_db` folder to `~/.preprint_server/`. Specify the following parameters:
* validation_db_path -- path where to store database
* crossref_path_to_file -- path to the file with CrossRef data
* semsch_path_to_files -- path to the folder with Semantic Scholar data

```sh
java -jar arxiv-s3/build/libs/arxiv-s3-1.0-all.jar
```
The following options is available:
* download-only -- only downoload arcives without processing
* ref-extractor -- reference extractor to use. Valid arguments 'c' for CustomReferenceExtractor, 'g' for GrobidReferenceExtractor (default argument: 'c')
* validators -- list of validators to use. Valid arguments 'l' for LocalValidator, 'c' for CrossRefValidator, 'a' for ArxivValidator (defalut argument: 'la')
* mpd -- maximum parallel downloads(default argument: '10')
* threads -- maximum number of threads to use when extracting references(default argument: '-1', number of threads equal to the number of CPU cores will be used)

For example:
```sh
java -jar arxiv-s3/build/libs/arxiv-s3-1.0-all.jar --download-only --ref-extractor=c --validators=la --mpd=16 --threads=8
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
   
    