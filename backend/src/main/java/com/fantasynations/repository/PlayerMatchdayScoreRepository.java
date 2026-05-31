package com.fantasynations.repository;

import com.fantasynations.entity.PlayerMatchdayScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlayerMatchdayScoreRepository extends JpaRepository<PlayerMatchdayScoreEntity, UUID> {
    List<PlayerMatchdayScoreEntity> findByMatchdayScoreId(UUID matchdayScoreId);
    void deleteByMatchdayScoreId(UUID matchdayScoreId);

    /**
     * Per-player points across one matchday. Every league writes its own
     * matchday_scores row, but per-player points are computed from the same
     * global stats so the values should be identical across leagues. MAX is
     * used defensively to collapse duplicate rows.
     */
    @Query("""
        SELECT p.playerId AS playerId, MAX(p.points) AS points
        FROM PlayerMatchdayScoreEntity p
        JOIN MatchdayScoreEntity ms ON ms.id = p.matchdayScoreId
        WHERE ms.matchdayId = :matchdayId
        GROUP BY p.playerId
    """)
    List<PlayerPointsRow> findPointsByMatchday(@Param("matchdayId") UUID matchdayId);

    /** Average historical points per player across every matchday with number STRICTLY LESS than the given value. */
    @Query("""
        SELECT p.playerId AS playerId, AVG(p.points) AS avgPoints
        FROM PlayerMatchdayScoreEntity p
        JOIN MatchdayScoreEntity ms ON ms.id = p.matchdayScoreId
        JOIN MatchdayEntity md ON md.id = ms.matchdayId
        WHERE md.number < :maxNumberExclusive
        GROUP BY p.playerId
    """)
    List<PlayerAvgRow> findHistoricalAveragesBefore(@Param("maxNumberExclusive") int maxNumberExclusive);

    interface PlayerPointsRow {
        UUID getPlayerId();
        Integer getPoints();
    }

    interface PlayerAvgRow {
        UUID getPlayerId();
        Double getAvgPoints();
    }
}
