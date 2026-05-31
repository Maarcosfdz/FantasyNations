package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "locked_lineup_players",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lineup_id", "matchday_id", "position_slot"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockedLineupPlayerEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "lineup_id", nullable = false)
    private UUID lineupId;

    @Column(name = "matchday_id", nullable = false)
    private UUID matchdayId;

    @Column(name = "league_id", nullable = false)
    private UUID leagueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "position_slot", nullable = false)
    private String positionSlot;

    @Column(name = "locked_at", nullable = false, updatable = false)
    private LocalDateTime lockedAt;

    @PrePersist
    protected void onCreate() {
        if (lockedAt == null) lockedAt = LocalDateTime.now();
    }
}
