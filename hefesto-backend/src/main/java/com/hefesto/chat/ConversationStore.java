package com.hefesto.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Armazenamento in-memory de conversas. Sem persistência — limpo a cada
 * restart do backend (decisão consciente do MVP).
 */
@Component
public class ConversationStore {

    private final Map<String, Conversation> byId = new ConcurrentHashMap<>();

    public Conversation create(String adapterId) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Conversation c = new Conversation(id, adapterId);
        byId.put(id, c);
        return c;
    }

    public Conversation getOrCreate(String id, String adapterId) {
        if (id == null || id.isBlank()) {
            return create(adapterId);
        }
        return byId.computeIfAbsent(id, k -> new Conversation(k, adapterId));
    }

    public Conversation get(String id) {
        return byId.get(id);
    }

    /**
     * Lista todas as conversas, mais recentemente atualizadas primeiro.
     */
    public List<Conversation> list() {
        return byId.values().stream()
            .sorted(Comparator.comparingLong(Conversation::updatedAt).reversed())
            .toList();
    }

    public void delete(String id) {
        byId.remove(id);
    }

    public int size() {
        return byId.size();
    }
}
