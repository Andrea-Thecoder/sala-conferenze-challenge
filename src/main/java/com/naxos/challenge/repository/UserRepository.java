package com.naxos.challenge.repository;

import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.model.User;

/**
 * Estende il contratto generico aggiungendo le query specifiche del dominio "User",
 * come la ricerca per email necessaria a login/registrazione.
 */
public interface UserRepository extends GenericRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    User getById(UUID id);

    boolean existsByEmail(String email);
}
