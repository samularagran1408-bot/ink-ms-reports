package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AnalyticsEventResponse {

    private String id;
    
    private String eventType;
    
    private String userId;
    
    private String module;
    
    private String metadata;
    
    private LocalDateTime createdAt;
}