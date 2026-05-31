package com.fantasynations.repository;

import com.fantasynations.entity.MarketPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketPlayerRepository extends JpaRepository<MarketPlayerEntity, UUID> {
    List<MarketPlayerEntity> findByLeagueIdAndAvailableUntilAfter(UUID leagueId, LocalDateTime now);
    long countByLeagueIdAndAvailableUntilAfter(UUID leagueId, LocalDateTime now);
    void deleteByLeagueId(UUID leagueId);
    Optional<MarketPlayerEntity> findByLeagueIdAndPlayerId(UUID leagueId, UUID playerId);
    List<MarketPlayerEntity> findByCycleId(UUID cycleId);
}
