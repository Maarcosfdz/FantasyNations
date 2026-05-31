package com.fantasynations.validation;

import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LineupValidator {

    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerRepository playerRepository;

    public void validate(Map<UUID, String> playerSlotMap, UUID squadId) {
        if (playerSlotMap == null || playerSlotMap.size() != Formation.LINEUP_SIZE) {
            throw new BadRequestException(
                    "Lineup must have exactly " + Formation.LINEUP_SIZE + " players");
        }

        Map<PlayerPosition, Integer> counts = new EnumMap<>(PlayerPosition.class);
        for (PlayerPosition p : PlayerPosition.values()) counts.put(p, 0);

        Set<String> usedSlots = new HashSet<>();
        for (Map.Entry<UUID, String> entry : playerSlotMap.entrySet()) {
            UUID playerId = entry.getKey();
            String slot = entry.getValue();

            if (slot == null || slot.isBlank()) {
                throw new BadRequestException("Slot is required for every player");
            }
            if (!usedSlots.add(slot)) {
                throw new BadRequestException("Slot used more than once: " + slot);
            }

            if (!squadPlayerRepository.existsBySquadIdAndPlayerId(squadId, playerId)) {
                throw new BadRequestException("Player not in your squad: " + playerId);
            }

            PlayerPosition slotPosition = parseSlotPosition(slot);
            var player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
            if (player.getPosition() != slotPosition) {
                throw new BadRequestException(
                        "Player " + player.getName() + " (" + player.getPosition()
                                + ") cannot be placed in a " + slotPosition + " slot"
                );
            }
            counts.merge(slotPosition, 1, Integer::sum);
        }

        int gk  = counts.get(PlayerPosition.GK);
        int def = counts.get(PlayerPosition.DEF);
        int mid = counts.get(PlayerPosition.MID);
        int fwd = counts.get(PlayerPosition.FWD);
        if (!Formation.isValid(gk, def, mid, fwd)) {
            throw new BadRequestException(
                    "Invalid formation " + gk + "-" + def + "-" + mid + "-" + fwd
                            + ". Allowed: " + Formation.ALL.stream()
                                    .map(Formation::code).toList());
        }
    }

    private PlayerPosition parseSlotPosition(String slot) {
        int dash = slot.indexOf('-');
        String prefix = dash >= 0 ? slot.substring(0, dash) : slot;
        try {
            return PlayerPosition.valueOf(prefix);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown slot position: " + slot);
        }
    }
}
