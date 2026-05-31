package com.fantasynations.entity;

import com.fantasynations.domain.MatchdayAggregationReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matchday_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"league_id", "user_id", "matchday_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchdayScoreEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "league_id", nullable = false)
    private UUID leagueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "matchday_id", nullable = false)
    private UUID matchdayId;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private MatchdayAggregationReason reason;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @PrePersist
    protected void onCreate() {
        if (aggregatedAt == null) aggregatedAt = LocalDateTime.now();
    }
}
