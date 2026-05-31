package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "squad_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SquadPlayerEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private SquadEntity squad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(name = "release_clause", nullable = false)
    private BigDecimal releaseClause;

    @Column(name = "fixed_release_clause_value")
    private BigDecimal fixedReleaseClauseValue;

    @Column(name = "release_clause_manually_raised", nullable = false)
    private boolean releaseClauseManuallyRaised;

    @Column(name = "protected_until")
    private LocalDateTime protectedUntil;

    @Column(name = "acquired_at", updatable = false)
    private LocalDateTime acquiredAt;

    @PrePersist
    protected void onCreate() {
        acquiredAt = LocalDateTime.now();
    }
}
