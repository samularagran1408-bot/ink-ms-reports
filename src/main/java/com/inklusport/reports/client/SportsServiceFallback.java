package com.inklusport.reports.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SportsServiceFallback implements SportsServiceClient {

    @Override
    public int getActiveEventsCount() {
        log.warn("Sports MS no disponible. Retornando 0 como eventos activos.");
        return 0;
    }

    @Override
    public int getTotalSports() {
        log.warn("Sports MS no disponible. Retornando 0 como total de deportes.");
        return 0;
    }

    @Override
    public int getTotalEvents() {
        log.warn("Sports MS no disponible. Retornando 0 como total de eventos.");
        return 0;
    }
}