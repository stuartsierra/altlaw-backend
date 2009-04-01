package org.altlaw.jobs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.altlaw.util.BiasedCoin;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class FileSeqSample extends Configured implements Tool {

    private static final String SAMPLE_CONFIG = "org.altlaw.sample.proportion";

    public static class MyMap extends MapReduceBase
        implements Mapper<Text, BytesWritable, NullWritable, NullWritable> {

        private JobConf conf;
        private BiasedCoin coin;
        private Path workPath;

        public final void configure(JobConf conf) {
            this.conf = conf;
            workPath = FileOutputFormat.getWorkOutputPath(conf);
            double p = Double.parseDouble(conf.get(SAMPLE_CONFIG));
            this.coin = new BiasedCoin(p);
        }

        public final void map(Text filename, BytesWritable data,
                              OutputCollector<NullWritable, NullWritable> output,
                              Reporter reporter)
            throws IOException {

            if (coin.flip()) {
                writeFile(filename, data);
            } else {
                reporter.progress();
            }
        }

        private void writeFile(Text filenameText, BytesWritable data) throws IOException {
            String filename = filenameText.toString();
            byte[] bytes = data.get();
            int size = data.getSize();
            Path filePath = new Path(workPath, filename);
            FileSystem fs = FileSystem.get(conf);
            OutputStream out = fs.create(filePath);
            InputStream in = new ByteArrayInputStream(bytes, 0, size);
            try {
                IOUtils.copy(in, out);
            } finally {
                in.close();
                out.close();
            }
        }
    }

    

    public final int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf, FileSeqSample.class);

        if (args.length != 3) {
            System.err.println("Usage: FileSeqSample input_path output_path proportion");
            System.exit(1);
        }

        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        double proportion = Double.parseDouble(args[2]);

        job.setJobName("FileSeqSample");
        job.set(SAMPLE_CONFIG, Double.toString(proportion));

        // paths
        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        // classes
        job.setMapperClass(MyMap.class);
        job.setNumReduceTasks(0);

        // input
        job.setInputFormat(SequenceFileInputFormat.class);

        // output
        job.setOutputFormat(SequenceFileOutputFormat.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(NullWritable.class);

        // compression
        
        JobClient.runJob(job);
        return 0;
    }

    public static void main(String[] args) throws Exception {
        int r = ToolRunner.run(new Configuration(), new FileSeqSample(), args);
        System.exit(r);
    }
}
