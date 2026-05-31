package com.fantasynations.service;

import com.fantasynations.dto.CreatePlayerRequestDto;
import com.fantasynations.dto.PlayerResponseDto;
import java.util.List;
import java.util.UUID;

public interface PlayerService {
    List<PlayerResponseDto> getAllActivePlayers();
    PlayerResponseDto getPlayer(UUID id);
    PlayerResponseDto createPlayer(CreatePlayerRequestDto request);
}
