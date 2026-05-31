package com.fantasynations.repository;

import com.fantasynations.entity.SquadPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SquadPlayerRepository extends JpaRepository<SquadPlayerEntity, UUID> {
    List<SquadPlayerEntity> findBySquadId(UUID squadId);
    Optional<SquadPlayerEntity> findBySquadIdAndPlayerId(UUID squadId, UUID playerId);
    boolean existsBySquadIdAndPlayerId(UUID squadId, UUID playerId);
    List<SquadPlayerEntity> findByPlayerId(UUID playerId);
}
