package com.chrisruffalo.shadowbadge.templates;

import io.quarkus.qute.TemplateExtension;
import org.apache.commons.lang3.StringUtils;

@TemplateExtension(namespace = "str")
public class TemplateStrings {

    public static String abbreviate(final String input, final int toLength) {
        return StringUtils.abbreviate(input, toLength);
    }

}
