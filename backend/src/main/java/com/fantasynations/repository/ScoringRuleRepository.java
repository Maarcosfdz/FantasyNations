package com.fantasynations.repository;

import com.fantasynations.entity.ScoringRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoringRuleRepository extends JpaRepository<ScoringRuleEntity, String> {
    List<ScoringRuleEntity> findByEnabledTrue();
}
