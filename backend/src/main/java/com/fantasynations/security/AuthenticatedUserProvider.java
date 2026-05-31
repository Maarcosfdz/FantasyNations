package com.fantasynations.security;

import com.fantasynations.entity.UserEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public UserEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Not authenticated");
        }
        String userId = ((UserDetails) auth.getPrincipal()).getUsername();
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ForbiddenException("User not found"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
