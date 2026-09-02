package com.naxos.challenge.services;

import com.naxos.challenge.config.AuthConfig;
import com.naxos.challenge.config.TransactionScope;
import com.naxos.challenge.dto.user.LoginCredentialsDTO;
import com.naxos.challenge.dto.user.UserRegistrationDTO;
import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.User;
import com.naxos.challenge.model.enumerator.Role;
import com.naxos.challenge.repository.UserRepository;
import com.naxos.challenge.security.PasswordEncoder;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Slf4j

public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    AuthConfig authConfig;

    @Inject
    UserTransaction userTransaction;


    public UUID registerUser(UserRegistrationDTO dto) {
        log.info("AuthService - registerUser : Starting Registration for new user.");
        User user = dto.toEntity();
        user.setPassword(PasswordEncoder.hash(dto.getPassword(), authConfig.bcryptRounds()));
        try (TransactionScope tx = new TransactionScope(userTransaction)) {
            userRepository.save(user, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("AuthService - registerUser : Error registering user", e);
            throw new ServiceException("Error while registering user. Try again later");
        }
        log.info("AuthService - registerUser : Ending Registration for new user with ID {}", user.getId());
        return user.getId();
    }

    public String login(LoginCredentialsDTO credentials) {
        log.info("AuthService - login : Login attempt for email {}", credentials.getEmail());

        User user = getUserByEmail(credentials);

        if (!user.isActive() || user.getRole() == Role.REVOKED) {
            log.error("AuthService - login : Login denied for disabled/revoked user {}", user.getId());
            throw new ServiceException("Invalid email or password");
        }

        String token = generateToken(user);

        log.info("AuthService - login : Issued token for user {}", user.getId());
        return token;
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
                .expiresIn(Duration.ofMinutes(authConfig.jwtExpirationMinutes()))
                .sign();
    }

}
