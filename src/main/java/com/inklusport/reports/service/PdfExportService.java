package com.inklusport.reports.service;

import com.inklusport.reports.dto.AuditExportRequest;
import com.inklusport.reports.dto.AuditLogExportItem;
import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private static final Color BRAND_RED = new Color(163, 13, 17);
    private static final Color HEADER_BG = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final DashboardService dashboardService;

    public byte[] exportDashboard(DashboardFilters filters) {
        DashboardResponse dashboard = dashboardService.getDashboard(filters);
        return buildDocument("Dashboard Report", "SYSTEM INTEGRITY", document -> {
            addSectionTitle(document, "Key Metrics");
            document.add(buildMetricsTable(dashboard.getMetrics()));
            document.add(spacer(10));

            addSectionTitle(document, "Weekly Activity Trend");
            document.add(buildKeyValueTable("Date", "Events", sortTrend(dashboard.getWeeklyTrend())));
            document.add(spacer(10));

            addSectionTitle(document, "Event Type Distribution");
            document.add(buildKeyValueTable("Event Type", "Count", toStringLongMap(dashboard.getEventCounts())));
        });
    }

    public byte[] exportAuditLogs(AuditExportRequest request) {
        List<AuditLogExportItem> logs = request != null && request.getLogs() != null
                ? request.getLogs()
                : List.of();

        return buildDocument("Detailed Activity Log", "SYSTEM INTEGRITY", document -> {
            document.add(metaLine("Total records: " + logs.size()));
            document.add(spacer(8));
            document.add(buildAuditTable(logs));
        });
    }

    public byte[] exportAnalysis(DashboardFilters filters, AuditExportRequest request) {
        DashboardResponse dashboard = dashboardService.getDashboard(filters);
        List<AuditLogExportItem> logs = request != null && request.getLogs() != null
                ? request.getLogs()
                : List.of();

        return buildDocument("Audit & Analysis", "SYSTEM INTEGRITY", document -> {
            addSectionTitle(document, "Key Metrics");
            document.add(buildMetricsTable(dashboard.getMetrics()));
            document.add(spacer(10));

            addSectionTitle(document, "Weekly Activity Trend");
            document.add(buildKeyValueTable("Date", "Events", sortTrend(dashboard.getWeeklyTrend())));
            document.add(spacer(10));

            addSectionTitle(document, "Event Type Distribution");
            document.add(buildKeyValueTable("Event Type", "Count", toStringLongMap(dashboard.getEventCounts())));
            document.add(spacer(14));

            addSectionTitle(document, "Detailed Activity Log");
            document.add(metaLine("Showing " + logs.size() + " audit records"));
            document.add(spacer(6));
            document.add(buildAuditTable(logs));
        });
    }

    @FunctionalInterface
    private interface DocumentWriter {
        void write(Document document) throws DocumentException;
    }

    private byte[] buildDocument(String title, String eyebrow, DocumentWriter writer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font eyebrowFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_RED);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);

            Paragraph eyebrowParagraph = new Paragraph(eyebrow, eyebrowFont);
            eyebrowParagraph.setSpacingAfter(4);
            document.add(eyebrowParagraph);

            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setSpacingAfter(4);
            document.add(titleParagraph);

            document.add(new Paragraph(
                    "Inklusport Admin · Generated " + LocalDateTime.now().format(GENERATED_AT),
                    metaFont
            ));
            document.add(spacer(14));

            writer.write(document);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private void addSectionTitle(Document document, String text) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private Paragraph metaLine(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
        return new Paragraph(text, font);
    }

    private Paragraph spacer(float points) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(points);
        return paragraph;
    }

    private PdfPTable buildMetricsTable(Map<String, Integer> metrics) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1, 1});

        addMetricCell(table, "Total Users", valueOf(metrics, "total_users"));
        addMetricCell(table, "Active Users", valueOf(metrics, "active_users"));
        addMetricCell(table, "Active Events", valueOf(metrics, "active_events"));
        addMetricCell(table, "Sports", valueOf(metrics, "total_sports"));
        return table;
    }

    private void addMetricCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(HEADER_BG);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);

        Paragraph content = new Paragraph();
        content.add(new Phrase(label.toUpperCase() + "\n", labelFont));
        content.add(new Phrase(value, valueFont));
        cell.addElement(content);
        table.addCell(cell);
    }

    private PdfPTable buildKeyValueTable(String keyHeader, String valueHeader, List<Map.Entry<String, String>> rows)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1});
        addHeaderCell(table, keyHeader);
        addHeaderCell(table, valueHeader);

        if (rows.isEmpty()) {
            addBodyCell(table, "No data", 2);
            return table;
        }

        for (Map.Entry<String, String> row : rows) {
            addBodyCell(table, row.getKey(), 1);
            addBodyCell(table, row.getValue(), 1);
        }
        return table;
    }

    private PdfPTable buildAuditTable(List<AuditLogExportItem> logs) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 2f, 2.2f, 1.6f, 3f});

        addHeaderCell(table, "Date & Time");
        addHeaderCell(table, "Action Type");
        addHeaderCell(table, "User Entity");
        addHeaderCell(table, "IP Address");
        addHeaderCell(table, "Activity Details");

        if (logs.isEmpty()) {
            addBodyCell(table, "No audit records", 5);
            return table;
        }

        for (AuditLogExportItem log : logs) {
            addBodyCell(table, safe(log.getCreatedAt()), 1);
            addBodyCell(table, safe(log.getAction()), 1);
            addBodyCell(table, firstNonBlank(log.getAdminEmail(), log.getTargetEmail(), "—"), 1);
            addBodyCell(table, safe(log.getIpAddress()), 1);
            addBodyCell(table, summarizeDetails(log), 1);
        }
        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED);
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(), font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int colspan) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        table.addCell(cell);
    }

    private List<Map.Entry<String, String>> sortTrend(Map<String, Integer> trend) {
        if (trend == null || trend.isEmpty()) {
            return List.of();
        }
        return trend.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
                .toList();
    }

    private List<Map.Entry<String, String>> toStringLongMap(Map<String, Long> map) {
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
                .toList();
    }

    private String valueOf(Map<String, Integer> metrics, String key) {
        if (metrics == null || metrics.get(key) == null) {
            return "0";
        }
        return String.valueOf(metrics.get(key));
    }

    private String summarizeDetails(AuditLogExportItem log) {
        String details = safe(log.getDetails());
        String target = firstNonBlank(log.getTargetEmail(), log.getTargetUserId(), "");
        if (!target.isBlank() && !target.equals(safe(log.getAdminEmail()))) {
            return "Target: " + target + (details.equals("—") ? "" : " · " + truncate(details, 80));
        }
        return truncate(details, 100);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "—";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed.isEmpty() ? "—" : trimmed;
        }
        return trimmed.substring(0, max - 1) + "…";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "—";
    }
}
