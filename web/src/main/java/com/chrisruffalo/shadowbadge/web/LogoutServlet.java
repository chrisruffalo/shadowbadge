package com.chrisruffalo.shadowbadge.web;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(
    name = "Logout Redirect Servlet",
    urlPatterns = "/logout"
)
public class LogoutServlet extends RedirectingServlet {

    private static final String UNCONFIGURED = "shadowbadge.keycloak.url.unconfigured";

    @ConfigProperty(name = "shadowbadge.keycloak.url", defaultValue = UNCONFIGURED)
    String keycloakUrl;

    @ConfigProperty(name = "shadowbadge.keycloak.realm", defaultValue = "")
    String realm;

    @Inject
    Redirection redirection;

    @Override
    protected String href(ServletRequest request) {
        final String redirectTarget = redirection.getRedirect(INDEX, request);
        String encodedRedirect = "";
        try {
            encodedRedirect = URLEncoder.encode(redirectTarget, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LoggerFactory.getLogger(this.getClass()).error("{}", e.getMessage(), e);
        }
        return String.format("%s/auth/realms/%s/protocol/openid-connect/logout?redirect_uri=%s", keycloakUrl, realm, encodedRedirect);
    }

    @Override
    protected String unconfigured() {
        return UNCONFIGURED;
    }

}
