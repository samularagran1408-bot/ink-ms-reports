package com.inklusport.reports.service;

import com.inklusport.reports.client.SportsServiceClient;
import com.inklusport.reports.client.UserServiceClient;
import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final UserServiceClient userServiceClient;
    private final SportsServiceClient sportsServiceClient;

    public DashboardResponse getDashboard(DashboardFilters filters) {
        LocalDateTime startDate = filters.getStartDate() != null ? 
                filters.getStartDate().atStartOfDay() : 
                LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = filters.getEndDate() != null ? 
                filters.getEndDate().atTime(LocalTime.MAX) : 
                LocalDateTime.now();

        /**
         * Métricas con fallbacks automáticos (si el MS falla, devuelve 0)
         */
        int totalUsers = userServiceClient.getTotalUsers();
        int activeUsers = userServiceClient.getActiveUsers();
        int activeEvents = sportsServiceClient.getActiveEventsCount();
        int totalSports = sportsServiceClient.getTotalSports();
        
        log.info("Dashboard metrics - Usuarios totales: {}, Usuarios activos: {}, Eventos activos: {}, Deportes: {}", 
                 totalUsers, activeUsers, activeEvents, totalSports);

        /**
         * Métricas principales
         */
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("total_users", totalUsers);
        metrics.put("active_users", activeUsers);
        metrics.put("active_events", activeEvents);
        metrics.put("total_sports", totalSports);

        /**
         * Conteo de eventos por tipo
         */
        List<Object[]> eventCountsRaw = analyticsEventRepository.countByEventTypeAndDateRange(startDate, endDate);
        Map<String, Long> eventCounts = eventCountsRaw.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        /**
         * Tendencia semanal
         */
        Map<String, Integer> weeklyTrend = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = analyticsEventRepository.countByDateRange(date.atStartOfDay(), date.atTime(LocalTime.MAX));
            weeklyTrend.put(date.toString(), (int) count);
        }

        return DashboardResponse.builder()
                .metrics(metrics)
                .eventCounts(eventCounts)
                .weeklyTrend(weeklyTrend)
                .build();
    }
}