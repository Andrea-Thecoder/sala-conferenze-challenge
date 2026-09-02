package com.naxos.challenge.audit;

import jakarta.inject.Inject;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.naxos.challenge.model.AbstractAudit;


public class AuditListener {

    @Inject
    JsonWebToken jwt;

    @PrePersist
    public void onCreate(AbstractAudit entity) {
        String currentUser = resolveCurrentUser();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
    }

    @PreUpdate
    public void onUpdate(AbstractAudit entity) {
        entity.setUpdatedBy(resolveCurrentUser());
    }

    private String resolveCurrentUser() {
        return jwt.getSubject() != null  ? jwt.getSubject() :null;
    }
}
