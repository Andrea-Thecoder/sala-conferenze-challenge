package com.naxos.challenge.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.config.TransactionScope;
import com.naxos.challenge.model.RefreshToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    @Inject
    RefreshTokenPanacheRepository panache;

    @Override
    public void save(RefreshToken entity, TransactionScope tx) {
        panache.persist(entity);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return panache.find("tokenHash", tokenHash).firstResultOptional();
    }

    @Override
    public void revoke(UUID id, TransactionScope tx) {
        // update(query, params) di Panache: bulk UPDATE via query abbreviata,
        // stessa cosa di prima ma senza scrivere "UPDATE RefreshToken r set..." a mano.
        log.info("RefreshTokenRepository - revoke: Revoking refresh token {}", id);
        panache.update("revokedAt = ?1 where id = ?2 and revokedAt is null", LocalDateTime.now(), id);
    }

    @Override
    public void revokeAllByFamilyId(UUID familyId, TransactionScope tx) {
        log.info("RefreshTokenRepository - revokeAllByFamilyId: Revoking family {}", familyId);
        panache.update("revokedAt = ?1 where familyId = ?2 and revokedAt is null", LocalDateTime.now(), familyId);
    }
}
