package com.naxos.challenge.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.naxos.challenge.config.AuthConfig;
import com.naxos.challenge.dto.PagedResultDTO;
import com.naxos.challenge.dto.booking.BaseDetailBookingDTO;
import com.naxos.challenge.dto.search.UserSearchRequest;
import com.naxos.challenge.dto.user.BaseDetailUserDTO;
import com.naxos.challenge.dto.user.DetailUserDTO;
import com.naxos.challenge.dto.auth.ChangePasswordDTO;
import com.naxos.challenge.dto.user.PhoneNumberUpdateDTO;
import com.naxos.challenge.dto.user.RoleUpdateDTO;
import com.naxos.challenge.dto.user.UserRegistrationDTO;
import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.User;
import com.naxos.challenge.model.enumerator.Role;
import com.naxos.challenge.repository.UserRepository;
import com.naxos.challenge.security.JwtInspector;
import com.naxos.challenge.security.PasswordEncoder;
import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class UserService {

    private static final Set<Role> NON_ADMIN_SEARCHABLE_ROLES = Set.of(Role.CUSTOMER, Role.ORGANIZER);

    @Inject
    UserRepository userRepository;

    @Inject
    Database database;

    @Inject
    AuthConfig authConfig;

    @Inject
    JwtInspector jwtInspector;

    @Inject
    BookingService bookingService;

    public UUID createUser(UserRegistrationDTO dto) {
        log.info("UserService - save : Creating new user");
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.error("UserService - save : Email {} already registered", dto.getEmail());
            throw new ServiceException("Email already registered");
        }
        User user = dto.toEntity();
        user.setPassword(PasswordEncoder.hash(dto.getPassword(), authConfig.bcryptRounds()));
        try (Transaction tx = database.beginTransaction()) {
            userRepository.save(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - save : Error creating user", e);
            throw new ServiceException("Error while creating user. Try again later.");
        }
        log.info("UserService - save : Created user {}", user.getId());
        return user.getId();
    }

    public void updatePhoneNumber(UUID userId, PhoneNumberUpdateDTO dto) {
        log.info("UserService - updatePhoneNumber : Updating phone number for user {}", userId);
        // Controllato subito, prima di existsByPhoneNumber: un CUSTOMER non autorizzato
        // non deve poter scoprire se un numero è già in uso su un altro account solo
        // provando ad aggiornarlo.
        jwtInspector.checkAccessAllowed(userId);
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            log.error("UserService - updatePhoneNumber : Phone number already in use for user {}", userId);
            throw new ServiceException("Phone number already in use");
        }
        try (Transaction tx = database.beginTransaction()) {
            User user = fetchUserById(userId);
            dto.toUpdate(user);
            userRepository.update(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - updatePhoneNumber : Error updating phone number for user {}", userId, e);
            throw new ServiceException("Error while updating phone number. Try again later.");
        }
    }

    /**
     * Sempre e solo se stessi, indipendentemente dal ruolo — a differenza di
     * checkAccessAllowed (che lascia passare ADMIN/ORGANIZER su qualunque userId),
     * qui la restrizione vale per tutti: serve conoscere la password attuale, quindi
     * anche un ADMIN non può usarla per "resettare" la password di qualcun altro.
     */
    public void changePassword(UUID userId, ChangePasswordDTO dto) {
        log.info("UserService - changePassword : Password change requested for user {}", userId);
        if (!jwtInspector.sameSubject(userId)) {
            log.error("UserService - changePassword : JWT subject {} attempted to change password of user {}",
                    jwtInspector.getSubject(), userId);
            throw new ServiceException("Invalid user.");
        }
        User user = userRepository.getUserById(userId);
        if (!PasswordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            log.error("UserService - changePassword : Wrong current password for user {}", userId);
            throw new ServiceException("Current password is incorrect");
        }
        try (Transaction tx = database.beginTransaction()) {
            user.setPassword(PasswordEncoder.hash(dto.getNewPassword(), authConfig.bcryptRounds()));
            userRepository.update(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - changePassword : Error changing password for user {}", userId, e);
            throw new ServiceException("Error while changing password. Try again later.");
        }
        log.info("UserService - changePassword : Password changed for user {}", userId);
    }

    public DetailUserDTO getUserById(UUID userId) {
        log.info("UserService - getUserById : Fetching user detail for {}", userId);
        User user = fetchUserById(userId);
        List<BaseDetailBookingDTO> bookings = bookingService.findBookingsByUserId(userId);
        return DetailUserDTO.of(user, bookings);
    }

    public User getUserBySubject() {
        return fetchUserById(jwtInspector.getSubject());
    }

    public User getUserEntityById(UUID userId) {
        return fetchUserById(userId);
    }

    public void changeRole(UUID userId, RoleUpdateDTO dto) {
        log.info("UserService - changeRole : Changing role of user {} to {}", userId, dto.getRole());
        if (dto.getRole() == Role.REVOKED) {
            log.error("UserService - changeRole : Attempted to set role REVOKED via changeRole for user {} — use the revoke endpoint instead", userId);
            throw new ServiceException("Use the revoke endpoint to revoke a user");
        }
        try (Transaction tx = database.beginTransaction()) {
            User user = fetchUserById(userId);
            user.setRole(dto.getRole());
            userRepository.update(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - changeRole : Error changing role for user {}", userId, e);
            throw new ServiceException("Error while changing role. Try again later.");
        }
        log.info("UserService - changeRole : Role of user {} changed to {}", userId, dto.getRole());
    }

    public Optional<User> findByEmail(String email) {
        log.info("UserService - findByEmail : Looking up user by email {}", email);
        return userRepository.findByEmail(email);
    }

    public void activateUser(UUID userId) {
        log.info("UserService - activateUser : Activating user {}", userId);
        try (Transaction tx = database.beginTransaction()) {
            User user = fetchUserById(userId);
            user.setActive(true);
            userRepository.update(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - activateUser : Error activating user {}", userId, e);
            throw new ServiceException("Error while activating user. Try again later.");
        }
        log.info("UserService - activateUser : User {} activated", userId);
    }

    public void revokeUser(UUID userId) {
        log.info("UserService - revokeUser : Revoking user {}", userId);
        try (Transaction tx = database.beginTransaction()) {
            revokeUser(userId, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - revokeUser : Error revoking user {}", userId, e);
            throw new ServiceException("Error while revoking user. Try again later.");
        }
        log.info("UserService - revokeUser : User {} revoked", userId);
    }


    void revokeUser(UUID userId, Transaction tx) {
        User user = fetchUserById(userId);
        user.setActive(false);
        user.setRole(Role.REVOKED);
        userRepository.update(user, tx);
    }

    public void deleteUser(UUID id) {
        log.info("UserService - delete : Deleting user {}", id);
        try (Transaction tx = database.beginTransaction()) {
            userRepository.delete(id, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - delete : Error deleting user {}", id, e);
            throw new ServiceException("Error while deleting user. Try again later.");
        }
    }


    public PagedResultDTO<BaseDetailUserDTO> findAll(UserSearchRequest request) {
        log.info("UserService - findAll : Searching users, page {} size {}", request.getPage(), request.getSize());
        Set<Role> roleConstraint = resolveRoleConstraint(request);
        PagedList<User> pagedList = userRepository.search(request, roleConstraint);
        return PagedResultDTO.of(pagedList, BaseDetailUserDTO::of);
    }

    private Set<Role> resolveRoleConstraint(UserSearchRequest request) {
        if (jwtInspector.hasRole(Role.ADMIN)) {
            return null;
        }
        if (request.getRole() == null) {
            return NON_ADMIN_SEARCHABLE_ROLES;
        }
        if (!NON_ADMIN_SEARCHABLE_ROLES.contains(request.getRole())) {
            log.error("UserService - findAll : Role filter {} not allowed for non-ADMIN caller", request.getRole());
            throw new ServiceException("You can only search for CUSTOMER or ORGANIZER users.");
        }
        return null;
    }

    private User fetchUserById(UUID userId) {
        jwtInspector.checkAccessAllowed(userId);
        return userRepository.getUserById(userId);
    }
}
