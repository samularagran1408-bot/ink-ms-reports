package com.inklusport.reports.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "report_name", nullable = false, length = 100)
    private String reportName;

    @Column(name = "filters", nullable = false, columnDefinition = "JSON")
    private String filters;

    @Column(name = "owner_id", columnDefinition = "CHAR(36)", nullable = false)
    private String ownerId;

    @Column(name = "last_run")
    private LocalDateTime lastRun;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}