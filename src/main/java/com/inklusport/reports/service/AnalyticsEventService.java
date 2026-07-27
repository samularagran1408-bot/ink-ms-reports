package com.inklusport.reports.service;

import com.inklusport.reports.dto.AnalyticsEventRequest;
import com.inklusport.reports.dto.AnalyticsEventResponse;
import com.inklusport.reports.entity.AnalyticsEvent;
import com.inklusport.reports.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventService {

    /**
     * Inyección de Repositorios
     */
    private final AnalyticsEventRepository analyticsEventRepository;

    /**
     * Registro de eventos para auditoría y KPIs
     * @param request
     * @param userId
     * @return
     */
    @Transactional
    public AnalyticsEventResponse registerEvent(AnalyticsEventRequest request, String userId) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(request.getEventType())
                .userId(userId != null ? userId : request.getUserId())
                .module(request.getModule())
                .metadata(request.getMetadata())
                .createdAt(LocalDateTime.now())
                .build();

        AnalyticsEvent saved = analyticsEventRepository.save(event);    
        log.info("Evento registrado: {} - {}", saved.getEventType(), saved.getModule());

        return convertToResponse(saved);
    }

    /**
     * Obtener los eventos por usuario
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<AnalyticsEventResponse> getEventsByUser(String userId) {
        return analyticsEventRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener los eventos por tipo
     * @param eventType
     * @return
     */
    @Transactional(readOnly = true)
    public List<AnalyticsEventResponse> getEventsByType(String eventType) {
        return analyticsEventRepository.findByEventType(eventType).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsEventResponse> getEventsByModule(String module) {
        return analyticsEventRepository.findByModule(module).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertir a response los atributos
     * @param event
     * @return
     */
    private AnalyticsEventResponse convertToResponse(AnalyticsEvent event) {
        return AnalyticsEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .module(event.getModule())
                .metadata(event.getMetadata())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
