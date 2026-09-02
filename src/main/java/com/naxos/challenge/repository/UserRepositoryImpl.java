package com.naxos.challenge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.config.TransactionScope;
import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Il CRUD generico è delegato a {@link UserPanacheRepository} (composizione, non
 * ereditarietà): AuthService continua a dipendere solo da {@link UserRepository},
 * mai da Panache — le scritture restano vincolate a passare una {@link TransactionScope},
 * perché quel contratto lo abbiamo scritto noi, non Panache.
 */
@ApplicationScoped
@Slf4j
public class UserRepositoryImpl implements UserRepository {

    @Inject
    UserPanacheRepository panache;

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(panache.findById(id));
    }

    @Override
    public List<User> findAll(int page, int size) {
        return panache.findAll().page(page, size).list();
    }

    @Override
    public void save(User entity, TransactionScope tx) {
        panache.persist(entity);
    }

    @Override
    public User update(UUID id, TransactionScope tx) {
        log.info("UserRepository - update: Update user with id {}", id);
        User user = findById(id)
                .orElseThrow(() -> {
                    log.error("UserRepository - update: User not found with id {}", id);
                    return new ServiceException("User not found");
                });
        return panache.getEntityManager().merge(user);
    }

    @Override
    public void delete(UUID id, TransactionScope tx) {
        log.info("UserRepository - delete: Delete user with id {}", id);
        panache.delete(findById(id)
                .orElseThrow(() -> {
                    log.error("UserRepository - delete: User not found with id {}", id);
                    return new ServiceException("User not found");
                }));
    }

    @Override
    public long count() {
        return panache.count();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return panache.find("email", email).firstResultOptional();
    }
}
