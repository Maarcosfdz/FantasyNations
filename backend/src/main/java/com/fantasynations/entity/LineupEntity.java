package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lineups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineupEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private LeagueEntity league;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "lineup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LineupPlayerEntity> players = new ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp at which the lineup was last frozen for a matchday. Users can
     * still edit the live lineup after this; the snapshot in
     * {@code locked_lineup_players} preserves the matchday view.
     */
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "frozen_for_matchday_id")
    private UUID frozenForMatchdayId;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
