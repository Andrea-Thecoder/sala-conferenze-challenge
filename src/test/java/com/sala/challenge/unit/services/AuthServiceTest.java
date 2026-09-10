package com.sala.challenge.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sala.challenge.services.AuthService;
import com.sala.challenge.services.UserService;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
import io.smallrye.jwt.build.JwtClaimsBuilder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserService userService;

    @Mock
    AppRefreshTokenRepository refreshTokenRepository;

    @Mock
    Database database;

    @Mock
    AuthConfig authConfig;

    @Mock
    JsonWebToken jwt;

    @Mock
    AccessTokenBlacklist accessTokenBlacklist;

    @Mock
    JwtInspector jwtInspector;

    @Mock
    Transaction transaction;

    @InjectMocks
    AuthService authService;

    private MockedStatic<PasswordEncoder> passwordEncoderMock;
    private MockedStatic<RefreshTokenHasher> refreshTokenHasherMock;
    private MockedStatic<Jwt> jwtMock;

    @BeforeEach
    void openStaticMocks() {
        passwordEncoderMock = mockStatic(PasswordEncoder.class);
        refreshTokenHasherMock = mockStatic(RefreshTokenHasher.class);
        jwtMock = mockStatic(Jwt.class);
    }

    @AfterEach
    void closeStaticMocks() {
        passwordEncoderMock.close();
        refreshTokenHasherMock.close();
        jwtMock.close();
    }

    private User activeCustomer() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("mario.rossi@example.com");
        user.setPassword("irrelevant-hash");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        return user;
    }

    private LoginCredentialsDTO credentials(String email, String password) {
        LoginCredentialsDTO dto = new LoginCredentialsDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private AppRefreshToken refreshTokenEntity(User user, LocalDateTime expiresAt, LocalDateTime revokedAt) {
        AppRefreshToken token = new AppRefreshToken();
        token.setUser(user);
        token.setFamilyId(UUID.randomUUID());
        token.setExpiresAt(expiresAt);
        token.setRevokedAt(revokedAt);
        return token;
    }

    private void stubTokenGeneration(String accessTokenToReturn) {
        JwtClaimsBuilder builder = mock(JwtClaimsBuilder.class);
        when(builder.subject(anyString())).thenReturn(builder);
        when(builder.upn(anyString())).thenReturn(builder);
        when(builder.groups(anySet())).thenReturn(builder);
        when(builder.claim(eq(Claims.jti), any())).thenReturn(builder);
        when(builder.expiresIn(any(Duration.class))).thenReturn(builder);
        when(builder.sign()).thenReturn(accessTokenToReturn);
        jwtMock.when(() -> Jwt.issuer(anyString())).thenReturn(builder);
    }

    // ---- registerUser ----

    @Test
    void registerUser_validDto_returnsCreatedUserId() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        UUID createdId = UUID.randomUUID();
        when(userService.createUser(dto)).thenReturn(createdId);

        UUID result = authService.registerUser(dto);

        assertThat(result).isEqualTo(createdId);
    }

    // ---- login ----

    @Test
    void login_validCredentials_returnsAccessTokenFromTokenBuilder() {
        User user = activeCustomer();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtIssuer()).thenReturn("https://sala-backend-challenge");
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);
        when(authConfig.jwtRefreshExpirationMinutes()).thenReturn(60L);
        refreshTokenHasherMock.when(RefreshTokenHasher::generate).thenReturn("raw-refresh-token");
        stubTokenGeneration("signed-access-token");

        AuthTokenDTO result = authService.login(credentials(user.getEmail(), "Password1!"));

        assertThat(result.getAccessToken()).isEqualTo("signed-access-token");
    }

    @Test
    void login_validCredentials_returnsRefreshTokenFromHasher() {
        User user = activeCustomer();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtIssuer()).thenReturn("https://sala-backend-challenge");
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);
        when(authConfig.jwtRefreshExpirationMinutes()).thenReturn(60L);
        refreshTokenHasherMock.when(RefreshTokenHasher::generate).thenReturn("raw-refresh-token");
        stubTokenGeneration("signed-access-token");

        AuthTokenDTO result = authService.login(credentials(user.getEmail(), "Password1!"));

        assertThat(result.getRefreshToken()).isEqualTo("raw-refresh-token");
    }

    @Test
    void login_validCredentials_persistsHashOfGeneratedRefreshToken() {
        User user = activeCustomer();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtIssuer()).thenReturn("https://sala-backend-challenge");
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);
        when(authConfig.jwtRefreshExpirationMinutes()).thenReturn(60L);
        refreshTokenHasherMock.when(RefreshTokenHasher::generate).thenReturn("raw-refresh-token");
        refreshTokenHasherMock.when(() -> RefreshTokenHasher.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        stubTokenGeneration("signed-access-token");
        ArgumentCaptor<AppRefreshToken> savedToken = ArgumentCaptor.forClass(AppRefreshToken.class);

        authService.login(credentials(user.getEmail(), "Password1!"));
        verify(refreshTokenRepository).save(savedToken.capture(), eq(transaction));

        assertThat(savedToken.getValue().getTokenHash()).isEqualTo("hashed-refresh-token");
    }

    @Test
    void login_userNotFound_throwsServiceException() {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> authService.login(credentials("ghost@example.com", "whatever")));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid email or password");
    }

    @Test
    void login_userNotFound_stillInvokesPasswordComparison() {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), any())).thenReturn(false);

        catchThrowable(() -> authService.login(credentials("ghost@example.com", "Password1!")));

        passwordEncoderMock.verify(() -> PasswordEncoder.matches(eq("Password1!"), any()));
    }

    @Test
    void login_wrongPassword_throwsServiceException() {
        User user = activeCustomer();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Throwable thrown = catchThrowable(() -> authService.login(credentials(user.getEmail(), "wrong-password")));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid email or password");
    }

    @Test
    void login_userNotActive_throwsServiceException() {
        User user = activeCustomer();
        user.setActive(false);
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> authService.login(credentials(user.getEmail(), "Password1!")));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid email or password");
    }

    @Test
    void login_userRevoked_throwsServiceException() {
        User user = activeCustomer();
        user.setRole(Role.REVOKED);
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwordEncoderMock.when(() -> PasswordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> authService.login(credentials(user.getEmail(), "Password1!")));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid email or password");
    }

    // ---- refresh ----

    @Test
    void refresh_tokenNotFound_throwsServiceException() {
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> authService.refresh("raw-token"));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid refresh token");
    }

    @Test
    void refresh_tokenAlreadyRevoked_revokesEntireTokenFamily() {
        AppRefreshToken current = refreshTokenEntity(activeCustomer(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusMinutes(1));
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.of(current));

        catchThrowable(() -> authService.refresh("raw-token"));

        verify(refreshTokenRepository).revokeAllByFamilyId(current.getFamilyId(), transaction);
    }

    @Test
    void refresh_tokenAlreadyRevoked_throwsServiceException() {
        AppRefreshToken current = refreshTokenEntity(activeCustomer(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusMinutes(1));
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.of(current));

        Throwable thrown = catchThrowable(() -> authService.refresh("raw-token"));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Refresh token reuse detected. Please login again");
    }

    @Test
    void refresh_tokenExpired_throwsServiceException() {
        AppRefreshToken current = refreshTokenEntity(activeCustomer(), LocalDateTime.now().minusMinutes(1), null);
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.of(current));

        Throwable thrown = catchThrowable(() -> authService.refresh("raw-token"));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Refresh token expired. Please login again");
    }

    @Test
    void refresh_userNotActiveOrRevoked_throwsServiceException() {
        User revokedUser = activeCustomer();
        revokedUser.setRole(Role.REVOKED);
        AppRefreshToken current = refreshTokenEntity(revokedUser, LocalDateTime.now().plusDays(1), null);
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.of(current));

        Throwable thrown = catchThrowable(() -> authService.refresh("raw-token"));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid refresh token");
    }

    @Test
    void refresh_validToken_returnsAccessTokenFromTokenBuilder() {
        AppRefreshToken current = refreshTokenEntity(activeCustomer(), LocalDateTime.now().plusDays(1), null);
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.of(current));
        when(authConfig.jwtIssuer()).thenReturn("https://sala-backend-challenge");
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);
        when(authConfig.jwtRefreshExpirationMinutes()).thenReturn(60L);
        refreshTokenHasherMock.when(RefreshTokenHasher::generate).thenReturn("new-raw-refresh-token");
        stubTokenGeneration("rotated-access-token");

        AuthTokenDTO result = authService.refresh("raw-token");

        assertThat(result.getAccessToken()).isEqualTo("rotated-access-token");
    }

    @Test
    void refresh_validToken_marksOldTokenAsRevoked() {
        AppRefreshToken current = refreshTokenEntity(activeCustomer(), LocalDateTime.now().plusDays(1), null);
        when(database.beginTransaction()).thenReturn(transaction);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(), eq(transaction))).thenReturn(Optional.of(current));
        when(authConfig.jwtIssuer()).thenReturn("https://sala-backend-challenge");
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);
        when(authConfig.jwtRefreshExpirationMinutes()).thenReturn(60L);
        refreshTokenHasherMock.when(RefreshTokenHasher::generate).thenReturn("new-raw-refresh-token");
        stubTokenGeneration("rotated-access-token");

        authService.refresh("raw-token");

        assertThat(current.getRevokedAt()).isNotNull();
    }

    // ---- logout ----

    @Test
    void logout_tokenFound_revokesRefreshToken() {
        AppRefreshToken token = refreshTokenEntity(activeCustomer(), LocalDateTime.now().plusDays(1), null);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(database.beginTransaction()).thenReturn(transaction);
        when(jwt.getTokenID()).thenReturn(null);

        authService.logout("raw-token");

        verify(refreshTokenRepository).revoke(token, transaction);
    }

    @Test
    void logout_tokenNotFound_doesNotThrow() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(jwt.getTokenID()).thenReturn(null);

        assertThatCode(() -> authService.logout("raw-token")).doesNotThrowAnyException();
    }

    @Test
    void logout_accessTokenPresent_blacklistsAccessToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(jwt.getTokenID()).thenReturn("jti-1");
        when(jwt.getExpirationTime()).thenReturn(Instant.now().getEpochSecond() + 300);

        authService.logout("raw-token");

        verify(accessTokenBlacklist).blacklistToken(eq("jti-1"), longThat(ttl -> ttl > 0));
    }

    @Test
    void logout_accessTokenMissing_doesNotAttemptBlacklist() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(jwt.getTokenID()).thenReturn(null);

        authService.logout("raw-token");

        verify(accessTokenBlacklist, never()).blacklistToken(any(), anyLong());
    }

    @Test
    void logout_accessTokenAlreadyExpired_doesNotAttemptBlacklist() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(jwt.getTokenID()).thenReturn("jti-1");
        when(jwt.getExpirationTime()).thenReturn(Instant.now().getEpochSecond() - 300);

        authService.logout("raw-token");

        verify(accessTokenBlacklist, never()).blacklistToken(any(), anyLong());
    }

    // ---- activateUser / changeRole ----

    @Test
    void activateUser_validUserId_delegatesToUserService() {
        UUID userId = UUID.randomUUID();

        authService.activateUser(userId);

        verify(userService).activateUser(userId);
    }

    @Test
    void changeRole_validRequest_delegatesToUserService() {
        UUID userId = UUID.randomUUID();
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole(Role.ORGANIZER);

        authService.changeRole(userId, dto);

        verify(userService).changeRole(userId, dto);
    }

    // ---- revokeUser ----

    @Test
    void revokeUser_success_revokesAllRefreshTokensWithinSameTransaction() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);

        authService.revokeUser(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId, transaction);
    }

    @Test
    void revokeUser_success_blacklistsAccessTokensForUser() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);

        authService.revokeUser(userId);

        verify(accessTokenBlacklist).revokeAllTokensForUser(userId.toString(), 300L);
    }

    @Test
    void revokeUser_transactionFails_throwsServiceException() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("db exploded")).when(userService).revokeUser(userId, transaction);

        Throwable thrown = catchThrowable(() -> authService.revokeUser(userId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    @Test
    void revokeUser_transactionFails_doesNotBlacklistAccessTokens() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("db exploded")).when(userService).revokeUser(userId, transaction);

        catchThrowable(() -> authService.revokeUser(userId));

        verify(accessTokenBlacklist, never()).revokeAllTokensForUser(any(), anyLong());
    }

    // ---- deleteUser ----

    // deleteUser_success_anonymizesUserWithinSameTransaction: non più verificabile da qui —
    // UserService.anonymizeUser è package-private in com.sala.challenge.services, e questo
    // file vive in com.sala.challenge.unit.services (spostamento esterno non richiesto,
    // accettato così su decisione esplicita). Resta coperta la revoca sessioni collegata.

    @Test
    void deleteUser_success_revokesAllSessionsWithinSameTransaction() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);

        authService.deleteUser(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId, transaction);
    }

    @Test
    void deleteUser_transactionFails_throwsServiceException() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("db exploded")).when(refreshTokenRepository).revokeAllByUserId(userId, transaction);

        Throwable thrown = catchThrowable(() -> authService.deleteUser(userId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- revokeAllSessions ----

    @Test
    void revokeAllSessions_success_revokesAllRefreshTokensForUser() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);

        authService.revokeAllSessions(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId, transaction);
    }

    @Test
    void revokeAllSessions_transactionFails_throwsServiceException() {
        UUID userId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("db exploded")).when(refreshTokenRepository).revokeAllByUserId(userId, transaction);

        Throwable thrown = catchThrowable(() -> authService.revokeAllSessions(userId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- revokeMySessions ----

    @Test
    void revokeMySessions_callerMatchesTarget_revokesSessions() {
        UUID userId = UUID.randomUUID();
        when(jwtInspector.sameSubject(userId)).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        when(authConfig.jwtExpirationMinutes()).thenReturn(5L);

        authService.revokeMySessions(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId, transaction);
    }

    @Test
    void revokeMySessions_callerDoesNotMatchTarget_throwsServiceException() {
        UUID userId = UUID.randomUUID();
        when(jwtInspector.sameSubject(userId)).thenReturn(false);
        when(jwtInspector.getSubject()).thenReturn(UUID.randomUUID());

        Throwable thrown = catchThrowable(() -> authService.revokeMySessions(userId));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Invalid user");
    }
}
