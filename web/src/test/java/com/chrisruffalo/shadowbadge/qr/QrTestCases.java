package com.chrisruffalo.shadowbadge.qr;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Singleton
@State(Scope.Benchmark)
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

        public String getResourcePath() {
            return QR_SAMPLE_RESOURCE_PATH + this.file;
        }

        public String getFileName() {
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
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(QR_SAMPLE_RESOURCE_PATH + this.getFileName());
        }
    }

    private List<QrDetectorTestCase> cases = new ArrayList<QrDetectorTestCase>(){{
        add(new QrDetectorTestCase("pi_digits.gif", "3141592654", false));
        add(new QrDetectorTestCase("wiki_url.png", "http://commons.wikimedia.org/wiki/Main_Page", false));
        add(new QrDetectorTestCase("badge_far.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        add(new QrDetectorTestCase("badge_almost.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        add(new QrDetectorTestCase("badge_ios_detects.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        add(new QrDetectorTestCase("badge_close.jpg", "DB1f568b88fbb991c6786c4e49c775e7bc-czgEsl6ZgfsKlmTktE55oCL4", false));
        add(new QrDetectorTestCase("really_big_qr_code.png", "ew0KICAicmVnaXN0cmF0aW9uX3Rva2VuIjogImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSlNVekkxTmlJc0luZzFkQ0k2SWxWcFNuSTRWRlp0WlVoTlUyaE5TRTVOWWsxVVNEWkhWRlZtVFNJc0ltdHBaQ0k2SWxWcFNuSTRWRlp0WlVoTlUyaE5TRTVOWWsxVVNEWkhWRlZtVFNKOS5leUp6WTI5d1pTSTZJazlUU0Y5SlJGQmZSSGx1UTJ4cFpXNTBVbVZuSWl3aVlYVjBiMTl5WldkcGMzUnlZWFJwYjI0aU9pSlVjblZsSWl3aVozSmhiblJmZEhsd1pTSTZJbU5zYVdWdWRGOWpjbVZrWlc1MGFXRnNjeUlzSW5ObGNuWmxjbDlqWlhKMFgzUm9kVzFpY0hKcGJuUnpJam9pSWl3aWFYTnpJam9pVDFOSVEyOXlaVWxrWlc1MGFYUjVVMlZ5ZG1WeUlpd2lZWFZrSWpvaVQxTklRMjl5WlVsa1pXNTBhWFI1VTJWeWRtVnlMM0psYzI5MWNtTmxjeUlzSW1WNGNDSTZNVFUxTURFM056Y3hPQ3dpYm1KbUlqb3hOVFV3TURVME5UazFmUS5pc0Nlb0FCblBzU2pIYTdpX1hjdkdXWXVaRDBJanNRZ2JsQUpuY19RTFMwUmE2V29xekFZVjVYbW03a24tVGM4M2R1V3hEaXFJMEgzaWt4OXBkcGp6MC1aTjkzeGxyLTRYMjZBdFQyRzVNcHpxWDBCZF9YT2Rva0h4aEVqTDhxY1ltUEZ0T2ZIUTY0ZUkxQW5fbEZET205NUtEVTZCUHY4WWNtckFsMlljVlk5THRHeHhrbFh5ZnNkc2tubW02WDRoUFMxV3ZDQ3Y5REF5QkpnTUNqS0paUmtndnhaV2U2d0NnMHlUM3VpekU0WGtuT21kYzBRcnEwTm1KcGxWQkVaRUptVko5TXNVZHRucG9pZGdTclh3WjltQmoxRDh3anZfZXFlaXBuNUhWTExoQ3g1Y0xiMkhGUDlyRTRnUGRQb2l6TlktNnhaVjVYS3p5cDNHdy1jZXciLA0KICAic29mdHdhcmVfc3RhdGVtZW50IjogImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSlNVekkxTmlJc0luZzFkQ0k2SWxWcFNuSTRWRlp0WlVoTlUyaE5TRTVOWWsxVVNEWkhWRlZtVFNJc0ltdHBaQ0k2SWxWcFNuSTRWRlp0WlVoTlUyaE5TRTVOWWsxVVNEWkhWRlZtVFNKOS5leUp6WTI5d1pTSTZJazFGVWxBZ1QxTklYMGxOWDFWelpYSnpYMUpsWVdRaUxDSnBjM01pT2lKUFUwaERiM0psU1dSbGJuUnBkSGxUWlhKMlpYSWlMQ0poZFdRaU9pSlBVMGhEYjNKbFNXUmxiblJwZEhsVFpYSjJaWEl2Y21WemIzVnlZMlZ6SWl3aVpYaHdJam94TlRVd01UYzNOekU0TENKdVltWWlPakUxTlRBd05UUTFPVFY5Lk9DTzZRRW41YlJXSVh5QURnYnlNOUlXSG42SVM2RXdqLWdOcmVvV1NSdmc0Y2oxc0ExWV9TVUVNSncwREFNUTl3UVVaMlYyM0lJc1h1cUx1bmJGeEltNENvZEo2VXB2NTVPejE5Sk1oNXpzNFU3aERjMTJYXy1jTEdYbWN4M1l3SXdKQXZSa2haODF4STVQQ2FIOXZoTkt3V2kzaExhcndMTy1KdFB1T2Etd1ZQRktFb3lTNzN1WjBtWFZWYU5xczAtaHA3TGhrcTlEc0NhdEYtNGNZUWpuREFvemp0UG1RZkNYTVBKQlcyLXhrLU9sU19Bcy1RQlhsNGV0clVRdVc0bldiLW1hOVF3UnZ0bnYwS0dZMkUtckYzQnNiY0tEaHp2QnJpRjkyMXVYQWRMVWJmOUZzZGpHZ1VaZV85cTVFVUw5cFhwNFJuNm5PczdxNXFVTnF4USIsDQogICJyZWdpc3RyYXRpb25fdXNlX3Byb3h5IjogdHJ1ZSwNCiAgInJlZ2lzdHJhdGlvbl91cmwiOiAiaHR0cHM6Ly9lZGdlLm9wYWwtaG9sZGluZy5jb20vczAzIiwNCiAgInJlZ2lzdHJhdGlvbl9wb3J0IjogMA0KfQ==", false));
    }};

    @Setup
    @PostConstruct
    public void init() {
        this.cases.forEach((item) -> {
            try (
                final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(item.getResourcePath());
            ){
                if (stream == null) {
                    throw new RuntimeException("Could not load resource " + item.getResourcePath());
                }
                item.setImage(ImageIO.read(stream));
            } catch (Exception e) {
                // this *seems* lazy but if this blows up just stop testing
                throw new RuntimeException(e);
            }
        });
    }

    @TearDown
    public void clear() {
        for (QrDetectorTestCase testCase : this.cases) {
            testCase.setImage(null);
        }
    }

    public List<QrDetectorTestCase> get() {
        return this.cases;
    }

}
