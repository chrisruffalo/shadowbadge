package com.chrisruffalo.shadowbadge.templates;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ITemplateResource;

import java.util.Map;

public class QuarkusThymeleafTemplateLoader implements ITemplateResolver {

    private static final String[] PREFIXES = {
      "",
      "META-INF/",
      "classes/",
      "META-INF/classes/"
    };

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public Integer getOrder() {
        return 1;
    }

    @Override
    public TemplateResolution resolveTemplate(IEngineConfiguration configuration, String ownerTemplate, String template, Map<String, Object> templateResolutionAttributes) {

        ITemplateResource resource = null;

        for (final String prefix : PREFIXES) {
            if (null != QuarkusTemplateResource.class.getClassLoader().getResource(prefix + template)) {
                resource = new QuarkusTemplateResource(template, prefix, QuarkusThymeleafTemplateLoader.class.getClassLoader());
            } else if (null != this.getClass().getClassLoader().getResource(prefix + template)) {
                resource = new QuarkusTemplateResource(template, prefix, this.getClass().getClassLoader());
            } else if (null != Thread.currentThread().getContextClassLoader().getResource(prefix + template)) {
                resource = new QuarkusTemplateResource(template, prefix, Thread.currentThread().getContextClassLoader());
            }

            if (null != resource) {
                break;
            }
        }

        if (resource == null) {
            return null;
        }

        return new TemplateResolution(
                resource,
                TemplateMode.HTML,
                (ICacheEntryValidity)resource
        );
    }
}
