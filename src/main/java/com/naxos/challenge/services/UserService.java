package com.naxos.challenge.services;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.naxos.challenge.config.AuthConfig;
import com.naxos.challenge.dto.PagedResultDTO;
import com.naxos.challenge.dto.search.UserSearchRequest;
import com.naxos.challenge.dto.user.BaseDetailUserDTO;
import com.naxos.challenge.dto.user.PhoneNumberUpdateDTO;
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

    public UUID createUser(UserRegistrationDTO dto) {
        log.info("UserService - save : Creating new user");
        try (Transaction tx = database.beginTransaction()) {
            User user = dto.toEntity();
            user.setPassword(PasswordEncoder.hash(dto.getPassword(), authConfig.bcryptRounds()));
            userRepository.save(user, tx);
            tx.commit();
            log.info("UserService - save : Created user {}", user.getId());
            return user.getId();
        } catch (Exception e) {
            log.error("UserService - save : Error creating user", e);
            throw new ServiceException("Error while creating user. Try again later.");
        }

    }

    public void updatePhoneNumber(UUID userId, PhoneNumberUpdateDTO dto) {
        log.info("UserService - updatePhoneNumber : Updating phone number for user {}", userId);
        // Controllato subito, prima di existsByPhoneNumber: un CUSTOMER non autorizzato
        // non deve poter scoprire se un numero è già in uso su un altro account solo
        // provando ad aggiornarlo.
        checkAccessAllowed(userId);
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            log.error("UserService - updatePhoneNumber : Phone number already in use for user {}", userId);
            throw new ServiceException("Phone number already in use");
        }
        try (Transaction tx = database.beginTransaction()) {
            User user = getUserById(userId);
            dto.toUpdate(user);
            userRepository.update(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("UserService - updatePhoneNumber : Error updating phone number for user {}", userId, e);
            throw new ServiceException("Error while updating phone number. Try again later.");
        }
    }

    public Optional<User> findByEmail(String email) {
        log.info("UserService - findByEmail : Looking up user by email {}", email);
        return userRepository.findByEmail(email);
    }

    public void activateUser(UUID userId) {
        log.info("UserService - activateUser : Activating user {}", userId);
        try (Transaction tx = database.beginTransaction()) {
            User user = getUserById(userId);
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
        User user = getUserById(userId);
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

    private User getUserById(UUID userId) {
        checkAccessAllowed(userId);
        return userRepository.getById(userId);
    }

    private void checkAccessAllowed(UUID userId){
        if(jwtInspector.hasRole(Role.CUSTOMER) && !jwtInspector.sameSubject(userId)) {
            log.error("UserService - checkAccessAllowed : JWT subject {} attempted to access user {}, which is not itself",
                    jwtInspector.getSubject(), userId);
            throw new ServiceException("Invalid user.");
        }
    }
}
