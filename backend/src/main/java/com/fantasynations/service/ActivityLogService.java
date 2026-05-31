package com.fantasynations.service;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.dto.ActivityEntryDto;
import com.fantasynations.entity.ActivityLogEntity;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void log(LeagueEntity league, UserEntity user, ActivityEventType eventType, Map<String, Object> payload) {
        var entry = ActivityLogEntity.builder()
                .league(league)
                .user(user)
                .eventType(eventType)
                .payload(payload)
                .build();
        activityLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ActivityEntryDto> getLeagueActivity(UUID leagueId) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        return activityLogRepository
                .findByLeagueIdAndCreatedAtAfterOrderByCreatedAtDesc(leagueId, oneMonthAgo)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ActivityEntryDto toDto(ActivityLogEntity e) {
        return new ActivityEntryDto(
                e.getId(),
                e.getEventType(),
                e.getUser() != null ? e.getUser().getNickname() : "System",
                e.getPayload(),
                e.getCreatedAt()
        );
    }
}
