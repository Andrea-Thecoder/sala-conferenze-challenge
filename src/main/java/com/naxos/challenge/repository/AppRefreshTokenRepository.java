package com.naxos.challenge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.model.AppRefreshToken;
import io.ebean.Transaction;

/**
 * Non estende GenericRepository: quel contratto porta findAll/count/update generico
 * che qui non hanno nessun caso d'uso reale — non elenchiamo mai "tutti i refresh
 * token", non li aggiorniamo con una update generica. Solo quello che il flusso
 * login/refresh/logout usa davvero.
 *
 * save/update separati apposta: save() è solo per entity nuove (persist), update()
 * per entity esistenti mutate (revoca, rotazione) — mai lasciare che sia save() a
 * indovinare insert-vs-update.
 */
public interface AppRefreshTokenRepository {

    void save(AppRefreshToken entity, Transaction tx);

    void update(AppRefreshToken entity, Transaction tx);

    Optional<AppRefreshToken> findByTokenHash(String tokenHash);

    /**
     * Come findByTokenHash, ma con SELECT ... FOR UPDATE: la riga resta bloccata per
     * tutta la transazione tx, così due /refresh concorrenti sullo stesso token si
     * serializzano invece di leggere entrambi lo stesso snapshot "attivo" — necessario
     * perché la reuse-detection su revokedAt scatti anche sotto concorrenza reale.
     */
    Optional<AppRefreshToken> findByTokenHashForUpdate(String tokenHash, Transaction tx);

    /**
     * Revoca l'entity passata (il chiamante l'ha già in mano, es. da findByTokenHash:
     * niente ri-fetch per id). No-op se è già revocata — idempotente, coerente con
     * /auth/logout che non deve fallire su un token già revocato.
     */
    void revoke(AppRefreshToken entity, Transaction tx);

    List<AppRefreshToken> revokeAllByFamilyId(UUID familyId, Transaction tx);

    List<AppRefreshToken> revokeAllByUserId(UUID userId, Transaction tx);
}
