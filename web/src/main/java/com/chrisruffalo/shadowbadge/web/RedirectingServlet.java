package com.chrisruffalo.shadowbadge.web;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class RedirectingServlet implements Servlet {

    protected static final String INDEX = "/index.html";

    protected abstract String href(ServletRequest request);

    protected abstract String unconfigured();

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
        if (servletResponse instanceof HttpServletResponse) {
            final HttpServletResponse response = (HttpServletResponse)servletResponse;
            final String href = this.href(servletRequest);
            if (!href.isEmpty() && !this.unconfigured().equals(href)) {
                response.sendRedirect(response.encodeRedirectURL(href));
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
