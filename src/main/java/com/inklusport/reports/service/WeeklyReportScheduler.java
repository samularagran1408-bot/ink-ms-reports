package com.inklusport.reports.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Dispara el envío semanal de reportes por correo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "reports.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class WeeklyReportScheduler {

    private final WeeklyReportScheduleService weeklyReportScheduleService;

    /**
     * Lunes 08:00 — envía el PDF del dashboard a quienes tienen la programación activa.
     */
    @Scheduled(cron = "${reports.weekly-email.cron:0 0 8 * * MON}")
    public void sendWeeklyReports() {
        log.info("Iniciando envío semanal de reportes por correo");
        weeklyReportScheduleService.processDueWeeklyReports();
    }
}
