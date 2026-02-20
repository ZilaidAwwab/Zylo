package com.example.zylo.auth.service;

import com.example.zylo.user.entity.User;
import com.example.zylo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Finding User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException(
                            "User not found: " + email
                    );
                });

        // Checking if the user account is deleted (soft delete)
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UsernameNotFoundException("User account has been deleted");
        }

        log.debug("User found: {} with role: {}", email, user.getRole());

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {

        // role must start with "ROLE_" for spring security
        String role = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singleton(
                        new SimpleGrantedAuthority(role)
                ))
                // These other details can also be used
                // .accountExpired(false)
                // .accountLocked(false)
                // .credentialsExpired(false)
                // .disabled(user.getIsDeleted())
                .build();
    }
}
