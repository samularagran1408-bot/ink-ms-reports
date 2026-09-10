package com.inklusport.reports.service;

import com.inklusport.reports.dto.AnalyticsEventRequest;
import com.inklusport.reports.dto.AnalyticsEventResponse;
import com.inklusport.reports.entity.AnalyticsEvent;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventServiceTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @InjectMocks
    private AnalyticsEventService analyticsEventService;

    @Test
    void registerEvent_usaUserIdDelContexto() {
        AnalyticsEventRequest request = new AnalyticsEventRequest();
        request.setEventType("LOGIN");
        request.setModule("auth");
        request.setUserId("otro");
        request.setMetadata("{}");

        when(analyticsEventRepository.save(any(AnalyticsEvent.class))).thenAnswer(invocation -> {
            AnalyticsEvent event = invocation.getArgument(0);
            event.setId("evt-1");
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        AnalyticsEventResponse response = analyticsEventService.registerEvent(request, "user-jwt");

        assertEquals("evt-1", response.getId());
        assertEquals("LOGIN", response.getEventType());
        assertEquals("user-jwt", response.getUserId());
        assertEquals("auth", response.getModule());
    }

    @Test
    void getEventsByUser_mapeaResultados() {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .id("evt-2")
                .eventType("VIEW")
                .userId("user-1")
                .module("sports")
                .createdAt(LocalDateTime.now())
                .build();
        when(analyticsEventRepository.findByUserId("user-1")).thenReturn(List.of(event));

        List<AnalyticsEventResponse> events = analyticsEventService.getEventsByUser("user-1");

        assertEquals(1, events.size());
        assertEquals("VIEW", events.get(0).getEventType());
    }
}
