package com.inklusport.reports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PanelDashboardResponse {

    private Map<String, Integer> metrics;
    private List<Map<String, Object>> events;
    private List<Map<String, Object>> registrations;
    private List<Map<String, Object>> routines;
    private List<Map<String, Object>> routineRegistrations;
    private List<Map<String, Object>> sports;
    private List<Map<String, Object>> disabilities;
    private List<Map<String, Object>> associations;
    private Map<String, List<Map<String, Object>>> waitlists;
    private List<Map<String, Object>> athleteSummaries;
    private List<Map<String, Object>> users;
    private List<Map<String, Object>> roles;
    private List<Map<String, Object>> auditLogs;
    private Map<String, Object> quizPrep;
    private Map<String, Long> eventCounts;
    private Map<String, Integer> weeklyTrend;
    private Integer athleteCount;
    private Double attendanceRatePercent;
    private Integer attendanceSampledEvents;
}
