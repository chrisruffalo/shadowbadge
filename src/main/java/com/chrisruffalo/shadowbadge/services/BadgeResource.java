package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.dal.BadgeRepo;
import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.chrisruffalo.shadowbadge.model.Badge;
import com.chrisruffalo.shadowbadge.model.BadgeInfo;
import com.chrisruffalo.shadowbadge.model.IconType;
import com.chrisruffalo.shadowbadge.model.LayoutStyle;
import com.chrisruffalo.shadowbadge.model.QRType;
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
import javax.ws.rs.QueryParam;
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
    public Response claim(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId, @QueryParam("secret") final String secret) throws ShadowbadgeException {
        return Response.ok(badges.claim(badgeId, ownerId, secret)).build();
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
            badges.claim(badgeId, ownerId, secret);
            return Response.seeOther(URI.create(redirection.getRedirect(String.format("/badges/secure/%s/detail.html", badgeId), this.servletRequest))).build();
        } catch (ShadowbadgeException e) {
            // this is "better-ish"
            return this.badges(ownerId, e);
        }
    }

    @Secure
    @GET
    @Path("secure/badges.html")
    @Produces(MediaType.TEXT_HTML)
    public Response badges(
        @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId
    ) throws ShadowbadgeException {
        return this.badges(ownerId, null);
    }

    private Response badges(final String ownerId, final Exception error) {
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
        return Response.ok(new ThymeLeafStreamingOutput(this.engine, "templates/badges.html", context)).build();
    }

    @GET
    @Path("{badgeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response detail(
        @PathParam("badgeId") final String badgeId,
        @HeaderParam(Constants.X_SHADOWBADGE_SECRET) final String secret
    ) throws ShadowbadgeException {
        this.logger.info("requested badge='{}' (secret='{}')", badgeId, secret);
        final Badge badge = badges.getByBadgeId(badgeId);
        if (badge == null) {
            return Response.noContent().build();
        }

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
        // create context
        final WebContext context = new WebContext(this.servletRequest, this.servletResponse, this.servletContext);
        context.setVariable("userid", ownerId);
        context.setVariable("badgeId", badgeId);

        final Badge badge = badges.getByBadgeId(badgeId);
        if (null == badge) {
            return Response.noContent().build();
        }

        // check owner id
        if (ownerId == null || !ownerId.equalsIgnoreCase(badge.getOwnerId())) {
            return this.badges(ownerId, new ShadowbadgeException("You have no claim on the selected badge."));
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
        return Response.ok("badge claim removed").build();
    }

    @Secure
    @GET
    @Path("secure/{badgeId}/unclaimAction")
    public Response unclaimAction(@PathParam("badgeId") final String badgeId, @HeaderParam(Constants.X_AUTH_SUBJECT) final String ownerId) throws ShadowbadgeException {
        this.badges.unclaim(badgeId, ownerId);
        // if all goes ok, return home
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/badges.html", this.servletRequest))).build();
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
        @FormParam("icon") final String iconString,
        @FormParam("style") final String styleString,
        @FormParam("qrType") final String qrTypeString,
        @FormParam("customQrCode") final String customQrCode
    ) throws RepositoryException {
        // get badge so we can get short id and calculate url if needed
        Badge badge = badges.getByBadgeId(badgeId);

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
        badges.updateInfo(badgeId, ownerId, info);

        // if all goes ok, return to badges page
        return Response.seeOther(URI.create(redirection.getRedirect("/badges/secure/badges.html", this.servletRequest))).build();
    }
}
