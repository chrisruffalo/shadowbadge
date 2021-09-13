package com.chrisruffalo.shadowbadge.qr;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;

import java.awt.image.BufferedImage;
import java.util.List;

public class BoofCVQRDetector implements QrDetector {

    @Override
    public String getQrText(BufferedImage image) throws ShadowbadgeException {
        try {
            final GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
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
            throw new ShadowbadgeException(e, "Could not find QR value (boofcv): %s", e.getMessage());
        }

        return "";
    }
}
