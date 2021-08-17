package com.chrisruffalo.shadowbadge.services.support;

import com.chrisruffalo.shadowbadge.exceptions.AlreadyClaimedBadgeException;
import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import io.quarkus.qute.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ShadowBadgeExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    @Context
    private HttpServletRequest httpServletRequest;

    @Override
    public Response toResponse(Throwable throwable) {
        final Logger logger = LoggerFactory.getLogger(throwable.getStackTrace()[0].getClassName());

        int statusCode = Response.Status.BAD_REQUEST.getStatusCode();

        if (!(throwable instanceof ShadowbadgeException)) {
            statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

            // print full stack trace for more serious exceptions that need more detail
            // but print small only the error for lesser-known exceptions
            if (
               throwable instanceof UnsatisfiedLinkError
            || throwable instanceof TemplateException
            ) {
                logger.error("", throwable);
            } else {
                logger.error("{}: {}", throwable.getClass().getName(), throwable.getMessage());
            }
        } else if(throwable instanceof AlreadyClaimedBadgeException) {
            final AlreadyClaimedBadgeException alreadyClaimedBadgeException = (AlreadyClaimedBadgeException) throwable;
            logger.error("Badge {} already claimed by user {}", alreadyClaimedBadgeException.getBadge(), alreadyClaimedBadgeException.getOwner());
        } else {
            logger.error(throwable.getMessage());
        }

        if (throwable instanceof NotFoundException) {
            statusCode = Response.Status.NOT_FOUND.getStatusCode();
        }

        // return response
        return Response.status(statusCode).build();
    }

}
