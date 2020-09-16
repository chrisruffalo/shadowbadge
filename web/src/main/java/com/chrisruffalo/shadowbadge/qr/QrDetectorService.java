package com.chrisruffalo.shadowbadge.qr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class QrDetectorService {

    private Logger logger;

    private List<QrDetector> detectors;

    @PostConstruct
    public void init() {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.detectors = new ArrayList<>(2);
        // boof is faster, if only slightly when you figure in latency/upload times
        this.detectors.add(new BoofCVQRDetector());
        // this is a good fallback detector in case boof can't get it right on the first go
        this.detectors.add(new ZxingQRDetector());
    }

    public String getQrText(final BufferedImage inputImage) {
        for (final QrDetector detector : this.detectors) {
            try {
                final String result = detector.getQrText(inputImage);
                if (result != null && !result.isEmpty()) {
                    this.logger.info("QR ({}) => '{}'", detector.getClass().getSimpleName(), result);
                    return result;
                }
            } catch (Exception e) {
                this.logger.error("Error reading QR code with reader: {}", e.getMessage(), e);
            }
        }

        this.logger.info("QR => < NO QR CODE FOUND >");
        return "";
    }

}
