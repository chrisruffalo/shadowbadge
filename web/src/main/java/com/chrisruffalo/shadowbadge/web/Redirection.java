package com.chrisruffalo.shadowbadge.web;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;
import javax.servlet.ServletRequest;

@Singleton
public class Redirection {

    @ConfigProperty(name = "shadowbadge.external.url", defaultValue = "http://localhost:8081")
    String externalUrl;

    public String getRedirect(final String to, ServletRequest request) {
        if (null == to || to.isEmpty()) {
            return "";
        }

        if (null == this.externalUrl || this.externalUrl.isEmpty()) {
            return to;
        }

        if (this.externalUrl.endsWith("/") || to.startsWith("/")) {
            return this.externalUrl + to;
        }

        return this.externalUrl + "/" + to;
    }
}
