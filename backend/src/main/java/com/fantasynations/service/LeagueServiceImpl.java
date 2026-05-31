package com.fantasynations.service;

import com.fantasynations.domain.LeagueRole;
import com.fantasynations.domain.LeagueRules;
import com.fantasynations.dto.*;
import com.fantasynations.entity.*;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.*;
import com.fantasynations.squad.InitialSquadAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeagueServiceImpl implements LeagueService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final UserRepository userRepository;
    private final SquadRepository squadRepository;
    private final ActivityLogService activityLogService;
    private final MarketService marketService;
    private final InitialSquadAssignmentService initialSquadAssignmentService;

    @Override
    @Transactional
    public LeagueResponseDto createLeague(CreateLeagueRequestDto request, UUID ownerId) {
        var owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var rules = new LeagueRules();
        // Standard FantasyNations start: startingBudget and initial-squad
        // settings are NOT user-configurable. Only league-shape settings may
        // be overridden via the request.
        if (request.moneyPerPoint() != null) rules.setMoneyPerPoint(request.moneyPerPoint());
        if (request.releaseClauseProtectionHours() != null) rules.setReleaseClauseProtectionHours(request.releaseClauseProtectionHours());
        if (request.marketRefreshIntervalHours() != null) rules.setMarketRefreshIntervalHours(request.marketRefreshIntervalHours());
        if (request.marketPlayersCount() != null) rules.setMarketPlayersCount(request.marketPlayersCount());

        var league = LeagueEntity.builder()
                .name(request.name())
                .owner(owner)
                .inviteCode(generateUniqueInviteCode())
                .rules(rules)
                .build();
        leagueRepository.save(league);

        var member = LeagueMemberEntity.builder()
                .league(league)
                .user(owner)
                .role(LeagueRole.OWNER)
                .money(BigDecimal.ZERO) // overwritten by the assignment service
                .build();
        leagueMemberRepository.save(member);

        var squad = SquadEntity.builder()
                .league(league)
                .user(owner)
                .build();
        squadRepository.save(squad);

        // Mandatory standard start: assign 15 random players + remaining money.
        initialSquadAssignmentService.assignFor(league, member, squad);

        activityLogService.log(league, owner, com.fantasynations.domain.ActivityEventType.LEAGUE_CREATED,
                java.util.Map.of("leagueName", league.getName()));

        marketService.initializeMarketIfMissing(league.getId());

        return toDto(league, 1);
    }

    @Override
    @Transactional
    public LeagueResponseDto joinLeague(JoinLeagueRequestDto request, UUID userId) {
        var league = leagueRepository.findByInviteCode(request.inviteCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Invalid invite code"));
        if (leagueMemberRepository.existsByLeagueIdAndUserId(league.getId(), userId)) {
            throw new BadRequestException("You are already a member of this league");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var member = LeagueMemberEntity.builder()
                .league(league)
                .user(user)
                .role(LeagueRole.MEMBER)
                .money(BigDecimal.ZERO) // overwritten by the assignment service
                .build();
        leagueMemberRepository.save(member);

        var squad = SquadEntity.builder()
                .league(league)
                .user(user)
                .build();
        squadRepository.save(squad);

        // Mandatory standard start: assign 15 random players + remaining money.
        initialSquadAssignmentService.assignFor(league, member, squad);

        activityLogService.log(league, user, com.fantasynations.domain.ActivityEventType.USER_JOINED,
                java.util.Map.of("nickname", user.getNickname()));

        int count = leagueMemberRepository.findByLeagueId(league.getId()).size();
        return toDto(league, count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeagueResponseDto> getUserLeagues(UUID userId) {
        return leagueMemberRepository.findByUserId(userId).stream()
                .map(m -> {
                    var league = m.getLeague();
                    int count = leagueMemberRepository.findByLeagueId(league.getId()).size();
                    return toDto(league, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LeagueResponseDto getLeague(UUID leagueId, UUID userId) {
        var league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException("League not found"));
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("You are not a member of this league");
        }
        int count = leagueMemberRepository.findByLeagueId(leagueId).size();
        return toDto(league, count);
    }

    @Override
    @Transactional
    public LeagueResponseDto updateLeagueSettings(UUID leagueId, LeagueRules rules, UUID userId) {
        var league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException("League not found"));
        if (!league.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the league owner can change settings");
        }
        // The standard FantasyNations start is mandatory. Settings updates must
        // NOT be able to override budget or initial-squad shape; preserve the
        // existing values and only honour the editable league-shape fields.
        LeagueRules current = league.getRules();
        current.setMoneyPerPoint(rules.getMoneyPerPoint());
        current.setReleaseClauseProtectionHours(rules.getReleaseClauseProtectionHours());
        current.setMarketRefreshIntervalHours(rules.getMarketRefreshIntervalHours());
        current.setMarketPlayersCount(rules.getMarketPlayersCount());
        current.setMaxPlayersPerSquad(rules.getMaxPlayersPerSquad());
        current.setMinLineupPlayers(rules.getMinLineupPlayers());
        current.setFormationRulesEnabled(rules.isFormationRulesEnabled());
        league.setRules(current);
        leagueRepository.save(league);

        var owner = league.getOwner();
        activityLogService.log(league, owner, com.fantasynations.domain.ActivityEventType.RULES_CHANGED,
                java.util.Map.of());

        int count = leagueMemberRepository.findByLeagueId(leagueId).size();
        return toDto(league, count);
    }

    @Override
    @Transactional(readOnly = true)
    public LeagueMemberMeResponseDto getCurrentMember(UUID leagueId, UUID userId) {
        var member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this league"));
        return new LeagueMemberMeResponseDto(
                member.getUser().getId(),
                member.getLeague().getId(),
                member.getMoney(),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    private LeagueResponseDto toDto(LeagueEntity league, int memberCount) {
        return new LeagueResponseDto(
                league.getId(),
                league.getName(),
                league.getInviteCode(),
                league.getOwner().getId(),
                league.getOwner().getNickname(),
                memberCount,
                league.getRules(),
                league.getCreatedAt()
        );
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateCode();
        } while (leagueRepository.findByInviteCode(code).isPresent());
        return code;
    }

    private String generateCode() {
        var sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
