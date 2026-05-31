package com.fantasynations.controller;

import com.fantasynations.dto.CreatePlayerRequestDto;
import com.fantasynations.dto.PlayerResponseDto;
import com.fantasynations.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public ResponseEntity<List<PlayerResponseDto>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllActivePlayers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponseDto> getPlayer(@PathVariable UUID id) {
        return ResponseEntity.ok(playerService.getPlayer(id));
    }

    @PostMapping
    public ResponseEntity<PlayerResponseDto> createPlayer(@Valid @RequestBody CreatePlayerRequestDto request) {
        return ResponseEntity.ok(playerService.createPlayer(request));
    }
}
