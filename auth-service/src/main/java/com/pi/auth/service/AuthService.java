package com.pi.auth.service;

import com.pi.auth.dto.ActivateAccountRequest;
import com.pi.auth.dto.AuthResponse;
import com.pi.auth.dto.ForgotPasswordRequest;
import com.pi.auth.dto.LoginRequest;
import com.pi.auth.dto.RefreshTokenRequest;
import com.pi.auth.dto.RegisterRequest;
import com.pi.auth.dto.RegisterResponse;
import com.pi.auth.dto.ResetPasswordRequest;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void activate(ActivateAccountRequest request);

    void logout(String refreshToken);

    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
