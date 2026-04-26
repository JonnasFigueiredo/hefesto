package com.hefesto.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hefesto.llm.Message;

/**
 * Representação em memória de uma conversa. Não é thread-safe — toda
 * mutação deve passar pelo {@link ConversationStore}, que sincroniza.
 */
public class Conversation {

    private final String id;
    private final long createdAt;
    private long updatedAt;
    private String title;
    private String adapterId;
    private final List<Message> messages;

    public Conversation(String id, String adapterId) {
        this.id = id;
        this.adapterId = adapterId;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.messages = new ArrayList<>();
        this.title = null;
    }

    public String id() { return id; }
    public long createdAt() { return createdAt; }
    public long updatedAt() { return updatedAt; }
    public String title() { return title; }
    public String adapterId() { return adapterId; }
    public List<Message> messages() { return Collections.unmodifiableList(messages); }

    public void addMessage(Message m) {
        messages.add(m);
        updatedAt = Instant.now().toEpochMilli();
        // Auto-titula com o início da primeira mensagem do usuário.
        if (title == null && m.role() == Message.Role.USER) {
            String content = m.content().strip();
            title = content.length() > 60 ? content.substring(0, 60) + "..." : content;
        }
    }

    public void setAdapterId(String adapterId) {
        this.adapterId = adapterId;
        this.updatedAt = Instant.now().toEpochMilli();
    }
}
