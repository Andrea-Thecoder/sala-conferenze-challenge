package com.sala.challenge.scheduler;

import java.time.LocalDateTime;

import com.sala.challenge.repository.AppRefreshTokenRepository;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * app_refresh_token accumula una riga per ogni login e per ogni rotazione (nessuna
 * TTL nativa, la "scadenza" è solo applicativa via expires_at) — senza pulizia la
 * tabella cresce senza limite. Gira una volta al giorno, non in orario di punta.
 */
@ApplicationScoped
@Slf4j
public class RefreshTokenCleanupJob {

    @Inject
    AppRefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "{refresh-token.cleanup.cron}")
    void cleanupExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredOrphaned(LocalDateTime.now());
        log.info("RefreshTokenCleanupJob - cleanupExpiredRefreshTokens : Deleted {} expired refresh tokens", deleted);
    }
}
