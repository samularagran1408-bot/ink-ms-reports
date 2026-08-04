package com.inklusport.reports.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AuditExportRequest {
    private List<AuditLogExportItem> logs = new ArrayList<>();
}
