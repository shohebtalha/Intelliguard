package com.intelliguard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliguard.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.login-per-minute:5}")
    private int loginLimitPerMinute;

    @Value("${app.rate-limit.api-per-minute:120}")
    private int apiLimitPerMinute;

    @Value("${app.security.trusted-proxies:}")
    private String trustedProxies;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = clientIp(request);

        boolean allowed;
        if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            allowed = rateLimitService.allow("login", ip, loginLimitPerMinute, Duration.ofMinutes(1));
        } else if (path.startsWith("/api/")) {
            allowed = rateLimitService.allow("api", ip, apiLimitPerMinute, Duration.ofMinutes(1));
        } else {
            allowed = true;
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "Rate limit exceeded",
                    "timestamp", LocalDateTime.now().toString()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (isTrustedProxy(remoteAddress) && forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        if (remoteAddress == null || trustedProxies == null || trustedProxies.isBlank()) {
            return false;
        }
        List<String> proxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return proxies.contains(remoteAddress);
    }
}
