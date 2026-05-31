package com.fantasynations.repository;

import com.fantasynations.entity.LineupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LineupRepository extends JpaRepository<LineupEntity, UUID> {
    Optional<LineupEntity> findByLeagueIdAndUserId(UUID leagueId, UUID userId);
}
