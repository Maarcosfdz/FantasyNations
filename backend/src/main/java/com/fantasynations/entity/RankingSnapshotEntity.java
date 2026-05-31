package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ranking_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingSnapshotEntity {

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

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "snapshot_at", updatable = false)
    private LocalDateTime snapshotAt;

    @PrePersist
    protected void onCreate() {
        snapshotAt = LocalDateTime.now();
    }
}
