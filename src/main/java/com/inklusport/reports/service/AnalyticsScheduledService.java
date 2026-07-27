package com.inklusport.reports.service;

import com.inklusport.reports.entity.DailyMetricsSummary;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import com.inklusport.reports.repository.DailyMetricsSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reports.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsScheduledService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsScheduledService.class);

    private final AnalyticsEventRepository analyticsEventRepository;
    private final DailyMetricsSummaryRepository metricsRepository;

    @Value("${reports.cleanup.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${reports.aggregation.cron:0 0 1 * * ?}")
    @Transactional
    public void aggregateDailyMetrics() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        long totalEvents = analyticsEventRepository.countByDateRange(start, end);
        upsertMetric(targetDate, "total_events", (int) totalEvents);

        List<Object[]> eventCounts = analyticsEventRepository.countByEventTypeAndDateRange(start, end);
        for (Object[] row : eventCounts) {
            String eventType = (String) row[0];
            int count = ((Long) row[1]).intValue();
            upsertMetric(targetDate, "events_" + eventType, count);
        }

        log.info("Agregación diaria completada para {}: {} eventos", targetDate, totalEvents);
    }

    @Scheduled(cron = "${reports.cleanup.cron:0 0 2 * * SUN}")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long deleted = analyticsEventRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Limpieza semanal: {} eventos anteriores a {} eliminados", deleted, cutoff);
    }

    private void upsertMetric(LocalDate date, String metricKey, int value) {
        metricsRepository.findBySummaryDateAndMetricKey(date, metricKey)
                .ifPresentOrElse(
                        existing -> metricsRepository.updateMetricValue(date, metricKey, value),
                        () -> metricsRepository.save(DailyMetricsSummary.builder()
                                .summaryDate(date)
                                .metricKey(metricKey)
                                .metricValue(value)
                                .build())
                );
    }
}
