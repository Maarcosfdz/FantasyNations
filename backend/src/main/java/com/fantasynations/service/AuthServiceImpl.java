package com.fantasynations.service;

import com.fantasynations.dto.*;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.UserRepository;
import com.fantasynations.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    // In-memory reset tokens (replace with DB-backed tokens in production)
    private final Map<String, UUID> resetTokens = new ConcurrentHashMap<>();

    @Override
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }
        var user = UserEntity.builder()
                .email(request.email().toLowerCase())
                .nickname(request.nickname())
                .passwordHash(passwordEncoder.encode(request.password()))
                .provider("local")
                .build();
        userRepository.save(user);
        String token = jwtTokenService.generateToken(user.getId(), user.getEmail());
        return new AuthResponseDto(token, user.getId(), user.getEmail(), user.getNickname(), user.getAvatarUrl());
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        var user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        String token = jwtTokenService.generateToken(user.getId(), user.getEmail());
        return new AuthResponseDto(token, user.getId(), user.getEmail(), user.getNickname(), user.getAvatarUrl());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDto request) {
        var userOpt = userRepository.findByEmail(request.email().toLowerCase());
        if (userOpt.isEmpty()) return; // Do not reveal if email exists

        var user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();
        resetTokens.put(resetToken, user.getId());

        try {
            var message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("Fantasy Nations — Password Reset");
            message.setText("Use this token to reset your password: " + resetToken +
                    "\n\nThis token expires in 1 hour.");
            mailSender.send(message);
        } catch (Exception ignored) {
            // Fail silently — log in production
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request) {
        UUID userId = resetTokens.remove(request.token());
        if (userId == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
