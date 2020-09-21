package com.chrisruffalo.shadowbadge.services;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.identity.SecurityIdentity;

import javax.inject.Inject;

public class BaseResource {
    @Inject
    SecurityIdentity identity;

    /**
     * Get the current user id from the token. In test environments just use "testuserid"
     * to make testing simpler without requiring keycloak.
     *
     * @return the current user id or the test user id in non-prod
     */
    protected String getCurrentUserId() {
        // return empty if no identity
        if (this.identity == null || this.identity.getPrincipal() == null) {
            return "";
        }
        // if a Oidc-style principal extract the actual subject (unique keycloak id)
        if (this.identity.getPrincipal() instanceof OidcJwtCallerPrincipal) {
            final OidcJwtCallerPrincipal principal = (OidcJwtCallerPrincipal)this.identity.getPrincipal();
            if (principal.getSubject() != null && !principal.getSubject().isEmpty()) {
                return principal.getSubject();
            }
        }
        // otherwise fall back to the name of the principal
        return this.identity.getPrincipal().getName();
    }

    protected SecurityIdentity getIdentity() {
        return this.identity;
    }

}
