package com.naxos.challenge.repository;

import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.config.TransactionScope;
import com.naxos.challenge.model.RefreshToken;

/**
 * Non estende GenericRepository: quel contratto porta findAll/findAll(page,size)/
 * count/update(id) che qui non hanno nessun caso d'uso reale — non elenchiamo mai
 * "tutti i refresh token", non li pagini, non li aggiorni con una update generica.
 * Solo quello che il flusso login/refresh/logout usa davvero.
 */
public interface RefreshTokenRepository {

    void save(RefreshToken entity, TransactionScope tx);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revoke(UUID id, TransactionScope tx);

    void revokeAllByFamilyId(UUID familyId, TransactionScope tx);
}
