package com.example.zylo.user.entity;

import com.example.zylo.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 60)
    private String password; // hash

    @Column(nullable = false, name = "is_active")
    @Builder.Default()
    private boolean isActive = true;

    @Column(nullable = false, name = "is_email_verified")
    @Builder.Default()
    private boolean isEmailVerified = false;
}
