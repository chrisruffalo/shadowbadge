package com.chrisruffalo.shadowbadge.qr;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;

public class ZxingQRDetector implements QrDetector {

    @Override
    public String getQrText(BufferedImage image) throws ShadowbadgeException {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException nfe) {
            return "";
        } catch (Exception e) {
            throw new ShadowbadgeException(e, "Could not find QR value (zxing): %s", e.getMessage());
        }
    }
}
