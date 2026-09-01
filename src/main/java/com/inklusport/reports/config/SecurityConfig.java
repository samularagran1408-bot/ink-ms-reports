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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Perfil Docker - sin seguridad (roles y JWT)
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Profile("docker")
    public SecurityFilterChain filterChainDocker(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // JWT necesario para @PreAuthorize en dashboard/reportes
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Perfil Local - con seguridad (roles y JWT)
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Profile("!docker")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "ORGANIZADOR", "ENTRENADOR")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "ORGANIZADOR", "ENTRENADOR")
                        .requestMatchers("/api/dashboard/home").authenticated()
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "ORGANIZADOR", "ENTRENADOR")
                        .requestMatchers("/api/admin/users/count").hasAnyRole("ADMIN", "ORGANIZADOR", "ENTRENADOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}