package com.chrisruffalo.shadowbadge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Transient;

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
public class Badge extends BaseEntity {

    public static final String QUERY_GET = "Badge.get";
    public static final String QUERY_ALL = "Badge.all";
    public static final String QUERY_GET_BADGE_ID = "Badge.getBadgeId";
    public static final String QUERY_OWNED = "Badge.forOwner";

    public static final String PARAM_ID = "id";
    public static final String PARAM_BADGE_ID = "badgeId";
    public static final String PARAM_OWNER_ID = "ownerId";

    @Column(unique = true)
    String badgeId;

    @Column
    private String ownerId;

    @Column
    private ConfigurationStatus status = ConfigurationStatus.NONE;

    @Transient
    @JsonInclude
    private String url;

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

    public String getUrl() {
        if (null == this.url || this.url.isEmpty()) {
            this.url = String.format("/badges/%s/capture", this.getId());
        }
        return this.url;
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
}
