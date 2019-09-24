package com.chrisruffalo.shadowbadge;

import com.chrisruffalo.shadowbadge.web.Constants;
import com.github.database.rider.cdi.api.DBUnitInterceptor;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
@DBUnitInterceptor
public class BadgeResourceTest extends BaseTest {

    @Test
    @DataSet("badges.yml")
    public void testClaim() {
        secure()
            .when().put("/badges/secure/test-id-1-badge/claim")
            .then()
            .statusCode(200)
            .assertThat().body("badgeId", Matchers.equalTo("test-id-1-badge"))
            .assertThat().body("ownerId", Matchers.equalTo(TEST_SUBJECT));

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .header(Constants.X_AUTH_SUBJECT, "bad subject")
            .header(Constants.X_AUTH_EMAIL, "bad email")
            .when().put("/badges/secure/test-id-1-badge/claim")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .when().put("/badges/secure/test-id-1-badge/claim")
            .then()
            .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @DataSet("badges.yml")
    public void testInfo() {
        secure()
            .when().get("/badges/i-have-info-badge")
            .then()
            .statusCode(200)
            .assertThat().body("heading", Matchers.equalTo("My Name"))
            .assertThat().body("title", Matchers.equalTo("Build Bot"));
    }

    @Test
    public void testUpdate() {

    }
}
