package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.dal.BadgeRepo;
import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.model.Badge;
import com.chrisruffalo.shadowbadge.model.BadgeInfo;
import com.chrisruffalo.shadowbadge.services.support.Secure;
import com.chrisruffalo.shadowbadge.services.support.ThymeLeafStreamingOutput;
import com.chrisruffalo.shadowbadge.templates.QuarkusThymeleafTemplateLoader;
import com.chrisruffalo.shadowbadge.web.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/badges")
public class BadgeResource {

    @Inject
    BadgeRepo badges;

    private TemplateEngine engine;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void init() {
        // create engine with resolver
        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(new QuarkusThymeleafTemplateLoader());
    }

    @Secure
    @PUT
    @POST
    @Path("secure/{badgeId}/claim")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response claim(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        return Response.ok(badges.claim(badgeId, ownerId)).build();
    }

    @Secure
    @GET // semantically dubious
    @Path("secure/{badgeId}/claim.html")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public Response claimHtml(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        // create context
        final Context context = new Context();
        context.setVariable("badgeid", badgeId);
        context.setVariable("userid", ownerId);

        try {
            final Badge badge = badges.claim(badgeId, ownerId);
            context.setVariable("claimed", badge);
            context.setVariable("error", false);
        } catch (ShadowbadgeException e) {
            // this is a claim error
            context.setVariable("error", true);
            context.setVariable("errorText", e.getMessage());
        }

        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/claim.html", context)).build();
    }

    @Secure
    @GET
    @Path("secure/home.html")
    @Produces(MediaType.TEXT_HTML)
    public Response home(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId,
        @HeaderParam(Constants.X_AUTH_EMAIL) final String email
    ) throws ShadowbadgeException {
        // create context
        final Context context = new Context();
        context.setVariable("userid", ownerId);
        context.setVariable("email", email);

        // get by owner id
        final List<Badge> list = badges.list(ownerId);
        context.setVariable("badges", list);

        // return streaming response
        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/home.html", context)).build();
    }

    @GET
    @Path("{badgeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response info(@PathParam("badgeId") final String badgeId) throws ShadowbadgeException {
        final BadgeInfo info = badges.info(badgeId);
        if (info == null) {
            return Response.noContent().build();
        }
        return Response.ok(info).build();
    }

    @Secure
    @GET
    @Path("secure/{badgeId}/detail.html")
    @Produces(MediaType.TEXT_HTML)
    public Response infoHtml(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId,
        @HeaderParam(Constants.X_AUTH_EMAIL) final String email
    ) throws ShadowbadgeException {
        // create context
        final Context context = new Context();
        context.setVariable("userid", ownerId);
        context.setVariable("email", email);
        context.setVariable("badgeId", badgeId);

        final Badge badge = badges.getByBadgeId(badgeId);
        if (null == badge) {
            return Response.noContent().build();
        }
        context.setVariable("displayName", badge.getDisplayName());

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
        @FormParam("tagline") final String tagline
    ) throws RepositoryException {
        // update display name
        badges.updateDisplayName(badgeId, ownerId, displayName);

        BadgeInfo info = badges.info(badgeId);
        if (null == info) {
            info = new BadgeInfo();
        }
        info.setHeading(heading);
        info.setGroup(group);
        info.setTitle(title);
        info.setLocation(location);
        info.setTagline(tagline);

        // update individual
        badges.updateInfo(badgeId, ownerId, info);

        // if all goes ok, return home
        return Response.seeOther(URI.create("/badges/secure/home.html")).build();
    }

}
