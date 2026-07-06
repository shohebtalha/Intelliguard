package com.intelliguard.service;

import com.intelliguard.entity.AppUser;
import com.intelliguard.entity.RefreshToken;
import com.intelliguard.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Transactional
    public IssuedRefreshToken issue(AppUser user) {
        return issue(user, UUID.randomUUID().toString());
    }

    @Transactional
    public IssuedRefreshToken rotate(String presentedToken) {
        String presentedHash = hash(presentedToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        LocalDateTime now = LocalDateTime.now();
        if (!existing.isActive(now)) {
            revokeFamily(existing);
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        String raw = randomToken();
        String newHash = hash(raw);
        existing.setRevokedAt(now);
        existing.setReplacedByHash(newHash);

        RefreshToken replacement = RefreshToken.builder()
                .tenantId(existing.getTenantId())
                .userId(existing.getUserId())
                .tokenHash(newHash)
                .tokenFamily(existing.getTokenFamily())
                .expiresAt(now.plusDays(refreshExpirationDays))
                .build();
        refreshTokenRepository.save(replacement);
        return new IssuedRefreshToken(raw, existing.getTokenFamily(), replacement.getExpiresAt(), existing.getUserId());
    }

    @Transactional
    public void revoke(String presentedToken) {
        refreshTokenRepository.findByTokenHash(hash(presentedToken))
                .ifPresent(token -> token.setRevokedAt(LocalDateTime.now()));
    }

    private IssuedRefreshToken issue(AppUser user, String family) {
        String raw = randomToken();
        RefreshToken saved = refreshTokenRepository.save(RefreshToken.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .tokenHash(hash(raw))
                .tokenFamily(family)
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .build());
        return new IssuedRefreshToken(raw, family, saved.getExpiresAt(), user.getId());
    }

    private void revokeFamily(RefreshToken token) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByUserIdAndTokenFamily(token.getUserId(), token.getTokenFamily())
                .forEach(member -> member.setRevokedAt(now));
    }

    private String randomToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    public record IssuedRefreshToken(String token, String family, LocalDateTime expiresAt, String userId) {
    }
}
