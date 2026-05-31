package com.fantasynations.repository;

import com.fantasynations.domain.BidStatus;
import com.fantasynations.entity.BidEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<BidEntity, UUID> {

    Optional<BidEntity> findByMarketPlayerIdAndUserId(UUID marketPlayerId, UUID userId);

    /**
     * Highest amount first, earliest submission breaks ties. Used by the
     * resolver to pick the winner.
     */
    List<BidEntity> findByMarketPlayerIdAndStatusOrderByAmountDescSubmittedAtAsc(
            UUID marketPlayerId, BidStatus status);

    List<BidEntity> findByCycleId(UUID cycleId);
}
