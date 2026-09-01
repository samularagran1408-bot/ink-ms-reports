package com.inklusport.reports.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/api/events")
    List<Map<String, Object>> getEvents();

    @GetMapping("/api/events/available")
    List<Map<String, Object>> getAvailableEvents();

    @GetMapping("/api/sports")
    List<Map<String, Object>> getSports();

    @GetMapping("/api/disabilities")
    List<Map<String, Object>> getDisabilities();

    @GetMapping("/api/disabilities/active")
    List<Map<String, Object>> getActiveDisabilities();

    @GetMapping("/api/sports/active")
    List<Map<String, Object>> getActiveSports();

    @GetMapping("/api/sport-disabilities")
    List<Map<String, Object>> getAssociations();

    @GetMapping("/api/registrations/{eventId}/waitlist")
    List<Map<String, Object>> getEventWaitlist(@PathVariable("eventId") String eventId);

    @GetMapping("/api/registrations/user/{userId}")
    List<Map<String, Object>> getRegistrationsByUser(@PathVariable("userId") String userId);

    @GetMapping("/api/routines")
    List<Map<String, Object>> getRoutines();

    @GetMapping("/api/routines/trainer/{trainerId}")
    List<Map<String, Object>> getRoutinesByTrainer(@PathVariable("trainerId") String trainerId);

    @GetMapping("/api/routines/{id}/registrations")
    List<Map<String, Object>> getRoutineRegistrations(@PathVariable("id") String id);

    @GetMapping("/api/routine-registrations/user/{userId}")
    List<Map<String, Object>> getRoutineRegistrationsByUser(@PathVariable("userId") String userId);

    @GetMapping("/api/attendance/report")
    Map<String, Object> getAttendanceReport(@RequestParam("eventId") String eventId);
}
