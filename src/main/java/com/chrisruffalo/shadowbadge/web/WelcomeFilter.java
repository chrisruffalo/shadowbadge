package com.chrisruffalo.shadowbadge.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final String ROOT = "/";

    private final String REDIRECT = "/index.html";

    private Logger logger;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest request = (HttpServletRequest)servletRequest;

            // only handle GET requests
            if (!"GET".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            final String requestUri = request.getRequestURI();

            if (null == requestUri || requestUri.isEmpty() || requestUri.equalsIgnoreCase(ROOT)) {
               this.redirect(REDIRECT, servletResponse);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void redirect(final String to, final ServletResponse servletResponse) throws IOException {
        this.logger.info("Welcome redirect to: {}", to);
        if (servletResponse instanceof HttpServletResponse) {
            ((HttpServletResponse)servletResponse).sendRedirect(((HttpServletResponse) servletResponse).encodeRedirectURL(to));
        }
    }
}
