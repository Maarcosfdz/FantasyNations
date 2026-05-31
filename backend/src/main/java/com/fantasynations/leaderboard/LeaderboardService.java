package com.fantasynations.leaderboard;

import com.fantasynations.dto.RankingEntryDto;
import com.fantasynations.entity.RankingSnapshotEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.RankingSnapshotRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RankingSnapshotRepository rankingSnapshotRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;

    @Transactional(readOnly = true)
    public List<RankingEntryDto> getLeagueRanking(UUID leagueId, UUID userId) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("Not a member of this league");
        }

        Map<UUID, BigDecimal> squadValueByUser = computeSquadValues(leagueId);

        var snapshots = rankingSnapshotRepository.findLatestByLeagueId(leagueId);

        if (!snapshots.isEmpty()) {
            return snapshots.stream()
                    .sorted(Comparator.comparingInt(RankingSnapshotEntity::getRank))
                    .map(s -> new RankingEntryDto(
                            s.getRank(),
                            s.getUser().getId(),
                            s.getUser().getNickname(),
                            s.getUser().getAvatarUrl(),
                            s.getTotalPoints(),
                            squadValueByUser.getOrDefault(s.getUser().getId(), BigDecimal.ZERO)))
                    .collect(Collectors.toList());
        }

        // No snapshots yet - seed entries from current members so squad values
        // still show on day one.
        var members = leagueMemberRepository.findByLeagueId(leagueId);
        var entries = new ArrayList<RankingEntryDto>();
        for (int i = 0; i < members.size(); i++) {
            var m = members.get(i);
            entries.add(new RankingEntryDto(
                    i + 1,
                    m.getUser().getId(),
                    m.getUser().getNickname(),
                    m.getUser().getAvatarUrl(),
                    0,
                    squadValueByUser.getOrDefault(m.getUser().getId(), BigDecimal.ZERO)));
        }
        return entries;
    }

    /**
     * Sum of {@code marketValue} for every player owned by each user in the
     * given league. Uses CURRENT marketValue, not initialMarketValue, per
     * the spec. Users with no squad map to 0.
     */
    private Map<UUID, BigDecimal> computeSquadValues(UUID leagueId) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        List<SquadEntity> squads = squadRepository.findByLeagueId(leagueId);
        for (SquadEntity squad : squads) {
            BigDecimal sum = BigDecimal.ZERO;
            for (SquadPlayerEntity sp : squadPlayerRepository.findBySquadId(squad.getId())) {
                BigDecimal v = sp.getPlayer().getMarketValue();
                if (v != null) sum = sum.add(v);
            }
            result.put(squad.getUser().getId(), sum);
        }
        return result;
    }
}
