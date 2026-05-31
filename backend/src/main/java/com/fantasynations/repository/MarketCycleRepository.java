package com.fantasynations.repository;

import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.entity.MarketCycleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketCycleRepository extends JpaRepository<MarketCycleEntity, UUID> {
    Optional<MarketCycleEntity> findFirstByLeagueIdAndStatusOrderByCycleNumberDesc(
            UUID leagueId, MarketCycleStatus status);

    Optional<MarketCycleEntity> findFirstByLeagueIdOrderByCycleNumberDesc(UUID leagueId);

    List<MarketCycleEntity> findByStatusAndClosesAtBefore(
            MarketCycleStatus status, LocalDateTime when);
}
