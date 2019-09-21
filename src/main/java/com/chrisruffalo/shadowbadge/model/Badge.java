package com.chrisruffalo.shadowbadge.model;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;

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
    private String badgeId;

    @Column
    private String ownerId;

    @Column
    private String displayName;

    @Embedded
    private BadgeInfo info;

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDisplayName() {
        if ((null == displayName || displayName.isEmpty()) && (null != this.badgeId && !this.badgeId.isEmpty())) {
            int max = 12;
            if (this.badgeId.length() < max) {
                max = this.badgeId.length();
            }
            this.displayName = "Badge " + this.badgeId.substring(0, max);
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
