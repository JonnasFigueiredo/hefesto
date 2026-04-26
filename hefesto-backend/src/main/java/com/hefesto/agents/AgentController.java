package com.hefesto.agents;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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
}
