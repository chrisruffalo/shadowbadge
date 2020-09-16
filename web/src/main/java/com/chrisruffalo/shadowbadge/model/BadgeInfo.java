package com.chrisruffalo.shadowbadge.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.qute.TemplateData;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Class that has all of the badge's displayed information in it. This class is intended to be the information
 * serialized to a badge (via JSON) when the badge requests it's own configuration.
 */
@Embeddable
@TemplateData
public class BadgeInfo {

    @Column
    private String displayName = "";

    @Column
    private String heading = "";

    @Column
    private IconType icon = IconType.RED_HAT;

    @Column
    private String location = "";

    @Column(name = "groupName")
    private String group = "";

    @Column
    private String title = "";

    @Column
    private String tagline = "";

    @Column
    private LayoutStyle style = LayoutStyle.ICON_RIGHT;

    @Column(name = "qr_type")
    @JsonProperty("qr_type")
    private QRType qrType = QRType.RELATIVE;

    @Column(name = "qr_code")
    @JsonProperty("url") // set for backwards compat
    private String qrCode = "";

    public String getDisplayName() {
        if (null == displayName || displayName.isEmpty()) {
            this.displayName = "Unnamed Badge";
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

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

    public IconType getIcon() {
        if (this.icon == null) {
            this.icon = IconType.RED_HAT;
        }
        return icon;
    }

    public void setIcon(IconType icon) {
        this.icon = icon;
    }

    public LayoutStyle getStyle() {
        if (this.style == null) {
            this.style = LayoutStyle.ICON_RIGHT;
        }
        return style;
    }

    public void setStyle(LayoutStyle style) {
        this.style = style;
    }

    public QRType getQrType() {
        if (this.qrType == null) {
            this.qrType = QRType.RELATIVE;
        }
        return qrType;
    }

    public void setQrType(QRType qrType) {
        this.qrType = qrType;
    }

    public String getQrCode() {
        if (this.qrCode == null) {
            this.qrCode = "";
        }
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
