package com.hefesto.chat;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositório do vínculo N:N entre conversas e anexos. Quando usuário
 * envia uma mensagem com attachmentIds, persistimos a associação aqui
 * pra ter histórico real de "quais anexos esta conversa usou".
 */
@Repository
public class ConversationAttachmentRepository {

    private final JdbcTemplate jdbc;

    public ConversationAttachmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Vincula um anexo a uma conversa. Idempotente — ignora duplicatas. */
    public void link(String conversationId, String attachmentId) {
        jdbc.update("""
            INSERT INTO conversation_attachments (conversation_id, attachment_id)
            VALUES (?, ?)
            ON CONFLICT(conversation_id, attachment_id) DO NOTHING
            """,
            conversationId, attachmentId);
    }

    /** Vincula múltiplos anexos de uma só vez. */
    public void linkAll(String conversationId, List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return;
        for (String aid : attachmentIds) {
            link(conversationId, aid);
        }
    }

    /** Anexos vinculados a uma conversa. */
    public List<String> findAttachmentIdsByConversation(String conversationId) {
        return jdbc.queryForList(
            "SELECT attachment_id FROM conversation_attachments WHERE conversation_id = ? ORDER BY attachment_id",
            String.class,
            conversationId);
    }

    public void unlink(String conversationId, String attachmentId) {
        jdbc.update(
            "DELETE FROM conversation_attachments WHERE conversation_id = ? AND attachment_id = ?",
            conversationId, attachmentId);
    }
}
