package com.inklusport.reports.controller;

import com.inklusport.reports.dto.ReportConfigRequest;
import com.inklusport.reports.dto.ReportConfigResponse;
import com.inklusport.reports.dto.ReportRunResponse;
import com.inklusport.reports.dto.WeeklyScheduleResponse;
import com.inklusport.reports.service.ReportService;
import com.inklusport.reports.service.WeeklyReportScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final WeeklyReportScheduleService weeklyReportScheduleService;

    @PostMapping("/configs")
    public ResponseEntity<ReportConfigResponse> createReportConfig(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReportConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReportConfig(userId, request));
    }

    @GetMapping("/configs")
    public ResponseEntity<List<ReportConfigResponse>> getMyReportConfigs(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(reportService.getMyReportConfigs(userId));
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<ReportConfigResponse> updateReportConfig(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @Valid @RequestBody ReportConfigRequest request) {
        return ResponseEntity.ok(reportService.updateReportConfig(id, userId, request));
    }

    @PostMapping("/configs/{id}/run")
    public ResponseEntity<ReportRunResponse> runReport(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return ResponseEntity.ok(reportService.runReport(id, userId));
    }

    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Void> deleteReportConfig(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        reportService.deleteReportConfig(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** CP7-HU35: programa el envío semanal del reporte por correo. */
    @PostMapping("/schedule/weekly")
    public ResponseEntity<WeeklyScheduleResponse> scheduleWeekly(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(weeklyReportScheduleService.scheduleWeekly(userId));
    }

    /** Estado de la programación semanal del usuario autenticado. */
    @GetMapping("/schedule/weekly")
    public ResponseEntity<WeeklyScheduleResponse> getWeeklySchedule(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(weeklyReportScheduleService.getWeekly(userId));
    }

    /** CP8-HU35: cancela la programación semanal activa. */
    @DeleteMapping("/schedule/weekly")
    public ResponseEntity<WeeklyScheduleResponse> cancelWeekly(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(weeklyReportScheduleService.cancelWeekly(userId));
    }
}
