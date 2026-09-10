package com.sala.challenge.security;


import com.sala.challenge.model.enumerator.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class JwtInspector {

    private static final Set<Role> ELEVATED_ROLES = Set.of(Role.ADMIN, Role.ORGANIZER);

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
     * Regola condivisa da User e Booking: si può accedere alle proprie risorse, o a
     * quelle di chiunque se il ruolo è ADMIN/ORGANIZER. Allow-list, non deny-list:
     * un ruolo assente/non mappabile (claim "groups" mancante o non riconosciuto) non
     * è più trattato come "non CUSTOMER quindi consentito" — deve comunque trattarsi
     * del proprietario della risorsa. Fail-closed di default.
     */
    public void checkAccessAllowed(UUID targetUserId) {
        Role role = getRole();
        boolean elevated = role != null && ELEVATED_ROLES.contains(role);
        if (!elevated && !sameSubject(targetUserId)) {
            log.error("JwtInspector - checkAccessAllowed : JWT subject {} attempted to access a resource belonging to {}",
                    getSubject(), targetUserId);
            throw new ForbiddenException("You are not allowed to access this resource.");
        }
    }
}
