package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.dal.BadgeRepo;
import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.model.Badge;
import com.chrisruffalo.shadowbadge.model.BadgeInfo;
import com.chrisruffalo.shadowbadge.model.IconType;
import com.chrisruffalo.shadowbadge.services.support.Secure;
import com.chrisruffalo.shadowbadge.services.support.ThymeLeafStreamingOutput;
import com.chrisruffalo.shadowbadge.templates.TemplateEngineFactory;
import com.chrisruffalo.shadowbadge.web.Constants;
import com.chrisruffalo.shadowbadge.web.Redirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/badges")
public class BadgeResource {

    @Inject
    Redirection redirection;

    @Inject
    BadgeRepo badges;

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

    @Secure
    @PUT
    @POST
    @Path("secure/{badgeId}/claim")
    @Produces(MediaType.APPLICATION_JSON)
    public Response claim(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        return Response.ok(badges.claim(badgeId, ownerId)).build();
    }

    @Secure
    @GET // semantically dubious
    @Path("secure/{badgeId}/claimAction")
    @Produces(MediaType.TEXT_HTML)
    public Response claimHtml(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {

        try {
            final Badge badge = badges.claim(badgeId, ownerId);
        } catch (ShadowbadgeException e) {
            // this is "better-ish"
            return this.home(badgeId, ownerId, e);
        }

        return Response.seeOther(URI.create(redirection.getRedirect(String.format("/badges/secure/%s/detail.html", badgeId), this.servletRequest))).build();
    }

    @Secure
    @GET
    @Path("secure/home.html")
    @Produces(MediaType.TEXT_HTML)
    public Response home(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        return this.home(badgeId, ownerId, null);
    }

    private Response home(final String badgeId, final String ownerId, final Exception error) {
        // create context
        final WebContext context = new WebContext(this.servletRequest, this.servletResponse, this.servletContext);
        context.setVariable("userid", ownerId);

        if (error != null) {
            context.setVariable("error", true);
            context.setVariable("errorMsg", error.getMessage());
        } else {
            context.setVariable("error", false);
        }

        // get by owner id
        final List<Badge> list = badges.listForOwner(ownerId);
        context.setVariable("badges", list);

        // return streaming response
        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/home.html", context)).build();
    }

    @GET
    @Path("{badgeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response detail(@PathParam("badgeId") final String badgeId) throws ShadowbadgeException {
        this.logger.info("Got request for json info badge='{}'", badgeId);
        final Badge badge = badges.getByBadgeId(badgeId);
        if (badge == null) {
            return Response.noContent().build();
        }
        return Response.ok(badge).build();
    }

    @Secure
    @GET
    @Path("secure/{badgeId}/detail.html")
    @Produces(MediaType.TEXT_HTML)
    public Response detailHtml(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId,
        @HeaderParam(Constants.X_AUTH_EMAIL) final String email
    ) throws ShadowbadgeException {
        // create context
        final WebContext context = new WebContext(this.servletRequest, this.servletResponse, this.servletContext);
        context.setVariable("userid", ownerId);
        context.setVariable("email", email);
        context.setVariable("badgeId", badgeId);

        final Badge badge = badges.getByBadgeId(badgeId);
        if (null == badge) {
            return Response.noContent().build();
        }

        final BadgeInfo info = badge.getInfo();
        if (null != info) {
            context.setVariable("info", info);
        } else {
            context.setVariable("info", new BadgeInfo());
        }

        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/detail.html", context)).build();
    }

    @Secure
    @PUT
    @POST
    @Path("secure/{badgeId}/update")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId, final BadgeInfo newInfo) throws ShadowbadgeException {
        return Response.ok(badges.updateInfo(badgeId, ownerId, newInfo)).build();
    }

    @Secure
    @DELETE
    @Path("secure/{badgeId}/unclaim")
    @Produces(MediaType.TEXT_PLAIN)
    public Response unclaim(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        this.badges.unclaim(badgeId, ownerId);
        // if all goes ok, return home
        return Response.ok("badge deleted").build();
    }

    @Secure
    @GET
    @Path("secure/{badgeId}/unclaimAction")
    public Response unclaimAction(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        this.badges.unclaim(badgeId, ownerId);
        // if all goes ok, return home
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/home.html", this.servletRequest))).build();
    }

    @Secure
    @PUT
    @POST
    @Path("secure/{badgeId}/updateForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateForm(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId,
        @FormParam("displayName") final String displayName,
        @FormParam("heading") final String heading,
        @FormParam("title") final String title,
        @FormParam("group") final String group,
        @FormParam("location") final String location,
        @FormParam("tagline") final String tagline,
        @FormParam("icon") final String iconString
    ) throws RepositoryException {
        BadgeInfo info = badges.info(badgeId);
        if (null == info) {
            info = new BadgeInfo();
        }
        info.setDisplayName(displayName);
        info.setHeading(heading);
        info.setGroup(group);
        info.setTitle(title);
        info.setLocation(location);
        info.setTagline(tagline);

        // try and load the icon from the enum, if it fails for any reason use the default icon
        try {
            final IconType icon = IconType.valueOf(iconString);
            info.setIcon(icon);
        } catch (Exception ex) {
            info.setIcon(IconType.RED_HAT);
        }

        // update individual
        badges.updateInfo(badgeId, ownerId, info);

        // if all goes ok, return home
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/home.html", this.servletRequest))).build();
    }
}
