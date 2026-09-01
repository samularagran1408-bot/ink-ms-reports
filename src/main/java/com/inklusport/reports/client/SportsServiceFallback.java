package com.inklusport.reports.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SportsServiceFallback implements SportsServiceClient {

    @Override
    public int getActiveEventsCount() {
        log.warn("Sports MS no disponible. Retornando 0 como eventos activos.");
        return 0;
    }

    @Override
    public int getTotalSports() {
        log.warn("Sports MS no disponible. Retornando 0 como total de deportes.");
        return 0;
    }

    @Override
    public int getTotalEvents() {
        log.warn("Sports MS no disponible. Retornando 0 como total de eventos.");
        return 0;
    }

    @Override
    public List<Map<String, Object>> getEvents() {
        log.warn("Sports MS no disponible. Retornando lista vacía de eventos.");
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getAvailableEvents() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getSports() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getDisabilities() {
        log.warn("Sports MS no disponible. Retornando lista vacía de discapacidades.");
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getActiveDisabilities() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getActiveSports() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getAssociations() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getEventWaitlist(String eventId) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getRegistrationsByUser(String userId) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getRoutines() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getRoutinesByTrainer(String trainerId) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getRoutineRegistrations(String id) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getRoutineRegistrationsByUser(String userId) {
        return List.of();
    }

    @Override
    public Map<String, Object> getAttendanceReport(String eventId) {
        return Map.of("totalRegistered", 0, "totalAttended", 0);
    }
}
