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
import java.util.List;
import java.util.stream.Collectors;

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

        // Ignorar endpoints públicos
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);

            if (roles == null) {
                roles = List.of();
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .flatMap(role -> {
                        String normalized = role.toUpperCase();
                        if ("ORGANIZADOR".equals(normalized)) {
                            normalized = "ORGANIZER";
                        }
                        List<SimpleGrantedAuthority> roleAuthorities = new ArrayList<>();
                        roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
                        if ("ORGANIZER".equals(normalized)) {
                            roleAuthorities.add(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"));
                        }
                        return roleAuthorities.stream();
                    })
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, token, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Usuario autenticado: {} con roles: {}", email, roles);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}