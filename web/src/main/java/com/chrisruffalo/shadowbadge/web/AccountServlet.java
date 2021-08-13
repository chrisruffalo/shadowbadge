package com.chrisruffalo.shadowbadge.web;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@WebServlet(
    name = "Account Redirect Servlet",
    urlPatterns = "/account"
)
public class AccountServlet implements Servlet {

    private static final String INDEX = "/index.html";

    @ConfigProperty(name = "shadowbadge.account.url", defaultValue = "")
    Optional<String> accountUrl;

    @Inject
    Redirection redirection;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        if (servletResponse instanceof HttpServletResponse ) {
            final HttpServletResponse response = (HttpServletResponse)servletResponse;
            if (accountUrl.isPresent() && !accountUrl.get().isEmpty()) {
                response.sendRedirect(response.encodeRedirectURL(accountUrl.get()));
            } else {
                response.sendRedirect(response.encodeRedirectURL(this.redirection.getRedirect(INDEX, servletRequest)));
            }
        }
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
