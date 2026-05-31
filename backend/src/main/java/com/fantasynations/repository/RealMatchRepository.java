package com.fantasynations.repository;

import com.fantasynations.entity.RealMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RealMatchRepository extends JpaRepository<RealMatchEntity, UUID> {
    List<RealMatchEntity> findByMatchdayId(UUID matchdayId);
}
