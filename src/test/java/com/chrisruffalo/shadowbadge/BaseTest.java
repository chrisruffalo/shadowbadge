package com.chrisruffalo.shadowbadge;

import com.chrisruffalo.shadowbadge.web.Constants;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public abstract class BaseTest {

    protected static final String TEST_EMAIL = "shadow@badge.com";
    protected static final String TEST_SUBJECT = "shaowbadge-subject";

    protected RequestSpecification secure() {
        return given()
                .header(Constants.X_AUTH_SUBJECT, TEST_SUBJECT)
                .header(Constants.X_AUTH_EMAIL, TEST_EMAIL);
    }

}
