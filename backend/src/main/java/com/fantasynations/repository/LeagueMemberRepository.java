package com.fantasynations.repository;

import com.fantasynations.entity.LeagueMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueMemberRepository extends JpaRepository<LeagueMemberEntity, UUID> {
    List<LeagueMemberEntity> findByUserId(UUID userId);
    List<LeagueMemberEntity> findByLeagueId(UUID leagueId);
    Optional<LeagueMemberEntity> findByLeagueIdAndUserId(UUID leagueId, UUID userId);
    boolean existsByLeagueIdAndUserId(UUID leagueId, UUID userId);
}
