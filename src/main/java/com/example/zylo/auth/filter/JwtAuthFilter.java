package com.example.zylo.auth.filter;

import com.example.zylo.auth.service.CustomUserDetailsService;
import com.example.zylo.auth.service.JwtService;
import com.example.zylo.user.entity.User;
import com.example.zylo.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // With RequiredArgsConstructor annotation, we don't need to create constructor injected with above class instances

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract token from Authorization header
        String token = extractTokenFromRequest(request);

        if (token == null) {
            // No token, continue as anonymous
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extract Email and token type from token
            String email = jwtService.extractEmail(token);
            String tokenType = jwtService.extractTokenType(token);

            // 3. Only process ACCESS token here
            // (Refresh token should only hit /auth/refresh)
            if (!"ACCESS".equals(tokenType)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 4. Only set auth if not already authenticated
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Load user - Redis first then MySQL
                User user = loadUserFromCacheOrDB(email);

                if (user == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 6. Validate token
                if (jwtService.isTokenValid(token, email)) {

                    // 7. Build authentication object
                    String role = "ROLE_" + user.getRole().name();

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(new SimpleGrantedAuthority(role)));

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 8. Set in spring security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authenticated User: {} with role: {}", email, role);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    // Loading User (First from Redis, then MySQL)
    // Saves user in Redis for 30 mins, so all the requests in the meantime are retrieved fast
    // Without Redis ~50ms
    // With Redis ~2ms
    private User loadUserFromCacheOrDB(String email) {
        String cacheKey = "user:session:" + email;

        // Checking Redis
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);

        if (cachedUser != null) {
            log.debug("Cache HIT for user: {}", email);
            return cachedUser;
        }

        // Cache miss - Loading from MySQL
        log.debug("Cache MISS for user: {} - Loading from DB", email);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // save into redis (cache) for 30 mins
            redisTemplate.opsForValue().set(
                    cacheKey,
                    user,
                    30, // 30 Mins
                    TimeUnit.MINUTES
            );
        }
        return user;
    }

    // Extract bearer token (from request)
    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Skip filter from public endpoints
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health");
    }
}
