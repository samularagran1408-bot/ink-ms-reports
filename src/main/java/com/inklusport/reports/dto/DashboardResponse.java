package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    private Map<String, Integer> metrics;
    
    private Map<String, Long> eventCounts;
    
    private Map<String, Integer> weeklyTrend;
}