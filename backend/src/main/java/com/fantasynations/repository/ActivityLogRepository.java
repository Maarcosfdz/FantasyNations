package com.fantasynations.repository;

import com.fantasynations.entity.ActivityLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, UUID> {
    List<ActivityLogEntity> findByLeagueIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID leagueId, LocalDateTime after);
}
