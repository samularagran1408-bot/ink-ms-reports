package com.inklusport.reports.controller;

import com.inklusport.reports.dto.AuditExportRequest;
import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
import com.inklusport.reports.dto.PanelDashboardResponse;
import com.inklusport.reports.service.DashboardService;
import com.inklusport.reports.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PdfExportService pdfExportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
    public ResponseEntity<DashboardResponse> getDashboard(@ModelAttribute DashboardFilters filters) {
        return ResponseEntity.ok(dashboardService.getDashboard(filters));
    }

    @GetMapping("/home")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PanelDashboardResponse> getHomePanel(
            @RequestParam(value = "userId", required = false) String userId) {
        return ResponseEntity.ok(dashboardService.getHomePanel(userId));
    }

    @GetMapping("/trainer")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getTrainerPanel(
            @RequestParam(value = "trainerId", required = false) String trainerId) {
        return ResponseEntity.ok(dashboardService.getTrainerPanel(trainerId));
    }

    @GetMapping("/organizer")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getOrganizerPanel(
            @RequestParam(value = "organizerId", required = false) String organizerId) {
        return ResponseEntity.ok(dashboardService.getOrganizerPanel(organizerId));
    }

    @GetMapping("/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PanelDashboardResponse> getEventsPanel(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "mode", required = false, defaultValue = "user") String mode) {
        return ResponseEntity.ok(dashboardService.getEventsPanel(userId, mode));
    }

    @GetMapping("/associations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    public ResponseEntity<PanelDashboardResponse> getAssociationsPanel() {
        return ResponseEntity.ok(dashboardService.getAssociationsPanel());
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getSessionsPanel(
            @RequestParam(value = "trainerId", required = false) String trainerId) {
        return ResponseEntity.ok(dashboardService.getSessionsPanel(trainerId));
    }

    @GetMapping("/athletes")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR', 'ENTRENADOR')")
    public ResponseEntity<PanelDashboardResponse> getAthletesPanel(
            @RequestParam(value = "organizerId", required = false) String organizerId,
            @RequestParam(value = "allEvents", required = false, defaultValue = "false") boolean allEvents) {
        return ResponseEntity.ok(dashboardService.getAthletesPanel(organizerId, allEvents));
    }

    @GetMapping("/sports")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    public ResponseEntity<PanelDashboardResponse> getSportsPanel() {
        return ResponseEntity.ok(dashboardService.getSportsPanel());
    }

    @GetMapping("/disabilities")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    public ResponseEntity<PanelDashboardResponse> getDisabilitiesPanel() {
        return ResponseEntity.ok(dashboardService.getDisabilitiesPanel());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getUsersPanel(
            @RequestParam(value = "filter", required = false, defaultValue = "active") String filter) {
        return ResponseEntity.ok(dashboardService.getUsersPanel(filter));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getRolesPanel() {
        return ResponseEntity.ok(dashboardService.getRolesPanel());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getAuditPanel() {
        return ResponseEntity.ok(dashboardService.getAuditPanel());
    }

    @GetMapping("/quiz")
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'ORGANIZADOR', 'ADMIN')")
    public ResponseEntity<PanelDashboardResponse> getQuizPanel(
            @RequestParam(value = "role", required = false, defaultValue = "trainer") String role,
            @RequestParam(value = "userId", required = false) String userId) {
        return ResponseEntity.ok(dashboardService.getQuizPanel(role, userId));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
    public ResponseEntity<byte[]> exportDashboardPdf(@ModelAttribute DashboardFilters filters) {
        byte[] pdf = pdfExportService.exportDashboard(filters);
        return pdfResponse(pdf, "inklusport-dashboard");
    }

    @PostMapping("/export/audit/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
    public ResponseEntity<byte[]> exportAuditPdf(@RequestBody AuditExportRequest request) {
        byte[] pdf = pdfExportService.exportAuditLogs(request);
        return pdfResponse(pdf, "inklusport-audit-logs");
    }

    @PostMapping("/export/analysis/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
    public ResponseEntity<byte[]> exportAnalysisPdf(
            @ModelAttribute DashboardFilters filters,
            @RequestBody(required = false) AuditExportRequest request) {
        byte[] pdf = pdfExportService.exportAnalysis(filters, request != null ? request : new AuditExportRequest());
        return pdfResponse(pdf, "inklusport-audit-analysis");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String prefix) {
        String filename = prefix + "-" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
