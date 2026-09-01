package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    private Map<String, Integer> metrics;

    private Map<String, Long> eventCounts;

    private Map<String, Integer> weeklyTrend;

    private List<Map<String, Object>> recentUsers;

    private List<Map<String, Object>> recentEvents;
}
