package com.chrisruffalo.shadowbadge.web;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;

@Singleton
public class Redirection {

    private static final String UNCONFIGURED_EXTERNAL_URL = "shadowbadge.external.url.unconfigured";
    private static final int STANDARD_HTTP_PORT = 80;
    private static final int STANDARD_HTTPS_PORT = 443;

    @ConfigProperty(name = "shadowbadge.external.url", defaultValue = UNCONFIGURED_EXTERNAL_URL)
    String externalUrl;

    @PostConstruct
    public void init() {
        LoggerFactory.getLogger(this.getClass()).info("Using external URL: {}", externalUrl);
    }

    public String getRedirect(final String to, ServletRequest request) {
        // don't redirect anywhere if to isn't specified
        if (null == to || to.isEmpty()) {
            return "";
        }

        // here we decide if the value has been configured and if it has not we try and build the path from the server name
        String redirectBase = externalUrl;
        if (UNCONFIGURED_EXTERNAL_URL.equals(externalUrl)) {
            if (("http".equals(request.getScheme()) && request.getServerPort() != STANDARD_HTTP_PORT)
            || ("https".equals(request.getScheme()) && request.getServerPort() != STANDARD_HTTPS_PORT)) {
                redirectBase = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
            } else {
                redirectBase = String.format("%s://%s", request.getScheme(), request.getServerName());
            }
        }

        // can't do a fully qualified redirect if no external url is provided
        if (null == redirectBase || redirectBase.isEmpty()) {
            return to;
        }

        // if they both have a url that makes "//" then only return "/"
        if (redirectBase.endsWith("/") && to.startsWith("/")) {
            return redirectBase + to.substring(1);
        }

        // otherwise only one of them has the slash so it is fine
        if (redirectBase.endsWith("/") || to.startsWith("/")) {
            return redirectBase + to;
        }

        // nobody has a slash so return both
        return redirectBase + "/" + to;
    }
}
