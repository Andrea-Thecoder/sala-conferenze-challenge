package com.naxos.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "app_refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_family", columnList = "family_id"),
                @Index(name = "idx_refresh_token_user", columnList = "user_app_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AppRefreshToken extends Model {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    @Schema(hidden = true)
    @JsonIgnore
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_app_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private AppRefreshToken replacedByToken;

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
