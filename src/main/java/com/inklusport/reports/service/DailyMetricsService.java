package com.inklusport.reports.service;

import com.inklusport.reports.dto.DailyMetricResponse;
import com.inklusport.reports.entity.DailyMetricsSummary;
import com.inklusport.reports.repository.DailyMetricsSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyMetricsService {

    private final DailyMetricsSummaryRepository metricsRepository;

    @Transactional(readOnly = true)
    public List<DailyMetricResponse> getDailyMetrics(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        return metricsRepository.findBySummaryDateBetween(start, end).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DailyMetricResponse toResponse(DailyMetricsSummary metric) {
        return DailyMetricResponse.builder()
                .summaryDate(metric.getSummaryDate())
                .metricKey(metric.getMetricKey())
                .metricValue(metric.getMetricValue())
                .updatedAt(metric.getUpdatedAt())
                .build();
    }
}
