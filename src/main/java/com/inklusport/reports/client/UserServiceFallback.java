package com.inklusport.reports.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {

    @Override
    public int getTotalUsers() {
        log.warn("Users MS no disponible. Retornando 0 como total de usuarios.");
        return 0;
    }

    @Override
    public int getActiveUsers() {
        log.warn("Users MS no disponible. Retornando 0 como usuarios activos.");
        return 0;
    }

    @Override
    public List<Map<String, Object>> getAllUsers() {
        log.warn("Users MS no disponible. Retornando lista vacía de usuarios.");
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getActiveUsersList() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getInactiveUsersList() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getRoles() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getAuditLogs() {
        return List.of();
    }

    @Override
    public Map<String, Object> getQuizPrep(String role, String userId) {
        return Map.of();
    }
}
