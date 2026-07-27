package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReportConfigResponse {

    private String id;
    
    private String reportName;
    
    private String filters;
    
    private String ownerId;
    
    private LocalDateTime lastRun;
    
    private LocalDateTime createdAt;
}