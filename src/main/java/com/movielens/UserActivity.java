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

public class UserActivity {
    public static class UserMapper extends Mapper<Object, Text, Text, Text> {
        private Text userId = new Text();
        private Text outValue = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty() || line.startsWith("userId") || line.startsWith("\"userId\"")) return;
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                String uid = parts[0].replaceAll("^\"|\"$", "");
                String ratingStr = parts[2].replaceAll("^\"|\"$", "");
                try {
                    Double.parseDouble(ratingStr);
                    userId.set(uid);
                    outValue.set("1," + ratingStr);
                    context.write(userId, outValue);
                } catch (NumberFormatException e) {}
            }
        }
    }

    public static class UserReducer extends Reducer<Text, Text, Text, Text> {
        private Text result = new Text();
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            long count = 0;
            double sum = 0.0;
            for (Text val : values) {
                String[] parts = val.toString().split(",");
                count += Long.parseLong(parts[0]);
                sum += Double.parseDouble(parts[1]);
            }
            if (count > 0) {
                result.set(count + "," + String.format("%.2f", sum/count));
                context.write(key, result);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "User Activity Job 3");
        job.setJarByClass(UserActivity.class);
        job.setMapperClass(UserMapper.class);
        job.setReducerClass(UserReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
