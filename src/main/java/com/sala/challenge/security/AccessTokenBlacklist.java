package com.sala.challenge.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * Blacklist degli access token su Redis, due livelli:
 *  - per singolo token (jti): logout normale, invalida solo quella sessione
 *  - per utente (subject): kill-switch, es. account compromesso o revocato da un
 *    admin — non un flag booleano "utente bloccato", ma una SOGLIA TEMPORALE. Un
 *    token emesso PRIMA della revoca viene rifiutato, uno emesso DOPO (es. un nuovo
 *    login legittimo fatto subito dopo) resta valido — altrimenti l'utente non
 *    riuscirebbe più ad accedere nemmeno dopo essersi autenticato di nuovo.
 *
 * Fail-closed by design: le eccezioni Redis non vengono catturate qui, risalgono al
 * chiamante (il filtro), che decide di rifiutare la richiesta invece di assumere che
 * il token sia valido.
 */
@ApplicationScoped
public class AccessTokenBlacklist {

    private static final String TOKEN_KEY_PREFIX = "blacklist:token:";
    private static final String USER_REVOKED_SINCE_PREFIX = "blacklist:user-since:";
    private static final String SETEX_PLACEHOLDER = "revoked";

    @Inject
    RedisDataSource redis;

    public void blacklistToken(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redis.value(String.class).setex(TOKEN_KEY_PREFIX + jti, ttlSeconds, SETEX_PLACEHOLDER);
    }

    public boolean isTokenBlacklisted(String jti) {
        return redis.key().exists(TOKEN_KEY_PREFIX + jti);
    }

    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS)
    @ExponentialBackoff
    public void revokeAllTokensForUser(String subject, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        String nowEpochSeconds = Long.toString(Instant.now().getEpochSecond());
        redis.value(String.class).setex(USER_REVOKED_SINCE_PREFIX + subject, ttlSeconds, nowEpochSeconds);
    }

    public boolean isTokenIssuedBeforeUserRevocation(String subject, long tokenIssuedAtEpochSeconds) {
        String revokedSince = redis.value(String.class).get(USER_REVOKED_SINCE_PREFIX + subject);
        if (revokedSince == null) {
            return false;
        }
        return tokenIssuedAtEpochSeconds < Long.parseLong(revokedSince);
    }
}
