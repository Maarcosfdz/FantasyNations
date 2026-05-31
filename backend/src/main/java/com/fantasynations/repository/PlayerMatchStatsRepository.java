package com.fantasynations.repository;

import com.fantasynations.entity.PlayerMatchStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerMatchStatsRepository extends JpaRepository<PlayerMatchStatsEntity, UUID> {
    Optional<PlayerMatchStatsEntity> findByPlayerIdAndRealMatchId(UUID playerId, UUID realMatchId);
    List<PlayerMatchStatsEntity> findByRealMatchId(UUID realMatchId);
    List<PlayerMatchStatsEntity> findByPlayerIdAndRealMatchIdIn(UUID playerId, List<UUID> realMatchIds);
    List<PlayerMatchStatsEntity> findByRealMatchIdIn(List<UUID> realMatchIds);
}
