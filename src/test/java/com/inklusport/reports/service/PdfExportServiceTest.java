package com.inklusport.reports.service;

import com.inklusport.reports.dto.AuditExportRequest;
import com.inklusport.reports.dto.AuditLogExportItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PdfExportServiceTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private PdfExportService pdfExportService;

    @Test
    void exportAuditLogs_generaPdfConRegistros() {
        AuditLogExportItem item = new AuditLogExportItem();
        item.setAdminEmail("admin@inklusport.test");
        item.setAction("ASSIGN_ROLE");
        item.setTargetEmail("ana@inklusport.test");
        item.setCreatedAt("2026-09-10 10:00");

        AuditExportRequest request = new AuditExportRequest();
        request.setLogs(List.of(item));

        byte[] pdf = pdfExportService.exportAuditLogs(request);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    }

    @Test
    void exportAuditLogs_aceptaRequestNulo() {
        byte[] pdf = pdfExportService.exportAuditLogs(null);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    }
}
