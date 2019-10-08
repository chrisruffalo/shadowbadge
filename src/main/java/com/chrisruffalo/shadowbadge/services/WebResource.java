package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.services.support.ThymeLeafStreamingOutput;
import com.chrisruffalo.shadowbadge.templates.TemplateEngineFactory;
import com.chrisruffalo.shadowbadge.web.Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class WebResource {

    @Context
    private HttpServletRequest servletRequest;

    @Context
    private HttpServletResponse servletResponse;

    @Context
    private ServletContext servletContext;

    private TemplateEngine engine;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void init() {
        // create engine with resolver
        this.engine = TemplateEngineFactory.INSTANCE.getTemplateEngine();
    }

    @GET
    @Path("index.html")
    @Produces(MediaType.TEXT_HTML)
    public Response indexHtml(
            @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        // create context
        final WebContext context = new WebContext(this.servletRequest, this.servletResponse, this.servletContext);
        context.setVariable("userid", ownerId);
        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/index.html", context)).build();
    }

    @GET
    @Path("help.html")
    @Produces(MediaType.TEXT_HTML)
    public Response helpHtml(
            @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        // create context
        final WebContext context = new WebContext(this.servletRequest, this.servletResponse, this.servletContext);
        context.setVariable("userid", ownerId);
        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/help.html", context)).build();
    }

    @GET
    @Path("downloads.html")
    @Produces(MediaType.TEXT_HTML)
    public Response downloadsHtml(
            @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        // create context
        final WebContext context = new WebContext(this.servletRequest, this.servletResponse, this.servletContext);
        context.setVariable("userid", ownerId);
        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/downloads.html", context)).build();
    }

}
