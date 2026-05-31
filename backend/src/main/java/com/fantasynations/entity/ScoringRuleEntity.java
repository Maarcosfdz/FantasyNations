package com.fantasynations.entity;

import com.fantasynations.domain.EventScope;
import com.fantasynations.domain.ScoringRuleCategory;
import com.fantasynations.domain.ScoringRulePosition;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scoring_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringRuleEntity {

    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 64)
    private String code;

    @Column(name = "value", nullable = false)
    private int value;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ScoringRuleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private ScoringRulePosition position;

    @Column(name = "threshold")
    private Integer threshold;

    @Column(name = "bucket_size")
    private Integer bucketSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_scope", nullable = false)
    private EventScope eventScope;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (eventScope == null) eventScope = EventScope.NORMAL_OR_EXTRA_TIME;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
