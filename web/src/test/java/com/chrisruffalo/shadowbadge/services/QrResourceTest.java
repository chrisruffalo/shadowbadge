package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.qr.QrTestCases;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.internal.RequestSpecificationImpl;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class QrResourceTest extends BaseResourceTest {

    @Inject
    QrTestCases cases;

    @Test
    @TestSecurity(user = "testsubject")
    public void testQrDetectionService() {
        this.cases.get().forEach((testCase) -> {
                given()
                    .multiPart("qr", testCase.getFileName(), testCase.stream())
                    .when()
                    .post("/qr/detect")
                    .then()
                    .statusCode(200)
                    .body(Matchers.equalTo(testCase.getExpected()))
                ;
        });
   }
}
