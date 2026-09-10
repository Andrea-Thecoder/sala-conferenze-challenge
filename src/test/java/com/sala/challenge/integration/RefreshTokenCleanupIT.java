package com.sala.challenge.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.AppRefreshToken;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.repository.AppRefreshTokenRepository;
import com.sala.challenge.security.RefreshTokenHasher;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * AppRefreshTokenRepositoryImpl.deleteExpiredOrphaned cancella solo le righe scadute
 * che nessuna riga più vecchia referenzia ancora via replaced_by_id (FK "on delete
 * restrict"), ripetendo la query finché non ne trova più — un token in mezzo a una
 * catena di rotazione diventa cancellabile solo dopo che il token successivo lo è
 * stato. Impossibile verificare questa logica con un repository mockato (il test
 * dello scheduler prova solo che il metodo viene chiamato, non che la query converga
 * correttamente sull'intera catena scaduta).
 */
@QuarkusTest
class RefreshTokenCleanupIT extends AbstractIntegrationTest {

    @Inject
    AppRefreshTokenRepository refreshTokenRepository;

    private User user;

    @AfterEach
    void cleanup() {
        if (user != null) deleteUser(user.getId());
    }

    private AppRefreshToken tokenEntity(User owner, LocalDateTime expiresAt) {
        AppRefreshToken token = new AppRefreshToken();
        token.setUser(owner);
        token.setFamilyId(UUID.randomUUID());
        token.setTokenHash(RefreshTokenHasher.hash(RefreshTokenHasher.generate()));
        token.setExpiresAt(expiresAt);
        token.save();
        return token;
    }

    @Test
    void deleteExpiredOrphaned_expiredChainWithValidTail_deletesOnlyTheExpiredLinks() {
        user = seedUser("cleanup-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        AppRefreshToken oldest = tokenEntity(user, LocalDateTime.now().minusDays(2));
        AppRefreshToken middle = tokenEntity(user, LocalDateTime.now().minusDays(1));
        AppRefreshToken currentlyValid = tokenEntity(user, LocalDateTime.now().plusHours(1));
        oldest.setReplacedByToken(middle);
        oldest.update();
        middle.setReplacedByToken(currentlyValid);
        middle.update();

        refreshTokenRepository.deleteExpiredOrphaned(LocalDateTime.now());

        assertThat(database.find(AppRefreshToken.class)
                .where().in("id", List.of(oldest.getId(), middle.getId()))
                .findList()).isEmpty();
    }

    @Test
    void deleteExpiredOrphaned_expiredChainWithValidTail_keepsTheStillValidToken() {
        user = seedUser("cleanup-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        AppRefreshToken oldest = tokenEntity(user, LocalDateTime.now().minusDays(2));
        AppRefreshToken middle = tokenEntity(user, LocalDateTime.now().minusDays(1));
        AppRefreshToken currentlyValid = tokenEntity(user, LocalDateTime.now().plusHours(1));
        oldest.setReplacedByToken(middle);
        oldest.update();
        middle.setReplacedByToken(currentlyValid);
        middle.update();

        refreshTokenRepository.deleteExpiredOrphaned(LocalDateTime.now());

        assertThat(database.find(AppRefreshToken.class, currentlyValid.getId())).isNotNull();
    }
}
