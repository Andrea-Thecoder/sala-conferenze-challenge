package com.sala.challenge.security;

import com.sala.challenge.config.RateLimitConfig;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Finestra fissa su Redis, non token bucket: più semplice e sufficiente per throttlare
 * i tentativi su un endpoint pubblico (register/login). INCR + EXPIRE non sono atomici
 * insieme (piccola race tra due richieste concorrenti sulla stessa chiave appena
 * creata), ma qui il rischio residuo è "il TTL non viene impostato una volta" — non un
 * bypass di sicurezza come lo sarebbe per la blacklist — quindi non giustifica uno
 * script Lua dedicato.
 */
@ApplicationScoped
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:auth:";

    @Inject
    RedisDataSource redis;

    @Inject
    RateLimitConfig config;

    public boolean isAllowed(String clientKey) {
        String key = KEY_PREFIX + clientKey;
        long attempts = redis.value(String.class).incr(key);
        if (attempts == 1) {
            redis.key().expire(key, config.windowSeconds());
        }
        return attempts <= config.maxAttempts();
    }
}
