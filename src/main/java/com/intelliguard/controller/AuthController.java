package com.intelliguard.controller;

import com.intelliguard.config.JwtUtil;
import com.intelliguard.dto.ApiResponse;
import com.intelliguard.entity.AppUser;
import com.intelliguard.repository.UserRepository;
import com.intelliguard.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * AuthController handles user authentication.
 *
 * POST /api/auth/login    → returns JWT token
 * POST /api/auth/register → creates new user (admin only in production)
 *
 * In a real system, register would be admin-only.
 * For demo purposes we keep it open so you can create users easily.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private static final Set<String> ALLOWED_ROLES = Set.of("ANALYST", "MANAGER", "ADMIN");

    @Value("${app.auth.allow-public-registration:false}")
    private boolean allowPublicRegistration;

    /**
     * POST /api/auth/login
     * Body: { "username": "admin", "password": "password123" }
     * Returns: { "token": "eyJhbGci...", "username": "admin", "role": "ADMIN" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @Valid @RequestBody LoginRequest request) {

        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account is disabled"));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getTenantId());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        log.info("Successful login: {} (role: {})", user.getUsername(), user.getRole());

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "token", token,
                        "refreshToken", refreshToken.token(),
                        "username", user.getUsername(),
                        "role", user.getRole(),
                        "tenantId", user.getTenantId()
                ),
                "Login successful"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        try {
            RefreshTokenService.IssuedRefreshToken rotated =
                    refreshTokenService.rotate(request.getRefreshToken());
            AppUser user = userRepository.findById(rotated.userId())
                    .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));
            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Account is disabled"));
            }

            String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getTenantId());
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of(
                            "token", accessToken,
                            "refreshToken", rotated.token(),
                            "username", user.getUsername(),
                            "role", user.getRole(),
                            "tenantId", user.getTenantId()
                    ),
                    "Token refreshed"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("ok", "Logged out"));
    }

    /**
     * POST /api/auth/register
     * Body: { "username": "analyst1", "password": "pass123", "role": "ANALYST" }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request) {

        if (!allowPublicRegistration) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Public registration is disabled"));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Username already exists"));
        }

        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase() : "ANALYST";
        if (!ALLOWED_ROLES.contains(requestedRole)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid role"));
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .tenantId(request.getTenantId() != null ? request.getTenantId() : "demo-bank")
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} (role: {})", user.getUsername(), user.getRole());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user.getUsername(), "User registered successfully"));
    }

    // ─── Request DTOs ─────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 8, max = 128)
        private String password;

        @Size(min = 3, max = 80)
        private String tenantId;

        @Pattern(regexp = "ANALYST|MANAGER|ADMIN", message = "Role must be ANALYST, MANAGER, or ADMIN")
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }
}
