package com.intelliguard;

import com.intelliguard.entity.AppUser;
import com.intelliguard.entity.RefreshToken;
import com.intelliguard.repository.RefreshTokenRepository;
import com.intelliguard.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 30L);
    }

    @Test
    void issue_shouldStoreOnlyHashedToken() {
        AppUser user = AppUser.builder()
                .id("user-1")
                .tenantId("tenant-a")
                .username("analyst")
                .role("ANALYST")
                .enabled(true)
                .build();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued = service.issue(user);

        RefreshToken saved = captor.getValue();
        assertThat(issued.token()).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(issued.token());
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getUserId()).isEqualTo("user-1");
    }

    @Test
    void rotate_shouldRevokeOldTokenAndCreateReplacement() {
        RefreshToken existing = RefreshToken.builder()
                .tenantId("tenant-a")
                .userId("user-1")
                .tokenHash("hash")
                .tokenFamily("family-1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken rotated = service.rotate("presented-token");

        assertThat(rotated.token()).isNotBlank();
        assertThat(rotated.family()).isEqualTo("family-1");
        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(existing.getReplacedByHash()).isNotBlank();
    }

    @Test
    void rotate_reusedRevokedToken_shouldRevokeFamily() {
        RefreshToken existing = RefreshToken.builder()
                .tenantId("tenant-a")
                .userId("user-1")
                .tokenHash("hash")
                .tokenFamily("family-1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revokedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        RefreshToken sibling = RefreshToken.builder()
                .tenantId("tenant-a")
                .userId("user-1")
                .tokenHash("sibling")
                .tokenFamily("family-1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.findByUserIdAndTokenFamily("user-1", "family-1"))
                .thenReturn(List.of(existing, sibling));

        assertThatThrownBy(() -> service.rotate("presented-token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(sibling.getRevokedAt()).isNotNull();
    }
}
