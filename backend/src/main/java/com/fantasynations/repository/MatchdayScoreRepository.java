package com.fantasynations.repository;

import com.fantasynations.entity.MatchdayScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchdayScoreRepository extends JpaRepository<MatchdayScoreEntity, UUID> {
    Optional<MatchdayScoreEntity> findByLeagueIdAndUserIdAndMatchdayId(
            UUID leagueId, UUID userId, UUID matchdayId);
    List<MatchdayScoreEntity> findByLeagueIdAndMatchdayId(UUID leagueId, UUID matchdayId);
    List<MatchdayScoreEntity> findByMatchdayId(UUID matchdayId);
}
