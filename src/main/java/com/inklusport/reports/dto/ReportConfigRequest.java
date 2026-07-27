package com.inklusport.reports.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportConfigRequest {

    @NotBlank(message = "El nombre del reporte es obligatorio")
    private String reportName;

    @NotBlank(message = "Los filtros son obligatorios")
    private String filters;
}