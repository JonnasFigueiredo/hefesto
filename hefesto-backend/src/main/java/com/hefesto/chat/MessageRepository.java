package com.hefesto.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.hefesto.llm.Message;

@Repository
public class MessageRepository {

    private static final RowMapper<StoredMessage> ROW_MAPPER = (rs, n) -> new StoredMessage(
        rs.getString("id"),
        rs.getString("conversation_id"),
        Message.Role.valueOf(rs.getString("role")),
        rs.getString("content"),
        rs.getLong("timestamp"),
        rs.getString("meta_json")
    );

    private final JdbcTemplate jdbc;

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StoredMessage append(String conversationId, Message message) {
        String id = UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
            INSERT INTO messages (id, conversation_id, role, content, timestamp, meta_json)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            id,
            conversationId,
            message.role().name(),
            message.content(),
            message.timestamp(),
            null
        );
        return new StoredMessage(id, conversationId, message.role(),
            message.content(), message.timestamp(), null);
    }

    public List<StoredMessage> findByConversation(String conversationId) {
        return jdbc.query(
            "SELECT * FROM messages WHERE conversation_id = ? ORDER BY timestamp ASC",
            ROW_MAPPER,
            conversationId);
    }

    public int countByConversation(String conversationId) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM messages WHERE conversation_id = ?",
            Integer.class,
            conversationId);
        return n == null ? 0 : n;
    }

    public void deleteByConversation(String conversationId) {
        jdbc.update("DELETE FROM messages WHERE conversation_id = ?", conversationId);
    }

    /**
     * Versão persistida de Message — adiciona id próprio (não confundir com
     * id da conversa) e referência à conversa.
     */
    public record StoredMessage(
        String id,
        String conversationId,
        Message.Role role,
        String content,
        long timestamp,
        String metaJson
    ) {
        public Message toMessage() {
            return new Message(role, content, timestamp);
        }
    }
}
