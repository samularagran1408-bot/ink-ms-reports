package com.inklusport.reports.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalyticsEventRequest {

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String eventType;

    private String userId;

    @NotBlank(message = "El módulo es obligatorio")
    private String module;

    private String metadata;
}