package com.intelliguard.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JwtUtil handles everything JWT-related:
 * - Generating tokens when user logs in
 * - Validating tokens on every request
 * - Extracting username from token
 *
 * HOW JWT WORKS:
 * 1. User logs in with username/password
 * 2. We verify credentials and return a JWT token
 * 3. Client stores token and sends it in every request header:
 *    Authorization: Bearer eyJhbGci...
 * 4. Our filter intercepts every request, validates the token,
 *    and sets the user in Spring Security context
 * 5. Spring Security allows/blocks the request based on role
 *
 * JWT = Header.Payload.Signature (base64 encoded, dot separated)
 * The signature proves the token wasn't tampered with.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // milliseconds

    @Value("${jwt.issuer:intelliguard}")
    private String issuer;

    @Value("${jwt.audience:intelliguard-api}")
    private String audience;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate a JWT token for a user after successful login.
     */
    public String generateToken(String username, String role) {
        return generateToken(username, role, "demo-bank");
    }

    public String generateToken(String username, String role, String tenantId) {
        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .claim("role", role)
                .claim("tenant_id", tenantId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from a JWT token.
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract role from a JWT token.
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String extractTenantId(String token) {
        return getClaims(token).get("tenant_id", String.class);
    }

    /**
     * Validate a JWT token — checks signature and expiry.
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
