package com.inklusport.reports.controller;

import com.inklusport.reports.dto.AnalyticsEventRequest;
import com.inklusport.reports.dto.AnalyticsEventResponse;
import com.inklusport.reports.dto.DailyMetricResponse;
import com.inklusport.reports.service.AnalyticsEventService;
import com.inklusport.reports.service.DailyMetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    /**
     * Inyección de Servicio
     */
    private final AnalyticsEventService analyticsEventService;
    private final DailyMetricsService dailyMetricsService;

    /**
     * Obtener los reportes de registros a eventos
     * @param userId
     * @param request
     * @return
     */
    @PostMapping("/events")
    public ResponseEntity<AnalyticsEventResponse> registerEvent(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AnalyticsEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(analyticsEventService.registerEvent(request, userId));
    }

    /**
     * Obtener los reportes de registros a eventos por usuario
     * @param userId
     * @return
     */
    @GetMapping("/events/user")
    public ResponseEntity<List<AnalyticsEventResponse>> getUserEvents(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(analyticsEventService.getEventsByUser(userId));
    }

    /**
     * Obtener los reportes de registros a eventos por tipo
     * @param eventType
     * @return
     */
    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<AnalyticsEventResponse>> getEventsByType(@PathVariable String eventType) {
        return ResponseEntity.ok(analyticsEventService.getEventsByType(eventType));
    }

    @GetMapping("/events/module/{module}")
    public ResponseEntity<List<AnalyticsEventResponse>> getEventsByModule(@PathVariable String module) {
        return ResponseEntity.ok(analyticsEventService.getEventsByModule(module));
    }

    @GetMapping("/metrics/daily")
    public ResponseEntity<List<DailyMetricResponse>> getDailyMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dailyMetricsService.getDailyMetrics(startDate, endDate));
    }
}