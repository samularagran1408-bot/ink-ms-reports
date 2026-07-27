package com.inklusport.reports.controller;

import com.inklusport.reports.dto.DashboardFilters;
import com.inklusport.reports.dto.DashboardResponse;
import com.inklusport.reports.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    /**
     * Inyección de Servicio
     */
    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
    public ResponseEntity<DashboardResponse> getDashboard(@ModelAttribute DashboardFilters filters) {
        return ResponseEntity.ok(dashboardService.getDashboard(filters));
    }
    
}
