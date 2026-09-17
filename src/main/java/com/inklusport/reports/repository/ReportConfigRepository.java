package com.inklusport.reports.repository;

import com.inklusport.reports.entity.ReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ReportConfigRepository extends JpaRepository<ReportConfig, String> {

    List<ReportConfig> findByOwnerId(String ownerId);

    List<ReportConfig> findByReportNameContainingIgnoreCase(String reportName);

    Optional<ReportConfig> findFirstByOwnerIdAndScheduleFrequency(String ownerId, String scheduleFrequency);

    List<ReportConfig> findByScheduleEnabledTrueAndScheduleFrequency(String scheduleFrequency);

    @Modifying
    @Transactional
    @Query("UPDATE ReportConfig r SET r.lastRun = CURRENT_TIMESTAMP WHERE r.id = :id")
    void updateLastRun(@Param("id") String id);
}