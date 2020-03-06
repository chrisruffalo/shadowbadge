package com.chrisruffalo.shadowbadge.qr;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class QrDetectorTest {

    @Inject
    QrDetectorService detector;

    @Inject
    QrTestCases cases;

    @Test
    public void testQrDetection() {
        this.cases.get().forEach((testCase) -> {
            try {
                final String result = detector.getQrText(testCase.image);
                if (testCase.isNotDetected()) {
                    Assert.assertEquals(testCase.getFileName(), "", result);
                } else {
                    Assert.assertEquals(testCase.getFileName(), testCase.getExpected(), result);
                }
            } catch (Exception ex) {
                Assert.fail(ex.getMessage());
            }
        });
    }
}
