package com.hefesto.chat;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hefesto.chat.dto.AdapterDto;
import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.chat.dto.ChatResponseDto;
import com.hefesto.chat.dto.ConversationDto;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.LlmAdapterRegistry;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final LlmAdapterRegistry registry;
    private final ConversationStore store;

    public ChatController(
        ChatService chatService,
        LlmAdapterRegistry registry,
        ConversationStore store
    ) {
        this.chatService = chatService;
        this.registry = registry;
        this.store = store;
    }

    @GetMapping("/llm/adapters")
    public List<AdapterDto> adapters() {
        return registry.list().stream()
            .map(a -> new AdapterDto(a.id(), a.displayName(), a.isAvailable()))
            .toList();
    }

    @PostMapping("/chat")
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        log.debug("POST /api/chat adapter={} conv={}", request.adapterId(), request.conversationId());
        return chatService.sendMessage(request);
    }

    @GetMapping("/conversations")
    public List<ConversationDto> conversations() {
        return store.list().stream().map(this::toDto).toList();
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDto> conversation(@PathVariable String id) {
        Conversation c = store.get(id);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDto(c));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String id) {
        store.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ConversationDto toDto(Conversation c) {
        return new ConversationDto(
            c.id(),
            c.title(),
            c.adapterId(),
            c.agentId(),
            c.jiraIssueKey(),
            store.attachmentsOf(c.id()),
            c.createdAt(),
            c.updatedAt(),
            c.archived(),
            store.storedMessagesOf(c.id()).stream()
                .map(m -> new ConversationDto.MessageDto(
                    m.id(),
                    m.role().name().toLowerCase(),
                    m.content(),
                    m.timestamp()))
                .toList()
        );
    }

    @ExceptionHandler(LlmAdapterException.class)
    public ResponseEntity<Map<String, Object>> handleLlmError(LlmAdapterException e) {
        log.warn("LLM adapter error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of(
                "error", "llm_adapter_error",
                "message", e.getMessage()
            ));
    }
}
