package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.web.Constants;
import io.quarkus.qute.TemplateInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Qute templates
     */
    private static class Templates {
        public static native TemplateInstance index();
        public static native TemplateInstance help();
        public static native TemplateInstance downloads();
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void init() {
    }

    @GET
    @Path("index.html")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance indexHtml(
            @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        return Templates.index().data("userid", ownerId);
    }

    @GET
    @Path("help.html")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance helpHtml(
            @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        return Templates.help().data("userid", ownerId);
    }

    @GET
    @Path("downloads.html")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance downloadsHtml(
            @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        return Templates.downloads().data("userid", ownerId);
    }

    // simple health service
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok().build();
    }

}
