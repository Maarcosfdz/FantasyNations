package com.fantasynations.entity;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fantasynations.domain.PlayerPosition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "national_team", nullable = false)
    private String nationalTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private PlayerPosition position;

    @Column(name = "base_value", nullable = false)
    private BigDecimal baseValue;

    @Column(name = "current_value", nullable = false)
    private BigDecimal currentValue;

    /** Original, immutable value assigned when the player was first imported. */
    @Column(name = "initial_market_value", nullable = false)
    private BigDecimal initialMarketValue;

    /** Current dynamic global market value. Persists across restarts. */
    @Column(name = "market_value", nullable = false)
    private BigDecimal marketValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance")
    private Importance importance;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_reputation")
    private LeagueReputation leagueReputation;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private AvailabilityStatus availabilityStatus;

    @Column(name = "image_ref")
    private String imageRef;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availabilityStatus == null) availabilityStatus = AvailabilityStatus.AVAILABLE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
