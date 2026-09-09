package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.security.AccessTokenBlacklist;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistTest {

    @Mock
    RedisDataSource redis;

    @Mock
    ValueCommands<String, String> valueCommands;

    @Mock
    KeyCommands<String> keyCommands;

    @InjectMocks
    AccessTokenBlacklist accessTokenBlacklist;

    // ---- blacklistToken ----

    @Test
    void blacklistToken_positiveTtl_setsKeyWithGivenTtl() {
        when(redis.value(String.class)).thenReturn(valueCommands);

        accessTokenBlacklist.blacklistToken("jti-1", 300L);

        verify(valueCommands).setex("blacklist:token:jti-1", 300L, "revoked");
    }

    @Test
    void blacklistToken_zeroTtl_doesNotSetKey() {
        accessTokenBlacklist.blacklistToken("jti-1", 0L);

        verify(valueCommands, never()).setex(anyString(), anyLong(), any());
    }

    @Test
    void blacklistToken_negativeTtl_doesNotSetKey() {
        accessTokenBlacklist.blacklistToken("jti-1", -1L);

        verify(valueCommands, never()).setex(anyString(), anyLong(), any());
    }

    // ---- isTokenBlacklisted ----

    @Test
    void isTokenBlacklisted_keyExists_returnsTrue() {
        when(redis.key()).thenReturn(keyCommands);
        when(keyCommands.exists("blacklist:token:jti-1")).thenReturn(true);

        boolean result = accessTokenBlacklist.isTokenBlacklisted("jti-1");

        assertThat(result).isTrue();
    }

    @Test
    void isTokenBlacklisted_keyMissing_returnsFalse() {
        when(redis.key()).thenReturn(keyCommands);
        when(keyCommands.exists("blacklist:token:jti-1")).thenReturn(false);

        boolean result = accessTokenBlacklist.isTokenBlacklisted("jti-1");

        assertThat(result).isFalse();
    }

    // ---- revokeAllTokensForUser ----

    @Test
    void revokeAllTokensForUser_positiveTtl_setsUserRevocationKey() {
        when(redis.value(String.class)).thenReturn(valueCommands);

        accessTokenBlacklist.revokeAllTokensForUser("user-1", 300L);

        verify(valueCommands).setex(eq("blacklist:user-since:user-1"), eq(300L), anyString());
    }

    @Test
    void revokeAllTokensForUser_zeroTtl_doesNotSetKey() {
        accessTokenBlacklist.revokeAllTokensForUser("user-1", 0L);

        verify(valueCommands, never()).setex(anyString(), anyLong(), any());
    }

    @Test
    void revokeAllTokensForUser_negativeTtl_doesNotSetKey() {
        accessTokenBlacklist.revokeAllTokensForUser("user-1", -1L);

        verify(valueCommands, never()).setex(anyString(), anyLong(), any());
    }

    // ---- isTokenIssuedBeforeUserRevocation ----

    @Test
    void isTokenIssuedBeforeUserRevocation_noRevocationRecorded_returnsFalse() {
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.get("blacklist:user-since:user-1")).thenReturn(null);

        boolean result = accessTokenBlacklist.isTokenIssuedBeforeUserRevocation("user-1", Instant.now().getEpochSecond());

        assertThat(result).isFalse();
    }

    @Test
    void isTokenIssuedBeforeUserRevocation_tokenIssuedBeforeRevocation_returnsTrue() {
        long revokedSince = Instant.now().getEpochSecond();
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.get("blacklist:user-since:user-1")).thenReturn(Long.toString(revokedSince));

        boolean result = accessTokenBlacklist.isTokenIssuedBeforeUserRevocation("user-1", revokedSince - 60);

        assertThat(result).isTrue();
    }

    @Test
    void isTokenIssuedBeforeUserRevocation_tokenIssuedAfterRevocation_returnsFalse() {
        long revokedSince = Instant.now().getEpochSecond();
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.get("blacklist:user-since:user-1")).thenReturn(Long.toString(revokedSince));

        boolean result = accessTokenBlacklist.isTokenIssuedBeforeUserRevocation("user-1", revokedSince + 60);

        assertThat(result).isFalse();
    }

    @Test
    void isTokenIssuedBeforeUserRevocation_tokenIssuedExactlyAtRevocation_returnsFalse() {
        long revokedSince = Instant.now().getEpochSecond();
        when(redis.value(String.class)).thenReturn(valueCommands);
        when(valueCommands.get("blacklist:user-since:user-1")).thenReturn(Long.toString(revokedSince));

        boolean result = accessTokenBlacklist.isTokenIssuedBeforeUserRevocation("user-1", revokedSince);

        assertThat(result).isFalse();
    }
}
