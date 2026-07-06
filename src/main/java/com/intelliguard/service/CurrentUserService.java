package com.intelliguard.service;

import com.intelliguard.config.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public static final String SYSTEM_TENANT = "demo-bank";
    public static final String SYSTEM_USER = "system";

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return new AuthenticatedUser(SYSTEM_USER, "SYSTEM", SYSTEM_TENANT);
        }
        return user;
    }

    public String tenantId() {
        return currentUser().tenantId();
    }

    public String username() {
        return currentUser().username();
    }
}
