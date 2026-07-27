package com.inklusport.reports.repository;

import com.inklusport.reports.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, String> {

    List<AnalyticsEvent> findByEventType(String eventType);

    List<AnalyticsEvent> findByUserId(String userId);

    List<AnalyticsEvent> findByModule(String module);

    @Query("SELECT COUNT(a) FROM AnalyticsEvent a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a.eventType, COUNT(a) FROM AnalyticsEvent a WHERE a.createdAt BETWEEN :startDate AND :endDate GROUP BY a.eventType")
    List<Object[]> countByEventTypeAndDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM AnalyticsEvent a WHERE a.createdAt < :cutoff")
    long deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}