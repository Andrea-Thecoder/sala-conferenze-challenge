package com.sala.challenge.services;

import com.sala.challenge.config.AuthConfig;
import com.sala.challenge.dto.auth.AuthTokenDTO;
import com.sala.challenge.dto.auth.LoginCredentialsDTO;
import com.sala.challenge.dto.user.RoleUpdateDTO;
import com.sala.challenge.dto.user.UserRegistrationDTO;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.AppRefreshToken;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.repository.AppRefreshTokenRepository;
import com.sala.challenge.security.AccessTokenBlacklist;
import com.sala.challenge.security.JwtInspector;
import com.sala.challenge.security.PasswordEncoder;
import com.sala.challenge.security.RefreshTokenHasher;
import io.ebean.Database;
import io.ebean.Transaction;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class AuthService {

    @Inject
    UserService userService;

    @Inject
    AppRefreshTokenRepository refreshTokenRepository;

    @Inject
    Database database;

    @Inject
    AuthConfig authConfig;

    @Inject
    JsonWebToken jwt;

    @Inject
    AccessTokenBlacklist accessTokenBlacklist;

    @Inject
    JwtInspector jwtInspector;

    /**
     * Hash BCrypt "civetta" con lo stesso costo delle password reali (stesso
     * bcryptRounds del profilo attivo): usato quando l'email non esiste, per far
     * comunque eseguire un confronto BCrypt e non rivelare via timing (~5ms vs
     * ~250-400ms) se un'email è registrata o no.
     */
    private String dummyPasswordHash;

    @PostConstruct
    void initDummyPasswordHash() {
        dummyPasswordHash = PasswordEncoder.hash(UUID.randomUUID().toString(), authConfig.bcryptRounds());
    }

    public UUID registerUser(UserRegistrationDTO dto) {
        log.info("AuthService - registerUser : Starting Registration for new user.");
        return userService.createUser(dto);
    }

    public AuthTokenDTO login(LoginCredentialsDTO credentials) {
        log.info("AuthService - login : Login attempt for email {}", credentials.getEmail());

        User user = getUserByEmail(credentials);

        if (!user.isActive() || user.getRole() == Role.REVOKED) {
            log.error("AuthService - login : Login denied for disabled/revoked user {}", user.getId());
            throw new ServiceException("Invalid email or password");
        }


        try (Transaction tx = database.beginTransaction()) {
            String accessToken = generateToken(user);
            String rawRefreshToken = RefreshTokenHasher.generate();
            issueRefreshToken(user, UUID.randomUUID(), rawRefreshToken, tx);
            tx.commit();
            log.info("AuthService - login : Issued token pair for user {}", user.getId());
            return AuthTokenDTO.of(accessToken, rawRefreshToken);
        } catch (Exception e) {
            log.error("AuthService - login : Error issuing refresh token", e);
            throw new ServiceException("Error while logging in. Try again later");
        }
    }

    public AuthTokenDTO refresh(String rawRefreshToken) {
        log.info("AuthService - refresh : Refresh attempt");
        try (Transaction tx = database.beginTransaction()) {
            AppRefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenHasher.hash(rawRefreshToken), tx)
                    .orElseThrow(() -> {
                        log.error("AuthService - refresh : Refresh token not found");
                        return new ServiceException("Invalid refresh token");
                    });

            if (current.getRevokedAt() != null) {
                log.error("AuthService - refresh : Reuse detected for family {}, revoking entire family", current.getFamilyId());
                UUID compromisedUserId = current.getUser().getId();
                refreshTokenRepository.revokeAllByFamilyId(current.getFamilyId(), tx);
                tx.commit();
                // Il riuso di un refresh token già revocato è il segnale di un furto di
                // sessione in corso: chi lo ha rubato può avere ancora in mano un access
                // token valido per i minuti restanti, va bruciato subito insieme alla
                // famiglia di refresh token, non lasciato scadere naturalmente.
                blacklistAllAccessTokensForUser(compromisedUserId);
                throw new ServiceException("Refresh token reuse detected. Please login again");
            }

            if (!current.isActive()) {
                log.error("AuthService - refresh : Refresh token expired for user {}", current.getUser().getId());
                throw new ServiceException("Refresh token expired. Please login again");
            }

            User user = current.getUser();
            if (!user.isActive() || user.getRole() == Role.REVOKED) {
                log.error("AuthService - refresh : Refresh denied for disabled/revoked user {}", user.getId());
                throw new ServiceException("Invalid refresh token");
            }

            String accessToken = generateToken(user);
            String newRawToken = RefreshTokenHasher.generate();
            AppRefreshToken newToken = issueRefreshToken(user, current.getFamilyId(), newRawToken, tx);

            current.setRevokedAt(LocalDateTime.now());
            current.setReplacedByToken(newToken);
            refreshTokenRepository.update(current, tx);

            tx.commit();
            log.info("AuthService - refresh : Rotated refresh token for user {}", user.getId());
            return AuthTokenDTO.of(accessToken, newRawToken);
        }
    }

    public void logout(String rawRefreshToken) {
        log.info("AuthService - logout : Logout attempt");

        refreshTokenRepository.findByTokenHash(RefreshTokenHasher.hash(rawRefreshToken))
                .ifPresentOrElse(token -> {
                    try (Transaction tx = database.beginTransaction()) {
                        refreshTokenRepository.revoke(token, tx);
                        tx.commit();
                    } catch (Exception e) {
                        log.error("AuthService - logout : Error revoking refresh token", e);
                        throw new ServiceException("Error while logging out. Try again later.");
                    }
                    log.info("AuthService - logout : Refresh token revoked");
                }, () -> log.info("AuthService - logout : Refresh token not found, nothing to revoke"));

        blacklistCurrentAccessToken();
    }


    public void activateUser(UUID userId) {
        log.info("AuthService - activateUser : Activating user {}", userId);
        userService.activateUser(userId);
        log.info("AuthService - activateUser : User {} activated", userId);
    }

    public void changeRole(UUID userId, RoleUpdateDTO dto) {
        log.info("AuthService - changeRole : Changing role of user {} to {}", userId, dto.getRole());
        userService.changeRole(userId, dto);
        // Un access token già emesso porta il VECCHIO ruolo nel claim "groups" ed è
        // stateless: senza questo, un ADMIN appena declassato continuerebbe a passare
        // ogni @RolesAllowed("ADMIN") fino alla scadenza naturale del token.
        blacklistAllAccessTokensForUser(userId);
        log.info("AuthService - changeRole : Role of user {} changed to {}", userId, dto.getRole());
    }

    public void revokeUser(UUID userId) {
        log.info("AuthService - revokeUser : Revoking user {}", userId);
        try (Transaction tx = database.beginTransaction()) {
            userService.revokeUser(userId, tx);
            revokeAllSessions(userId, tx);
            tx.commit();
            log.info("AuthService - revokeUser : User {} revoked", userId);
        } catch (Exception e) {
            log.error("AuthService - revokeUser : Error revoking user {}", userId, e);
            throw new ServiceException("Error revoking user. Try again later.");
        }
        blacklistAllAccessTokensForUser(userId);
    }

    public void deleteUser(UUID userId) {
        log.info("AuthService - deleteUser : Deleting (anonymizing) user {}", userId);
        try (Transaction tx = database.beginTransaction()) {
            userService.anonymizeUser(userId, tx);
            revokeAllSessions(userId, tx);
            tx.commit();
            log.info("AuthService - deleteUser : User {} anonymized and revoked", userId);
        } catch (Exception e) {
            log.error("AuthService - deleteUser : Error deleting user {}", userId, e);
            throw new ServiceException("Error deleting user. Try again later.");
        }
        blacklistAllAccessTokensForUser(userId);
    }

    public void revokeAllSessions(UUID userId) {
        try (Transaction tx = database.beginTransaction()) {
            revokeAllSessions(userId, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("AuthService - revokeAllSessions : Error revoking sessions for user {}", userId, e);
            throw new ServiceException("Error while revoking sessions. Try again later.");
        }
        blacklistAllAccessTokensForUser(userId);
    }

    public void revokeMySessions(UUID userId) {
        log.info("AuthService - revokeMySessions : Self-service session revoke requested for user {}", userId);
        if (!jwtInspector.sameSubject(userId)) {
            log.error("AuthService - revokeMySessions : JWT subject {} attempted to revoke sessions of user {}",
                    jwtInspector.getSubject(), userId);
            throw new ServiceException("Invalid user");
        }
        revokeAllSessions(userId);
    }

    private void revokeAllSessions(UUID userId, Transaction tx) {
        log.info("AuthService - revokeAllSessions : Revoking all sessions for user {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId, tx);
    }

    private void blacklistAllAccessTokensForUser(UUID userId) {
        long ttlSeconds = authConfig.jwtExpirationMinutes() * 60;
        try {
            accessTokenBlacklist.revokeAllTokensForUser(userId.toString(), ttlSeconds);
            log.info("AuthService - blacklistAllAccessTokensForUser : Access tokens issued before now blacklisted for {}s for user {}", ttlSeconds, userId);
        } catch (Exception e) {
            log.error("AuthService - blacklistAllAccessTokensForUser : Could not blacklist access tokens for user {} (Redis unreachable?), refresh token revocation still applied", userId, e);
        }
    }

    private void blacklistCurrentAccessToken() {
        try {
            String jti = jwt.getTokenID();
            if (jti == null) {
                log.info("AuthService - logout : No access token in request context, nothing to blacklist");
                return;
            }
            long ttlSeconds = jwt.getExpirationTime() - Instant.now().getEpochSecond();
            if (ttlSeconds <= 0) {
                log.info("AuthService - logout : Access token {} already expired, nothing to blacklist", jti);
                return;
            }
            accessTokenBlacklist.blacklistToken(jti, ttlSeconds);
            log.info("AuthService - logout : Access token {} blacklisted for {}s", jti, ttlSeconds);
        } catch (Exception e) {
            log.warn("AuthService - logout : Could not blacklist access token (missing/invalid/expired, or Redis unreachable), refresh token revocation still applied", e);
        }
    }

    private AppRefreshToken issueRefreshToken(User user, UUID familyId, String rawToken, Transaction tx) {
        AppRefreshToken refreshToken = new AppRefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(RefreshTokenHasher.hash(rawToken));
        refreshToken.setFamilyId(familyId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusMinutes(authConfig.jwtRefreshExpirationMinutes()));
        refreshTokenRepository.save(refreshToken, tx);
        return refreshToken;
    }

    private User getUserByEmail(LoginCredentialsDTO credentials) {
        Optional<User> maybeUser = userService.findByEmail(credentials.getEmail());
        String hashToVerify = maybeUser.map(User::getPassword).orElse(dummyPasswordHash);
        // Il confronto BCrypt gira SEMPRE, esista o no l'utente: altrimenti l'assenza
        // del confronto (email inesistente) risponderebbe in ~5ms contro i ~250-400ms
        // di una password sbagliata su un'email valida, rivelando via timing quali
        // email sono registrate anche se il messaggio d'errore resta identico.
        boolean passwordMatches = PasswordEncoder.matches(credentials.getPassword(), hashToVerify);
        if (maybeUser.isEmpty() || !passwordMatches) {
            log.error("AuthService - getUserByEmail : Invalid login attempt for {}", credentials.getEmail());
            throw new ServiceException("Invalid email or password");
        }
        return maybeUser.get();
    }

    private String generateToken(User user) {
        return Jwt.issuer(authConfig.jwtIssuer())
                .subject(user.getId().toString())
                .upn(user.getEmail())
                .groups(Set.of(user.getRole().name()))
                .claim(Claims.jti, UUID.randomUUID().toString())
                .expiresIn(Duration.ofMinutes(authConfig.jwtExpirationMinutes()))
                .sign();
    }

}
