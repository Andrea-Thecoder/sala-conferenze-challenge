package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.config.RateLimitConfig;
import com.sala.challenge.security.LoginRateLimiter;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    @Mock
    RedisDataSource redis;

    @Mock
    ValueCommands<String, String> valueCommands;

    @Mock
    KeyCommands<String> keyCommands;

    @Mock
    RateLimitConfig config;

    @InjectMocks
    LoginRateLimiter loginRateLimiter;

    @Test
    void isAllowed_firstAttempt_setsExpiryOnKey() {
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(redis.key()).thenReturn(keyCommands);
        when(valueCommands.incr("ratelimit:auth:1.2.3.4")).thenReturn(1L);
        when(config.windowSeconds()).thenReturn(60L);
        when(config.maxAttempts()).thenReturn(5);

        loginRateLimiter.isAllowed("1.2.3.4");

        verify(keyCommands).expire("ratelimit:auth:1.2.3.4", 60L);
    }

    @Test
    void isAllowed_subsequentAttempt_doesNotResetExpiry() {
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.incr("ratelimit:auth:1.2.3.4")).thenReturn(2L);
        when(config.maxAttempts()).thenReturn(5);

        loginRateLimiter.isAllowed("1.2.3.4");

        verify(keyCommands, never()).expire(anyString(), anyLong());
    }

    @Test
    void isAllowed_attemptsWithinLimit_returnsTrue() {
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.incr("ratelimit:auth:1.2.3.4")).thenReturn(5L);
        when(config.maxAttempts()).thenReturn(5);

        boolean result = loginRateLimiter.isAllowed("1.2.3.4");

        assertThat(result).isTrue();
    }

    @Test
    void isAllowed_attemptsBeyondLimit_returnsFalse() {
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.incr("ratelimit:auth:1.2.3.4")).thenReturn(6L);
        when(config.maxAttempts()).thenReturn(5);

        boolean result = loginRateLimiter.isAllowed("1.2.3.4");

        assertThat(result).isFalse();
    }
}
