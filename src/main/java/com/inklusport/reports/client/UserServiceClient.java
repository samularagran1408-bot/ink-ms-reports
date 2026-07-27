package com.inklusport.reports.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "users-ms", 
             url = "${users.service.url:http://localhost:3002}",
             fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/admin/users/count")
    int getTotalUsers();
    
    @GetMapping("/api/admin/users/active/count")
    int getActiveUsers();
}