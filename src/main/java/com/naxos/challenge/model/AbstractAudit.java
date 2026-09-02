package com.naxos.challenge.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.ebean.annotation.WhoCreated;
import io.ebean.annotation.WhoModified;
import jakarta.persistence.*;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;

/**
 * Superclasse comune di auditing per le entity JPA.
 *
 * @MappedSuperclass: i campi qui sotto diventano colonne della tabella di
 * ciascuna sottoclasse (nessuna tabella propria, nessun join) — a differenza
 * di @Entity con ereditarietà JOINED/SINGLE_TABLE, che invece serve quando le
 * sottoclassi sono davvero query-abili/polimorfiche come entità a sé stanti.
 * <p>
 * createdBy/updatedBy sono valorizzati automaticamente da Ebean (@WhoCreated/@WhoModified)
 * tramite {@link com.naxos.challenge.config.CurrentUserProviderImpl}, che legge il subject
 * dal JWT: nessuna entity concreta deve occuparsene esplicitamente.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractAudit extends Model {

    @Version
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "version", nullable = false)
    private Long version;

    @WhenCreated
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @WhenModified
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @WhoCreated
    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    @Schema(hidden = true)
    @JsonIgnore
    private String createdBy;

    @WhoModified
    @Column(name = "updated_by", nullable = false, length = 255)
    @Schema(hidden = true)
    @JsonIgnore
    private String updatedBy;
}
