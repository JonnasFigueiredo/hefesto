package com.hefesto.telemetry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints somente leitura pra inspecionar a telemetria. Base pra
 * Etapa 10 (dashboard).
 */
@RestController
@RequestMapping("/api/usage")
public class UsageEventController {

    private final UsageEventRepository repo;

    public UsageEventController(UsageEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/events")
    public List<UsageEvent> events(
        @RequestParam(value = "limit", defaultValue = "100") int limit,
        @RequestParam(value = "type", required = false) String type
    ) {
        int safeLimit = Math.min(Math.max(1, limit), 1000);
        if (type != null && !type.isBlank()) {
            return repo.findByType(type, safeLimit);
        }
        return repo.findRecent(safeLimit);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalEvents", repo.totalEvents());
        body.put("countByType", repo.countByType());
        body.put("avgDurationMsByType", repo.avgDurationByType());
        return body;
    }

    @GetMapping("/timeseries")
    public List<UsageEventRepository.DailyCount> timeseries(
        @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        int safe = Math.min(Math.max(1, days), 365);
        return repo.countByDay(safe);
    }
}
