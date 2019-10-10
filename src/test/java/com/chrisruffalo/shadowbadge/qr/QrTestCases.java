package com.chrisruffalo.shadowbadge.qr;

import com.google.common.io.ByteStreams;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Singleton
public class QrTestCases {

    private static final String QR_SAMPLE_RESOURCE_PATH = "qr_samples/";

    public static class QrDetectorTestCase {
        final String file;
        final String expected;
        // if not detected don't check expected text
        final boolean notDetected;
        // store image in struct to share same load logic
        BufferedImage image;

        public QrDetectorTestCase(String file, String expected, boolean notDetected) {
            this.file = file;
            this.expected = expected;
            this.notDetected = notDetected;
        }

        public String getFile() {
            return file;
        }

        public String getExpected() {
            return expected;
        }

        public boolean isNotDetected() {
            return notDetected;
        }

        public BufferedImage getImage() {
            return image;
        }

        public void setImage(BufferedImage image) {
            this.image = image;
        }

        public InputStream stream() {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(QR_SAMPLE_RESOURCE_PATH + this.getFile());
        }
    }

    private List<QrDetectorTestCase> cases;

    @PostConstruct
    public void init() {
        this.cases = new ArrayList<>();

        this.cases.add(new QrDetectorTestCase("pi_digits.gif", "3141592654", false));
        this.cases.add(new QrDetectorTestCase("wiki_url.png", "http://commons.wikimedia.org/wiki/Main_Page", false));
        this.cases.add(new QrDetectorTestCase("badge_far.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        this.cases.add(new QrDetectorTestCase("badge_almost.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        this.cases.add(new QrDetectorTestCase("badge_ios_detects.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        this.cases.add(new QrDetectorTestCase("badge_close.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));

        this.cases.forEach((item) -> {
            try (
                final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(QR_SAMPLE_RESOURCE_PATH + item.getFile());
            ){
                if (stream != null) {
                    item.setImage(ImageIO.read(stream));
                } else {
                    throw new RuntimeException("Could not load file " + item.getFile() + " from " + QR_SAMPLE_RESOURCE_PATH + " path");
                }
            } catch (Exception e) {
                // this *seems* lazy but if this blows up just stop testing
                throw new RuntimeException(e);
            }
        });
    }

    public List<QrDetectorTestCase> get() {
        return this.cases;
    }

}
