package com.naxos.challenge.services;

import com.naxos.challenge.config.AuthConfig;
import com.naxos.challenge.dto.user.AuthTokenDTO;
import com.naxos.challenge.dto.user.LoginCredentialsDTO;
import com.naxos.challenge.dto.user.UserRegistrationDTO;
import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.AppRefreshToken;
import com.naxos.challenge.model.User;
import com.naxos.challenge.model.enumerator.Role;
import com.naxos.challenge.repository.AppRefreshTokenRepository;
import com.naxos.challenge.repository.UserRepository;
import com.naxos.challenge.security.AccessTokenBlacklist;
import com.naxos.challenge.security.PasswordEncoder;
import com.naxos.challenge.security.RefreshTokenHasher;
import io.ebean.Database;
import io.ebean.Transaction;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class AuthService {

    @Inject
    UserRepository userRepository;

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


    public UUID registerUser(UserRegistrationDTO dto) {
        log.info("AuthService - registerUser : Starting Registration for new user.");
        User user = dto.toEntity();
        user.setPassword(PasswordEncoder.hash(dto.getPassword(), authConfig.bcryptRounds()));
        try (Transaction tx = database.beginTransaction()) {
            userRepository.save(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("AuthService - registerUser : Error registering user", e);
            throw new ServiceException("Error while registering user. Try again later");
        }
        log.info("AuthService - registerUser : Ending Registration for new user with ID {}", user.getId());
        return user.getId();
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

        AppRefreshToken current = refreshTokenRepository.findByTokenHash(RefreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> {
                    log.error("AuthService - refresh : Refresh token not found");
                    return new ServiceException("Invalid refresh token");
                });

        try (Transaction tx = database.beginTransaction()) {
            if (current.getRevokedAt() != null) {
                log.error("AuthService - refresh : Reuse detected for family {}, revoking entire family", current.getFamilyId());
                refreshTokenRepository.revokeAllByFamilyId(current.getFamilyId(), tx);
                tx.commit();
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
            refreshTokenRepository.save(current, tx);

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
                        refreshTokenRepository.revoke(token.getId(), tx);
                        tx.commit();
                    }
                    log.info("AuthService - logout : Refresh token revoked");
                }, () -> log.info("AuthService - logout : Refresh token not found, nothing to revoke"));

        blacklistCurrentAccessToken();
    }


    public void revokeAllSessions(UUID userId) {
        log.info("AuthService - revokeAllSessions : Revoking all sessions for user {}", userId);
        try (Transaction tx = database.beginTransaction()) {
            refreshTokenRepository.revokeAllByUserId(userId, tx);
            tx.commit();
        }
        long ttlSeconds = authConfig.jwtExpirationMinutes() * 60;
        try {
            accessTokenBlacklist.revokeAllTokensForUser(userId.toString(), ttlSeconds);
            log.info("AuthService - revokeAllSessions : All refresh tokens revoked, access tokens issued before now blacklisted for {}s", ttlSeconds);
        } catch (Exception e) {
            log.error("AuthService - revokeAllSessions : Could not blacklist access tokens for user {} (Redis unreachable?), refresh token revocation still applied", userId, e);
        }
    }

    private void blacklistCurrentAccessToken() {
        String jti = jwt.getTokenID();
        if (jti == null) {
            log.info("AuthService - logout : No access token in request context, nothing to blacklist");
            return;
        }
        long ttlSeconds = jwt.getExpirationTime() - Instant.now().getEpochSecond();
        try {
            accessTokenBlacklist.blacklistToken(jti, ttlSeconds);
            log.info("AuthService - logout : Access token {} blacklisted for {}s", jti, ttlSeconds);
        } catch (Exception e) {
            // Il refresh token è già revocato su Postgres (fatto sopra, non annullabile
            // da qui): quello è il danno più grande e più duraturo, già evitato. Redis
            // giù per il blacklist dell'access token è un buco residuo di al massimo
            // pochi minuti (la durata dell'access token), non un fallimento totale del
            // logout — non lo rilancio, altrimenti il chiamante crede che nulla sia
            // stato revocato quando in realtà la parte più importante lo è stata.
            log.error("AuthService - logout : Could not blacklist access token {} (Redis unreachable?), refresh token revocation still applied", jti, e);
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
        return userRepository.findByEmail(credentials.getEmail())
                .filter(u -> PasswordEncoder.matches(credentials.getPassword(), u.getPassword()))
                .orElseThrow(() -> {
                    log.error("AuthService - getUserByEmail : User not found for {}", credentials.getEmail());
                    return new ServiceException("Invalid email or password");
                });
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
