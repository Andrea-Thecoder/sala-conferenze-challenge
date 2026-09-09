package com.sala.challenge.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sala.challenge.dto.search.UserSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class UserRepositoryImpl implements UserRepository {

    @Inject
    Database database;

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(database.find(User.class, id));
    }

    @Override
    public User getUserById(UUID id) {
        return findById(id).orElseThrow(() -> {
                    log.error("Error finding user by id {}", id);
                    return new ServiceException("User not found");
                }
        );
    }

    @Override
    public List<User> findAll(int page, int size) {
        return database.find(User.class)
                .setFirstRow(page * size)
                .setMaxRows(size)
                .findList();
    }

    @Override
    public void save(User entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public void update(User entity, Transaction tx) {
        log.info("UserRepository - update: Update user with id {}", entity.getId());
        entity.update(tx);
    }

    @Override
    public void delete(UUID id, Transaction tx) {
        log.info("UserRepository - delete: Delete user with id {}", id);
        User user = getUserById(id);
        user.delete(tx);
    }

    @Override
    public long count() {
        return database.find(User.class).findCount();
    }

    @Override
    public boolean existsByEmail(String email) {
        return database.find(User.class).where().eq("email", email).exists();
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return database.find(User.class).where().eq("phoneNumber", phoneNumber).exists();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return database.find(User.class)
                .where().eq("email", email)
                .findOneOrEmpty();
    }

    @Override
    public PagedList<User> search(UserSearchRequest request, Set<Role> roleConstraint) {
        ExpressionList<User> exl = database.find(User.class).where();
        request.applyFilters(exl);
        if (roleConstraint != null && !roleConstraint.isEmpty()) {
            exl.in("role", roleConstraint);
        }
        request.applySortAndPagination(exl, "lastName");
        return exl.findPagedList();
    }
}
