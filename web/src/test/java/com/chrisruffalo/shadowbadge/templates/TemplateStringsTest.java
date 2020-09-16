package com.chrisruffalo.shadowbadge.templates;

import org.junit.Assert;
import org.junit.Test;

public class TemplateStringsTest {

    @Test
    public void testAbbreviate() {

        Assert.assertEquals("test", TemplateStrings.abbreviate("test", 25));
        Assert.assertEquals("test", TemplateStrings.abbreviate("test", 4));
        Assert.assertEquals("t...", TemplateStrings.abbreviate("test12", 4));

    }

}
