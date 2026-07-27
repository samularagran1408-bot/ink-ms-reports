package com.inklusport.reports.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DashboardFilters {
    
    private LocalDate startDate;

    private LocalDate endDate;
    
    private String module;
}
