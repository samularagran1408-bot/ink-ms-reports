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

/**
 * Genera exportaciones PDF del dashboard, auditoría y análisis.
 */
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

    /**
     * Exporta el dashboard a PDF con métricas, tendencia semanal y tipos de evento.
     *
     * @param filters filtros del dashboard
     * @return bytes del PDF generado
     */
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

    /**
     * Exporta el listado de bitácora de auditoría a PDF.
     *
     * @param request solicitud con los registros a incluir
     * @return bytes del PDF generado
     */
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

    /**
     * Exporta un PDF combinado de métricas del dashboard y bitácora de auditoría.
     *
     * @param filters filtros del dashboard
     * @param request solicitud con los registros de auditoría
     * @return bytes del PDF generado
     */
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

    /**
     * Callback para escribir el cuerpo de un documento PDF.
     */
    @FunctionalInterface
    private interface DocumentWriter {
        /**
         * Escribe el contenido del documento.
         *
         * @param document documento PDF abierto
         * @throws DocumentException si falla la escritura
         */
        void write(Document document) throws DocumentException;
    }

    /**
     * Crea un PDF A4 horizontal con cabecera y el contenido del escritor.
     *
     * @param title   título del documento
     * @param eyebrow texto de marca sobre el título
     * @param writer  escritor del cuerpo
     * @return bytes del PDF
     */
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

    /**
     * Añade un título de sección al documento.
     *
     * @param document documento PDF
     * @param text     texto del título
     * @throws DocumentException si falla la escritura
     */
    private void addSectionTitle(Document document, String text) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    /**
     * Crea una línea de metadatos en estilo secundario.
     *
     * @param text texto a mostrar
     * @return párrafo de metadatos
     */
    private Paragraph metaLine(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
        return new Paragraph(text, font);
    }

    /**
     * Crea un espacio vertical en el documento.
     *
     * @param points separación posterior en puntos
     * @return párrafo vacío con espaciado
     */
    private Paragraph spacer(float points) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(points);
        return paragraph;
    }

    /**
     * Construye la tabla de métricas clave del dashboard.
     *
     * @param metrics mapa de métricas
     * @return tabla PDF
     * @throws DocumentException si falla el armado
     */
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

    /**
     * Añade una celda de métrica con etiqueta y valor.
     *
     * @param table tabla destino
     * @param label etiqueta de la métrica
     * @param value valor a mostrar
     */
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

    /**
     * Construye una tabla de dos columnas clave-valor.
     *
     * @param keyHeader   encabezado de la clave
     * @param valueHeader encabezado del valor
     * @param rows        filas a pintar
     * @return tabla PDF
     * @throws DocumentException si falla el armado
     */
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

    /**
     * Construye la tabla de registros de auditoría.
     *
     * @param logs registros a exportar
     * @return tabla PDF
     * @throws DocumentException si falla el armado
     */
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

    /**
     * Añade una celda de encabezado a la tabla.
     *
     * @param table tabla destino
     * @param text  texto del encabezado
     */
    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED);
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(), font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    /**
     * Añade una celda de cuerpo, con colspan opcional.
     *
     * @param table   tabla destino
     * @param text    contenido
     * @param colspan columnas que ocupa
     */
    private void addBodyCell(PdfPTable table, String text, int colspan) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        table.addCell(cell);
    }

    /**
     * Ordena la tendencia semanal por fecha y la convierte a texto.
     *
     * @param trend mapa fecha-cantidad
     * @return entradas ordenadas
     */
    private List<Map.Entry<String, String>> sortTrend(Map<String, Integer> trend) {
        if (trend == null || trend.isEmpty()) {
            return List.of();
        }
        return trend.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
                .toList();
    }

    /**
     * Convierte un mapa de conteos a filas de texto ordenadas por valor.
     *
     * @param map mapa tipo-cantidad
     * @return entradas listas para la tabla
     */
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

    /**
     * Obtiene el valor textual de una métrica, o {@code 0} si falta.
     *
     * @param metrics mapa de métricas
     * @param key     clave buscada
     * @return valor como texto
     */
    private String valueOf(Map<String, Integer> metrics, String key) {
        if (metrics == null || metrics.get(key) == null) {
            return "0";
        }
        return String.valueOf(metrics.get(key));
    }

    /**
     * Resume los detalles de un registro de auditoría para la tabla.
     *
     * @param log registro de auditoría
     * @return texto resumido
     */
    private String summarizeDetails(AuditLogExportItem log) {
        String details = safe(log.getDetails());
        String target = firstNonBlank(log.getTargetEmail(), log.getTargetUserId(), "");
        if (!target.isBlank() && !target.equals(safe(log.getAdminEmail()))) {
            return "Target: " + target + (details.equals("—") ? "" : " · " + truncate(details, 80));
        }
        return truncate(details, 100);
    }

    /**
     * Trunca un texto al máximo indicado y usa un guion si está vacío.
     *
     * @param value texto original
     * @param max   longitud máxima
     * @return texto recortado
     */
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

    /**
     * Normaliza un texto nulo o en blanco a un guion.
     *
     * @param value texto original
     * @return texto recortado o {@code —}
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    /**
     * Devuelve el primer valor no vacío, o un guion si todos están en blanco.
     *
     * @param values candidatos
     * @return primer texto útil
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "—";
    }
}
