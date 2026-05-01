package com.hefesto.agents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentRegistry registry;

    public AgentController(AgentRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<AgentDto> list() {
        return registry.list().stream().map(AgentDto::from).toList();
    }

    /**
     * Recarrega agentes do disco. Útil após editar/adicionar arquivos .md
     * sem precisar reiniciar o backend.
     */
    @PostMapping("/reload")
    public Map<String, Object> reload() {
        int count = registry.reload();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loaded", count);
        body.put("path", registry.configuredPath());
        body.put("agents", registry.list().stream().map(Agent::name).toList());
        return body;
    }
}
