package com.inklusport.reports.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "sports-ms", 
             url = "${sports.service.url:http://localhost:3003}",
             fallback = SportsServiceFallback.class)
public interface SportsServiceClient {

    @GetMapping("/api/events/active/count")
    int getActiveEventsCount();
    
    @GetMapping("/api/sports/count")
    int getTotalSports();
    
    @GetMapping("/api/events/count")
    int getTotalEvents();
}