package com.inklusport.reports.dto;

import lombok.Data;

@Data
public class AuditLogExportItem {
    private String id;
    private String adminEmail;
    private String action;
    private String targetEmail;
    private String targetUserId;
    private String details;
    private String ipAddress;
    private String createdAt;
}
