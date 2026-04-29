package com.hefesto.telemetry;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UsageEventRepository {

    private static final RowMapper<UsageEvent> ROW_MAPPER = (rs, n) -> {
        long durationRaw = rs.getLong("duration_ms");
        Long duration = rs.wasNull() ? null : durationRaw;
        return new UsageEvent(
            rs.getLong("id"),
            rs.getLong("timestamp"),
            rs.getString("event_type"),
            rs.getString("conversation_id"),
            rs.getString("payload_json"),
            duration
        );
    };

    private final JdbcTemplate jdbc;

    public UsageEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(UsageEvent event) {
        jdbc.update("""
            INSERT INTO usage_events
                (timestamp, event_type, conversation_id, payload_json, duration_ms)
            VALUES (?, ?, ?, ?, ?)
            """,
            event.timestamp(),
            event.eventType(),
            event.conversationId(),
            event.payloadJson(),
            event.durationMs()
        );
    }

    /** Últimos N eventos, mais recentes primeiro. */
    public List<UsageEvent> findRecent(int limit) {
        return jdbc.query(
            "SELECT * FROM usage_events ORDER BY timestamp DESC LIMIT ?",
            ROW_MAPPER, limit);
    }

    public List<UsageEvent> findByType(String eventType, int limit) {
        return jdbc.query(
            "SELECT * FROM usage_events WHERE event_type = ? ORDER BY timestamp DESC LIMIT ?",
            ROW_MAPPER, eventType, limit);
    }

    /** Contagem por tipo de evento. */
    public Map<String, Long> countByType() {
        return jdbc.query(
            "SELECT event_type, COUNT(*) AS n FROM usage_events GROUP BY event_type ORDER BY n DESC",
            (rs, n) -> Map.entry(rs.getString("event_type"), rs.getLong("n"))
        ).stream().collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey, Map.Entry::getValue,
            (a, b) -> a, java.util.LinkedHashMap::new));
    }

    /** Latência média (ms) por tipo, ignorando nulos. */
    public Map<String, Double> avgDurationByType() {
        return jdbc.query("""
            SELECT event_type, AVG(duration_ms) AS avg_ms
            FROM usage_events
            WHERE duration_ms IS NOT NULL
            GROUP BY event_type
            """,
            (rs, n) -> Map.entry(rs.getString("event_type"), rs.getDouble("avg_ms"))
        ).stream().collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey, Map.Entry::getValue,
            (a, b) -> a, java.util.LinkedHashMap::new));
    }

    public long totalEvents() {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM usage_events", Long.class);
        return n == null ? 0L : n;
    }

    /**
     * Série temporal: contagem de eventos por dia (UTC), últimos N dias.
     * Útil pra dashboard de Analytics.
     */
    public List<DailyCount> countByDay(int lastDays) {
        long since = System.currentTimeMillis() - (long) lastDays * 24L * 60L * 60L * 1000L;
        return jdbc.query("""
            SELECT
                strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') AS day,
                event_type,
                COUNT(*) AS n
            FROM usage_events
            WHERE timestamp >= ?
            GROUP BY day, event_type
            ORDER BY day ASC
            """,
            (rs, n) -> new DailyCount(
                rs.getString("day"),
                rs.getString("event_type"),
                rs.getLong("n")
            ),
            since
        );
    }

    public record DailyCount(String day, String eventType, long count) {}
}
