package com.chrisruffalo.shadowbadge.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
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

    @Test
    @TestSecurity(user = "0001-000-000-333")
    public void testBadgeList() {
        given()
                .when().put("/badges/test-owned-badge/claim")
                .then()
                .statusCode(200);

        final Response response = given()
                                    .when().get("/badges/list.html")
                                    .andReturn();

        Assertions.assertEquals(200, response.statusCode());
        final String html = response.getBody().asPrettyString();

        // we are simply looking for the link to the owned badge to be somewhere in the page
        Assertions.assertTrue(html.contains("/badges/test-owned-badge/detail.html"));
    }

}
