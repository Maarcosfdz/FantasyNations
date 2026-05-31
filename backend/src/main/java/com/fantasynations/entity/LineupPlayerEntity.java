package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "lineup_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineupPlayerEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineup_id", nullable = false)
    private LineupEntity lineup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(name = "position_slot", nullable = false)
    private String positionSlot;
}
