package com.chrisruffalo.shadowbadge;

import com.github.database.rider.cdi.api.DBUnitInterceptor;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@QuarkusTest
@DBUnitInterceptor
public class BadgeResourceTest extends BaseTest {

    @Test
    @DataSet("badges.yml")
    public void testClaim() {
        secure()
            .when().put("/api/badges/secure/test-id-1-badge/claim")
            .then()
            .statusCode(200)
            .assertThat().body("badgeId", Matchers.equalTo("test-id-1-badge"))
            .assertThat().body("ownerId", Matchers.equalTo(TEST_SUBJECT));

        secure()
            .contentType(MediaType.TEXT_PLAIN)
            .body("test-user-id")
            .when().put("/api/badges/secure/test-id-1-badge/claim")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DataSet("badges.yml")
    public void testInfo() {
        secure()
            .when().get("/api/badges/i-have-info-badge")
            .then()
            .statusCode(200)
            .assertThat().body("heading", Matchers.equalTo("My Name"))
            .assertThat().body("title", Matchers.equalTo("Build Bot"));
    }

    @Test
    public void testUpdate() {

    }
}
