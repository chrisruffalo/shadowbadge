package com.chrisruffalo.shadowbadge.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class WebResourceTest extends BaseResourceTest {

    @Test
    public void testIndex() {
        given()
            .when().get("/index.html")
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testsubject")
    public void testSecureIndex() {
        given()
            .when().get("/index.html")
            .then()
            .statusCode(200);
    }

}
