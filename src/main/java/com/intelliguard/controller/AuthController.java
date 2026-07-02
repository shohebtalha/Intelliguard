package com.intelliguard.controller;

import com.intelliguard.config.JwtUtil;
import com.intelliguard.dto.ApiResponse;
import com.intelliguard.entity.AppUser;
import com.intelliguard.repository.UserRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/auth/login
     * Body: { "username": "admin", "password": "password123" }
     * Returns: { "token": "eyJhbGci...", "username": "admin", "role": "ADMIN" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody LoginRequest request) {

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

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        log.info("Successful login: {} (role: {})", user.getUsername(), user.getRole());

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "role", user.getRole()
                ),
                "Login successful"
        ));
    }

    /**
     * POST /api/auth/register
     * Body: { "username": "analyst1", "password": "pass123", "role": "ANALYST" }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Username already exists"));
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : "ANALYST")
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
        private String username;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        private String username;
        private String password;
        private String role;
    }
}