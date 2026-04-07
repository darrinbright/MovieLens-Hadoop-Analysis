package com.movielens;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

// Job 1: Find Most Rated Movies and Average Rating in Java

public class MovieRatings {

    public static class RatingsMapper extends Mapper<Object, Text, Text, Text> {
        private Text movieId = new Text();
        private Text outValue = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty() || line.startsWith("userId") || line.startsWith("\"userId\"")) {
                return;
            }

            String[] parts = line.split(",");
            if (parts.length >= 3) {
                String mid = parts[1].replaceAll("^\"|\"$", "");
                String ratingStr = parts[2].replaceAll("^\"|\"$", "");
                
                try {
                    Double.parseDouble(ratingStr);
                    movieId.set(mid);
                    outValue.set("1," + ratingStr);
                    context.write(movieId, outValue);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    public static class RatingsReducer extends Reducer<Text, Text, Text, Text> {
        private Text result = new Text();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            long count = 0;
            double sum = 0.0;
            for (Text val : values) {
                String[] parts = val.toString().split(",");
                if (parts.length >= 2) {
                    try {
                        count += Long.parseLong(parts[0]);
                        sum += Double.parseDouble(parts[1]);
                    } catch (NumberFormatException e) { }
                }
            }
            if (count > 0) {
                double avg = sum / count;
                result.set(count + "," + String.format("%.2f", avg));
                context.write(key, result);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Movie Ratings Job 1");
        job.setJarByClass(MovieRatings.class);
        job.setMapperClass(RatingsMapper.class);
        job.setReducerClass(RatingsReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
