package com.inklusport.reports.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_metrics_summary", indexes = {
        @Index(name = "idx_metrics_key_date", columnList = "metric_key, summary_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMetricsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "metric_key", nullable = false, length = 50)
    private String metricKey;

    @Column(name = "metric_value", nullable = false)
    private Integer metricValue;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}