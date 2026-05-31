package com.fantasynations.datasource.normalizer;

import com.fantasynations.datasource.dto.ExternalPlayerDto;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.PlayerEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PlayerNormalizer {

    public PlayerEntity normalize(ExternalPlayerDto external) {
        return PlayerEntity.builder()
                .name(external.name())
                .nationalTeam(external.nationalTeam())
                .position(normalizePosition(external.position()))
                .baseValue(BigDecimal.valueOf(external.baseValue()))
                .currentValue(BigDecimal.valueOf(external.baseValue()))
                .imageRef(external.imageRef())
                .active(true)
                .build();
    }

    private PlayerPosition normalizePosition(String pos) {
        return switch (pos.toUpperCase()) {
            case "GK", "GOALKEEPER" -> PlayerPosition.GK;
            case "DEF", "DEFENDER", "CB", "LB", "RB" -> PlayerPosition.DEF;
            case "MID", "MIDFIELDER", "CM", "CDM", "CAM", "LM", "RM" -> PlayerPosition.MID;
            case "FWD", "FORWARD", "ST", "CF", "LW", "RW" -> PlayerPosition.FWD;
            default -> PlayerPosition.MID;
        };
    }
}
