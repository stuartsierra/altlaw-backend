package org.altlaw.jobs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.altlaw.util.BiasedCoin;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Writable;
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

public class SeqFileSampler extends Configured implements Tool {

    private static final String SAMPLE_CONFIG = "org.altlaw.sample.proportion";

    public static class MyMap extends MapReduceBase
        implements Mapper<Writable, Writable, Writable, Writable> {

        private JobConf conf;
        private BiasedCoin coin;

        public final void configure(JobConf conf) {
            this.conf = conf;
            double p = Double.parseDouble(conf.get(SAMPLE_CONFIG));
            this.coin = new BiasedCoin(p);
        }

        public final void map(Writable key, Writable value,
                              OutputCollector<Writable, Writable> output,
                              Reporter reporter)
            throws IOException {

            if (coin.flip()) {
                output.collect(key, value);
            } else {
                reporter.progress();
            }
        }
    }

    

    public final int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf, SeqFileSampler.class);

        /* Parse Command Line */
        CommandLine cmd = parseOptions(args);
        Path in = new Path(cmd.getOptionValue("i"));
        Path out = new Path(cmd.getOptionValue("o"));
        Class keyClass = Class.forName(cmd.getOptionValue("k"));
        Class valueClass = Class.forName(cmd.getOptionValue("v"));
        double proportion = Double.parseDouble(cmd.getOptionValue("s"));

        job.setJobName("SeqFileSampler");
        job.set(SAMPLE_CONFIG, Double.toString(proportion));

        // paths
        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        // classes
        job.setMapperClass(MyMap.class);
        job.setReducerClass(IdentityReducer.class);
        //job.setNumReduceTasks(0);

        // input
        job.setInputFormat(SequenceFileInputFormat.class);

        // output
        job.setOutputFormat(SequenceFileOutputFormat.class);
        job.setOutputKeyClass(keyClass);
        job.setOutputValueClass(valueClass);

        // compression
        
        JobClient.runJob(job);
        return 0;
    }

    public static void main(String[] args) throws Exception {
        int r = ToolRunner.run(new Configuration(), new SeqFileSampler(), args);
        System.exit(r);
    }

    private static CommandLine parseOptions(String[] args) {
        CommandLineParser parser = new PosixParser();
        Options opts = makeOptions();
        try {
            return parser.parse(opts, args);
        } catch (ParseException e) {
            System.err.println("Command line error: " + e.toString());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SeqFileSampler", opts);
            System.exit(1);
            return null;  // keep compiler happy
        }
    }

    private static Options makeOptions() {
        Options opts = new Options();
        opts.addOption(OptionBuilder.hasArg()
                       .withDescription("input path (required)")
                       .isRequired()
                       .create("i"));
        opts.addOption(OptionBuilder.hasArg()
                       .withDescription("output path (required)")
                       .isRequired()
                       .create("o"));
        opts.addOption(OptionBuilder.hasArg()
                       .withDescription("sample proportion (required)")
                       .isRequired()
                       .create("s"));
        opts.addOption(OptionBuilder.hasArg()
                       .withDescription("key class (required)")
                       .isRequired()
                       .create("k"));
        opts.addOption(OptionBuilder.hasArg()
                       .withDescription("value class (required)")
                       .isRequired()
                       .create("v"));
        return opts;
    }
}
