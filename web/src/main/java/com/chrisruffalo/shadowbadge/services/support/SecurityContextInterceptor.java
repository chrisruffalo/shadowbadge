package com.chrisruffalo.shadowbadge.services.support;

import com.chrisruffalo.shadowbadge.web.Constants;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Since this application is intended to sit behind gatekeeper authentication it only needs to check to make sure
 * the auth headers are available before going forward. If they are not then the gatekeeper will have blocked it anyway
 * which makes testing **really** easy.
 *
 */
@Provider
public class SecurityContextInterceptor implements ContainerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(SecurityContextInterceptor.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        final ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) containerRequestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");

        final Method invokingMethod = methodInvoker.getMethod();
        final Class<?> invokingClass = invokingMethod.getDeclaringClass();

        if (invokingClass.isAnnotationPresent(Secure.class) || invokingMethod.isAnnotationPresent(Secure.class)) {
            this.secure(containerRequestContext, invokingClass, invokingMethod);
        } else {
            logger.debug("Insecure invoke: {}#{}", invokingClass.getSimpleName(), invokingMethod.getName());
        }
    }

    private void secure(final ContainerRequestContext containerRequestContext, final Class<?> invokingClass, final Method invokingMethod) {
        // get security header values, require an email (because we use that to work) and an auth subject (because that comes from keycloak)
        final String authId = containerRequestContext.getHeaderString(Constants.X_AUTH_SUBJECT);
        final String email = containerRequestContext.getHeaderString(Constants.X_AUTH_EMAIL);

        // no security context provided
        if (null == email || email.isEmpty() || null == authId || authId.isEmpty()) {
            logger.debug("Denied secure invoke: {}#{} (subject={}, email={})", invokingClass.getSimpleName(), invokingMethod.getName(), authId, email);
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        } else {
            logger.debug("Secure invoke: {}#{} (subject={}, email={})", invokingClass.getSimpleName(), invokingMethod.getName(), authId, email);
        }
    }
}
