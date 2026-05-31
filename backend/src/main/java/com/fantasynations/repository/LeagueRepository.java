package com.fantasynations.repository;

import com.fantasynations.entity.LeagueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository extends JpaRepository<LeagueEntity, UUID> {
    Optional<LeagueEntity> findByInviteCode(String inviteCode);
}
