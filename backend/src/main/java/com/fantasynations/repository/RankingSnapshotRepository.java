package com.fantasynations.repository;

import com.fantasynations.entity.RankingSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshotEntity, UUID> {
    @Query("SELECT r FROM RankingSnapshotEntity r WHERE r.league.id = :leagueId " +
           "AND r.snapshotAt = (SELECT MAX(r2.snapshotAt) FROM RankingSnapshotEntity r2 WHERE r2.league.id = :leagueId)")
    List<RankingSnapshotEntity> findLatestByLeagueId(UUID leagueId);
}
