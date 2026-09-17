package com.inklusport.reports.config;

import com.inklusport.reports.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                String email = jwtTokenProvider.getEmailFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);
                if (roles == null) {
                    roles = List.of();
                }

                List<SimpleGrantedAuthority> authorities = toAuthorities(roles);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, token, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Usuario autenticado: {} con roles={} authorities={}", email, roles, authorities);
            } catch (Exception e) {
                log.error("Token presente pero no se pudo construir Authentication: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Normaliza roles del JWT a authorities Spring (ROLE_*), alineado con users/auth.
     */
    private List<SimpleGrantedAuthority> toAuthorities(List<String> roles) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String value = role.trim().toUpperCase();
            if (value.startsWith("ROLE_")) {
                value = value.substring(5);
            }
            if ("ADMINISTRADOR".equals(value)) {
                value = "ADMIN";
            }
            if ("ORGANIZER".equals(value)) {
                normalized.add("ORGANIZADOR");
                normalized.add("ORGANIZER");
                continue;
            }
            if ("ORGANIZADOR".equals(value)) {
                normalized.add("ORGANIZADOR");
                normalized.add("ORGANIZER");
                continue;
            }
            normalized.add(value);
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String value : normalized) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + value));
        }
        return authorities;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
