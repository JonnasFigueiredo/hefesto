package com.hefesto.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.hefesto.chat.MessageRepository.StoredMessage;
import com.hefesto.llm.Message;

/**
 * Fachada sobre {@link ConversationRepository} e {@link MessageRepository}.
 * Mantém a API histórica (getOrCreate, get, list, delete) usada pelo
 * ChatService/ChatController/WebSocketHandler. Toda escrita persiste no
 * SQLite imediatamente.
 */
@Component
public class ConversationStore {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConversationAttachmentRepository attachmentLinks;

    public ConversationStore(
        ConversationRepository conversations,
        MessageRepository messages,
        ConversationAttachmentRepository attachmentLinks
    ) {
        this.conversations = conversations;
        this.messages = messages;
        this.attachmentLinks = attachmentLinks;
    }

    public Conversation create(String adapterId) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Conversation c = new Conversation(id, adapterId);
        conversations.save(c);
        return c;
    }

    public Conversation getOrCreate(String id, String adapterId) {
        if (id == null || id.isBlank()) {
            return create(adapterId);
        }
        return conversations.findById(id).orElseGet(() -> {
            Conversation c = new Conversation(id, adapterId);
            conversations.save(c);
            return c;
        });
    }

    public Conversation get(String id) {
        return conversations.findById(id).orElse(null);
    }

    /** Lista conversas ativas (não arquivadas), mais recentes primeiro. */
    public List<Conversation> list() {
        return conversations.findActive();
    }

    public void delete(String id) {
        // FKs com ON DELETE CASCADE cuidam de messages e conversation_attachments.
        conversations.delete(id);
    }

    public int size() {
        return conversations.count();
    }

    /**
     * Anexa uma mensagem à conversa e persiste. Atualiza updated_at e
     * auto-titula se for a primeira mensagem do usuário.
     */
    public StoredMessage appendMessage(String conversationId, Message message) {
        Conversation c = get(conversationId);
        if (c == null) {
            throw new IllegalArgumentException("Conversa não encontrada: " + conversationId);
        }
        StoredMessage stored = messages.append(conversationId, message);
        if (message.role() == Message.Role.USER) {
            c.autoTitleFrom(message.content());
        }
        c.touch();
        conversations.save(c);
        return stored;
    }

    public List<Message> messagesOf(String conversationId) {
        return messages.findByConversation(conversationId).stream()
            .map(StoredMessage::toMessage)
            .toList();
    }

    public List<StoredMessage> storedMessagesOf(String conversationId) {
        return messages.findByConversation(conversationId);
    }

    /** Persiste alterações de metadados (title, agent, jira). */
    public void save(Conversation c) {
        c.touch();
        conversations.save(c);
    }

    /** Vincula anexos a uma conversa (idempotente). */
    public void linkAttachments(String conversationId, List<String> attachmentIds) {
        attachmentLinks.linkAll(conversationId, attachmentIds);
    }

    /** IDs dos anexos vinculados à conversa. */
    public List<String> attachmentsOf(String conversationId) {
        return attachmentLinks.findAttachmentIdsByConversation(conversationId);
    }
}
