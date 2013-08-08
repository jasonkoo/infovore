package com.ontology2.millipede.primitiveTriples;

import java.util.Iterator;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.ontology2.millipede.Codec;

public class PrimitiveTripleCodec implements Codec<PrimitiveTriple> {
    final Splitter tripleSplitter=Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().limit(3);
    final CharMatcher period=CharMatcher.is('.');

    @Override
    public String encode(PrimitiveTriple obj) {
        StringBuilder output=new StringBuilder();
        output.append(obj.subject);
        output.append("\t");
        output.append(obj.predicate);
        output.append("\t");
        output.append(obj.object);
        output.append(".");
        return output.toString();
    }

    @Override
    public PrimitiveTriple decode(String obj) {
        Iterator<String> parts=tripleSplitter.split(obj).iterator();
        String subject = parts.next();
        String predicate = parts.next();
        String object = trimObject(parts.next());
        return new PrimitiveTriple(subject,predicate,object);
    }

    private String trimObject(String that) {
        that=period.trimTrailingFrom(that); // nuke the period
        that=CharMatcher.WHITESPACE.trimTrailingFrom(that);
        return that;
    }

}
