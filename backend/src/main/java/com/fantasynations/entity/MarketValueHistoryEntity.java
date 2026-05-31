package com.fantasynations.entity;

import com.fantasynations.domain.MarketValueChangeReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "market_value_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketValueHistoryEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "old_value", nullable = false)
    private BigDecimal oldValue;

    @Column(name = "new_value", nullable = false)
    private BigDecimal newValue;

    @Column(name = "delta", nullable = false)
    private BigDecimal delta;

    @Column(name = "delta_percent")
    private BigDecimal deltaPercent;

    @Column(name = "momentum_score")
    private Integer momentumScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private MarketValueChangeReason reason;

    @Column(name = "matchday_id")
    private UUID matchdayId;

    @Column(name = "market_cycle_id")
    private UUID marketCycleId;

    @Column(name = "breakdown_json", columnDefinition = "TEXT")
    private String breakdownJson;

    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) calculatedAt = LocalDateTime.now();
    }
}
