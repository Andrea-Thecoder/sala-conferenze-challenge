package com.naxos.challenge.repository;

import java.util.UUID;

import com.naxos.challenge.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI che espone il CRUD generato da Panache per {@link User}. Non è il contratto
 * pubblico usato dai Service (quello resta {@link UserRepository}): è composto, non
 * ereditato, dentro {@link UserRepositoryImpl} — così il nostro repository resta libero
 * di definire le proprie firme (es. findById che ritorna Optional) senza collidere con
 * quelle di Panache, che seguono una semantica diversa (nullable, non Optional).
 * Package-private: nessuno fuori da questo package deve poter bypassare UserRepository
 * e parlare direttamente con Panache.
 */
@ApplicationScoped
class UserPanacheRepository implements PanacheRepositoryBase<User, UUID> {
}
