package com.chrisruffalo.shadowbadge.templates;

import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.templateresource.ITemplateResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class QuarkusTemplateResource implements ITemplateResource, ICacheEntryValidity {

    private ClassLoader classLoader;
    private String name;
    private String prefix;

    public QuarkusTemplateResource(final String name, final String prefix, final ClassLoader loader) {
        this.name = name;
        this.prefix = prefix;

        if (null == prefix) {
            this.prefix = "";
        } else {
            this.prefix = prefix;
        }

        if (!this.prefix.isEmpty() && !this.prefix.endsWith("/")) {
            this.prefix = this.prefix + "/";
        }

        this.classLoader = loader;
    }

    @Override
    public String getDescription() {
        return this.classLoader.getClass().getSimpleName() + ":" + this.prefix + this.name;
    }

    @Override
    public String getBaseName() {
        return this.name;
    }

    @Override
    public boolean exists() {
        return null != this.classLoader.getResource(this.prefix + this.name);
    }

    @Override
    public Reader reader() throws IOException {
        final InputStream ios = this.classLoader.getResourceAsStream(this.prefix + this.name);
        if (null == ios) {
            return null;
        }
        return new InputStreamReader(ios);
    }

    @Override
    public ITemplateResource relative(String s) {
       return new QuarkusTemplateResource(s, this.prefix, this.classLoader);
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    @Override
    public boolean isCacheStillValid() {
        return true;
    }
}
