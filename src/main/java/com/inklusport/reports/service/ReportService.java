package com.inklusport.reports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inklusport.reports.dto.ReportConfigRequest;
import com.inklusport.reports.dto.ReportConfigResponse;
import com.inklusport.reports.dto.ReportRunResponse;
import com.inklusport.reports.entity.ReportConfig;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import com.inklusport.reports.repository.ReportConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestiona configuraciones de reportes y su ejecución sobre eventos de analítica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportConfigRepository reportConfigRepository;
    private final AnalyticsEventRepository analyticsEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea una configuración de reporte para el propietario indicado.
     *
     * @param ownerId identificador del dueño
     * @param request datos de la configuración
     * @return configuración persistida
     */
    @Transactional
    public ReportConfigResponse createReportConfig(String ownerId, ReportConfigRequest request) {
        ReportConfig config = ReportConfig.builder()
                .reportName(request.getReportName())
                .filters(request.getFilters())
                .ownerId(ownerId)
                .build();

        ReportConfig saved = reportConfigRepository.save(config);
        log.info("Configuración de reporte creada: {} para usuario {}", saved.getReportName(), ownerId);

        return convertToResponse(saved);
    }

    /**
     * Actualiza una configuración de reporte del propietario.
     *
     * @param id      identificador de la configuración
     * @param ownerId identificador del dueño
     * @param request nuevos datos
     * @return configuración actualizada
     */
    @Transactional
    public ReportConfigResponse updateReportConfig(String id, String ownerId, ReportConfigRequest request) {
        ReportConfig config = findOwnedConfig(id, ownerId);

        config.setReportName(request.getReportName());
        config.setFilters(request.getFilters());

        ReportConfig saved = reportConfigRepository.save(config);
        log.info("Configuración de reporte actualizada: {}", id);

        return convertToResponse(saved);
    }

    /**
     * Lista las configuraciones de reporte del propietario.
     *
     * @param ownerId identificador del dueño
     * @return configuraciones del usuario
     */
    @Transactional(readOnly = true)
    public List<ReportConfigResponse> getMyReportConfigs(String ownerId) {
        return reportConfigRepository.findByOwnerId(ownerId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Elimina una configuración de reporte del propietario.
     *
     * @param id      identificador de la configuración
     * @param ownerId identificador del dueño
     */
    @Transactional
    public void deleteReportConfig(String id, String ownerId) {
        ReportConfig config = findOwnedConfig(id, ownerId);
        reportConfigRepository.delete(config);
        log.info("Configuración de reporte eliminada: {}", id);
    }

    /**
     * Ejecuta un reporte, agrega eventos del periodo y actualiza la última ejecución.
     *
     * @param id      identificador de la configuración
     * @param ownerId identificador del dueño
     * @return resultado de la ejecución
     */
    @Transactional
    public ReportRunResponse runReport(String id, String ownerId) {
        ReportConfig config = findOwnedConfig(id, ownerId);
        Map<String, String> filters = parseFilters(config.getFilters());

        LocalDateTime startDate = parseStartDate(filters.get("start_date"), LocalDate.now().minusDays(30));
        LocalDateTime endDate = parseEndDate(filters.get("end_date"), LocalDate.now());

        List<Object[]> eventCountsRaw = analyticsEventRepository.countByEventTypeAndDateRange(startDate, endDate);
        Map<String, Long> eventCounts = eventCountsRaw.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        long totalEvents = analyticsEventRepository.countByDateRange(startDate, endDate);

        Map<String, Object> results = new HashMap<>();
        results.put("totalEvents", totalEvents);
        results.put("eventCounts", eventCounts);
        results.put("startDate", startDate.toLocalDate().toString());
        results.put("endDate", endDate.toLocalDate().toString());
        results.put("filters", filters);

        LocalDateTime executedAt = LocalDateTime.now();
        config.setLastRun(executedAt);
        reportConfigRepository.save(config);

        log.info("Reporte ejecutado: {} ({})", config.getReportName(), id);

        return ReportRunResponse.builder()
                .reportId(config.getId())
                .reportName(config.getReportName())
                .executedAt(executedAt)
                .lastRun(executedAt)
                .results(results)
                .build();
    }

    /**
     * Busca una configuración y valida que pertenezca al propietario.
     *
     * @param id      identificador de la configuración
     * @param ownerId identificador del dueño
     * @return configuración encontrada
     */
    private ReportConfig findOwnedConfig(String id, String ownerId) {
        ReportConfig config = reportConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));

        if (!config.getOwnerId().equals(ownerId)) {
            throw new IllegalStateException("No autorizado");
        }

        return config;
    }

    /**
     * Parsea el JSON de filtros; si falla, retorna un mapa vacío.
     *
     * @param filtersJson filtros serializados
     * @return mapa de filtros
     */
    private Map<String, String> parseFilters(String filtersJson) {
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("No se pudieron parsear filtros, usando valores por defecto: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Interpreta la fecha de inicio o usa el valor por defecto al inicio del día.
     *
     * @param value       fecha en texto
     * @param defaultDate fecha de respaldo
     * @return inicio del día
     */
    private LocalDateTime parseStartDate(String value, LocalDate defaultDate) {
        if (value == null || value.isBlank()) {
            return defaultDate.atStartOfDay();
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (Exception e) {
            return defaultDate.atStartOfDay();
        }
    }

    /**
     * Interpreta la fecha de fin o usa el valor por defecto al final del día.
     *
     * @param value       fecha en texto
     * @param defaultDate fecha de respaldo
     * @return fin del día
     */
    private LocalDateTime parseEndDate(String value, LocalDate defaultDate) {
        if (value == null || value.isBlank()) {
            return defaultDate.atTime(LocalTime.MAX);
        }
        try {
            return LocalDate.parse(value).atTime(LocalTime.MAX);
        } catch (Exception e) {
            return defaultDate.atTime(LocalTime.MAX);
        }
    }

    /**
     * Convierte una configuración persistida a su DTO de respuesta.
     *
     * @param config entidad persistida
     * @return DTO de la configuración
     */
    private ReportConfigResponse convertToResponse(ReportConfig config) {
        return ReportConfigResponse.builder()
                .id(config.getId())
                .reportName(config.getReportName())
                .filters(config.getFilters())
                .ownerId(config.getOwnerId())
                .lastRun(config.getLastRun())
                .createdAt(config.getCreatedAt())
                .build();
    }
}
