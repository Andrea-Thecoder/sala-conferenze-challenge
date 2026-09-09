package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.api.AuthResource;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.auth.AuthTokenDTO;
import com.sala.challenge.dto.auth.LoginCredentialsDTO;
import com.sala.challenge.dto.auth.RefreshTokenDTO;
import com.sala.challenge.dto.user.RoleUpdateDTO;
import com.sala.challenge.dto.user.UserRegistrationDTO;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.services.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    AuthService authService;

    @InjectMocks
    AuthResource authResource;

    @Test
    void register_validDto_returnsCreatedUserIdInPayload() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        UUID createdId = UUID.randomUUID();
        when(authService.registerUser(dto)).thenReturn(createdId);

        SimpleResultDTO<UUID> result = authResource.register(dto);

        assertThat(result.getPayload()).isEqualTo(createdId);
    }

    @Test
    void login_validCredentials_returnsServiceResult() {
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        AuthTokenDTO tokens = AuthTokenDTO.of("access-token", "refresh-token");
        when(authService.login(credentials)).thenReturn(tokens);

        AuthTokenDTO result = authResource.login(credentials);

        assertThat(result).isEqualTo(tokens);
    }

    @Test
    void refresh_validRefreshToken_returnsServiceResult() {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("raw-refresh-token");
        AuthTokenDTO tokens = AuthTokenDTO.of("new-access-token", "new-refresh-token");
        when(authService.refresh("raw-refresh-token")).thenReturn(tokens);

        AuthTokenDTO result = authResource.refresh(dto);

        assertThat(result).isEqualTo(tokens);
    }

    @Test
    void logout_validRefreshToken_delegatesToService() {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("raw-refresh-token");

        authResource.logout(dto);

        verify(authService).logout("raw-refresh-token");
    }

    @Test
    void activateUser_validUserId_delegatesToService() {
        UUID userId = UUID.randomUUID();

        authResource.activateUser(userId);

        verify(authService).activateUser(userId);
    }

    @Test
    void changeRole_validRequest_delegatesToService() {
        UUID userId = UUID.randomUUID();
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole(Role.ORGANIZER);

        authResource.changeRole(userId, dto);

        verify(authService).changeRole(userId, dto);
    }

    @Test
    void revokeUser_validUserId_delegatesToService() {
        UUID userId = UUID.randomUUID();

        authResource.revokeUser(userId);

        verify(authService).revokeUser(userId);
    }

    @Test
    void revokeMySessions_validUserId_delegatesToService() {
        UUID userId = UUID.randomUUID();

        authResource.revokeMySessions(userId);

        verify(authService).revokeMySessions(userId);
    }

    @Test
    void revokeAllSessions_validUserId_delegatesToService() {
        UUID userId = UUID.randomUUID();

        authResource.revokeAllSessions(userId);

        verify(authService).revokeAllSessions(userId);
    }
}
