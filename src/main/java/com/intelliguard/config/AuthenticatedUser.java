package com.intelliguard.config;

public record AuthenticatedUser(String username, String role, String tenantId) {
}
