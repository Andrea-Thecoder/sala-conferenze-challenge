package com.naxos.challenge.security;


import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.enumerator.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@ApplicationScoped
@Slf4j
public class JwtInspector {

    @Inject
    JsonWebToken jwt;


    public UUID getSubject() {
        return UUID.fromString(jwt.getSubject());
    }

    public boolean sameSubject(UUID subject) {
        if(subject == null) return false;
        return getSubject().equals(subject);
    }

    public Role getRole() {
        return jwt.getGroups().stream()
                .findFirst()
                .map(Role::valueOf)
                .orElse(null);
    }

    public boolean hasRole(Role role) {
        if (role == null) return false;
        Role userRole = getRole();
        if (userRole == null) return false;
        return userRole.equals(role);
    }

    /**
     * Regola condivisa da User e Booking: una CUSTOMER può accedere solo alle
     * proprie risorse, ADMIN/ORGANIZER a quelle di chiunque.
     */
    public void checkAccessAllowed(UUID targetUserId) {
        if (hasRole(Role.CUSTOMER) && !sameSubject(targetUserId)) {
            log.error("JwtInspector - checkAccessAllowed : JWT subject {} attempted to access a resource belonging to {}",
                    getSubject(), targetUserId);
            throw new ServiceException("Invalid user.");
        }
    }
}
