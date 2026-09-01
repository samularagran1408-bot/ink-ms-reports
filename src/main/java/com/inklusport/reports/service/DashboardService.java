package com.inklusport.reports.service;

import com.inklusport.reports.client.SportsServiceClient;
import com.inklusport.reports.client.UserServiceClient;
import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
import com.inklusport.reports.dto.PanelDashboardResponse;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final UserServiceClient userServiceClient;
    private final SportsServiceClient sportsServiceClient;

    public DashboardResponse getDashboard(DashboardFilters filters) {
        LocalDateTime startDate = filters.getStartDate() != null
                ? filters.getStartDate().atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = filters.getEndDate() != null
                ? filters.getEndDate().atTime(LocalTime.MAX)
                : LocalDateTime.now();

        int totalUsers = userServiceClient.getTotalUsers();
        int activeUsers = userServiceClient.getActiveUsers();
        int activeEvents = sportsServiceClient.getActiveEventsCount();
        int totalSports = sportsServiceClient.getTotalSports();
        List<Map<String, Object>> disabilities = safeList(sportsServiceClient.getDisabilities());
        List<Map<String, Object>> users = safeList(userServiceClient.getAllUsers());
        List<Map<String, Object>> events = safeList(sportsServiceClient.getEvents());

        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("total_users", totalUsers);
        metrics.put("active_users", activeUsers);
        metrics.put("active_events", activeEvents);
        metrics.put("total_sports", totalSports);
        metrics.put("total_disabilities", disabilities.size());

        List<Object[]> eventCountsRaw = analyticsEventRepository.countByEventTypeAndDateRange(startDate, endDate);
        Map<String, Long> eventCounts = eventCountsRaw.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        Map<String, Integer> weeklyTrend = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = analyticsEventRepository.countByDateRange(date.atStartOfDay(), date.atTime(LocalTime.MAX));
            weeklyTrend.put(date.toString(), (int) count);
        }

        return DashboardResponse.builder()
                .metrics(metrics)
                .eventCounts(eventCounts)
                .weeklyTrend(weeklyTrend)
                .recentUsers(users.stream().limit(6).toList())
                .recentEvents(events.stream().limit(4).toList())
                .build();
    }

    public PanelDashboardResponse getHomePanel(String userId) {
        List<Map<String, Object>> events = safeList(sportsServiceClient.getEvents());
        List<Map<String, Object>> registrations = userId == null || userId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRegistrationsByUser(userId));
        List<Map<String, Object>> routines = safeList(sportsServiceClient.getRoutines());
        List<Map<String, Object>> routineRegistrations = userId == null || userId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRoutineRegistrationsByUser(userId));
        return PanelDashboardResponse.builder()
                .events(events)
                .registrations(registrations)
                .sports(safeList(sportsServiceClient.getActiveSports()))
                .disabilities(safeList(sportsServiceClient.getActiveDisabilities()))
                .associations(safeList(sportsServiceClient.getAssociations()))
                .routines(routines)
                .routineRegistrations(routineRegistrations)
                .build();
    }

    public PanelDashboardResponse getEventsPanel(String userId, String mode) {
        boolean manage = "manage".equalsIgnoreCase(mode);
        List<Map<String, Object>> events = manage
                ? safeList(sportsServiceClient.getEvents())
                : safeList(sportsServiceClient.getAvailableEvents());
        List<Map<String, Object>> registrations = !manage && userId != null && !userId.isBlank()
                ? safeList(sportsServiceClient.getRegistrationsByUser(userId))
                : List.of();
        List<Map<String, Object>> sports = manage
                ? safeList(sportsServiceClient.getActiveSports())
                : List.of();
        Map<String, List<Map<String, Object>>> waitlists = new HashMap<>();
        if (manage) {
            for (Map<String, Object> event : events) {
                Object id = event.get("id");
                if (id == null) {
                    continue;
                }
                waitlists.put(String.valueOf(id), safeList(sportsServiceClient.getEventWaitlist(String.valueOf(id))));
            }
        }
        return PanelDashboardResponse.builder()
                .events(events)
                .registrations(registrations)
                .sports(sports)
                .waitlists(waitlists)
                .build();
    }

    public PanelDashboardResponse getAssociationsPanel() {
        return PanelDashboardResponse.builder()
                .sports(safeList(sportsServiceClient.getSports()))
                .disabilities(safeList(sportsServiceClient.getActiveDisabilities()))
                .associations(safeList(sportsServiceClient.getAssociations()))
                .build();
    }

    public PanelDashboardResponse getSessionsPanel(String trainerId) {
        List<Map<String, Object>> routines = trainerId == null || trainerId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRoutinesByTrainer(trainerId));
        return PanelDashboardResponse.builder()
                .routines(routines)
                .sports(safeList(sportsServiceClient.getActiveSports()))
                .build();
    }

    public PanelDashboardResponse getAthletesPanel(String organizerId, boolean allEvents) {
        List<Map<String, Object>> events = safeList(sportsServiceClient.getEvents());
        if (!allEvents && organizerId != null && !organizerId.isBlank()) {
            List<Map<String, Object>> own = events.stream()
                    .filter(event -> organizerId.equals(String.valueOf(event.get("createdBy"))))
                    .toList();
            if (!own.isEmpty()) {
                events = own;
            }
        }
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Map<String, Object> event : events) {
            Object id = event.get("id");
            if (id == null) {
                continue;
            }
            String eventId = String.valueOf(id);
            Map<String, Object> summary = new HashMap<>();
            summary.put("event", event);
            summary.put("waitlist", safeList(sportsServiceClient.getEventWaitlist(eventId)));
            Map<String, Object> report = sportsServiceClient.getAttendanceReport(eventId);
            summary.put("attendanceReport", report == null ? Map.of() : report);
            summaries.add(summary);
        }
        return PanelDashboardResponse.builder()
                .events(events)
                .athleteSummaries(summaries)
                .build();
    }

    public PanelDashboardResponse getTrainerPanel(String trainerId) {
        List<Map<String, Object>> routines = trainerId == null || trainerId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRoutinesByTrainer(trainerId));
        List<Map<String, Object>> disabilities = safeList(sportsServiceClient.getActiveDisabilities());
        Set<String> athleteIds = new HashSet<>();
        for (Map<String, Object> routine : routines) {
            Object id = routine.get("id");
            if (id == null) {
                continue;
            }
            for (Map<String, Object> registration : safeList(sportsServiceClient.getRoutineRegistrations(String.valueOf(id)))) {
                Object userId = registration.get("userId");
                if (userId != null) {
                    athleteIds.add(String.valueOf(userId));
                }
            }
        }
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("routines", routines.size());
        metrics.put("published", (int) routines.stream()
                .filter(routine -> "published".equals(String.valueOf(routine.get("status"))))
                .count());
        metrics.put("athletes", athleteIds.size());
        metrics.put("disabilities", disabilities.size());
        return PanelDashboardResponse.builder()
                .metrics(metrics)
                .routines(routines)
                .disabilities(disabilities)
                .athleteCount(athleteIds.size())
                .build();
    }

    public PanelDashboardResponse getOrganizerPanel(String organizerId) {
        List<Map<String, Object>> allEvents = safeList(sportsServiceClient.getEvents());
        List<Map<String, Object>> sports = safeList(sportsServiceClient.getActiveSports());
        List<Map<String, Object>> events = allEvents;
        if (organizerId != null && !organizerId.isBlank()) {
            List<Map<String, Object>> own = allEvents.stream()
                    .filter(event -> organizerId.equals(String.valueOf(event.get("createdBy"))))
                    .toList();
            if (!own.isEmpty()) {
                events = own;
            }
        }
        int athleteCount = events.stream().mapToInt(this::occupied).sum();
        List<Map<String, Object>> sample = events.stream().limit(8).toList();
        int registered = 0;
        int attended = 0;
        for (Map<String, Object> event : sample) {
            Object id = event.get("id");
            if (id == null) {
                continue;
            }
            Map<String, Object> report = sportsServiceClient.getAttendanceReport(String.valueOf(id));
            if (report == null) {
                continue;
            }
            registered += toInt(report.get("totalRegistered"));
            attended += toInt(report.get("totalAttended"));
        }
        Double rate = registered > 0 ? Math.round((attended * 10000.0) / registered) / 100.0 : 0d;
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("active_events", sportsServiceClient.getActiveEventsCount());
        metrics.put("athletes", athleteCount);
        return PanelDashboardResponse.builder()
                .metrics(metrics)
                .events(events)
                .sports(sports)
                .athleteCount(athleteCount)
                .attendanceRatePercent(sample.isEmpty() ? null : rate)
                .attendanceSampledEvents(sample.size())
                .build();
    }

    public PanelDashboardResponse getSportsPanel() {
        return PanelDashboardResponse.builder()
                .sports(safeList(sportsServiceClient.getSports()))
                .build();
    }

    public PanelDashboardResponse getDisabilitiesPanel() {
        return PanelDashboardResponse.builder()
                .disabilities(safeList(sportsServiceClient.getDisabilities()))
                .build();
    }

    public PanelDashboardResponse getUsersPanel(String filter) {
        List<Map<String, Object>> users;
        if ("inactive".equalsIgnoreCase(filter)) {
            users = safeList(userServiceClient.getInactiveUsersList());
        } else if ("all".equalsIgnoreCase(filter)) {
            users = safeList(userServiceClient.getAllUsers());
        } else {
            users = safeList(userServiceClient.getActiveUsersList());
        }
        return PanelDashboardResponse.builder()
                .users(users)
                .build();
    }

    public PanelDashboardResponse getRolesPanel() {
        return PanelDashboardResponse.builder()
                .roles(safeList(userServiceClient.getRoles()))
                .users(safeList(userServiceClient.getAllUsers()))
                .build();
    }

    public PanelDashboardResponse getAuditPanel() {
        DashboardResponse dashboard = getDashboard(new DashboardFilters());
        return PanelDashboardResponse.builder()
                .metrics(dashboard.getMetrics())
                .eventCounts(dashboard.getEventCounts())
                .weeklyTrend(dashboard.getWeeklyTrend())
                .users(safeList(userServiceClient.getAllUsers()))
                .auditLogs(safeList(userServiceClient.getAuditLogs()))
                .build();
    }

    public PanelDashboardResponse getQuizPanel(String role, String userId) {
        Map<String, Object> quizPrep = Map.of();
        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            Map<String, Object> prep = userServiceClient.getQuizPrep(role, userId);
            if (prep != null) {
                quizPrep = prep;
            }
        }
        return PanelDashboardResponse.builder()
                .sports(safeList(sportsServiceClient.getActiveSports()))
                .quizPrep(quizPrep)
                .build();
    }

    private int occupied(Map<String, Object> event) {
        int max = toInt(event.get("maxCapacity"));
        int available = event.get("availableCapacity") == null ? max : toInt(event.get("availableCapacity"));
        return Math.max(max - available, 0);
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private List<Map<String, Object>> safeList(List<Map<String, Object>> value) {
        return value == null ? List.of() : value.stream().filter(Objects::nonNull).toList();
    }
}
