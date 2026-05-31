package com.fantasynations.service;

import com.fantasynations.dto.CreatePlayerRequestDto;
import com.fantasynations.dto.PlayerResponseDto;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    @Override
    public List<PlayerResponseDto> getAllActivePlayers() {
        return playerRepository.findByActiveTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PlayerResponseDto getPlayer(UUID id) {
        var player = playerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found"));
        return toDto(player);
    }

    @Override
    public PlayerResponseDto createPlayer(CreatePlayerRequestDto request) {
        var player = PlayerEntity.builder()
                .name(request.name())
                .nationalTeam(request.nationalTeam())
                .position(request.position())
                .baseValue(request.baseValue())
                .currentValue(request.baseValue())
                .imageRef(request.imageRef())
                .active(true)
                .build();
        playerRepository.save(player);
        return toDto(player);
    }

    private PlayerResponseDto toDto(PlayerEntity p) {
        return new PlayerResponseDto(
                p.getId(), p.getName(), p.getNationalTeam(),
                p.getPosition(), p.getBaseValue(), p.getCurrentValue(),
                p.getImageRef(), p.isActive()
        );
    }
}
