package com.chrisruffalo.shadowbadge.services;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
@DBRider
public class BadgeResourceTest extends BaseResourceTest {

    @Test
    @TestSecurity(user = "testsubject")
    @DataSet("badges.yml")
    public void testClaim() {
        given()
            .when().put("/badges/test-id-1-badge/claim")
            .then()
            .statusCode(200)
            .assertThat().body("badgeId", Matchers.equalTo("test-id-1-badge"))
            .assertThat().body("ownerId", Matchers.equalTo("testsubject"));
    }


    @Test
    @TestSecurity(user = "testbadsubject")
    @DataSet("badges.yml")
    public void testBadClaim() {
        given()
            .when().put("/badges/test-id-2-badge/claim")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DataSet("badges.yml")
    public void testInfo() {
        given()
            .when().get("/badges/i-have-info-badge")
            .then()
            .statusCode(200)
            .assertThat().body("heading", Matchers.equalTo("My Name"))
            .assertThat().body("title", Matchers.equalTo("Build Bot"));
    }

    @Test
    @DataSet("badges.yml")
    public void seen() {
        given()
            .when().get("/badges/i-will-be-seen-badge")
            .then()
            .statusCode(200)
            .assertThat().body("seen", Matchers.equalTo(200));

        // see this one (and extract count)
        given()
            .when().get("/badges/see-me-now/seen")
            .then()
            .statusCode(200)
            .body("seen", Matchers.equalTo(201));

        // nothing here, find the "none" segment
        given()
                .when().get("/badges/do-not-see-me/seen")
                .then()
                .statusCode(200)
                .body("seen", Matchers.equalTo(0));
    }
}
