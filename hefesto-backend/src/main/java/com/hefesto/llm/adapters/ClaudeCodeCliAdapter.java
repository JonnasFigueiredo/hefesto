package com.hefesto.llm.adapters;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.Message;
import com.hefesto.llm.process.ProcessLauncher;
import com.hefesto.llm.process.ProcessLaunchException;
import com.hefesto.llm.process.ProcessResult;

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
