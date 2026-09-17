package com.inklusport.reports.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad de reports. En Docker el gateway ya filtra el acceso público;
 * aquí el JWT se usa sobre todo para Feign y auditoría, no para bloquear paneles.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Docker: HTTP permitAll y SIN @PreAuthorize, para que los paneles no caigan
     * en 403 Access Denied si la validación remota contra auth falla.
     * El filtro JWT sigue activo para reenviar el token a users/sports.
     */
    @Bean
    @Profile("docker")
    public SecurityFilterChain filterChainDocker(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Local/dev: method security + roles estrictos.
     */
    @Configuration
    @Profile("!docker")
    @EnableMethodSecurity
    @RequiredArgsConstructor
    static class MethodSecurityLocalConfig {
        // Marcador: EnableMethodSecurity solo fuera de Docker.
    }

    @Bean
    @Profile("!docker")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "ORGANIZADOR", "ORGANIZER", "ENTRENADOR")
                        .requestMatchers("/api/reports/schedule/**").authenticated()
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "ORGANIZADOR", "ORGANIZER", "ENTRENADOR")
                        .requestMatchers("/api/dashboard/home").authenticated()
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "ORGANIZADOR", "ORGANIZER", "ENTRENADOR")
                        .requestMatchers("/api/admin/users/count").hasAnyRole("ADMIN", "ORGANIZADOR", "ORGANIZER", "ENTRENADOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
