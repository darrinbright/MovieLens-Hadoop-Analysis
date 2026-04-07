# MovieLens Big Data Analytics with Apache Hadoop

This project performs exploratory data analysis on the MovieLens 20M dataset using **Apache Hadoop** and **Pure Java MapReduce**. It processes over 24 million records across 6 datasets to extract insights, which are then visualized using a **Streamlit Dashboard**.

---

## 1. Architecture Overview

To process massive amounts of data efficiently, this project uses a three-tier Big Data architecture:

### Tier 1: Storage Layer (HDFS)
The raw datasets (amounting to ~1 GB of CSV data) are not processed on the local file system. Instead, they are distributed across the **Hadoop Distributed File System (HDFS)**.
- **Data location**: `/user/darrin/movielens/*.csv`
- HDFS chunks the data into blocks, enabling parallel processing.

### Tier 2: Computation Layer (Java MapReduce on YARN)
This is where the heavy lifting occurs. The **YARN ResourceManager** allocates CPU and memory containers to run **6 distinct MapReduce Jobs** written in pure Java.
- **Mapper Phase**: Parses the distributed CSV splits concurrently, filtering headers/errors and emitting `(Key, Value)` pairs.
- **Reducer Phase**: Aggregates the emitted values across the cluster to generate finalized insights.
- The results are dumped back into HDFS as partitioned output files.

### Tier 3: Presentation Layer (Streamlit)
Since Hadoop's output is structured TSV text distributed across HDFS, we export the aggregated results back to the local file system (`/results/`). A **Python Streamlit Application** then reads these lightweight TSV files to render interactive metrics, pie charts, and bar graphs.

---

## 2. MapReduce Algorithms Explained

This project executes 6 separate Java MapReduce jobs, each analyzing a different entity within the MovieLens dataset.

### Job 1: Movie Ratings Analytics (`MovieRatings.java`)
- **Input**: `rating.csv` (20 million rows)
- **Mapper**: Extracts `movieId` as Key, and `(1, rating)` as Value.
- **Reducer**: Sums the counts to find total ratings per movie, and calculates the exact average rating.
- **Output**: `movieId \t count,avg_rating`

### Job 2: Genre Popularity (`GenrePopularity.java`)
- **Input**: `movie.csv`
- **Mapper**: Parses complex pipe-delimited genres (`Action|Adventure|Sci-Fi`) and emits `(genre, 1)` for each distinct genre in a movie.
- **Reducer**: Sums all 1s to find the total frequency of every genre across cinema history.
- **Output**: `genre \t count`

### Job 3: User Activity Profiling (`UserActivity.java`)
- **Input**: `rating.csv` 
- **Mapper**: Extracts `userId` as Key, and `(1, rating)` as Value.
- **Reducer**: Finds the most voracious movie watchers by counting total movies rated per user and their personal average rating bias.
- **Output**: `userId \t count,avg_rating`

### Job 4: TMDB Link Discovery (`LinkDiscovery.java`)
- **Input**: `link.csv`
- **Mapper**: Extracts the `tmdbId` identifier.
- **Reducer**: Aggregates them to ensure valid linkage maps for external API usage.
- **Output**: `tmdbId \t count`

### Job 5: Genome Tag Frequency (`TagFrequency.java`)
- **Input**: `genome_scores.csv`
- **Mapper**: Extracts `tagId` from machine-learning generated genome scores.
- **Reducer**: Measures how often specific genome tags are applied to movies.
- **Output**: `tagId \t frequency`

### Job 6: Alphabetical Tag Grouping (`AlphaTagGrouping.java`)
- **Input**: `genome_tags.csv`
- **Mapper**: Extracts the very first letter of every user-generated tag.
- **Reducer**: Groups and counts them to show the alphabetical distribution of the tag taxonomy.
- **Output**: `letter \t count`

---

## 3. Execution Pipeline

If you were to run this project from a pristine cluster, the flow is as follows:

1. **Ingestion**: 
   ```bash
   hdfs dfs -put *.csv /user/darrin/movielens/
   ```
2. **Compilation**: 
   The Java MapReduce source code is compiled directly against the Hadoop Classpath.
   ```bash
   javac -cp \$(hadoop classpath) -d bin src/main/java/com/movielens/*.java
   jar -cvf movielens_analytics.jar -C bin .
   ```
3. **Execution**: 
   We trigger YARN to crunch the data on the cluster:
   ```bash
   hadoop jar movielens_analytics.jar com.movielens.MovieRatings /user/darrin/movielens/rating.csv /user/darrin/movielens/java_out1
   ```
4. **Extraction**: 
   The distributed HDFS parts are merged and brought to the local disk.
   ```bash
   hdfs dfs -getmerge /user/darrin/movielens/java_out1 results/job1_final.tsv
   ```
5. **Visualization**: 
   The Streamlit dashboard binds to the `results/` directory to display the final polished UI.
   ```bash
   streamlit run dashboard.py
   ```
