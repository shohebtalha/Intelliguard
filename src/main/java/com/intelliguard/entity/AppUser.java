package com.intelliguard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AppUser represents a system user who can access IntelliGuard.
 *
 * We name it AppUser (not User) because PostgreSQL has a reserved
 * keyword "user" — naming the table "users" is fine but the class
 * name User can cause confusion with Spring Security's User class.
 *
 * Roles:
 * ANALYST  — can view transactions and explain decisions
 * MANAGER  — can view + approve/reject REVIEW transactions
 * ADMIN    — full access including user management
 */
@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // stored as BCrypt hash — never plaintext

    @Column(nullable = false)
    private String role; // ANALYST, MANAGER, ADMIN

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}