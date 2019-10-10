package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.qr.QrDetector;
import com.chrisruffalo.shadowbadge.services.support.Secure;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Path("/qr")
public class QrResource {
    private static int PEEK_LIMIT = 100;
    private static int LOG_URL_LEN = 20;

    @Inject
    QrDetector detector;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // this is secure for no other reason than to prevent over-use
    @Secure
    @POST
    @Path("detect")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response detect(
        final MultipartFormDataInput input
    ) {
        final List<InputPart> parts = input.getFormDataMap().get("qr");

        for (final InputPart part : parts) {
            try (
                final InputStream workingStream = part.getBody(InputStream.class, null);
            ) {
                // read image
                final BufferedImage image = ImageIO.read(workingStream);
                final String text = this.detector.getQrText(image);
                if (text != null && !text.isEmpty()) {
                    this.logger.info("QR => '{}'", text);
                    return Response.ok(text).build();
                }
            } catch (IOException ex) {
                this.logger.error("Exception while loading image from provided data url", ex);
                return Response.serverError().build();
            }

            this.logger.info("QR => <NO QR CODE FOUND>");
        }
        return Response.noContent().build();
    }

}
