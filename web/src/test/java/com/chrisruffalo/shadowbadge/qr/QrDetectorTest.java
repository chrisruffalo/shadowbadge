package com.chrisruffalo.shadowbadge.qr;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
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
                    Assertions.assertEquals("", result, testCase.getFileName());
                } else {
                    Assertions.assertEquals(testCase.getExpected(), result, testCase.getFileName());
                }
            } catch (Exception ex) {
                Assertions.fail(ex.getMessage());
            }
        });
    }
}
