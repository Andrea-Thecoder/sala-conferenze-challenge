package com.naxos.challenge.repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.naxos.challenge.dto.search.UserSearchRequest;
import com.naxos.challenge.model.User;
import com.naxos.challenge.model.enumerator.Role;
import io.ebean.PagedList;

/**
 * Estende il contratto generico aggiungendo le query specifiche del dominio "User",
 * come la ricerca per email necessaria a login/registrazione.
 */
public interface UserRepository extends GenericRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    User getById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    PagedList<User> search(UserSearchRequest request, Set<Role> roleConstraint);
}
