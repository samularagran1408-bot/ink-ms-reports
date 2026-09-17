package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WeeklyScheduleResponse {

    private boolean enabled;
    private String reportConfigId;
    private String recipientEmail;
    private String frequency;
    private LocalDateTime lastRun;
    private LocalDateTime createdAt;
}
