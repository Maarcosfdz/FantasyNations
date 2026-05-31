package com.fantasynations.repository;

import com.fantasynations.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
    List<PlayerEntity> findByActiveTrue();
    List<PlayerEntity> findByNationalTeam(String nationalTeam);
    Optional<PlayerEntity> findByNameAndNationalTeam(String name, String nationalTeam);
}
