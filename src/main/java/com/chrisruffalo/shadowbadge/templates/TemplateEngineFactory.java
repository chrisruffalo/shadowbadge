package com.chrisruffalo.shadowbadge.templates;

import org.thymeleaf.TemplateEngine;

// I'd rather not do a singleton this way but I haven't gotten injection to work the way it should on Quarkus yet
public enum TemplateEngineFactory {

    INSTANCE;

    private final TemplateEngine engine;

    private TemplateEngineFactory() {
        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(new QuarkusThymeleafTemplateLoader());
    }

    public TemplateEngine getTemplateEngine() {
        return this.engine;
    }
}
