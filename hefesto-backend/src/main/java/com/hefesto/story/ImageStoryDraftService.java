package com.hefesto.story;

import com.hefesto.agents.Agent;
import com.hefesto.agents.AgentRegistry;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.process.ProcessLauncher;
import com.hefesto.llm.process.ProcessResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Gera um rascunho de história a partir de uma IMAGEM de tela/design.
 *
 * <p>Só funciona com um modelo que enxerga imagem. No setup atual isso é o
 * Claude via CLI: rodamos {@code claude -p --allowedTools Read} e pedimos pra ele
 * LER o arquivo da imagem (o Read tool do Claude renderiza imagens). Modelos
 * locais .gguf são texto-only e não cobrem este fluxo.</p>
 */
@Service
public class ImageStoryDraftService {

    private static final String AGENT = "EscritorDeHistorias";

    private final ProcessLauncher launcher;
    private final AgentRegistry agents;
    private final String claudePath;
    private final Duration timeout;

    public ImageStoryDraftService(
            ProcessLauncher launcher,
            AgentRegistry agents,
            @Value("${claude.cli.path:claude}") String claudePath,
            @Value("${claude.cli.imageTimeoutSeconds:240}") long timeoutSeconds) {
        this.launcher = launcher;
        this.agents = agents;
        this.claudePath = claudePath;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public StoryDraft draftFromImage(Path imagePath, String context) {
        Agent agent = agents.getOrDefault(AGENT);
        String prompt = buildPrompt(agent, imagePath, context);

        ProcessResult result;
        try {
            // Read liberado pro Claude conseguir abrir/enxergar o arquivo de imagem.
            result = launcher.run(prompt, timeout, claudePath, "-p", "--allowedTools", "Read");
        } catch (Exception e) {
            throw new LlmAdapterException(
                "Falha ao gerar história a partir da imagem (claude): " + e.getMessage(), e);
        }
        if (!result.success()) {
            String stderr = result.stderr() == null ? "" : result.stderr().strip();
            throw new LlmAdapterException(
                "Claude retornou exit=" + result.exitCode() + (stderr.isEmpty() ? "" : " :: " + stderr));
        }
        return StoryDraftService.parse(result.stdout() == null ? "" : result.stdout());
    }

    private String buildPrompt(Agent agent, Path imagePath, String context) {
        StringBuilder sb = new StringBuilder();
        if (agent != null && agent.hasSystemPrompt()) {
            sb.append(agent.systemPrompt().strip()).append("\n\n");
        }
        sb.append("# DESIGN / TELA\n")
          .append("Leia a imagem da tela/design no caminho a seguir e use-a como base PRINCIPAL ")
          .append("da história (elementos, campos, ações, fluxos visíveis):\n")
          .append(imagePath.toAbsolutePath()).append("\n\n");
        if (context != null && !context.isBlank()) {
            sb.append("# CONTEXTO ADICIONAL\n").append(context.strip()).append("\n\n");
        }
        sb.append("# MENSAGEM\n")
          .append("Gere a história com base na tela acima, respondendo APENAS com o JSON especificado.");
        return sb.toString();
    }
}
