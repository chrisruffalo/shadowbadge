package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.dal.BadgeRepo;
import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.model.*;
import com.chrisruffalo.shadowbadge.services.support.Secure;
import com.chrisruffalo.shadowbadge.web.Constants;
import com.chrisruffalo.shadowbadge.web.Redirection;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/badges")
public class BadgeResource {

    @Inject
    Redirection redirection;

    @Inject
    BadgeRepo badgeRepo;

    @Inject
    Template badges;

    @Inject
    Template detail;

    @Context
    HttpServletRequest servletRequest;

    @Context
    HttpServletResponse servletResponse;

    @Context
    ServletContext servletContext;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void init() {
    }

    @Secure
    @PUT
    @Path("secure/{badgeId}/claim")
    @Produces(MediaType.APPLICATION_JSON)
    public Response claim(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId, @QueryParam("secret") final String secret) throws ShadowbadgeException {
        return Response.ok(badgeRepo.claim(badgeId, ownerId, secret)).build();
    }

    @Secure
    @GET // semantically dubious
    @Path("secure/{badgeId}/claimAction")
    @Produces(MediaType.TEXT_HTML)
    public Response claimAction(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId,
        @QueryParam("secret") final String secret
    ) throws ShadowbadgeException {
        try {
            badgeRepo.claim(badgeId, ownerId, secret);
            return Response.seeOther(URI.create(redirection.getRedirect(String.format("/badges/secure/%s/detail.html", badgeId), this.servletRequest))).build();
        } catch (ShadowbadgeException e) {
            // this is "better-ish"
            return Response.ok(this.badges(ownerId, e)).build();
        }
    }

    @Secure
    @GET
    @Path("secure/badges.html")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance badges(
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        return this.badges(ownerId, null);
    }

    private TemplateInstance badges(final String ownerId, final Exception error) {
        // get by owner id
        final List<Badge> list = badgeRepo.listForOwner(ownerId);
        final TemplateInstance badgeInstance = this.badges.data("badges", list);

        if (error != null) {
            badgeInstance.data("error", true);
            badgeInstance.data("errorMsg", error.getMessage());
        } else {
            badgeInstance.data("error", false);
        }

        badgeInstance.data("userid", ownerId);

        // return instance
        return badgeInstance;
    }

    @GET
    @Path("{badgeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response detail(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_SHADOWBADGE_SECRET) final String secret
    ) throws ShadowbadgeException {
        final Badge badge = badgeRepo.getByBadgeId(badgeId);
        if (badge == null) {
            this.logger.info("no badge for badge='{}'", badgeId);
            return Response.noContent().build();
        }

        this.logger.info("requested badge='{}' (secret='{}', hash='{}')", badgeId, secret, badge.getHash());

        // require that the presented secret equals the secret registered with the badge in order to
        // actually return information. prevents people from randomly browsing looking for badges that
        // don't belong to them
        if (badge.getSecret() != null && !badge.getSecret().isEmpty() && !badge.getSecret().equals(secret)) {
            this.logger.warn("mismatched secret (b:{} != r:{})", badge.getSecret(), secret);
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
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        final Badge badge = badgeRepo.getByBadgeId(badgeId);
        if (null == badge) {
            return Response.noContent().build();
        }

        // check owner id
        if (ownerId == null || !ownerId.equalsIgnoreCase(badge.getOwnerId())) {
            return Response.ok(this.badges(ownerId, new ShadowbadgeException("You have no claim on the selected badge."))).build();
        }

        final BadgeInfo info = badge.getInfo();
        final TemplateInstance detail = this.detail
            .data("info", info)
            .data("userid", ownerId)
            .data("badgeId", badgeId);

        return Response.ok(detail).build();
    }

    @Secure
    @POST
    @Path("secure/{badgeId}/update")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId, final BadgeInfo newInfo) throws ShadowbadgeException {
        return Response.ok(badgeRepo.updateInfo(badgeId, ownerId, newInfo)).build();
    }

    @Secure
    @DELETE
    @Path("secure/{badgeId}/unclaim")
    @Produces(MediaType.TEXT_PLAIN)
    public Response unclaim(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        this.badgeRepo.unclaim(badgeId, ownerId);
        return Response.ok("badge claim removed").build();
    }

    @Secure
    @GET
    @Path("secure/{badgeId}/unclaimAction")
    public Response unclaimAction(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        this.badgeRepo.unclaim(badgeId, ownerId);
        // if all goes ok, return home
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/badges.html", this.servletRequest))).build();
    }

    @Secure
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
        @FormParam("icon") final String iconString,
        @FormParam("style") final String styleString,
        @FormParam("qrType") final String qrTypeString,
        @FormParam("customQrCode") final String customQrCode
    ) throws RepositoryException {
        // get badge so we can get short id and calculate url if needed
        Badge badge = badgeRepo.getByBadgeId(badgeId);

        // get info from badge
        BadgeInfo info = badge.getInfo();
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

        // same for layout style
        try {
            final LayoutStyle style = LayoutStyle.valueOf(styleString);
            info.setStyle(style);
        } catch (Exception ex) {
            info.setStyle(LayoutStyle.ICON_RIGHT);
        }

        try {
            final QRType qrType = QRType.valueOf(qrTypeString);
            info.setQrType(qrType);
        } catch (Exception ex) {
            info.setQrType(QRType.RELATIVE);
        }

        // calculate QR code if relative
        if (QRType.RELATIVE.equals(info.getQrType())) {
            info.setQrCode(String.format("/badges/%s/seen", badge.getShortId()));
        } else if(QRType.CUSTOM.equals(info.getQrType())) {
            info.setQrCode(customQrCode);
        } else if(QRType.NONE.equals(info.getQrType())) {
            info.setQrCode("");
        }

        // update info
        badgeRepo.updateInfo(badgeId, ownerId, info);

        // if all goes ok, return to badges page
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/badges.html", this.servletRequest))).build();
    }
}
