package com.fantasynations.service;

import com.fantasynations.dto.*;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto request);
    AuthResponseDto login(LoginRequestDto request);
    void forgotPassword(ForgotPasswordRequestDto request);
    void resetPassword(ResetPasswordRequestDto request);
}
