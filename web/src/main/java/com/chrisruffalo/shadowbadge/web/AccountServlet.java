package com.chrisruffalo.shadowbadge.web;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(
    name = "Account Redirect Servlet",
    urlPatterns = "/account"
)
public class AccountServlet extends RedirectingServlet {

    private static final String UNCONFIGURED = "shadowbadge.account.url.unconfigured";

    @ConfigProperty(name = "shadowbadge.account.url", defaultValue = UNCONFIGURED)
    String accountUrl;


    @Override
    protected String href(ServletRequest request) {
        return accountUrl;
    }

    @Override
    protected String unconfigured() {
        return UNCONFIGURED;
    }
}
