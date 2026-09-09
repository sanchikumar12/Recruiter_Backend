package com.pi.auth.service;

import com.pi.auth.dto.ActivateAccountRequest;
import com.pi.auth.dto.AuthResponse;
import com.pi.auth.dto.ForgotPasswordRequest;
import com.pi.auth.dto.LoginRequest;
import com.pi.auth.dto.RefreshTokenRequest;
import com.pi.auth.dto.RegisterRequest;
import com.pi.auth.dto.RegisterResponse;
import com.pi.auth.dto.ResetPasswordRequest;
import com.pi.auth.entity.ActivationToken;
import com.pi.auth.entity.AuthUser;
import com.pi.auth.entity.PasswordResetToken;
import com.pi.auth.enums.AccountStatus;
import com.pi.auth.enums.Role;
import com.pi.auth.exception.AuthException;
import com.pi.auth.exception.InvalidTokenException;
import com.pi.auth.repository.ActivationTokenRepository;
import com.pi.auth.repository.AuthUserRepository;
import com.pi.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthUser sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new AuthUser();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("test@366pi.com");
        sampleUser.setPasswordHash("hashedPassword");
        sampleUser.setRole(Role.CANDIDATE);
        sampleUser.setStatus(AccountStatus.ACTIVE);
        sampleUser.setEmailVerified(true);
        sampleUser.setFailedLoginAttempts(0);
        sampleUser.setCreatedAt(Instant.now());
        sampleUser.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("register should generate secure random password when none provided")
    void register_ServerGeneratedPassword_Success() {
        RegisterRequest request = new RegisterRequest("new@366pi.com", null);

        when(authUserRepository.existsByEmailIgnoreCase("new@366pi.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedRandomHash");

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("new@366pi.com", response.email());
        assertNotNull(response.generatedPassword());
        assertTrue(response.generatedPassword().length() >= 8);
        assertEquals("ACTIVE", response.status());
        verify(authUserRepository).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("register should throw conflict exception when email exists")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("existing@366pi.com", "Password123!");

        when(authUserRepository.existsByEmailIgnoreCase("existing@366pi.com")).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class, () -> authService.register(request));
        assertEquals("EMAIL_ALREADY_REGISTERED", ex.getCode());
        verify(authUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("login should succeed for ACTIVE account with valid password")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@366pi.com", "validPassword");

        when(authUserRepository.findByEmailIgnoreCase("test@366pi.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("validPassword", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("jwt.access.token");
        when(refreshTokenService.createRefreshToken(sampleUser.getId())).thenReturn("refresh-token-uuid");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.access.token", response.accessToken());
        assertEquals("refresh-token-uuid", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals(0, sampleUser.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("login should reject when account is PENDING_ACTIVATION")
    void login_PendingActivation_ThrowsException() {
        sampleUser.setStatus(AccountStatus.PENDING_ACTIVATION);
        LoginRequest request = new LoginRequest("test@366pi.com", "validPassword");

        when(authUserRepository.findByEmailIgnoreCase("test@366pi.com")).thenReturn(Optional.of(sampleUser));

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("ACCOUNT_NOT_ACTIVE", ex.getCode());
    }

    @Test
    @DisplayName("login should increment failed login attempts on wrong password")
    void login_WrongPassword_IncrementsFailedAttempts() {
        LoginRequest request = new LoginRequest("test@366pi.com", "wrongPassword");

        when(authUserRepository.findByEmailIgnoreCase("test@366pi.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        assertEquals(1, sampleUser.getFailedLoginAttempts());
        verify(authUserRepository).save(sampleUser);
    }

    @Test
    @DisplayName("login should lock account on 5th failed attempt")
    void login_FifthFailedAttempt_LocksAccount() {
        sampleUser.setFailedLoginAttempts(4);
        LoginRequest request = new LoginRequest("test@366pi.com", "wrongPassword");

        when(authUserRepository.findByEmailIgnoreCase("test@366pi.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        assertEquals(5, sampleUser.getFailedLoginAttempts());
        assertEquals(AccountStatus.LOCKED, sampleUser.getStatus());
        assertNotNull(sampleUser.getLockedUntil());
        verify(authUserRepository).save(sampleUser);
    }

    @Test
    @DisplayName("refresh should rotate token and return new access token")
    void refresh_Success() {
        UUID userId = sampleUser.getId();
        RefreshTokenRequest request = new RefreshTokenRequest("rawRefreshToken");

        when(refreshTokenService.rotateRefreshToken("rawRefreshToken"))
                .thenReturn(new RefreshTokenService.RotationResult(userId, "newRawRefreshToken"));
        when(authUserRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("new.access.token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new.access.token", response.accessToken());
        assertEquals("newRawRefreshToken", response.refreshToken());
    }

    @Test
    @DisplayName("forgotPassword should generate token when user exists")
    void forgotPassword_UserExists_GeneratesToken() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@366pi.com");

        when(authUserRepository.findByEmailIgnoreCase("test@366pi.com")).thenReturn(Optional.of(sampleUser));
        when(refreshTokenService.hashToken(anyString())).thenReturn("hashedResetToken");

        String resetToken = authService.forgotPassword(request);

        assertNotNull(resetToken);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("forgotPassword should return null when user does not exist")
    void forgotPassword_UserNotFound_ReturnsNull() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@366pi.com");

        when(authUserRepository.findByEmailIgnoreCase("nonexistent@366pi.com")).thenReturn(Optional.empty());

        String resetToken = authService.forgotPassword(request);

        assertNull(resetToken);
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword should update password and revoke refresh tokens on valid token")
    void resetPassword_ValidToken_Success() {
        UUID userId = sampleUser.getId();
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenHash("hashedResetToken");
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        token.setUsed(false);

        ResetPasswordRequest request = new ResetPasswordRequest("rawResetToken", "NewSecurePassword123!");

        when(refreshTokenService.hashToken("rawResetToken")).thenReturn("hashedResetToken");
        when(passwordResetTokenRepository.findByTokenHash("hashedResetToken")).thenReturn(Optional.of(token));
        when(authUserRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("newEncodedHash");

        authService.resetPassword(request);

        assertTrue(token.isUsed());
        assertEquals("newEncodedHash", sampleUser.getPasswordHash());
        verify(passwordResetTokenRepository).save(token);
        verify(authUserRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllForUser(userId);
    }

    @Test
    @DisplayName("logout should revoke refresh token")
    void logout_Success() {
        authService.logout("my-refresh-token");
        verify(refreshTokenService).revokeRefreshToken("my-refresh-token");
    }
}
