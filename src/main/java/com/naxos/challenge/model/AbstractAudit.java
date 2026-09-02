package com.naxos.challenge.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

import com.naxos.challenge.audit.AuditListener;

/**
 * Superclasse comune di auditing per le entity JPA.
 *
 * @MappedSuperclass: i campi qui sotto diventano colonne della tabella di
 * ciascuna sottoclasse (nessuna tabella propria, nessun join) — a differenza
 * di @Entity con ereditarietà JOINED/SINGLE_TABLE, che invece serve quando le
 * sottoclassi sono davvero query-abili/polimorfiche come entità a sé stanti.
 *
 * createdBy/updatedBy sono valorizzati automaticamente da AuditListener,
 * leggendo l'utente autenticato dal JWT (via SecurityIdentity): nessuna
 * entity concreta deve occuparsene esplicitamente.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditListener.class)
public abstract class AbstractAudit {

    @Version
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    @Schema(hidden = true)
    @JsonIgnore
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 255)
    @Schema(hidden = true)
    @JsonIgnore
    private String updatedBy;
}
