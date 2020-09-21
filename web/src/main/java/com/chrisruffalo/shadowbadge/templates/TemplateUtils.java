package com.chrisruffalo.shadowbadge.templates;

import io.quarkus.qute.TemplateExtension;
import io.quarkus.security.identity.SecurityIdentity;

import java.util.Collection;

@TemplateExtension(namespace = "util")
public class TemplateUtils {

    public static boolean hasContents(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static boolean enumMatches(final Enum<?> enumValue, final String stringValue) {
        return enumValue != null && enumValue.name().equals(stringValue);
    }

    public static boolean isAnon(final SecurityIdentity identity) {
        return identity == null || identity.isAnonymous();
    }

}
