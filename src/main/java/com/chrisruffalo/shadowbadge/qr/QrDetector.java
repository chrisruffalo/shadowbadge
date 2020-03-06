package com.chrisruffalo.shadowbadge.qr;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;

import java.awt.image.BufferedImage;

public interface QrDetector {

    String getQrText(final BufferedImage image) throws ShadowbadgeException ;

}
