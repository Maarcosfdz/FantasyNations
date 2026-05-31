package com.fantasynations.repository;

import com.fantasynations.entity.MarketValueHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarketValueHistoryRepository
        extends JpaRepository<MarketValueHistoryEntity, UUID> {

    List<MarketValueHistoryEntity> findByPlayerIdOrderByCalculatedAtDesc(UUID playerId);

    /**
     * Used by the dynamic market-value update to guarantee idempotency: a
     * (player, matchday) pair never gets a second history row from the
     * post-matchday update.
     */
    boolean existsByPlayerIdAndMatchdayId(UUID playerId, UUID matchdayId);
}
