package com.fantasynations.entity;

import com.fantasynations.domain.MatchdayPhase;
import com.fantasynations.domain.MatchdayStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matchdays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchdayEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "number", nullable = false, unique = true)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private MatchdayPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchdayStatus status;

    @Column(name = "lock_at")
    private LocalDateTime lockAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = MatchdayStatus.SCHEDULED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
