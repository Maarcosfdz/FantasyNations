package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "player_matchday_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"matchday_score_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMatchdayScoreEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "matchday_score_id", nullable = false)
    private UUID matchdayScoreId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "position_slot", nullable = false)
    private String positionSlot;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "breakdown_json", columnDefinition = "TEXT")
    private String breakdownJson;
}
