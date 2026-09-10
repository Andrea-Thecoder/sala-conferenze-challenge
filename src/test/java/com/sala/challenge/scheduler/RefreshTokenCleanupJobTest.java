package com.sala.challenge.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.repository.AppRefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTest {

    @Mock
    AppRefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    RefreshTokenCleanupJob cleanupJob;

    @Test
    void cleanupExpiredRefreshTokens_delegatesToRepository() {
        when(refreshTokenRepository.deleteExpiredOrphaned(any())).thenReturn(3);

        cleanupJob.cleanupExpiredRefreshTokens();

        verify(refreshTokenRepository).deleteExpiredOrphaned(any());
    }
}
