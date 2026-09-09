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

import com.sala.challenge.api.UserResource;
import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.auth.ChangePasswordDTO;
import com.sala.challenge.dto.search.UserSearchRequest;
import com.sala.challenge.dto.user.BaseDetailUserDTO;
import com.sala.challenge.dto.user.DetailUserDTO;
import com.sala.challenge.dto.user.PhoneNumberUpdateDTO;
import com.sala.challenge.services.AuthService;
import com.sala.challenge.services.UserService;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

    @Mock
    UserService userService;

    @Mock
    AuthService authService;

    @InjectMocks
    UserResource userResource;

    @Test
    void updatePhoneNumber_validRequest_delegatesToService() {
        UUID userId = UUID.randomUUID();
        PhoneNumberUpdateDTO dto = new PhoneNumberUpdateDTO();

        userResource.updatePhoneNumber(userId, dto);

        verify(userService).updatePhoneNumber(userId, dto);
    }

    @Test
    void changePassword_validRequest_delegatesToService() {
        UUID userId = UUID.randomUUID();
        ChangePasswordDTO dto = new ChangePasswordDTO();

        userResource.changePassword(userId, dto);

        verify(userService).changePassword(userId, dto);
    }

    @Test
    void getUserById_validId_returnsServiceResult() {
        UUID userId = UUID.randomUUID();
        DetailUserDTO detail = new DetailUserDTO();
        when(userService.getUserById(userId)).thenReturn(detail);

        DetailUserDTO result = userResource.getUserById(userId);

        assertThat(result).isEqualTo(detail);
    }

    @Test
    void findAllUsers_delegatesToServiceWithRequest() {
        UserSearchRequest request = new UserSearchRequest();
        PagedResultDTO<BaseDetailUserDTO> pagedResult = new PagedResultDTO<>();
        when(userService.findAll(request)).thenReturn(pagedResult);

        PagedResultDTO<BaseDetailUserDTO> result = userResource.findAllUsers(request);

        assertThat(result).isEqualTo(pagedResult);
    }

    @Test
    void deleteUser_validId_delegatesToAuthService() {
        UUID userId = UUID.randomUUID();

        userResource.deleteUser(userId);

        verify(authService).deleteUser(userId);
    }
}
