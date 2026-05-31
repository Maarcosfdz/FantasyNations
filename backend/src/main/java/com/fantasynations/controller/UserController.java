package com.fantasynations.controller;

import com.fantasynations.dto.AuthResponseDto;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.repository.UserRepository;
import com.fantasynations.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getMe() {
        UserEntity user = userProvider.getCurrentUser();
        return ResponseEntity.ok(new AuthResponseDto(null, user.getId(), user.getEmail(), user.getNickname(), user.getAvatarUrl()));
    }

    @PatchMapping("/me")
    public ResponseEntity<AuthResponseDto> updateProfile(@RequestBody Map<String, String> body) {
        UserEntity user = userProvider.getCurrentUser();
        String nickname = body.get("nickname");
        if (nickname != null) {
            if (nickname.isBlank() || nickname.length() < 3 || nickname.length() > 64) {
                throw new BadRequestException("Nickname must be between 3 and 64 characters");
            }
            user.setNickname(nickname.trim());
        }
        String avatarUrl = body.get("avatarUrl");
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.isBlank() ? null : avatarUrl);
        }
        userRepository.save(user);
        return ResponseEntity.ok(new AuthResponseDto(null, user.getId(), user.getEmail(), user.getNickname(), user.getAvatarUrl()));
    }
}
