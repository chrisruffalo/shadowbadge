package com.chrisruffalo.shadowbadge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quarkus.qute.TemplateData;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Entity
@Table(name = "badges")
@NamedQueries({
    @NamedQuery(
        name = Badge.QUERY_GET,
        query = "SELECT badge FROM Badge badge WHERE id = :" + Badge.PARAM_ID,
        hints = {
                @QueryHint(name = "org.hibernate.cacheable", value = "true")
        }
    ),
    @NamedQuery(
        name = Badge.QUERY_GET_BADGE_ID,
        query = "SELECT badge FROM Badge badge WHERE badgeId = :" + Badge.PARAM_BADGE_ID,
        hints = {
                @QueryHint(name = "org.hibernate.cacheable", value = "true")
        }
    ),
    @NamedQuery(
            name = Badge.QUERY_GET_SHORT_ID,
            query = "SELECT badge FROM Badge badge WHERE shortId = :" + Badge.PARAM_SHORT_ID,
            hints = {
                    @QueryHint(name = "org.hibernate.cacheable", value = "true")
            }
    ),
    @NamedQuery(
        name = Badge.QUERY_ALL,
        query = "SELECT badge FROM Badge badge",
        hints = {
                @QueryHint(name = "org.hibernate.cacheable", value = "true")
        }
    ),
    @NamedQuery(
        name = Badge.QUERY_OWNED,
        query = "SELECT badge FROM Badge badge WHERE ownerId =:" + Badge.PARAM_OWNER_ID,
        hints = {
            @QueryHint(name = "org.hibernate.cacheable", value = "true")
        }
    )
})
@TemplateData
public class Badge extends BaseEntity {

    public static final String QUERY_GET = "Badge.get";
    public static final String QUERY_ALL = "Badge.all";
    public static final String QUERY_GET_BADGE_ID = "Badge.getBadgeId";
    public static final String QUERY_OWNED = "Badge.forOwner";
    public static final String QUERY_GET_SHORT_ID = "Badge.getShortId";

    public static final String PARAM_ID = "id";
    public static final String PARAM_BADGE_ID = "badgeId";
    public static final String PARAM_OWNER_ID = "ownerId";
    public static final String PARAM_SHORT_ID = "shortId";

    @Column(unique = true)
    private String badgeId;

    @Column(unique = true)
    private String shortId;

    @Column
    @JsonIgnore
    private String secret;

    @Column
    private String ownerId;

    @Column
    private ConfigurationStatus status = ConfigurationStatus.NONE;

    @Embedded
    @JsonUnwrapped
    private BadgeInfo info;

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public BadgeInfo getInfo() {
        if (this.info == null) {
            this.info = new BadgeInfo();
        }
        return info;
    }

    public void setInfo(BadgeInfo info) {
        this.info = info;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public ConfigurationStatus getStatus() {
        if (null == this.status ) {
            this.status = ConfigurationStatus.NONE;
        }
        return status;
    }

    public void setStatus(ConfigurationStatus status) {
        this.status = status;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Transient
    @JsonInclude
    public String getHash() {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // order. is. important.
        final List<String> fields = new ArrayList<>();

        // badge id
        fields.add(this.getBadgeId());

        // add status
        if (this.getStatus() != null) {
            fields.add(this.getStatus().name());
        }

        // all info
        if (null != this.getInfo()) {
            fields.add(this.getInfo().getHeading());
            fields.add(this.getInfo().getGroup());
            fields.add(this.getInfo().getLocation());
            fields.add(this.getInfo().getTitle());
            fields.add(this.getInfo().getTagline());
            fields.add(this.getInfo().getIcon().name());
            fields.add(this.getInfo().getStyle().name());
            fields.add(this.getInfo().getQrType().name());
            fields.add(this.getInfo().getQrCode());
        }

        if (null != this.getOwnerId()) {
            fields.add(this.getOwnerId());
        }

        // go through fields and hash them, discarding null fields
        fields.forEach((field) -> {
            if (null != field) {
                digest.update(field.getBytes());
            }
        });

        // base64 encoding
        return Base64.getUrlEncoder().encodeToString(digest.digest());
    }
}
