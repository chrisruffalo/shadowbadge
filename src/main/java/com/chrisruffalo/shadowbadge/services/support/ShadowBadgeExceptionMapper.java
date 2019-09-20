package com.chrisruffalo.shadowbadge.services.support;

import com.chrisruffalo.shadowbadge.exceptions.ShadowbadgeException;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ShadowBadgeExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable throwable) {
        int statusCode = Response.Status.BAD_REQUEST.getStatusCode();
        if (throwable instanceof ShadowbadgeException) {
            LoggerFactory.getLogger(throwable.getStackTrace()[0].getClassName()).error(throwable.getMessage());
        } else {
            statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            LoggerFactory.getLogger(throwable.getStackTrace()[0].getClassName()).error(throwable.getMessage());
        }
        return Response.status(statusCode, throwable.getMessage()).build();
    }

}
