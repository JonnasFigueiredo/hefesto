package com.hefesto.chat;

import java.time.Instant;

/**
 * Metadados de uma conversa. Mensagens não ficam mais embutidas aqui;
 * vivem no {@link com.hefesto.chat.MessageRepository} e são consultadas
 * sob demanda. Isso evita carregar todo o histórico de N mensagens só
 * pra atualizar título ou status.
 *
 * <p>POJO mutável — pensado pra ser usado pelo {@link ConversationStore}
 * que controla o ciclo de persistência.</p>
 */
public class Conversation {

    private final String id;
    private final long createdAt;
    private long updatedAt;
    private String title;
    private String adapterId;
    private String agentId;
    private String jiraIssueKey;
    private boolean archived;

    public Conversation(String id, String adapterId) {
        this.id = id;
        this.adapterId = adapterId;
        this.agentId = "default";
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.archived = false;
    }

    /** Construtor de hidratação (vindo do banco). */
    public Conversation(
        String id,
        String title,
        String adapterId,
        String agentId,
        String jiraIssueKey,
        long createdAt,
        long updatedAt,
        boolean archived
    ) {
        this.id = id;
        this.title = title;
        this.adapterId = adapterId;
        this.agentId = agentId == null ? "default" : agentId;
        this.jiraIssueKey = jiraIssueKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archived = archived;
    }

    public String id() { return id; }
    public long createdAt() { return createdAt; }
    public long updatedAt() { return updatedAt; }
    public String title() { return title; }
    public String adapterId() { return adapterId; }
    public String agentId() { return agentId; }
    public String jiraIssueKey() { return jiraIssueKey; }
    public boolean archived() { return archived; }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public void setAdapterId(String adapterId) {
        this.adapterId = adapterId;
        touch();
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
        touch();
    }

    public void setJiraIssueKey(String jiraIssueKey) {
        this.jiraIssueKey = jiraIssueKey;
        touch();
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
        touch();
    }

    public void touch() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * Auto-titula a partir do conteúdo da primeira mensagem do usuário.
     * Idempotente — só seta se ainda não tem título.
     */
    public void autoTitleFrom(String firstUserMessage) {
        if (title != null) return;
        if (firstUserMessage == null) return;
        String content = firstUserMessage.strip();
        if (content.isEmpty()) return;
        this.title = content.length() > 60 ? content.substring(0, 60) + "..." : content;
        touch();
    }
}
