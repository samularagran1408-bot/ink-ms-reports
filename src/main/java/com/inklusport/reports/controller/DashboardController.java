package com.inklusport.reports.controller;

import com.inklusport.reports.dto.AuditExportRequest;
import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
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
