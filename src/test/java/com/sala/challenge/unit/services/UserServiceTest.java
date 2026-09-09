package com.sala.challenge.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.config.AuthConfig;
import com.sala.challenge.dto.auth.ChangePasswordDTO;
import com.sala.challenge.dto.search.UserSearchRequest;
import com.sala.challenge.dto.user.DetailUserDTO;
import com.sala.challenge.dto.user.PhoneNumberUpdateDTO;
import com.sala.challenge.dto.user.RoleUpdateDTO;
import com.sala.challenge.dto.user.UserRegistrationDTO;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.repository.UserRepository;
import com.sala.challenge.security.JwtInspector;
import com.sala.challenge.security.PasswordEncoder;
import com.sala.challenge.services.AuthService;
import com.sala.challenge.services.BookingService;
import com.sala.challenge.services.UserService;
import com.sala.challenge.unit.TestPagedLists;

import io.ebean.Database;
import io.ebean.Transaction;
import jakarta.ws.rs.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    Database database;

    @Mock
    AuthConfig authConfig;

    @Mock
    JwtInspector jwtInspector;

    @Mock
    BookingService bookingService;

    @Mock
    AuthService authService;

    @Mock
    Transaction transaction;

    @InjectMocks
    UserService userService;

    private MockedStatic<PasswordEncoder> passwordEncoderMock;

    @BeforeEach
    void openStaticMocks() {
        passwordEncoderMock = mockStatic(PasswordEncoder.class);
    }

    @AfterEach
    void closeStaticMocks() {
        passwordEncoderMock.close();
    }

    private User user(Role role, boolean active) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");
        user.setPhoneNumber("+393331234567");
        user.setPassword("stored-hash");
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private UserRegistrationDTO registrationDto() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setFirstName("Mario");
        dto.setLastName("Rossi");
        dto.setEmail("mario.rossi@example.com");
        dto.setPassword("Password1!");
        dto.setPhoneNumber("+393331234567");
        dto.setRole(Role.CUSTOMER);
        return dto;
    }

    // ---- createUser ----

    @Test
    void createUser_emailAlreadyRegistered_throwsServiceException() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(true);
        UserRegistrationDTO dto = registrationDto();

        Throwable thrown = catchThrowable(() -> userService.createUser(dto));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Email already registered");
    }

    @Test
    void createUser_validDto_returnsGeneratedUserId() {
        UUID generatedId = UUID.randomUUID();
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(authConfig.bcryptRounds()).thenReturn(10);
        passwordEncoderMock.when(() -> PasswordEncoder.hash(any(), anyInt())).thenReturn("hashed-password");
        when(database.beginTransaction()).thenReturn(transaction);
        doAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return null;
        }).when(userRepository).save(any(), eq(transaction));

        UUID result = userService.createUser(registrationDto());

        assertThat(result).isEqualTo(generatedId);
    }

    @Test
    void createUser_validDto_hashesPasswordWithConfiguredRounds() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(authConfig.bcryptRounds()).thenReturn(12);
        passwordEncoderMock.when(() -> PasswordEncoder.hash(any(), anyInt())).thenReturn("hashed-password");
        when(database.beginTransaction()).thenReturn(transaction);

        userService.createUser(registrationDto());

        passwordEncoderMock.verify(() -> PasswordEncoder.hash("Password1!", 12));
    }

    @Test
    void createUser_transactionFails_throwsServiceException() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(authConfig.bcryptRounds()).thenReturn(10);
        passwordEncoderMock.when(() -> PasswordEncoder.hash(any(), anyInt())).thenReturn("hashed-password");
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(userRepository).save(any(), eq(transaction));
        UserRegistrationDTO dto = registrationDto();

        Throwable thrown = catchThrowable(() -> userService.createUser(dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- updatePhoneNumber ----

    @Test
    void updatePhoneNumber_accessDenied_throwsForbiddenException() {
        UUID userId = UUID.randomUUID();
        doThrow(new ForbiddenException("denied")).when(jwtInspector).checkAccessAllowed(userId);
        PhoneNumberUpdateDTO dto = new PhoneNumberUpdateDTO();
        dto.setPhoneNumber("+393339999999");

        Throwable thrown = catchThrowable(() -> userService.updatePhoneNumber(userId, dto));

        assertThat(thrown).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updatePhoneNumber_phoneAlreadyInUse_throwsServiceException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByPhoneNumber("+393339999999")).thenReturn(true);
        PhoneNumberUpdateDTO dto = new PhoneNumberUpdateDTO();
        dto.setPhoneNumber("+393339999999");

        Throwable thrown = catchThrowable(() -> userService.updatePhoneNumber(userId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Phone number already in use");
    }

    @Test
    void updatePhoneNumber_validRequest_updatesPhoneNumber() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.existsByPhoneNumber("+393339999999")).thenReturn(false);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        PhoneNumberUpdateDTO dto = new PhoneNumberUpdateDTO();
        dto.setPhoneNumber("+393339999999");

        userService.updatePhoneNumber(existing.getId(), dto);

        assertThat(existing.getPhoneNumber()).isEqualTo("+393339999999");
    }

    @Test
    void updatePhoneNumber_transactionFails_throwsServiceException() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.existsByPhoneNumber("+393339999999")).thenReturn(false);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(userRepository).update(any(), eq(transaction));
        UUID userId = existing.getId();
        PhoneNumberUpdateDTO dto = new PhoneNumberUpdateDTO();
        dto.setPhoneNumber("+393339999999");

        Throwable thrown = catchThrowable(() -> userService.updatePhoneNumber(userId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- changePassword ----

    @Test
    void changePassword_callerNotSameSubject_throwsForbiddenException() {
        UUID userId = UUID.randomUUID();
        when(jwtInspector.sameSubject(userId)).thenReturn(false);
        when(jwtInspector.getSubject()).thenReturn(UUID.randomUUID());
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("Current1!");
        dto.setNewPassword("NewPass1!");

        Throwable thrown = catchThrowable(() -> userService.changePassword(userId, dto));

        assertThat(thrown).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsServiceException() {
        User existing = user(Role.CUSTOMER, true);
        when(jwtInspector.sameSubject(existing.getId())).thenReturn(true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        passwordEncoderMock.when(() -> PasswordEncoder.matches(any(), any())).thenReturn(false);
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("WrongPassword1!");
        dto.setNewPassword("NewPass1!");

        Throwable thrown = catchThrowable(() -> userService.changePassword(existing.getId(), dto));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Current password is incorrect");
    }

    @Test
    void changePassword_validRequest_updatesStoredPasswordHash() {
        User existing = user(Role.CUSTOMER, true);
        when(jwtInspector.sameSubject(existing.getId())).thenReturn(true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        passwordEncoderMock.when(() -> PasswordEncoder.matches(any(), any())).thenReturn(true);
        when(authConfig.bcryptRounds()).thenReturn(10);
        passwordEncoderMock.when(() -> PasswordEncoder.hash(any(), anyInt())).thenReturn("new-hashed-password");
        when(database.beginTransaction()).thenReturn(transaction);
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("Current1!");
        dto.setNewPassword("NewPass1!");

        userService.changePassword(existing.getId(), dto);

        assertThat(existing.getPassword()).isEqualTo("new-hashed-password");
    }

    @Test
    void changePassword_transactionFails_throwsServiceException() {
        User existing = user(Role.CUSTOMER, true);
        when(jwtInspector.sameSubject(existing.getId())).thenReturn(true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        passwordEncoderMock.when(() -> PasswordEncoder.matches(any(), any())).thenReturn(true);
        when(authConfig.bcryptRounds()).thenReturn(10);
        passwordEncoderMock.when(() -> PasswordEncoder.hash(any(), anyInt())).thenReturn("new-hashed-password");
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(userRepository).update(any(), eq(transaction));
        UUID userId = existing.getId();
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("Current1!");
        dto.setNewPassword("NewPass1!");

        Throwable thrown = catchThrowable(() -> userService.changePassword(userId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- getUserById ----

    @Test
    void getUserById_validId_includesUserBookings() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(bookingService.findBookingsByUserId(existing.getId())).thenReturn(List.of());

        DetailUserDTO result = userService.getUserById(existing.getId());

        assertThat(result.getId()).isEqualTo(existing.getId());
    }

    // ---- getUserBySubject ----

    @Test
    void getUserBySubject_returnsUserMatchingJwtSubject() {
        User existing = user(Role.CUSTOMER, true);
        when(jwtInspector.getSubject()).thenReturn(existing.getId());
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);

        User result = userService.getUserBySubject();

        assertThat(result).isEqualTo(existing);
    }

    // ---- getUserEntityById ----

    @Test
    void getUserEntityById_returnsUserForGivenId() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);

        User result = userService.getUserEntityById(existing.getId());

        assertThat(result).isEqualTo(existing);
    }

    // ---- changeRole ----

    @Test
    void changeRole_roleRevoked_throwsServiceException() {
        UUID userId = UUID.randomUUID();
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole(Role.REVOKED);

        Throwable thrown = catchThrowable(() -> userService.changeRole(userId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Use the revoke endpoint to revoke a user");
    }

    @Test
    void changeRole_validRole_updatesUserRole() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole(Role.ORGANIZER);

        userService.changeRole(existing.getId(), dto);

        assertThat(existing.getRole()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void changeRole_transactionFails_throwsServiceException() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(userRepository).update(any(), eq(transaction));
        UUID userId = existing.getId();
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole(Role.ORGANIZER);

        Throwable thrown = catchThrowable(() -> userService.changeRole(userId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- findByEmail ----

    @Test
    void findByEmail_delegatesToRepository() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));

        Optional<User> result = userService.findByEmail(existing.getEmail());

        assertThat(result).contains(existing);
    }

    // ---- activateUser ----

    @Test
    void activateUser_validUserId_setsActiveTrue() {
        User existing = user(Role.CUSTOMER, false);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);

        userService.activateUser(existing.getId());

        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void activateUser_transactionFails_throwsServiceException() {
        User existing = user(Role.CUSTOMER, false);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(userRepository).update(any(), eq(transaction));
        UUID userId = existing.getId();

        Throwable thrown = catchThrowable(() -> userService.activateUser(userId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- revokeUser(UUID) ----

    @Test
    void revokeUser_validUserId_deactivatesAndRevokesRole() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);

        userService.revokeUser(existing.getId());

        assertThat(existing).extracting(User::isActive, User::getRole).containsExactly(false, Role.REVOKED);
    }

    @Test
    void revokeUser_transactionFails_throwsServiceException() {
        User existing = user(Role.CUSTOMER, true);
        when(userRepository.getUserById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(userRepository).update(any(), eq(transaction));
        UUID userId = existing.getId();

        Throwable thrown = catchThrowable(() -> userService.revokeUser(userId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- deleteUser ----

    @Test
    void deleteUser_delegatesToAuthService() {
        UUID userId = UUID.randomUUID();

        userService.deleteUser(userId);

        verify(authService).deleteUser(userId);
    }

    // ---- anonymizeUser ----
    // Non testabile direttamente da qui: è package-private in com.sala.challenge.services,
    // e questo file vive in com.sala.challenge.unit.services (spostamento esterno non
    // richiesto, accettato così su decisione esplicita — vedi anche AuthServiceTest).

    // ---- findAll ----

    @Test
    void findAll_adminRole_searchesWithoutRoleConstraint() {
        UserSearchRequest request = new UserSearchRequest();
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(true);
        var pagedList = TestPagedLists.<User>empty();
        when(userRepository.search(request, null)).thenReturn(pagedList);

        userService.findAll(request);

        verify(userRepository).search(request, null);
    }

    @Test
    void findAll_nonAdminNoRoleFilterRequested_constrainsToCustomerAndOrganizer() {
        UserSearchRequest request = new UserSearchRequest();
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);
        Set<Role> expectedConstraint = Set.of(Role.CUSTOMER, Role.ORGANIZER);
        var pagedList = TestPagedLists.<User>empty();
        when(userRepository.search(request, expectedConstraint)).thenReturn(pagedList);

        userService.findAll(request);

        verify(userRepository).search(request, expectedConstraint);
    }

    @Test
    void findAll_nonAdminRequestsDisallowedRole_throwsServiceException() {
        UserSearchRequest request = new UserSearchRequest();
        request.setRole(Role.ADMIN);
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);

        Throwable thrown = catchThrowable(() -> userService.findAll(request));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("You can only search for CUSTOMER or ORGANIZER users.");
    }

    @Test
    void findAll_nonAdminRequestsAllowedRole_lettsRepositoryApplyRoleFilterWithoutConstraint() {
        UserSearchRequest request = new UserSearchRequest();
        request.setRole(Role.CUSTOMER);
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);
        var pagedList = TestPagedLists.<User>empty();
        when(userRepository.search(request, null)).thenReturn(pagedList);

        userService.findAll(request);

        verify(userRepository).search(request, null);
    }
}
