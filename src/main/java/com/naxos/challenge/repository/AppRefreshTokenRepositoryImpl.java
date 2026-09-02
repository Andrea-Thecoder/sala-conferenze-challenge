package com.naxos.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.model.AppRefreshToken;
import io.ebean.Database;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class AppRefreshTokenRepositoryImpl implements AppRefreshTokenRepository {

    @Inject
    Database database;

    @Override
    public void save(AppRefreshToken entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public Optional<AppRefreshToken> findByTokenHash(String tokenHash) {
        return database.find(AppRefreshToken.class)
                .where().eq("tokenHash", tokenHash)
                .findOneOrEmpty();
    }

    @Override
    public void revoke(UUID id, Transaction tx) {
        log.info("AppRefreshTokenRepository - revoke: Revoking refresh token {}", id);
        AppRefreshToken token = findById(id);
        if (token == null || token.getRevokedAt() != null) {
            return;
        }
        token.setRevokedAt(LocalDateTime.now());
        token.save(tx);
    }

    @Override
    public void revokeAllByFamilyId(UUID familyId, Transaction tx) {
        log.info("AppRefreshTokenRepository - revokeAllByFamilyId: Revoking family {}", familyId);
        List<AppRefreshToken> activeTokens = findAllByFamilyId(familyId);
        LocalDateTime now = LocalDateTime.now();
        for (AppRefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.save(tx);
        }
    }

    @Override
    public void revokeAllByUserId(UUID userId, Transaction tx) {
        log.info("AppRefreshTokenRepository - revokeAllByUserId: Revoking all sessions for user {}", userId);
        List<AppRefreshToken> activeTokens = database.find(AppRefreshToken.class)
                .where().eq("user.id", userId)
                .isNull("revokedAt")
                .findList();
        LocalDateTime now = LocalDateTime.now();
        for (AppRefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.save(tx);
        }
    }

    private List<AppRefreshToken> findAllByFamilyId(UUID familyId) {
        return database.find(AppRefreshToken.class)
                .where().eq("familyId", familyId)
                .isNull("revokedAt")
                .findList();
    }

    private AppRefreshToken findById(UUID id) {
        return database.find(AppRefreshToken.class, id);
    }
}
