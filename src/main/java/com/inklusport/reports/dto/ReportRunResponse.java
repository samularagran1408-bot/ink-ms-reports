package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ReportRunResponse {

    private String reportId;
    private String reportName;
    private LocalDateTime executedAt;
    private LocalDateTime lastRun;
    private Map<String, Object> results;
}
