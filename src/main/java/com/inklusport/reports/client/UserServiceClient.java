package com.inklusport.reports.client;

import com.inklusport.reports.dto.PagedUsersResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "users-ms",
             url = "${users.service.url:http://localhost:3002}",
             fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/admin/users/count")
    int getTotalUsers();

    @GetMapping("/api/admin/users/active/count")
    int getActiveUsers();

    @GetMapping("/api/admin/users")
    List<Map<String, Object>> getAllUsers();

    @GetMapping("/api/admin/users/page")
    PagedUsersResponse getUsersPage(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "disability", required = false) String disability);

    @GetMapping("/api/admin/users/active")
    List<Map<String, Object>> getActiveUsersList();

    @GetMapping("/api/admin/users/inactive")
    List<Map<String, Object>> getInactiveUsersList();

    @GetMapping("/api/admin/users/roles")
    List<Map<String, Object>> getRoles();

    @GetMapping("/api/admin/users/audit")
    List<Map<String, Object>> getAuditLogs();

    @GetMapping("/api/users/verify/quiz/prep/{role}/{userId}")
    Map<String, Object> getQuizPrep(
            @PathVariable("role") String role,
            @PathVariable("userId") String userId);
}
