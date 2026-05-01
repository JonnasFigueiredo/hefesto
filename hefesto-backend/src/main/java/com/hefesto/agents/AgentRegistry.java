package com.hefesto.agents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Registro de agentes carregado de uma pasta no disco. Cada arquivo
 * {@code .md} dentro de {@code agents.path} (default {@code ./agentes})
 * vira um agente disponível na UI.
 *
 * <p>O nome exibido no dropdown é o nome do arquivo (ex: "QAseniorAgent.md").
 * O ID é o nome sem extensão.</p>
 *
 * <p>Reload em runtime via {@link #reload()} — sem precisar reiniciar o
 * backend pra adicionar/editar agentes.</p>
 */
@Component
public class AgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    private final String configuredPath;
    private final Map<String, Agent> byId = new LinkedHashMap<>();

    public AgentRegistry(@Value("${agents.path:./agentes}") String configuredPath) {
        this.configuredPath = configuredPath;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * Recarrega agentes do disco. Substitui a lista atual atomicamente —
     * se a leitura falhar, mantém o estado anterior.
     *
     * @return número de agentes carregados.
     */
    public synchronized int reload() {
        Map<String, Agent> loaded = readFromDisk();
        if (loaded.isEmpty()) {
            log.warn("Nenhum agente encontrado em '{}' — usando agente padrão embutido.",
                configuredPath);
            loaded.put(Agent.DEFAULT_ID, fallbackDefault());
        }
        synchronized (byId) {
            byId.clear();
            byId.putAll(loaded);
        }
        log.info("Agentes carregados ({}): {}", loaded.size(), loaded.keySet());
        return loaded.size();
    }

    public List<Agent> list() {
        synchronized (byId) {
            return List.copyOf(byId.values());
        }
    }

    public Optional<Agent> get(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        synchronized (byId) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    public Agent getOrDefault(String id) {
        return get(id).orElseGet(() -> {
            synchronized (byId) {
                Agent dflt = byId.get(Agent.DEFAULT_ID);
                if (dflt != null) return dflt;
                // Sem default — pega o primeiro disponível.
                return byId.values().stream().findFirst().orElse(fallbackDefault());
            }
        });
    }

    public String configuredPath() {
        return configuredPath;
    }

    // -----------------------------------------------------------------------

    private Map<String, Agent> readFromDisk() {
        Map<String, Agent> result = new LinkedHashMap<>();
        Path dir = resolvePath();

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.info("Pasta de agentes '{}' não existe. Criando estrutura mínima recomendada não foi solicitada — pulando.",
                dir);
            return result;
        }

        try (Stream<Path> files = Files.list(dir)) {
            files
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String fn = p.getFileName().toString().toLowerCase();
                    return fn.endsWith(".md") || fn.endsWith(".markdown");
                })
                .sorted()
                .forEach(p -> {
                    try {
                        String content = Files.readString(p, StandardCharsets.UTF_8);
                        Agent a = AgentFileParser.parse(p.getFileName().toString(), content);
                        if (a.id() != null && !a.id().isBlank()) {
                            result.put(a.id(), a);
                        }
                    } catch (IOException e) {
                        log.warn("Falha ao ler agente {}: {}", p.getFileName(), e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.warn("Falha ao listar pasta de agentes '{}': {}", dir, e.getMessage());
        }
        return result;
    }

    private Path resolvePath() {
        Path raw = Paths.get(configuredPath);
        if (raw.isAbsolute()) return raw;

        // Tenta resolver relativo ao working dir primeiro, depois um nível acima
        // (caso backend rode em hefesto-backend/ e a pasta agentes/ esteja na raiz).
        Path cwd = Paths.get(System.getProperty("user.dir")).resolve(raw).normalize();
        if (Files.exists(cwd)) return cwd;

        Path parent = Paths.get(System.getProperty("user.dir")).getParent();
        if (parent != null) {
            Path fromParent = parent.resolve(raw).normalize();
            if (Files.exists(fromParent)) return fromParent;
        }
        return cwd;
    }

    /**
     * Agente Default mínimo, usado quando a pasta de agentes está vazia ou
     * inacessível. Garante que o sistema funciona out-of-the-box.
     */
    private static Agent fallbackDefault() {
        return new Agent(
            Agent.DEFAULT_ID,
            "Default.md",
            "Sem persona específica. Responde como um assistente geral.",
            "",
            null,
            "✦",
            false
        );
    }
}
