package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DailyMetricResponse {

    private LocalDate summaryDate;
    private String metricKey;
    private Integer metricValue;
    private LocalDateTime updatedAt;
}
