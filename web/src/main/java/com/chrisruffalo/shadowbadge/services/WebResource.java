package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/")
public class WebResource extends BaseResource {
    @Context
    HttpServletRequest servletRequest;

    @Context
    HttpServletResponse servletResponse;

    @Context
    ServletContext servletContext;

    @Inject
    Template index;

    @Inject
    Template help;

    @Inject
    Template downloads;

    @PostConstruct
    public void init() {
    }

    @GET
    @Path("index.html")
    @Produces(MediaType.TEXT_HTML)
    @PermitAll
    public TemplateInstance indexHtml() throws ShadowbadgeException {
        return this.index.data("identity", this.getIdentity());
    }

    @GET
    @Path("help.html")
    @Produces(MediaType.TEXT_HTML)
    @PermitAll
    public TemplateInstance helpHtml() throws ShadowbadgeException {
        return this.help.data("identity", this.getIdentity());
    }

    @GET
    @Path("downloads.html")
    @Produces(MediaType.TEXT_HTML)
    @PermitAll
    public TemplateInstance downloadsHtml() throws ShadowbadgeException {
        return this.downloads.data("identity", this.getIdentity());
    }

    // simple health service
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok("ok").build();
    }

}
