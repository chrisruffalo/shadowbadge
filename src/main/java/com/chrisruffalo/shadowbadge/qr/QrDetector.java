package com.chrisruffalo.shadowbadge.qr;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.util.List;

@Singleton
public class QrDetector {

    private Logger logger;

    @PostConstruct
    public void init() {
        this.logger = LoggerFactory.getLogger(this.getClass());
        // try and get more performance out of imageio reading by removing the on-disk cache
        ImageIO.setUseCache(false);
    }

    public String getQrText(final BufferedImage inputImage) {
        try {
            String result = this.getQrTextZxing(inputImage);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (ShadowbadgeException e) {
            this.logger.debug(e.getMessage());
        }

        try {
            return getQrTextBoofCV(inputImage);
        } catch (ShadowbadgeException e) {
            this.logger.debug(e.getMessage());
        }

        return "";
    }

    String getQrTextBoofCV(final BufferedImage inputImage) throws ShadowbadgeException  {
        try {
            final GrayU8 gray = ConvertBufferedImage.convertFrom(inputImage, (GrayU8) null);
            final QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);

            detector.process(gray);

            final List<QrCode> codes = detector.getDetections();
            if (codes == null || codes.isEmpty()) {
                return "";
            }

            for (final QrCode code : codes) {
                // if the code is null keep moving
                if (code == null || !QrCode.Failure.NONE.equals(code.failureCause)) {
                    continue;
                }

                // if a message is available, use it
                if (code.message != null && !code.message.isEmpty()) {
                    return code.message;
                }
            }
        } catch (Exception e) {
            throw new ShadowbadgeException("Could not find QR value (boofcv)", e.getMessage());
        }

        return "";
    }

    String getQrTextZxing(final BufferedImage inputImage) throws ShadowbadgeException {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(inputImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException nfe) {
            return "";
        } catch (Exception e) {
            throw new ShadowbadgeException("Could not find QR value (zxing)", e.getMessage());
        }
    }
}
