package com.chrisruffalo.shadowbadge.services.support;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ThymeLeafStreamingOutput implements StreamingOutput {

    TemplateEngine engine;
    String template;
    IContext context;

    private ThymeLeafStreamingOutput() {

    }

    public ThymeLeafStreamingOutput(final TemplateEngine engine, final String template, final IContext context) {
        this();

        this.engine = engine;
        this.template = template;
        this.context = context;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        this.engine.process(this.template, this.context, new OutputStreamWriter(outputStream));
    }
}
