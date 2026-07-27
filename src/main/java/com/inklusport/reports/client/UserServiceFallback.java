package com.inklusport.reports.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {

    @Override
    public int getTotalUsers() {
        log.warn(" Users MS no disponible. Retornando 0 como total de usuarios.");
        return 0;
    }

    @Override
    public int getActiveUsers() {
        log.warn(" Users MS no disponible. Retornando 0 como usuarios activos.");
        return 0;
    }
}