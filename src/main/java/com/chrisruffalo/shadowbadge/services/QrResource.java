package com.chrisruffalo.shadowbadge.services;

import com.chrisruffalo.shadowbadge.qr.QrDetectorService;
import com.chrisruffalo.shadowbadge.services.support.Secure;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/qr")
public class QrResource {
    private static int PEEK_LIMIT = 100;
    private static int LOG_URL_LEN = 20;

    @Inject
    QrDetectorService detector;

    private Logger logger;

    @PostConstruct
    public void init() {
        this.logger = LoggerFactory.getLogger(this.getClass());
        // try and get more performance out of imageio reading by removing the on-disk cache
        ImageIO.setUseCache(false);
    }

    // this is secure for no other reason than to prevent over-use
    @Secure
    @POST
    @Path("detect")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> detect(
        @HeaderParam("Content-Length") final String contentLength,
        final MultipartFormDataInput input
    ) throws IOException {
        final List<InputPart> parts = input.getFormDataMap().get("qr");
        if (parts == null || parts.isEmpty()) {
            return CompletableFuture.supplyAsync(() -> Response.noContent().build());
        }

        // get first part
        final InputPart part = parts.get(0);
        if (part == null) {
            return CompletableFuture.supplyAsync(() -> Response.noContent().build());
        }
        final InputStream workingStream = part.getBody(InputStream.class, null);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // read image
                final BufferedImage image = ImageIO.read(workingStream);

                final String text = this.detector.getQrText(image);
                if (text != null && !text.isEmpty()) {
                    return Response.ok(text).build();
                }
                return Response.noContent().build();
            } catch (IOException ex) {
                this.logger.error("Exception while loading image from provided data url", ex);
                return Response.serverError().build();
            } finally {
                // cannot use try-with-resources because if the part.getBody is invoked
                // in the async portion the context is lost and an exception is thrown
                // so this needs to be done the old-school way
                try {
                    workingStream.close();
                } catch (IOException e) {
                    this.logger.info("Could not close QR working stream");
                }
            }
        });
    }

    private static long parse(String value, long defaultVal) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

}
