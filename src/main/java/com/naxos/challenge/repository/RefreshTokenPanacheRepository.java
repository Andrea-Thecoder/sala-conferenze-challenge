package com.naxos.challenge.repository;

import java.util.UUID;

import com.naxos.challenge.model.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI che espone il CRUD generato da Panache per {@link RefreshToken}, composto
 * (non ereditato) dentro {@link RefreshTokenRepositoryImpl} — stesso motivo di
 * {@link UserPanacheRepository}: tenere Panache fuori dal contratto pubblico.
 */
@ApplicationScoped
class RefreshTokenPanacheRepository implements PanacheRepositoryBase<RefreshToken, UUID> {
}
