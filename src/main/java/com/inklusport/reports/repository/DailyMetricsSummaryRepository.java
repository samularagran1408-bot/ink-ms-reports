package com.inklusport.reports.repository;

import com.inklusport.reports.entity.DailyMetricsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMetricsSummaryRepository extends JpaRepository<DailyMetricsSummary, String> {

    Optional<DailyMetricsSummary> findBySummaryDateAndMetricKey(LocalDate date, String metricKey);

    List<DailyMetricsSummary> findBySummaryDate(LocalDate date);

    List<DailyMetricsSummary> findBySummaryDateBetween(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Transactional
    @Query("UPDATE DailyMetricsSummary d SET d.metricValue = :value WHERE d.summaryDate = :date AND d.metricKey = :key")
    void updateMetricValue(@Param("date") LocalDate date, @Param("key") String metricKey, @Param("value") Integer value);
}