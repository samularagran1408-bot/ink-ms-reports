package com.inklusport.reports.service;

import com.inklusport.reports.client.SportsServiceClient;
import com.inklusport.reports.client.UserServiceClient;
import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
import com.inklusport.reports.dto.PagedEventsResponse;
import com.inklusport.reports.dto.PagedUsersResponse;
import com.inklusport.reports.dto.PanelDashboardResponse;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Agrega datos de usuarios y deportes para los paneles del dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final UserServiceClient userServiceClient;
    private final SportsServiceClient sportsServiceClient;

    /**
     * Construye el dashboard general con métricas, tendencias y listados recientes.
     *
     * @param filters rango de fechas y filtros de consulta
     * @return resumen del dashboard
     */
    public DashboardResponse getDashboard(DashboardFilters filters) {
        int totalUsers = userServiceClient.getTotalUsers();
        int activeUsers = userServiceClient.getActiveUsers();
        int activeEvents = sportsServiceClient.getActiveEventsCount();
        int totalSports = sportsServiceClient.getTotalSports();
        List<Map<String, Object>> disabilities = safeList(sportsServiceClient.getDisabilities());
        List<Map<String, Object>> users = pagedUsers(0, 6, "all", null, null).getContent();
        List<Map<String, Object>> events = safeList(sportsServiceClient.getEvents());
        if (events.isEmpty()) {
            events = pagedEvents(0, 50, null, false, null).getContent();
        }

        int inscriptions = events.stream().mapToInt(this::occupied).sum();
        int capacity = events.stream().mapToInt(event -> toInt(event.get("maxCapacity"))).sum();
        int freeSpots = events.stream().mapToInt(event -> {
            int max = toInt(event.get("maxCapacity"));
            return event.get("availableCapacity") == null ? max : toInt(event.get("availableCapacity"));
        }).sum();

        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("total_users", totalUsers);
        metrics.put("active_users", activeUsers);
        metrics.put("active_events", activeEvents);
        metrics.put("total_sports", totalSports);
        metrics.put("total_disabilities", disabilities.size());
        metrics.put("total_events", events.size());
        metrics.put("inscriptions", inscriptions);
        metrics.put("free_spots", freeSpots);
        metrics.put("occupancy_pct", capacity <= 0 ? 0 : Math.round(inscriptions * 100f / capacity));

        Map<String, Long> eventCounts = countBySport(events);
        Map<String, Integer> weeklyTrend = weeklyActivity(events, List.of());

        return DashboardResponse.builder()
                .metrics(metrics)
                .eventCounts(eventCounts)
                .weeklyTrend(weeklyTrend)
                .recentUsers(users.stream().limit(6).toList())
                .recentEvents(events.stream().limit(6).toList())
                .build();
    }

    /**
     * Obtiene los datos del panel de inicio para un usuario.
     *
     * @param userId identificador del usuario
     * @return datos del panel de inicio
     */
    public PanelDashboardResponse getHomePanel(String userId) {
        List<Map<String, Object>> events = pagedEvents(0, 24, null, true, null).getContent();
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

    /**
     * Obtiene el panel de eventos en modo gestión o inscripción.
     *
     * @param userId identificador del usuario
     * @param mode   {@code manage} para gestión; cualquier otro valor para inscripción
     * @param page  página 0-based
     * @param size  tamaño de página (sports lo limita a 50)
     * @param q     texto opcional
     */
    public PanelDashboardResponse getEventsPanel(String userId, String mode, int page, int size, String q) {
        boolean manage = "manage".equalsIgnoreCase(mode);
        // Gestión: organizador solo ve eventos que él creó; admin ve todos.
        String createdBy = null;
        if (manage && !isAdmin() && userId != null && !userId.isBlank()) {
            createdBy = userId.trim();
        }
        PagedEventsResponse paged = pagedEvents(page, size <= 0 ? 20 : size, q, !manage, createdBy);
        List<Map<String, Object>> events = paged.getContent();
        List<Map<String, Object>> registrations = !manage && userId != null && !userId.isBlank()
                ? safeList(sportsServiceClient.getRegistrationsByUser(userId))
                : List.of();
        List<Map<String, Object>> sports = manage
                ? safeList(sportsServiceClient.getActiveSports())
                : List.of();
        return PanelDashboardResponse.builder()
                .events(events)
                .registrations(registrations)
                .sports(sports)
                .waitlists(Map.of())
                .eventsTotal(paged.getTotalElements())
                .eventsPage(paged.getNumber())
                .eventsSize(paged.getSize())
                .eventsTotalPages(paged.getTotalPages())
                .build();
    }

    /**
     * Obtiene el panel de asociaciones entre deportes y discapacidades.
     *
     * @return datos del panel de asociaciones
     */
    public PanelDashboardResponse getAssociationsPanel() {
        return PanelDashboardResponse.builder()
                .sports(safeList(sportsServiceClient.getSports()))
                .disabilities(safeList(sportsServiceClient.getActiveDisabilities()))
                .associations(safeList(sportsServiceClient.getAssociations()))
                .build();
    }

    /**
     * Obtiene el panel de sesiones del entrenador, con inscritos y asistencia por rutina.
     *
     * @param trainerId identificador del entrenador
     * @return rutinas, deportes activos y resúmenes de inscritos
     */
    public PanelDashboardResponse getSessionsPanel(String trainerId) {
        List<Map<String, Object>> routines = trainerId == null || trainerId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRoutinesByTrainer(trainerId));
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Map<String, Object> routine : routines) {
            Object id = routine.get("id");
            if (id == null) {
                continue;
            }
            List<Map<String, Object>> registrations = safeList(
                    sportsServiceClient.getRoutineRegistrations(String.valueOf(id)));
            int enrolled = 0;
            int attended = 0;
            for (Map<String, Object> registration : registrations) {
                String status = String.valueOf(registration.get("status"));
                if ("cancelled".equalsIgnoreCase(status)) {
                    continue;
                }
                enrolled++;
                if ("completed".equalsIgnoreCase(status)) {
                    attended++;
                }
            }
            Map<String, Object> summary = new HashMap<>();
            summary.put("routine", routine);
            summary.put("registrations", registrations);
            summary.put("enrolledCount", enrolled);
            summary.put("attendedCount", attended);
            summary.put("absentCount", Math.max(enrolled - attended, 0));
            summaries.add(summary);
        }
        return PanelDashboardResponse.builder()
                .routines(routines)
                .sports(safeList(sportsServiceClient.getActiveSports()))
                .sessionSummaries(summaries)
                .build();
    }

    /**
     * Obtiene el panel de atletas con espera y asistencia por evento.
     *
     * @param organizerId identificador del organizador
     * @param allEvents   {@code true} para incluir todos los eventos
     * @return eventos y resúmenes de atletas
     */
    public PanelDashboardResponse getAthletesPanel(String organizerId, boolean allEvents) {
        // Sin fallback a catálogo global: organizador sin eventos propios → lista vacía.
        String ownerFilter = allEvents ? null : (organizerId == null || organizerId.isBlank() ? null : organizerId.trim());
        if (!allEvents && (organizerId == null || organizerId.isBlank())) {
            return PanelDashboardResponse.builder()
                    .events(List.of())
                    .athleteSummaries(List.of())
                    .build();
        }
        PagedEventsResponse paged = pagedEvents(0, 20, null, false, ownerFilter);
        List<Map<String, Object>> events = paged.getContent();
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

    /**
     * Obtiene el panel del entrenador con rutinas, atletas y discapacidades.
     *
     * @param trainerId identificador del entrenador
     * @return métricas y listados del entrenador
     */
    public PanelDashboardResponse getTrainerPanel(String trainerId) {
        List<Map<String, Object>> routines = trainerId == null || trainerId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRoutinesByTrainer(trainerId));
        List<Map<String, Object>> disabilities = safeList(sportsServiceClient.getActiveDisabilities());
        Set<String> athleteIds = new HashSet<>();
        List<Map<String, Object>> enrollments = new ArrayList<>();
        int occupiedSeats = 0;
        int routineCapacity = 0;
        for (Map<String, Object> routine : routines) {
            occupiedSeats += occupied(routine);
            routineCapacity += toInt(routine.get("maxCapacity"));
            Object id = routine.get("id");
            if (id == null) {
                continue;
            }
            for (Map<String, Object> registration : safeList(sportsServiceClient.getRoutineRegistrations(String.valueOf(id)))) {
                enrollments.add(registration);
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
        metrics.put("drafts", (int) routines.stream()
                .filter(routine -> !"published".equals(String.valueOf(routine.get("status"))))
                .count());
        metrics.put("athletes", athleteIds.size());
        metrics.put("enrollments", enrollments.size());
        metrics.put("disabilities", disabilities.size());
        metrics.put("occupancy_pct", routineCapacity <= 0 ? 0 : Math.round(occupiedSeats * 100f / routineCapacity));
        return PanelDashboardResponse.builder()
                .metrics(metrics)
                .routines(routines)
                .disabilities(disabilities)
                .athleteCount(athleteIds.size())
                .weeklyTrend(weeklyActivity(routines, enrollments))
                .eventCounts(countByKey(routines, "disabilityFocus", "General"))
                .build();
    }

    /**
     * Obtiene el panel del organizador con eventos, aforo y tasa de asistencia.
     *
     * @param organizerId identificador del organizador
     * @return métricas y eventos del organizador
     */
    public PanelDashboardResponse getOrganizerPanel(String organizerId) {
        // Sin fallback a “todos”: si no tiene eventos, la lista queda vacía.
        String ownerFilter = isAdmin() ? null : (organizerId == null || organizerId.isBlank() ? null : organizerId.trim());
        if (!isAdmin() && (organizerId == null || organizerId.isBlank())) {
            return PanelDashboardResponse.builder()
                    .events(List.of())
                    .sports(safeList(sportsServiceClient.getActiveSports()))
                    .metrics(Map.of(
                            "active_events", 0,
                            "athletes", 0,
                            "upcoming", 0,
                            "finished", 0,
                            "occupancy_pct", 0,
                            "sports", 0))
                    .build();
        }
        PagedEventsResponse paged = pagedEvents(0, 50, null, false, ownerFilter);
        List<Map<String, Object>> sports = safeList(sportsServiceClient.getActiveSports());
        List<Map<String, Object>> events = paged.getContent();
        int athleteCount = events.stream().mapToInt(this::occupied).sum();
        int capacity = events.stream().mapToInt(event -> toInt(event.get("maxCapacity"))).sum();
        int upcoming = 0;
        int finished = 0;
        LocalDate today = LocalDate.now();
        for (Map<String, Object> event : events) {
            String status = String.valueOf(event.getOrDefault("status", "")).toLowerCase();
            LocalDate day = parseDay(event.get("eventDate"));
            if ("finished".equals(status) || "cancelled".equals(status) || (day != null && day.isBefore(today))) {
                finished++;
            } else {
                upcoming++;
            }
        }
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
        metrics.put("active_events", isAdmin()
                ? sportsServiceClient.getActiveEventsCount()
                : (int) events.stream()
                        .filter(e -> "active".equalsIgnoreCase(String.valueOf(e.getOrDefault("status", ""))))
                        .count());
        metrics.put("athletes", athleteCount);
        metrics.put("upcoming", upcoming);
        metrics.put("finished", finished);
        metrics.put("occupancy_pct", capacity <= 0 ? 0 : Math.round(athleteCount * 100f / capacity));
        metrics.put("sports", sports.size());
        return PanelDashboardResponse.builder()
                .metrics(metrics)
                .events(events)
                .sports(sports)
                .athleteCount(athleteCount)
                .attendanceRatePercent(sample.isEmpty() ? null : rate)
                .attendanceSampledEvents(sample.size())
                .weeklyTrend(weeklyActivity(events, List.of()))
                .eventCounts(countBySport(events))
                .build();
    }

    /**
     * Obtiene el panel de catálogo de deportes.
     *
     * @return listado de deportes
     */
    public PanelDashboardResponse getSportsPanel() {
        return PanelDashboardResponse.builder()
                .sports(safeList(sportsServiceClient.getSports()))
                .build();
    }

    /**
     * Obtiene el panel de catálogo de discapacidades.
     *
     * @return listado de discapacidades
     */
    public PanelDashboardResponse getDisabilitiesPanel() {
        return PanelDashboardResponse.builder()
                .disabilities(safeList(sportsServiceClient.getDisabilities()))
                .build();
    }

    /**
     * Obtiene el panel de usuarios según el filtro indicado.
     *
     * @param filter     {@code inactive}, {@code all} o activos por defecto
     * @param page       página 0-based
     * @param size       tamaño (users lo limita a 50)
     * @param name       texto opcional (nombre o email)
     * @param disability filtro opcional de discapacidad
     * @return listado de usuarios
     */
    public PanelDashboardResponse getUsersPanel(
            String filter, int page, int size, String name, String disability) {
        PagedUsersResponse paged = pagedUsers(page, size, filter, name, disability);
        return PanelDashboardResponse.builder()
                .users(paged.getContent())
                .usersTotal(paged.getTotalElements())
                .usersPage(paged.getNumber())
                .usersSize(paged.getSize())
                .usersTotalPages(paged.getTotalPages())
                .build();
    }

    /**
     * Obtiene el panel de roles y usuarios.
     *
     * @return roles y listado de usuarios
     */
    public PanelDashboardResponse getRolesPanel() {
        return PanelDashboardResponse.builder()
                .roles(safeList(userServiceClient.getRoles()))
                .users(pagedUsers(0, 50, "all", null, null).getContent())
                .build();
    }

    /**
     * Obtiene el panel de auditoría con métricas, usuarios y bitácora.
     *
     * @return datos del panel de auditoría
     */
    public PanelDashboardResponse getAuditPanel() {
        DashboardResponse dashboard = getDashboard(new DashboardFilters());
        return PanelDashboardResponse.builder()
                .metrics(dashboard.getMetrics())
                .eventCounts(dashboard.getEventCounts())
                .weeklyTrend(dashboard.getWeeklyTrend())
                .users(pagedUsers(0, 50, "all", null, null).getContent())
                .auditLogs(safeList(userServiceClient.getAuditLogs()))
                .build();
    }

    /**
     * Obtiene el panel de cuestionario de preparación según rol y usuario.
     *
     * @param role   rol del usuario
     * @param userId identificador del usuario
     * @return deportes activos y datos de preparación
     */
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

    /**
     * Pide una página de eventos a sports-ms. Nunca retorna null.
     */
    private PagedEventsResponse pagedEvents(int page, int size, String q, boolean availableOnly, String createdBy) {
        PagedEventsResponse paged = sportsServiceClient.getEventsPage(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                q,
                null,
                null,
                availableOnly,
                createdBy);
        if (paged == null) {
            paged = new PagedEventsResponse();
        }
        if (paged.getContent() == null) {
            paged.setContent(List.of());
        } else {
            paged.setContent(paged.getContent().stream().filter(Objects::nonNull).toList());
        }
        return paged;
    }

    /** Admin ve todos los eventos; organizador solo los propios ({@code createdBy}). */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if ("ROLE_ADMIN".equals(value) || "ADMIN".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pide una página de usuarios a users-ms. Nunca retorna null.
     */
    private PagedUsersResponse pagedUsers(int page, int size, String filter, String name, String disability) {
        PagedUsersResponse paged = userServiceClient.getUsersPage(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                filter,
                name,
                disability);
        if (paged == null) {
            paged = new PagedUsersResponse();
        }
        if (paged.getContent() == null) {
            paged.setContent(List.of());
        } else {
            paged.setContent(paged.getContent().stream().filter(Objects::nonNull).toList());
        }
        return paged;
    }

    /**
     * Calcula los cupos ocupados de un evento.
     *
     * @param event evento con capacidad máxima y disponible
     * @return plazas ocupadas, o 0 si no hay datos
     */
    private int occupied(Map<String, Object> event) {
        int max = toInt(event.get("maxCapacity"));
        int available = event.get("availableCapacity") == null ? max : toInt(event.get("availableCapacity"));
        return Math.max(max - available, 0);
    }

    /**
     * Actividad de 7 días: eventos programados o creados + inscripciones (cupos ocupados).
     * Prefiere la semana actual; si está vacía, la próxima; si no, la ventana con más actividad.
     */
    private Map<String, Integer> weeklyActivity(
            List<Map<String, Object>> items,
            List<Map<String, Object>> extraDates) {
        LocalDate today = LocalDate.now();
        Map<String, Integer> past = fillWeek(today.minusDays(6), items, extraDates);
        if (hasPositive(past)) {
            return past;
        }
        Map<String, Integer> next = fillWeek(today, items, extraDates);
        if (hasPositive(next)) {
            return next;
        }
        LocalDate densest = densestWeekStart(items, extraDates);
        return densest == null ? past : fillWeek(densest, items, extraDates);
    }

    private boolean hasPositive(Map<String, Integer> trend) {
        return trend.values().stream().anyMatch(value -> value != null && value > 0);
    }

    private LocalDate densestWeekStart(
            List<Map<String, Object>> items,
            List<Map<String, Object>> extraDates) {
        List<LocalDate> days = new ArrayList<>();
        for (Map<String, Object> item : items) {
            LocalDate eventDay = parseDay(firstPresent(item.get("eventDate"), item.get("sessionDate")));
            if (eventDay != null) {
                days.add(eventDay);
            }
            LocalDate created = parseDay(item.get("createdAt"));
            if (created != null) {
                days.add(created);
            }
        }
        if (extraDates != null) {
            for (Map<String, Object> row : extraDates) {
                LocalDate day = parseDay(firstPresent(
                        row.get("registrationDate"),
                        row.get("createdAt"),
                        row.get("enrolledAt"),
                        row.get("registeredAt")));
                if (day != null) {
                    days.add(day);
                }
            }
        }
        if (days.isEmpty()) {
            return null;
        }
        LocalDate min = days.stream().min(LocalDate::compareTo).orElse(null);
        LocalDate max = days.stream().max(LocalDate::compareTo).orElse(null);
        if (min == null || max == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate best = min;
        int bestScore = -1;
        long bestDist = Long.MAX_VALUE;
        for (LocalDate start = min; !start.isAfter(max); start = start.plusDays(1)) {
            LocalDate end = start.plusDays(6);
            int score = 0;
            for (LocalDate day : days) {
                if (!day.isBefore(start) && !day.isAfter(end)) {
                    score++;
                }
            }
            long dist = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, start));
            if (score > bestScore || (score == bestScore && dist < bestDist)) {
                bestScore = score;
                bestDist = dist;
                best = start;
            }
        }
        return bestScore <= 0 ? null : best;
    }

    private Map<String, Integer> fillWeek(
            LocalDate start,
            List<Map<String, Object>> items,
            List<Map<String, Object>> extraDates) {
        Map<String, Integer> trend = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            trend.put(start.plusDays(i).toString(), 0);
        }
        for (Map<String, Object> item : items) {
            LocalDate eventDay = parseDay(firstPresent(item.get("eventDate"), item.get("sessionDate")));
            if (eventDay != null && trend.containsKey(eventDay.toString())) {
                trend.merge(eventDay.toString(), 1 + occupied(item), Integer::sum);
            }
            LocalDate created = parseDay(item.get("createdAt"));
            if (created != null && trend.containsKey(created.toString()) && !created.equals(eventDay)) {
                int weight = eventDay == null ? 1 + occupied(item) : 1;
                trend.merge(created.toString(), weight, Integer::sum);
            }
        }
        if (extraDates != null) {
            for (Map<String, Object> row : extraDates) {
                LocalDate day = parseDay(firstPresent(
                        row.get("registrationDate"),
                        row.get("createdAt"),
                        row.get("enrolledAt")));
                if (day != null && trend.containsKey(day.toString())) {
                    trend.merge(day.toString(), 1, Integer::sum);
                }
            }
        }
        return trend;
    }

    private Map<String, Long> countBySport(List<Map<String, Object>> events) {
        return countByKey(events, "sportName", "Sin deporte");
    }

    private Map<String, Long> countByKey(List<Map<String, Object>> items, String key, String fallback) {
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> item : items) {
            String label = String.valueOf(item.getOrDefault(key, fallback));
            if (label.isBlank() || "null".equalsIgnoreCase(label)) {
                label = fallback;
            }
            counts.merge(label, 1L, Long::sum);
        }
        return counts;
    }

    private LocalDate parseDay(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        if (raw.length() >= 10) {
            try {
                return LocalDate.parse(raw.substring(0, 10));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Object firstPresent(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank() && !"null".equalsIgnoreCase(String.valueOf(value))) {
                return value;
            }
        }
        return null;
    }

    /**
     * Convierte un valor a entero; si no es numérico, retorna 0.
     *
     * @param value valor a convertir
     * @return entero equivalente
     */
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

    /**
     * Devuelve una lista segura, sin nulos, o vacía si el origen es {@code null}.
     *
     * @param value lista de origen
     * @return lista filtrada o vacía
     */
    private List<Map<String, Object>> safeList(List<Map<String, Object>> value) {
        return value == null ? List.of() : value.stream().filter(Objects::nonNull).toList();
    }
}
