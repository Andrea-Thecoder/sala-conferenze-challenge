package com.sala.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sala.challenge.model.AppRefreshToken;
import io.ebean.Database;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class AppRefreshTokenRepositoryImpl implements AppRefreshTokenRepository {

    @Inject
    Database db;

    @Override
    public void save(AppRefreshToken entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public void update(AppRefreshToken entity, Transaction tx) {
        entity.update(tx);
    }

    @Override
    public Optional<AppRefreshToken> findByTokenHash(String tokenHash) {
        return db.find(AppRefreshToken.class)
                .fetch("user")
                .fetch("replacedByToken")
                .where().eq("tokenHash", tokenHash)
                .findOneOrEmpty();
    }

    /**
     * Niente fetch("replacedByToken") qui: quella relazione è nullable (LEFT JOIN
     * lato Postgres) e "FOR UPDATE cannot be applied to the nullable side of an
     * outer join" — errore SQL reale, non ipotetico, se combinati. AuthService.refresh
     * scrive replacedByToken su questa entity ma non lo legge mai, quindi il fetch
     * non serve comunque in questa query.
     */
    @Override
    public Optional<AppRefreshToken> findByTokenHashForUpdate(String tokenHash, Transaction tx) {
        return db.find(AppRefreshToken.class)
                .forUpdate()
                .fetch("user")
                .where().eq("tokenHash", tokenHash)
                .usingTransaction(tx)
                .findOneOrEmpty();
    }

    @Override
    public void revoke(AppRefreshToken entity, Transaction tx) {
        if (entity.getRevokedAt() != null) {
            log.info("AppRefreshTokenRepository - revoke: Token {} already revoked, nothing to do", entity.getId());
            return;
        }
        log.info("AppRefreshTokenRepository - revoke: Revoking refresh token {}", entity.getId());
        entity.setRevokedAt(LocalDateTime.now());
        entity.update(tx);
    }

    @Override
    public List<AppRefreshToken> revokeAllByFamilyId(UUID familyId, Transaction tx) {
        log.info("AppRefreshTokenRepository - revokeAllByFamilyId: Revoking family {}", familyId);
        List<AppRefreshToken> activeTokens = findAllByFamilyId(familyId);
        LocalDateTime now = LocalDateTime.now();
        for (AppRefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.update(tx);
        }
        return activeTokens;
    }

    @Override
    public List<AppRefreshToken> revokeAllByUserId(UUID userId, Transaction tx) {
        log.info("AppRefreshTokenRepository - revokeAllByUserId: Revoking all sessions for user {}", userId);
        List<AppRefreshToken> activeTokens = db.find(AppRefreshToken.class)
                .where().eq("user.id", userId)
                .isNull("revokedAt")
                .findList();
        LocalDateTime now = LocalDateTime.now();
        for (AppRefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.update(tx);
        }
        return activeTokens;
    }

    private List<AppRefreshToken> findAllByFamilyId(UUID familyId) {
        return db.find(AppRefreshToken.class)
                .where().eq("familyId", familyId)
                .isNull("revokedAt")
                .findList();
    }
}
