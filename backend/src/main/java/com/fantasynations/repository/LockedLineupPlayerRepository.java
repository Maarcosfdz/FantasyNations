package com.fantasynations.repository;

import com.fantasynations.entity.LockedLineupPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LockedLineupPlayerRepository extends JpaRepository<LockedLineupPlayerEntity, UUID> {
    List<LockedLineupPlayerEntity> findByLineupIdAndMatchdayId(UUID lineupId, UUID matchdayId);
    List<LockedLineupPlayerEntity> findByLeagueIdAndMatchdayId(UUID leagueId, UUID matchdayId);
    boolean existsByLineupIdAndMatchdayId(UUID lineupId, UUID matchdayId);
}
