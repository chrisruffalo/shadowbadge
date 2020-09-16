package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.qr.QrTestCases;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class QrResourceTest extends BaseResourceTest {

    @Inject
    QrTestCases cases;

    @Test
    public void testQrDetectionService() {
        this.cases.get().forEach((testCase) -> {
                secure()
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
