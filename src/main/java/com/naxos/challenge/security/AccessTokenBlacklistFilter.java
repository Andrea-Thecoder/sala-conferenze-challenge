package com.naxos.challenge.security;

import java.io.IOException;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro globale (nessun @NameBinding: si applica a tutte le risorse JAX-RS). Su una
 * richiesta senza JWT (endpoint pubblici come login/registrazione) jwt.getTokenID()
 * torna null e il filtro passa oltre senza fare nulla — quindi agisce solo dove un
 * token è effettivamente presente, cioè sulle richieste protette, senza bisogno di
 * marcare esplicitamente quali endpoint controllare.
 *
 * Fail-closed: un'eccezione da Redis (irraggiungibile, timeout) fa rifiutare la
 * richiesta con 503, non passare come se il token non fosse in blacklist.
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
@ApplicationScoped
@Slf4j
public class AccessTokenBlacklistFilter implements ContainerRequestFilter {

    @Inject
    JsonWebToken jwt;

    @Inject
    AccessTokenBlacklist blacklist;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String jti = jwt.getTokenID();
        if (jti == null) {
            return;
        }

        boolean revoked;
        try {
            revoked = blacklist.isTokenBlacklisted(jti)
                    || blacklist.isTokenIssuedBeforeUserRevocation(jwt.getSubject(), jwt.getIssuedAtTime());
        } catch (Exception e) {
            log.error("AccessTokenBlacklistFilter - Redis unreachable, failing closed", e);
            requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Authentication service temporarily unavailable")
                    .build());
            return;
        }

        if (revoked) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token revoked")
                    .build());
        }
    }
}
