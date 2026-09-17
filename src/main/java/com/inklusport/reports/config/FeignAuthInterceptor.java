package com.inklusport.reports.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reenvía el Bearer JWT a users/sports.
 * 1) credentials del SecurityContext
 * 2) header Authorization de la petición HTTP entrante
 */
@Component
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String token = tokenFromSecurityContext();
        if (token == null || token.isBlank()) {
            token = tokenFromIncomingRequest();
        }

        if (token != null && !token.isBlank()) {
            if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                template.header("Authorization", token);
            } else {
                template.header("Authorization", "Bearer " + token);
            }
            log.debug("Token agregado a petición Feign: {}", template.url());
        } else {
            log.warn("No hay token disponible para petición Feign: {}", template.url());
        }
    }

    private String tokenFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }
        return authentication.getCredentials().toString();
    }

    private String tokenFromIncomingRequest() {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            if (request == null) {
                return null;
            }
            return request.getHeader("Authorization");
        } catch (Exception e) {
            return null;
        }
    }
}
