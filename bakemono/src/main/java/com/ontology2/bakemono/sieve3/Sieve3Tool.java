package com.ontology2.bakemono.sieve3;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.ontology2.bakemono.Main;
import com.ontology2.bakemono.Main.IncorrectUsageException;
import com.ontology2.bakemono.jena.SPOTripleOutputFormat;
import com.ontology2.bakemono.mappers.pse3.PSE3Mapper;
import com.ontology2.bakemono.mapred.RealMultipleOutputs;
import com.ontology2.bakemono.mapred.RealMultipleOutputsMainOutputWrapper;
import com.ontology2.bakemono.primitiveTriple.PrimitiveTripleInputFormat;
import com.ontology2.bakemono.primitiveTriple.SPOPrimitiveTripleOutputFormat;
import com.ontology2.bakemono.tools.pse3Tool.PSE3Tool;
import com.ontology2.centipede.primitiveTriples.PrimitiveTriple;
import com.ontology2.bakemono.sieve3.Sieve3Configuration.Rule;

/**
 * Sieve3Tool processes one Triple at a time,  meaning that it runs as a pure
 * Map without an associated reduce phase.
 * 
 * The is a general tool with the specialization being the configuration of a list of
 * (triplePredicate,hdfsPath) pairs that are processed as a series of rules.  In sequence,
 * the triple will be tested against every predicate and if it matches,  that triple will be sent to that path.
 * 
 * Once a triple matches a rule we are through with it and we move to the next triple;
 * if a triple matches no rules it falls out to the default output stream.  If the hdfsPath is null for
 * a matching predicate,  the system will ignore that triple entirely.
 *
 *
 */
public class Sieve3Tool implements Tool {
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
    public int run(String[] arg0) throws Exception {
        try {
            PeekingIterator<String> a=Iterators.peekingIterator(Iterators.forArray(arg0));    
            if (!a.hasNext())
                usage();
            
            String input=a.next();
            
            if (!a.hasNext())
                usage();
            
            Path output=new Path(a.next());
            conf.set("mapred.compress.map.output", "true");
            conf.set("mapred.output.compression.type", "BLOCK"); 
            conf.set("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.GzipCodec");
            
            Job job=new Job(conf,"pse3");
            FileInputFormat.addInputPath(job, new Path(input));
            
            job.setJarByClass(Sieve3Tool.class);
            job.setMapperClass(Sieve3Mapper.class);
            job.setNumReduceTasks(0);
            
            Path otherPath=new Path(output,"other");
            FileOutputFormat.setOutputPath(job, otherPath);
            
            FileOutputFormat.setCompressOutput(job, true);
            FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);
            
            Sieve3Configuration sieve3Config = generateConfiguration();
            for(Rule r:sieve3Config.getRules())
                RealMultipleOutputs.addNamedOutput(job,
                        r.getOutputName(),
                        new Path(output,r.getOutputName()),
                        SPOPrimitiveTripleOutputFormat.class, 
                        PrimitiveTriple.class, 
                        LongWritable.class);
            
            job.setOutputFormatClass(RealMultipleOutputsMainOutputWrapper.class);
            RealMultipleOutputsMainOutputWrapper.setRootOutputFormat(job, SPOPrimitiveTripleOutputFormat.class);
            return job.waitForCompletion(true) ? 0 :1;
        } catch(Main.IncorrectUsageException iue) {
            return 2;
        }
    }
    
    private static void usage() throws IncorrectUsageException {
        throw new Main.IncorrectUsageException("incorrect arguments");
    };
    
    public static Sieve3Configuration generateConfiguration() {
        return new Sieve3Configuration(
                new Rule("a", matchesA())
        );
    }

    private static Predicate<PrimitiveTriple> matchesA() {
        return new Predicate<PrimitiveTriple>() {
            @Override public boolean apply(PrimitiveTriple input) {
                return input.predicate.equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
            }
        };
    };
}
