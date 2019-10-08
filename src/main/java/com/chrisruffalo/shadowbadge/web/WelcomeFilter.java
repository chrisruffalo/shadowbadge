package com.chrisruffalo.shadowbadge.web;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Sets up to handle welcome pages
 */
@WebFilter(
    filterName = "WelcomeFilter",
    dispatcherTypes = DispatcherType.REQUEST,
    value = {
        "/*"
    }
)
public class WelcomeFilter implements Filter {

    @ConfigProperty(name = "shadowbadge.log.headers", defaultValue = "false")
    String logHeaders;

    private final String ROOT = "/";

    private final String REDIRECT = "/index.html";

    @Inject
    Redirection redirection;

    private Logger logger;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest request = (HttpServletRequest)servletRequest;

            // log headers when the setting is enabled
            if ("true".equalsIgnoreCase(logHeaders)) {
                final Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    final String headerName = headerNames.nextElement();
                    logger.info("{} = {}", headerName, request.getHeader(headerName));
                }
            }

            // only handle GET requests
            if (!"GET".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            final String requestUri = request.getRequestURI();

            if (null == requestUri || requestUri.isEmpty() || requestUri.equalsIgnoreCase(ROOT)) {
               this.redirect(REDIRECT, servletRequest, servletResponse);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void redirect(final String to, final ServletRequest servletRequest, final ServletResponse servletResponse) throws IOException {
        if (servletResponse instanceof HttpServletResponse) {
            final String url = this.redirection.getRedirect(to, servletRequest);
            this.logger.info("Redirect to: {}", url);
            ((HttpServletResponse)servletResponse).sendRedirect(((HttpServletResponse) servletResponse).encodeRedirectURL(url));
        }
    }
}
