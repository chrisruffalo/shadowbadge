package com.chrisruffalo.shadowbadge.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Class that has all of the badge's displayed information in it. This class is intended to be the information
 * serialized to a badge (via JSON) when the badge requests it's own configuration.
 */
@Embeddable
public class BadgeInfo {

    @Column
    private String heading;

    @Column
    private String location;

    @Column(name = "groupName")
    private String group;

    @Column
    private String title;

    @Column
    private String tagline;

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }
}
