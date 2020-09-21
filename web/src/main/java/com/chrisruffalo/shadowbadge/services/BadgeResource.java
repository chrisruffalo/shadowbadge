package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.dal.BadgeRepo;
import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.model.*;
import com.chrisruffalo.shadowbadge.web.Constants;
import com.chrisruffalo.shadowbadge.web.Redirection;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
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
public class BadgeResource extends BaseResource {
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

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void init() {
    }

    @Authenticated
    @PUT
    @Path("secure/{badgeId}/claim")
    @Produces(MediaType.APPLICATION_JSON)
    public Response claim(@PathParam("badgeId") final String badgeId, @QueryParam("secret") final String secret) throws ShadowbadgeException {
        return Response.ok(badgeRepo.claim(badgeId, this.getCurrentUserId(), secret)).build();
    }

    // semantically dubious but since this is the link that is created from the QR code there's no way to tell the browser to
    // do a post unless we redirect to a static page that serves javascript and then does a post and then something else
    // which is all a bit of a rube goldberg machine to do the exact same thing that we find semantically dubious here so in
    // my estimation the reduced complexity pays off
    @Authenticated
    @GET
    @Path("secure/{badgeId}/claimAction")
    @Produces(MediaType.TEXT_HTML)
    public Response claimAction(
        @PathParam("badgeId") final String badgeId,
        @QueryParam("secret") final String secret
    ) throws ShadowbadgeException {
        final String userId = this.getCurrentUserId();
        try {
            badgeRepo.claim(badgeId, userId, secret);
            return Response.seeOther(URI.create(redirection.getRedirect(String.format("/badges/secure/%s/detail.html", badgeId), this.servletRequest))).build();
        } catch (ShadowbadgeException e) {
            // this is "better-ish"
            return Response.ok(this.badges(userId, e)).build();
        }
    }

    @Authenticated
    @GET
    @Path("secure/badges.html")
    @Produces(MediaType.TEXT_HTML)
    @NoCache
    public TemplateInstance badges() throws ShadowbadgeException {
        return this.badges(this.getCurrentUserId(), null);
    }

    private TemplateInstance badges(final String userId, final Exception error) {
        // get by owner id
        final List<Badge> list = badgeRepo.listForOwner(userId);
        final TemplateInstance badgeInstance = this.badges.data("badges", list);

        if (error != null) {
            badgeInstance.data("error", true);
            badgeInstance.data("errorMsg", error.getMessage());
        } else {
            badgeInstance.data("error", false);
        }

        badgeInstance.data("userid", userId);

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

    @Authenticated
    @GET
    @Path("secure/{badgeId}/detail.html")
    @Produces(MediaType.TEXT_HTML)
    public Response detailHtml(
        @PathParam("badgeId") final String badgeId
    ) throws ShadowbadgeException {
        final Badge badge = badgeRepo.getByBadgeId(badgeId);
        if (null == badge) {
            return Response.noContent().build();
        }

        // check owner id
        final String userId = this.getCurrentUserId();
        if (userId == null || !userId.equalsIgnoreCase(badge.getOwnerId())) {
            return Response.ok(this.badges(userId, new ShadowbadgeException("You have no claim on the selected badge."))).build();
        }

        final BadgeInfo info = badge.getInfo();
        final TemplateInstance detail = this.detail
            .data("info", info)
            .data("userid", userId)
            .data("badgeId", badgeId);

        return Response.ok(detail).build();
    }

    @Authenticated
    @POST
    @Path("secure/{badgeId}/update")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("badgeId") final String badgeId, final String ownerId, final BadgeInfo newInfo) throws ShadowbadgeException {
        return Response.ok(badgeRepo.updateInfo(badgeId, ownerId, newInfo)).build();
    }

    @Authenticated
    @DELETE
    @Path("secure/{badgeId}/unclaim")
    @Produces(MediaType.TEXT_PLAIN)
    public Response unclaim(@PathParam("badgeId") final String badgeId) throws ShadowbadgeException {
        this.badgeRepo.unclaim(badgeId, this.getCurrentUserId());
        return Response.ok("badge claim removed").build();
    }

    @Authenticated
    @GET
    @Path("secure/{badgeId}/unclaimAction")
    public Response unclaimAction(@PathParam("badgeId") final String badgeId) throws ShadowbadgeException {
        this.badgeRepo.unclaim(badgeId, this.getCurrentUserId());
        // if all goes ok, return home
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/badges.html", this.servletRequest))).build();
    }

    @Authenticated
    @POST
    @Path("secure/{badgeId}/updateForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateForm(
        @PathParam("badgeId") final String badgeId,
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
        badgeRepo.updateInfo(badgeId, this.getCurrentUserId(), info);

        // if all goes ok, return to badges page
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/badges.html", this.servletRequest))).build();
    }
}
