package com.example.zylo.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "user", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        // @Index(name = "idx_cognito_sub", columnList = "cognito_sub"),
        // @Index(name = "idx_phone", columnList = "phone_no"),
        @Index(name = "idx_role", columnList = "role")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(name = "cognito_sub", unique = true, nullable = false)
    // private String cognitoSub;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_no", length = 20)
    private String phoneNo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_email_verified")
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Role {
        CUSTOMER, ADMIN
    }
}
