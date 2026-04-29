package com.hefesto.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wrapper conveniente sobre {@link UsageEventRepository} pra uso em
 * services. Garante que falhas em telemetria nunca quebrem o fluxo
 * principal — só logam.
 */
@Service
public class UsageEventService {

    private static final Logger log = LoggerFactory.getLogger(UsageEventService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UsageEventRepository repo;

    public UsageEventService(UsageEventRepository repo) {
        this.repo = repo;
    }

    /**
     * Registra um evento de telemetria. Falhas são engolidas e logadas
     * — não devem propagar pro chamador.
     */
    public void record(String eventType, String conversationId, Object payload, Long durationMs) {
        try {
            String json = payload == null ? null : MAPPER.writeValueAsString(payload);
            repo.save(UsageEvent.now(eventType, conversationId, json, durationMs));
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar payload de telemetria para {}: {}", eventType, e.getMessage());
        } catch (Exception e) {
            log.warn("Falha ao gravar evento de telemetria {}: {}", eventType, e.getMessage());
        }
    }

    /** Atalho pra eventos sem payload e sem duração. */
    public void record(String eventType, String conversationId) {
        record(eventType, conversationId, null, null);
    }
}
