package com.fantasynations.service;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.dto.SquadPlayerResponseDto;
import com.fantasynations.entity.*;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SquadServiceImpl implements SquadService {

    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public List<SquadPlayerResponseDto> getSquad(UUID leagueId, UUID userId) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("Not a member of this league");
        }
        var squad = squadRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new NotFoundException("Squad not found"));
        return squadPlayerRepository.findBySquadId(squad.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void payReleaseClause(UUID leagueId, UUID playerId, UUID buyerId) {
        var league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException("League not found"));
        var buyerMember = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, buyerId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this league"));

        // Find which squad has this player
        var squadPlayers = squadPlayerRepository.findByPlayerId(playerId);
        var sellerEntry = squadPlayers.stream()
                .filter(sp -> sp.getSquad().getLeague().getId().equals(leagueId))
                .filter(sp -> !sp.getSquad().getUser().getId().equals(buyerId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Player not owned by another member in this league"));

        if (sellerEntry.getProtectedUntil() != null &&
                sellerEntry.getProtectedUntil().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Player is protected from release clause purchases");
        }

        BigDecimal clause = sellerEntry.getReleaseClause();
        if (buyerMember.getMoney().compareTo(clause) < 0) {
            throw new BadRequestException("Insufficient funds to pay release clause");
        }

        // Transfer money and player
        var sellerMember = leagueMemberRepository
                .findByLeagueIdAndUserId(leagueId, sellerEntry.getSquad().getUser().getId())
                .orElseThrow();

        buyerMember.setMoney(buyerMember.getMoney().subtract(clause));
        sellerMember.setMoney(sellerMember.getMoney().add(clause.multiply(BigDecimal.valueOf(0.8))));
        leagueMemberRepository.save(buyerMember);
        leagueMemberRepository.save(sellerMember);

        // Move player to buyer's squad
        var player = sellerEntry.getPlayer();
        squadPlayerRepository.delete(sellerEntry);

        var buyerSquad = squadRepository.findByLeagueIdAndUserId(leagueId, buyerId)
                .orElseThrow(() -> new NotFoundException("Buyer squad not found"));
        int protectionHours = league.getRules().getReleaseClauseProtectionHours();
        var newEntry = SquadPlayerEntity.builder()
                .squad(buyerSquad)
                .player(player)
                .releaseClause(clause.multiply(BigDecimal.valueOf(1.5)))
                .protectedUntil(LocalDateTime.now().plusHours(protectionHours))
                .build();
        squadPlayerRepository.save(newEntry);

        var buyer = userRepository.findById(buyerId).orElseThrow();
        activityLogService.log(league, buyer, ActivityEventType.CLAUSE_PAID,
                Map.of("playerName", player.getName(), "clauseAmount", clause.toString()));
    }

    @Override
    @Transactional
    public SquadPlayerResponseDto updateReleaseClause(UUID leagueId, UUID playerId, UUID userId, BigDecimal newClause) {
        if (newClause == null || newClause.signum() <= 0) {
            throw new BadRequestException("Release clause must be positive");
        }
        var league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException("League not found"));
        var squad = squadRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this league"));

        var entry = squadPlayerRepository.findBySquadIdAndPlayerId(squad.getId(), playerId)
                .orElseThrow(() -> new NotFoundException("Player not in your squad"));

        if (newClause.compareTo(entry.getReleaseClause()) <= 0) {
            throw new BadRequestException("New clause must be higher than the current clause");
        }

        BigDecimal previous = entry.getReleaseClause();
        entry.setReleaseClause(newClause);
        squadPlayerRepository.save(entry);

        var user = userRepository.findById(userId).orElseThrow();
        activityLogService.log(league, user, ActivityEventType.RELEASE_CLAUSE_CHANGED,
                Map.of(
                        "playerName", entry.getPlayer().getName(),
                        "previousClause", previous.toString(),
                        "newClause", newClause.toString()
                ));

        return toDto(entry);
    }

    private SquadPlayerResponseDto toDto(SquadPlayerEntity sp) {
        var p = sp.getPlayer();
        return new SquadPlayerResponseDto(
                sp.getId(), p.getId(), p.getName(), p.getNationalTeam(),
                p.getPosition(), p.getImageRef(), p.getCurrentValue(),
                sp.getReleaseClause(), sp.getProtectedUntil(), sp.getAcquiredAt()
        );
    }
}
