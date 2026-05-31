package com.fantasynations.repository;

import com.fantasynations.entity.MatchdayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchdayRepository extends JpaRepository<MatchdayEntity, UUID> {
    Optional<MatchdayEntity> findByNumber(int number);
}
