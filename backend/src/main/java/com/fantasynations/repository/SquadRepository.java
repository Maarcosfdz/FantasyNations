package com.fantasynations.repository;

import com.fantasynations.entity.SquadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SquadRepository extends JpaRepository<SquadEntity, UUID> {
    Optional<SquadEntity> findByLeagueIdAndUserId(UUID leagueId, UUID userId);
    java.util.List<SquadEntity> findByLeagueId(UUID leagueId);
}
