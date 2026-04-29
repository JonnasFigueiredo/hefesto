package com.hefesto.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepository {

    private static final RowMapper<Conversation> ROW_MAPPER = (rs, n) -> new Conversation(
        rs.getString("id"),
        rs.getString("title"),
        rs.getString("adapter_id"),
        rs.getString("agent_id"),
        rs.getString("jira_issue_key"),
        rs.getLong("created_at"),
        rs.getLong("updated_at"),
        rs.getInt("archived") == 1
    );

    private final JdbcTemplate jdbc;

    public ConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Conversation save(Conversation c) {
        // INSERT OR REPLACE — simples e suficiente pro nosso domínio (id é UUID truncado).
        jdbc.update("""
            INSERT INTO conversations
                (id, title, adapter_id, agent_id, jira_issue_key, created_at, updated_at, archived)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                adapter_id = excluded.adapter_id,
                agent_id = excluded.agent_id,
                jira_issue_key = excluded.jira_issue_key,
                updated_at = excluded.updated_at,
                archived = excluded.archived
            """,
            c.id(),
            c.title(),
            c.adapterId(),
            c.agentId(),
            c.jiraIssueKey(),
            c.createdAt(),
            c.updatedAt(),
            c.archived() ? 1 : 0
        );
        return c;
    }

    public Optional<Conversation> findById(String id) {
        if (id == null) return Optional.empty();
        List<Conversation> result = jdbc.query(
            "SELECT * FROM conversations WHERE id = ?", ROW_MAPPER, id);
        return result.stream().findFirst();
    }

    /** Lista todas, mais recentes primeiro. Inclui arquivadas. */
    public List<Conversation> findAll() {
        return jdbc.query(
            "SELECT * FROM conversations ORDER BY updated_at DESC", ROW_MAPPER);
    }

    public List<Conversation> findActive() {
        return jdbc.query(
            "SELECT * FROM conversations WHERE archived = 0 ORDER BY updated_at DESC",
            ROW_MAPPER);
    }

    public void delete(String id) {
        // Cascade pela FK das tabelas messages e conversation_attachments.
        jdbc.update("DELETE FROM conversations WHERE id = ?", id);
    }

    public int count() {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM conversations WHERE archived = 0", Integer.class);
        return n == null ? 0 : n;
    }
}
