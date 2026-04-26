package com.hefesto.llm.adapters;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.llm.ChatChunk;
import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.ChatStreamHandler;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.Message;
import com.hefesto.llm.process.ProcessLauncher;
import com.hefesto.llm.process.ProcessLaunchException;
import com.hefesto.llm.process.ProcessResult;
import com.hefesto.llm.process.StreamingProcess;

/**
 * Adapter para o CLI oficial do Claude Code da Anthropic.
 *
 * <p>Etapa 2 (esta): execução síncrona com {@code claude -p "<prompt>"},
 * captura stdout completo e devolve como única resposta.</p>
 *
 * <p>Etapa 3 (futura): trocar por {@code claude --output-format stream-json},
 * parsear linhas JSON e emitir chunks via Flux.</p>
 *
 * <p>O caminho do binário é configurado por {@code claude.cli.path}
 * (default {@code claude}, resolvendo via PATH).</p>
 */
@Component
public class ClaudeCodeCliAdapter implements LlmAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeCliAdapter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String cliPath;
    private final ProcessLauncher launcher;
    private final Duration chatTimeout;

    public ClaudeCodeCliAdapter(
        @Value("${claude.cli.path:claude}") String cliPath,
        @Value("${claude.cli.timeoutSeconds:120}") long timeoutSeconds,
        ProcessLauncher launcher
    ) {
        this.cliPath = cliPath;
        this.chatTimeout = Duration.ofSeconds(timeoutSeconds);
        this.launcher = launcher;
    }

    @Override
    public String id() {
        return "claude-code";
    }

    @Override
    public String displayName() {
        return "Claude Code";
    }

    @Override
    public boolean isAvailable() {
        try {
            ProcessResult result = launcher.run(null, Duration.ofSeconds(5), cliPath, "--version");
            boolean ok = result.success();
            if (!ok) {
                log.warn("claude --version returned exit={} stderr={}", result.exitCode(), result.stderr());
            }
            return ok;
        } catch (ProcessLaunchException e) {
            log.warn("claude CLI not available at '{}': {}", cliPath, e.getMessage());
            return false;
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String prompt = buildPrompt(request);
        long start = Instant.now().toEpochMilli();

        if (log.isDebugEnabled()) {
            log.debug("Sending prompt to Claude ({} chars, {} history messages):\n----\n{}\n----",
                prompt.length(), request.history().size(), truncate(prompt, 800));
        }

        // Modo headless: -p ativa não-interativo. O prompt vai via STDIN em vez
        // de argumento — isso evita o inferno de quoting do cmd.exe no Windows
        // (newlines, aspas, & | < > > etc.) e garante UTF-8 limpo.
        ProcessResult result;
        try {
            result = launcher.run(prompt, chatTimeout, cliPath, "-p");
        } catch (ProcessLaunchException e) {
            throw new LlmAdapterException(
                "Falha ao executar Claude Code CLI: " + e.getMessage(), e);
        }

        long latency = Instant.now().toEpochMilli() - start;

        if (!result.success()) {
            String stderr = result.stderr() == null ? "" : result.stderr().trim();
            throw new LlmAdapterException(
                "Claude Code retornou exit=" + result.exitCode()
                    + (stderr.isEmpty() ? "" : " :: " + truncate(stderr, 500)));
        }

        String content = result.stdout() == null ? "" : result.stdout().stripTrailing();
        if (content.isEmpty()) {
            throw new LlmAdapterException("Claude Code retornou stdout vazio");
        }

        return new ChatResponse(content, id(), latency, null);
    }

    @Override
    public void chatStream(ChatRequest request, ChatStreamHandler handler) {
        String prompt = buildPrompt(request);
        long start = Instant.now().toEpochMilli();

        if (log.isDebugEnabled()) {
            log.debug("Streaming prompt to Claude ({} chars, {} history messages)",
                prompt.length(), request.history().size());
        }

        StringBuilder fullText = new StringBuilder();
        String lastAssistantText = "";
        String model = null;
        String sessionId = null;

        try (StreamingProcess proc = launcher.startStreaming(
                prompt,
                cliPath, "-p",
                "--output-format", "stream-json",
                "--verbose")) {

            String line;
            while ((line = proc.readLine()) != null) {
                if (handler.isAborted()) {
                    proc.abort();
                    return;
                }
                if (line.isBlank()) continue;

                try {
                    JsonNode root = MAPPER.readTree(line);
                    String type = root.path("type").asText("");

                    switch (type) {
                        case "system":
                            if (root.has("model") && model == null) {
                                model = root.path("model").asText(null);
                            }
                            if (root.has("session_id") && sessionId == null) {
                                sessionId = root.path("session_id").asText(null);
                            }
                            break;

                        case "assistant":
                            JsonNode content = root.path("message").path("content");
                            if (content.isArray()) {
                                StringBuilder thisMsg = new StringBuilder();
                                for (JsonNode block : content) {
                                    if ("text".equals(block.path("type").asText())) {
                                        thisMsg.append(block.path("text").asText(""));
                                    }
                                }
                                String thisText = thisMsg.toString();
                                if (!thisText.isEmpty()) {
                                    String delta;
                                    if (thisText.startsWith(lastAssistantText) && lastAssistantText.length() < thisText.length()) {
                                        // incremental: emite só o que é novo
                                        delta = thisText.substring(lastAssistantText.length());
                                    } else {
                                        // mensagem nova / sem prefixo comum: emite inteiro
                                        delta = thisText;
                                        if (fullText.length() > 0) fullText.append("\n");
                                    }
                                    handler.onChunk(ChatChunk.content(delta));
                                    fullText.append(delta);
                                    lastAssistantText = thisText;
                                }
                            }
                            break;

                        case "result":
                            if (model == null && root.has("model")) {
                                model = root.path("model").asText(null);
                            }
                            // se nada chegou via 'assistant' mas há 'result', usa
                            if (fullText.length() == 0 && root.has("result")) {
                                String r = root.path("result").asText("");
                                if (!r.isEmpty()) {
                                    handler.onChunk(ChatChunk.content(r));
                                    fullText.append(r);
                                }
                            }
                            break;

                        default:
                            // user/tool_use/etc — ignora silenciosamente
                            break;
                    }
                } catch (Exception parseEx) {
                    log.debug("Failed to parse stream line: '{}' ({})",
                        truncate(line, 200), parseEx.getMessage());
                }
            }

            ProcessResult finalResult = proc.awaitCompletion(chatTimeout);
            if (handler.isAborted()) return;

            if (!finalResult.success()) {
                String stderr = finalResult.stderr() == null ? "" : finalResult.stderr().trim();
                handler.onError(new LlmAdapterException(
                    "Claude Code retornou exit=" + finalResult.exitCode()
                        + (stderr.isEmpty() ? "" : " :: " + truncate(stderr, 500))));
                return;
            }

            if (fullText.length() == 0) {
                handler.onError(new LlmAdapterException("Claude Code retornou stream vazio"));
                return;
            }

            long latency = Instant.now().toEpochMilli() - start;
            ChatResponse response = new ChatResponse(fullText.toString(), id(), latency, model);
            handler.onComplete(response);
        } catch (ProcessLaunchException e) {
            handler.onError(new LlmAdapterException(
                "Falha ao executar Claude Code CLI: " + e.getMessage(), e));
        } catch (Throwable t) {
            handler.onError(new LlmAdapterException(
                "Erro inesperado durante streaming: " + t.getMessage(), t));
        }
    }

    /**
     * Concatena history + nova mensagem em um único prompt textual.
     * Estratégia simples para Etapa 2; Etapa 3 mandará via stdin estruturado.
     */
    private String buildPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : request.history()) {
            sb.append("[").append(msg.role().name()).append("]\n");
            sb.append(msg.content()).append("\n\n");
        }
        sb.append("[USER]\n").append(request.userMessage());
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
