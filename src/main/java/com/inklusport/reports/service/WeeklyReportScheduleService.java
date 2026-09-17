package com.inklusport.reports.service;

import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.WeeklyScheduleResponse;
import com.inklusport.reports.entity.ReportConfig;
import com.inklusport.reports.repository.ReportConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Programa, cancela y ejecuta el envío semanal de reportes por correo (HU35).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduleService {

    public static final String FREQUENCY_WEEKLY = "WEEKLY";
    public static final String WEEKLY_REPORT_NAME = "Reporte semanal automático";
    private static final String DEFAULT_FILTERS = "{\"module\":\"dashboard\",\"window\":\"7d\"}";

    private final ReportConfigRepository reportConfigRepository;
    private final PdfExportService pdfExportService;
    private final ReportEmailService reportEmailService;

    /**
     * Activa o actualiza la programación semanal para el propietario (email del JWT).
     */
    @Transactional
    public WeeklyScheduleResponse scheduleWeekly(String ownerEmail) {
        String email = requireEmail(ownerEmail);
        ReportConfig config = reportConfigRepository
                .findFirstByOwnerIdAndScheduleFrequency(email, FREQUENCY_WEEKLY)
                .orElseGet(() -> ReportConfig.builder()
                        .reportName(WEEKLY_REPORT_NAME)
                        .filters(DEFAULT_FILTERS)
                        .ownerId(email)
                        .scheduleFrequency(FREQUENCY_WEEKLY)
                        .build());

        config.setScheduleEnabled(true);
        config.setScheduleFrequency(FREQUENCY_WEEKLY);
        config.setRecipientEmail(email);
        if (config.getReportName() == null || config.getReportName().isBlank()) {
            config.setReportName(WEEKLY_REPORT_NAME);
        }
        if (config.getFilters() == null || config.getFilters().isBlank()) {
            config.setFilters(DEFAULT_FILTERS);
        }

        ReportConfig saved = reportConfigRepository.save(config);
        log.info("Programación semanal activada para {}", email);
        return toResponse(saved);
    }

    /**
     * Cancela la programación semanal activa del propietario (CP8-HU35).
     */
    @Transactional
    public WeeklyScheduleResponse cancelWeekly(String ownerEmail) {
        String email = requireEmail(ownerEmail);
        ReportConfig config = reportConfigRepository
                .findFirstByOwnerIdAndScheduleFrequency(email, FREQUENCY_WEEKLY)
                .orElse(null);

        if (config == null) {
            return WeeklyScheduleResponse.builder()
                    .enabled(false)
                    .recipientEmail(email)
                    .frequency(FREQUENCY_WEEKLY)
                    .build();
        }

        config.setScheduleEnabled(false);
        ReportConfig saved = reportConfigRepository.save(config);
        log.info("Programación semanal cancelada para {}", email);
        return toResponse(saved);
    }

    /**
     * Estado actual de la programación semanal del propietario.
     */
    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getWeekly(String ownerEmail) {
        String email = requireEmail(ownerEmail);
        return reportConfigRepository
                .findFirstByOwnerIdAndScheduleFrequency(email, FREQUENCY_WEEKLY)
                .map(this::toResponse)
                .orElseGet(() -> WeeklyScheduleResponse.builder()
                        .enabled(false)
                        .recipientEmail(email)
                        .frequency(FREQUENCY_WEEKLY)
                        .build());
    }

    /**
     * Procesa todas las programaciones semanales activas.
     */
    @Transactional
    public int processDueWeeklyReports() {
        List<ReportConfig> due = reportConfigRepository
                .findByScheduleEnabledTrueAndScheduleFrequency(FREQUENCY_WEEKLY);
        int sent = 0;
        for (ReportConfig config : due) {
            if (sendOne(config)) {
                sent++;
            }
        }
        log.info("Envíos semanales procesados: {}/{}", sent, due.size());
        return sent;
    }

    private boolean sendOne(ReportConfig config) {
        String to = config.getRecipientEmail() != null ? config.getRecipientEmail() : config.getOwnerId();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        DashboardFilters filters = new DashboardFilters();
        filters.setStartDate(start);
        filters.setEndDate(end);

        try {
            byte[] pdf = pdfExportService.exportDashboard(filters);
            String subject = "InkluSport: reporte semanal (" + start + " – " + end + ")";
            String body = """
                    <!DOCTYPE html>
                    <html><body style="font-family:Arial,sans-serif;color:#0f172a;">
                      <h2 style="color:#A30D11;">InkluSport</h2>
                      <p>Adjunto encontrarás el reporte semanal del dashboard.</p>
                      <p style="color:#64748b;font-size:13px;">Periodo: %s a %s</p>
                    </body></html>
                    """.formatted(start, end);

            boolean ok = reportEmailService.sendWeeklyReport(
                    to, subject, body, pdf, "reporte-semanal-" + end + ".pdf");
            if (ok) {
                config.setLastRun(LocalDateTime.now());
                reportConfigRepository.save(config);
            }
            return ok;
        } catch (Exception e) {
            log.error("Fallo al generar/enviar reporte semanal para {}: {}", to, e.getMessage());
            return false;
        }
    }

    private WeeklyScheduleResponse toResponse(ReportConfig config) {
        return WeeklyScheduleResponse.builder()
                .enabled(Boolean.TRUE.equals(config.getScheduleEnabled()))
                .reportConfigId(config.getId())
                .recipientEmail(config.getRecipientEmail() != null ? config.getRecipientEmail() : config.getOwnerId())
                .frequency(config.getScheduleFrequency() != null ? config.getScheduleFrequency() : FREQUENCY_WEEKLY)
                .lastRun(config.getLastRun())
                .createdAt(config.getCreatedAt())
                .build();
    }

    private String requireEmail(String ownerEmail) {
        if (ownerEmail == null || ownerEmail.isBlank()) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return ownerEmail.trim().toLowerCase();
    }
}
