package com.ontology2.bakemono.entityCentric;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.ontology2.bakemono.configuration.HadoopTool;
import com.ontology2.bakemono.diffFacts.DiffFactsOptions;
import com.ontology2.centipede.parser.OptionParser;
import com.ontology2.centipede.shell.UsageException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;

@HadoopTool("extractIsA")
public class ExtractIsATool implements Tool {
    @Autowired
    ApplicationContext applicationContext;
    private Configuration conf;

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    @Override
    public void setConf(Configuration arg0) {
        this.conf=arg0;
    }

    @Override
    public int run(String[] strings) throws Exception {
        ExtractIsAOptions options = extractOptions(strings);
        configureOutputCompression();

        List<String> nodes=Lists.newArrayList();
        for(String link:options.type)
            nodes.add("<"+link+">");
        conf.set(EntityIsAReducer.TYPE_LIST, Joiner.on(",").join(nodes));

        Job job=new Job(conf,"extractIsA");
        job.setJarByClass(this.getClass());
        job.setMapperClass(EntityCentricMapper.class);
        job.setReducerClass(EntityIsAReducer.class);

        if(options.reducerCount<1) {
            options.reducerCount=1;
        }

        job.setNumReduceTasks(options.reducerCount);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        for(String path: options.input) {
            FileInputFormat.addInputPath(job, new Path(path));
        }

        FileOutputFormat.setOutputPath(job, new Path(options.output));
        FileOutputFormat.setCompressOutput(job,true);
        FileOutputFormat.setOutputCompressorClass(job,GzipCodec.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    void configureOutputCompression() {
        conf.set("mapred.compress.map.output", "true");
        conf.set("mapred.output.compression.type", "BLOCK");
        conf.set("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.GzipCodec");
    }

    ExtractIsAOptions extractOptions(String[] strings) throws IllegalAccessException {
        OptionParser parser=new OptionParser(ExtractIsAOptions.class);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(parser);

        ExtractIsAOptions options=(ExtractIsAOptions) parser.parse(Lists.newArrayList(strings));
        if (options.input.isEmpty())
            throw new UsageException("You did not specify a value for -input");

        if (options.output==null || options.output.isEmpty())
            throw new UsageException("You did not specify a value for -output");

        if (options.type.isEmpty())
            throw new UsageException("You did not specify a value for -type");
        return options;
    }
}