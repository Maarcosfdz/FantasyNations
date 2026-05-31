package com.fantasynations.entity;

import com.fantasynations.domain.RealMatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "real_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealMatchEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "matchday_id", nullable = false)
    private UUID matchdayId;

    @Column(name = "kickoff", nullable = false)
    private LocalDateTime kickoff;

    @Column(name = "home_team", nullable = false)
    private String homeTeam;

    @Column(name = "away_team", nullable = false)
    private String awayTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RealMatchStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = RealMatchStatus.SCHEDULED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
