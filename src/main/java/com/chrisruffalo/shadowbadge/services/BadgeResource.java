package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.dal.BadgeRepo;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.model.BadgeInfo;
import com.chrisruffalo.shadowbadge.services.support.Secure;
import com.chrisruffalo.shadowbadge.web.Constants;
import com.chrisruffalo.shadowbadge.web.templates.base;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/badges")
public class BadgeResource {

    @Inject
    BadgeRepo badges;

    @PostConstruct
    public void init() {

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response claimHtml(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        return Response.ok("<html/>").build();
    }

    @Secure
    @GET
    @Path("secure/home.html")
    @Produces(MediaType.TEXT_HTML)
    public Response home(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        // return streaming response
        return Response.ok(base.template().render().toString()).build();
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
    @Path("secure/{badgeId}/details.html")
    @Produces(MediaType.TEXT_HTML)
    public Response infoHtml(@PathParam("badgeId") final String badgeId) throws ShadowbadgeException {
        final BadgeInfo info = badges.info(badgeId);
        if (info == null) {
            return Response.noContent().build();
        }
        return Response.ok("<html/>").build();
    }

    @Secure
    @PUT
    @POST
    @Path("secure/{badgeId}/update")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId,  final BadgeInfo newInfo) throws ShadowbadgeException {
        return Response.ok(badges.updateInfo(badgeId, ownerId, newInfo)).build();
    }
}
