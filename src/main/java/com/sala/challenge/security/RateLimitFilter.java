package com.sala.challenge.security;

import java.io.IOException;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Legato solo agli endpoint @RateLimited (register/login), non globale.
 *
 * IP letto da HttpServerRequest.remoteAddress(): è l'indirizzo del socket TCP, corretto
 * se il traffico arriva diretto all'app. Se in futuro ci sarà un reverse proxy davanti,
 * l'unico cambio necessario è abilitare quarkus.http.proxy.proxy-address-forwarding
 * (+ trusted-proxies) in config — è Vert.x stesso a riscrivere remoteAddress() di
 * conseguenza, questo filtro non deve cambiare. Deliberatamente NON si legge
 * X-Forwarded-For a mano qui: senza un proxy fidato configurato, è un header
 * arbitrario impostabile dal client, che permetterebbe di bypassare il limite
 * dichiarando un IP diverso ad ogni richiesta.
 *
 * Fail-open: se Redis non risponde, si logga e si lascia passare la richiesta — a
 * differenza del fail-closed di AccessTokenBlacklistFilter, qui il rischio residuo è
 * l'assenza temporanea di throttling, non un bypass di autenticazione.
 */
@Provider
@RateLimited
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
@Slf4j
public class RateLimitFilter implements ContainerRequestFilter {

    @Context
    HttpServerRequest vertxRequest;

    @Inject
    LoginRateLimiter rateLimiter;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean allowed;
        try {
            String clientIp = vertxRequest.remoteAddress().host();
            String path = requestContext.getUriInfo().getPath();
            allowed = rateLimiter.isAllowed(path + ":" + clientIp);
        } catch (Exception e) {
            log.error("RateLimitFilter - unable to evaluate rate limit, failing open", e);
            return;
        }

        if (!allowed) {
            requestContext.abortWith(Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity("Too many attempts. Try again later.")
                    .build());
        }
    }
}
