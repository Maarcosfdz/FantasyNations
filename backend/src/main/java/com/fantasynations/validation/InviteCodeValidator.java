package com.fantasynations.validation;

import com.fantasynations.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class InviteCodeValidator {

    public void validate(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Invite code is required");
        }
        if (code.length() != 8) {
            throw new BadRequestException("Invalid invite code format");
        }
        if (!code.matches("[A-Z0-9]+")) {
            throw new BadRequestException("Invalid invite code characters");
        }
    }
}
