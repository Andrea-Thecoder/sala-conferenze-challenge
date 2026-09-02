package com.naxos.challenge.repository;

import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.model.AppRefreshToken;
import io.ebean.Transaction;

/**
 * Non estende GenericRepository: quel contratto porta findAll/count/update generico
 * che qui non hanno nessun caso d'uso reale — non elenchiamo mai "tutti i refresh
 * token", non li aggiorniamo con una update generica. Solo quello che il flusso
 * login/refresh/logout usa davvero.
 */
public interface AppRefreshTokenRepository {

    void save(AppRefreshToken entity, Transaction tx);

    Optional<AppRefreshToken> findByTokenHash(String tokenHash);

    void revoke(UUID id, Transaction tx);

    void revokeAllByFamilyId(UUID familyId, Transaction tx);

    void revokeAllByUserId(UUID userId, Transaction tx);
}
