package com.hefesto.testcases;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class TestCaseRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;

    public TestCaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<TestCase> rowMapper = (rs, n) -> new TestCase(
        rs.getString("id"),
        rs.getString("conversation_id"),
        rs.getString("message_id"),
        rs.getString("code"),
        rs.getString("category"),
        rs.getString("title"),
        rs.getString("preconditions"),
        parseSteps(rs.getString("steps_json")),
        rs.getString("expected_result"),
        rs.getString("priority"),
        rs.getString("status"),
        rs.getString("notes"),
        rs.getInt("position"),
        rs.getLong("created_at"),
        rs.getLong("updated_at")
    );

    /** Insert. Gera id se vier null. Retorna o caso salvo. */
    public TestCase save(TestCase tc) {
        String id = tc.id() == null ? UUID.randomUUID().toString().substring(0, 12) : tc.id();
        long now = System.currentTimeMillis();
        long created = tc.createdAt() == 0 ? now : tc.createdAt();

        jdbc.update("""
            INSERT INTO test_cases
                (id, conversation_id, message_id, code, category, title, preconditions,
                 steps_json, expected_result, priority, status, notes, position,
                 created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                category = excluded.category,
                preconditions = excluded.preconditions,
                steps_json = excluded.steps_json,
                expected_result = excluded.expected_result,
                priority = excluded.priority,
                status = excluded.status,
                notes = excluded.notes,
                position = excluded.position,
                updated_at = excluded.updated_at
            """,
            id,
            tc.conversationId(),
            tc.messageId(),
            tc.code(),
            tc.category(),
            tc.title(),
            tc.preconditions(),
            stringifySteps(tc.steps()),
            tc.expectedResult(),
            tc.priority(),
            tc.status() == null ? TestCase.STATUS_PENDING : tc.status(),
            tc.notes(),
            tc.position(),
            created,
            now
        );

        return new TestCase(id, tc.conversationId(), tc.messageId(), tc.code(),
            tc.category(), tc.title(), tc.preconditions(), tc.steps(),
            tc.expectedResult(), tc.priority(),
            tc.status() == null ? TestCase.STATUS_PENDING : tc.status(),
            tc.notes(), tc.position(), created, now);
    }

    public Optional<TestCase> findById(String id) {
        if (id == null) return Optional.empty();
        return jdbc.query("SELECT * FROM test_cases WHERE id = ?", rowMapper, id)
            .stream().findFirst();
    }

    public List<TestCase> findByConversation(String conversationId) {
        return jdbc.query(
            "SELECT * FROM test_cases WHERE conversation_id = ? ORDER BY position ASC, created_at ASC",
            rowMapper, conversationId);
    }

    public List<TestCase> findByMessage(String messageId) {
        if (messageId == null) return List.of();
        return jdbc.query(
            "SELECT * FROM test_cases WHERE message_id = ? ORDER BY position ASC",
            rowMapper, messageId);
    }

    public void updateStatus(String id, String status, String notes) {
        jdbc.update("""
            UPDATE test_cases SET status = ?, notes = ?, updated_at = ?
            WHERE id = ?
            """,
            status, notes, System.currentTimeMillis(), id);
    }

    public void delete(String id) {
        jdbc.update("DELETE FROM test_cases WHERE id = ?", id);
    }

    private String stringifySteps(List<String> steps) {
        if (steps == null) steps = List.of();
        try {
            return MAPPER.writeValueAsString(steps);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseSteps(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
