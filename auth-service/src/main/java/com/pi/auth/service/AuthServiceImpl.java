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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final AuthUserRepository authUserRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            AuthUserRepository authUserRepository,
            ActivationTokenRepository activationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.authUserRepository = authUserRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (authUserRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email already registered");
        }

        // Determine password: use supplied password or generate secure random one
        String rawPassword;
        if (request.password() != null && !request.password().isBlank()) {
            rawPassword = request.password();
        } else {
            rawPassword = PasswordGenerator.generateRandomPassword(12);
        }

        // Determine role: use supplied role or default to USER
        Role assignedRole = request.role() != null ? request.role() : Role.USER;

        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();

        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(assignedRole);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        authUserRepository.save(user);

        log.info("User registered [{}] with role [{}] and login password [{}]. Status: ACTIVE", email, assignedRole, rawPassword);

        return new RegisterResponse(
                "User registered successfully.",
                email,
                rawPassword,
                "ACTIVE",
                assignedRole.name(),
                userId
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        AuthUser user = authUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "Invalid credentials"));

        Instant now = Instant.now();

        // Check if account is locked
        if (user.getStatus() == AccountStatus.LOCKED) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                throw new AuthException("ACCOUNT_LOCKED", "Account is temporarily locked. Please try again in a few minutes.");
            } else {
                // Lock expired, unlock automatically
                user.setStatus(AccountStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }
        }

        // Check account activation
        if (user.getStatus() == AccountStatus.PENDING_ACTIVATION) {
            throw new AuthException("ACCOUNT_NOT_ACTIVE", "Account is not active. Please activate your account.");
        }

        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new AuthException("ACCOUNT_DISABLED", "Account has been disabled. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(AccountStatus.LOCKED);
                user.setLockedUntil(now.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
                log.warn("User [{}] locked out for {} minutes after {} failed login attempts", email, LOCKOUT_MINUTES, failedAttempts);
            }

            authUserRepository.save(user);
            throw new AuthException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        // Reset failed attempts on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        authUserRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpiration() / 1000
        );
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.RotationResult rotationResult = refreshTokenService.rotateRefreshToken(request.refreshToken());

        AuthUser user = authUserRepository.findById(rotationResult.userId())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }

        String newAccessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(
                newAccessToken,
                rotationResult.newRefreshToken(),
                "Bearer",
                jwtService.getAccessTokenExpiration() / 1000
        );
    }

    @Override
    @Transactional
    public void activate(ActivateAccountRequest request) {
        String tokenHash = refreshTokenService.hashToken(request.token());

        ActivationToken token = activationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Activation token is invalid or has expired"));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Activation token is invalid or has expired");
        }

        AuthUser user = authUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User associated with token not found"));

        token.setUsed(true);
        activationTokenRepository.save(token);

        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedAt(Instant.now());
        authUserRepository.save(user);

        log.info("User [{}] successfully activated account", user.getEmail());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<AuthUser> userOpt = authUserRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) {
            log.warn("Forgot password requested for non-registered email [{}]", email);
            return null;
        }

        AuthUser user = userOpt.get();
        String rawResetToken = UUID.randomUUID().toString();
        String tokenHash = refreshTokenService.hashToken(rawResetToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setId(UUID.randomUUID());
        resetToken.setUserId(user.getId());
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        resetToken.setUsed(false);
        resetToken.setCreatedAt(Instant.now());

        passwordResetTokenRepository.save(resetToken);

        log.info("Issued password reset token [{}] for user [{}]. Valid for 15 minutes.", rawResetToken, email);
        return rawResetToken;
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = refreshTokenService.hashToken(request.token());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Password reset token is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token is invalid or has expired");
        }

        AuthUser user = authUserRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User associated with token not found"));

        // Mark token used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Update password with BCrypt hash
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        authUserRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenService.revokeAllForUser(user.getId());

        log.info("Password successfully reset and active sessions revoked for user [{}]", user.getEmail());
    }
}
