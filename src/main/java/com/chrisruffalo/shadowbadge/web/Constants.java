package com.chrisruffalo.shadowbadge.web;

public class Constants {
    // headers from keycloak/gatekeeper from https://www.keycloak.org/docs/latest/securing_apps/index.html#upstream-headers
    public final static String X_AUTH_EMAIL = "X-Auth-Email";
    public final static String X_AUTH_GROUPS = "X-Auth-Groups";
    public final static String X_AUTH_ROLES = "X-Auth-Roles";
    public final static String X_AUTH_SUBJECT = "X-Auth-Subject";
    public final static String X_AUTH_TOKEN = "X-Auth-Token";
    public final static String X_AUTH_USERID = "X-Auth-Userid";
    public final static String X_AUTH_UUSERNAME = "X-Auth-Username";

    // values from badge
    public final static String X_SHADOWBADGE_SECRET = "X-Shadowbadge-Secret";

}
