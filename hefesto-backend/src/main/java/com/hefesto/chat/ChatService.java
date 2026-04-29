package com.hefesto.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hefesto.agents.Agent;
import com.hefesto.agents.AgentRegistry;
import com.hefesto.attachments.Attachment;
import com.hefesto.attachments.AttachmentStore;
import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.chat.dto.ChatResponseDto;
import com.hefesto.jira.JiraService;
import com.hefesto.jira.dto.IssueDto;
import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.LlmAdapterRegistry;
import com.hefesto.llm.Message;
import com.hefesto.telemetry.UsageEvent;
import com.hefesto.telemetry.UsageEventService;
import com.hefesto.testcases.TestCaseService;
import com.hefesto.chat.MessageRepository.StoredMessage;

/**
 * Orquestra o envio de uma mensagem: resolve o adapter, agente, anexos e
 * issue Jira; constrói o prompt em camadas via {@link PromptBuilder};
 * dispara o chat e retorna a resposta DTO.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LlmAdapterRegistry registry;
    private final ConversationStore store;
    private final AgentRegistry agents;
    private final AttachmentStore attachments;
    private final JiraService jiraService;
    private final PromptBuilder promptBuilder;
    private final UsageEventService telemetry;
    private final TestCaseService testCases;

    public ChatService(
        LlmAdapterRegistry registry,
        ConversationStore store,
        AgentRegistry agents,
        AttachmentStore attachments,
        JiraService jiraService,
        PromptBuilder promptBuilder,
        UsageEventService telemetry,
        TestCaseService testCases
    ) {
        this.registry = registry;
        this.store = store;
        this.agents = agents;
        this.attachments = attachments;
        this.jiraService = jiraService;
        this.promptBuilder = promptBuilder;
        this.telemetry = telemetry;
        this.testCases = testCases;
    }

    public ChatResponseDto sendMessage(ChatRequestDto req) {
        validate(req);
        LlmAdapter adapter = resolveAdapter(req.adapterId());

        Conversation conv = store.getOrCreate(req.conversationId(), req.adapterId());
        conv.setAdapterId(req.adapterId());
        if (req.agentId() != null && !req.agentId().isBlank()) conv.setAgentId(req.agentId());
        if (req.jiraIssueKey() != null) conv.setJiraIssueKey(req.jiraIssueKey());
        store.save(conv);

        if (req.attachmentIds() != null && !req.attachmentIds().isEmpty()) {
            store.linkAttachments(conv.id(), req.attachmentIds());
        }

        store.appendMessage(conv.id(), Message.user(req.message()));

        String layeredPrompt = buildLayeredPrompt(req);
        int attachmentCount = req.attachmentIds() == null ? 0 : req.attachmentIds().size();
        log.debug("Layered prompt length: {} chars (agent={}, attachments={}, jira={})",
            layeredPrompt.length(),
            req.agentId(),
            attachmentCount,
            req.jiraIssueKey());

        telemetry.record(UsageEvent.Type.CHAT_START, conv.id(), java.util.Map.of(
            "adapterId", req.adapterId(),
            "agentId", req.agentId() == null ? "default" : req.agentId(),
            "attachmentCount", attachmentCount,
            "hasJira", req.jiraIssueKey() != null,
            "messageChars", req.message().length(),
            "promptChars", layeredPrompt.length()
        ), null);

        ChatRequest llmRequest = new ChatRequest(
            conv.id(),
            Collections.emptyList(), // sem history; PromptBuilder controla camadas
            layeredPrompt,
            null
        );

        long start = System.currentTimeMillis();
        try {
            ChatResponse response = adapter.chat(llmRequest);
            StoredMessage assistantStored = store.appendMessage(
                conv.id(), Message.assistant(response.content()));

            // Auto-extração de test cases se o agente for QA Specialist.
            int extractedCount = testCases.extractAndSave(
                req.agentId(),
                conv.id(),
                assistantStored.id(),
                response.content()
            ).size();

            telemetry.record(UsageEvent.Type.CHAT_COMPLETE, conv.id(), java.util.Map.of(
                "adapterId", response.adapterId(),
                "model", response.model() == null ? "" : response.model(),
                "responseChars", response.content().length(),
                "testCasesExtracted", extractedCount
            ), response.latencyMs());

            return new ChatResponseDto(
                conv.id(),
                response.content(),
                response.adapterId(),
                response.latencyMs(),
                response.model()
            );
        } catch (RuntimeException e) {
            telemetry.record(UsageEvent.Type.CHAT_ERROR, conv.id(), java.util.Map.of(
                "adapterId", req.adapterId(),
                "type", e.getClass().getSimpleName(),
                "message", e.getMessage() == null ? "" : e.getMessage()
            ), System.currentTimeMillis() - start);
            throw e;
        }
    }

    /**
     * Constrói o prompt em camadas resolvendo agente, anexos e Jira a partir
     * dos ids do request. Usado também pelo handler de WebSocket.
     */
    public String buildLayeredPrompt(ChatRequestDto req) {
        Agent agent = agents.getOrDefault(req.agentId());
        List<Attachment> files = resolveAttachments(req.attachmentIds());
        IssueDto issue = resolveJiraIssue(req.jiraIssueKey());
        return promptBuilder.build(agent, files, issue, req.message());
    }

    public LlmAdapter resolveAdapter(String adapterId) {
        if (adapterId == null || adapterId.isBlank()) {
            throw new LlmAdapterException("adapterId é obrigatório");
        }
        LlmAdapter adapter = registry.get(adapterId)
            .orElseThrow(() -> new LlmAdapterException(
                "Adapter desconhecido: " + adapterId));
        if (!adapter.isAvailable()) {
            throw new LlmAdapterException(
                "Adapter '" + adapter.displayName() + "' indisponível no momento");
        }
        return adapter;
    }

    private void validate(ChatRequestDto req) {
        if (req.adapterId() == null || req.adapterId().isBlank()) {
            throw new LlmAdapterException("adapterId é obrigatório");
        }
        if (req.message() == null || req.message().isBlank()) {
            throw new LlmAdapterException("message não pode ser vazio");
        }
    }

    private List<Attachment> resolveAttachments(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Attachment> resolved = new ArrayList<>();
        for (String id : ids) {
            Attachment a = attachments.get(id);
            if (a != null) {
                resolved.add(a);
            } else {
                log.warn("Attachment id={} não encontrado — ignorando", id);
            }
        }
        return resolved;
    }

    private IssueDto resolveJiraIssue(String key) {
        if (key == null || key.isBlank()) return null;
        if (!jiraService.isConfigured()) {
            log.warn("Jira não configurado — ignorando jiraIssueKey={}", key);
            return null;
        }
        try {
            return jiraService.getIssue(key);
        } catch (Exception e) {
            log.warn("Falha ao buscar issue {} para contexto: {}", key, e.getMessage());
            return null;
        }
    }
}
