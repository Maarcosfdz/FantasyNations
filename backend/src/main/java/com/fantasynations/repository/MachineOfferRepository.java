package com.fantasynations.repository;

import com.fantasynations.domain.MachineOfferStatus;
import com.fantasynations.entity.MachineOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MachineOfferRepository extends JpaRepository<MachineOfferEntity, UUID> {

    List<MachineOfferEntity> findByLeagueIdAndSellerUserIdAndStatus(
            UUID leagueId, UUID sellerUserId, MachineOfferStatus status);

    Optional<MachineOfferEntity> findBySquadPlayerIdAndStatus(
            UUID squadPlayerId, MachineOfferStatus status);

    List<MachineOfferEntity> findByCycleIdAndStatus(UUID cycleId, MachineOfferStatus status);
}
