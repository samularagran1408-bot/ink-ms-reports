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
        List<Map<String, Object>> users = pagedUsers(0, 6, "all", null, null).getContent();
        List<Map<String, Object>> events = pagedEvents(0, 8, null, false, null).getContent();

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
        PagedEventsResponse paged = pagedEvents(page, size <= 0 ? 20 : size, q, !manage, null);
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
     * Obtiene el panel de sesiones del entrenador.
     *
     * @param trainerId identificador del entrenador
     * @return rutinas y deportes activos
     */
    public PanelDashboardResponse getSessionsPanel(String trainerId) {
        List<Map<String, Object>> routines = trainerId == null || trainerId.isBlank()
                ? List.of()
                : safeList(sportsServiceClient.getRoutinesByTrainer(trainerId));
        return PanelDashboardResponse.builder()
                .routines(routines)
                .sports(safeList(sportsServiceClient.getActiveSports()))
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
        PagedEventsResponse paged = pagedEvents(0, 20, null, false, allEvents ? null : organizerId);
        List<Map<String, Object>> events = paged.getContent();
        if (!allEvents && organizerId != null && !organizerId.isBlank() && events.isEmpty()) {
            events = pagedEvents(0, 20, null, false, null).getContent();
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

    /**
     * Obtiene el panel del organizador con eventos, aforo y tasa de asistencia.
     *
     * @param organizerId identificador del organizador
     * @return métricas y eventos del organizador
     */
    public PanelDashboardResponse getOrganizerPanel(String organizerId) {
        PagedEventsResponse paged = pagedEvents(0, 50, null, false, organizerId);
        List<Map<String, Object>> sports = safeList(sportsServiceClient.getActiveSports());
        List<Map<String, Object>> events = paged.getContent();
        if (organizerId != null && !organizerId.isBlank() && events.isEmpty()) {
            events = pagedEvents(0, 50, null, false, null).getContent();
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
