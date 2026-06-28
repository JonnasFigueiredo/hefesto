package com.hefesto.mcp;

import com.hefesto.llm.local.LocalModelsRegistrar;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring do servidor MCP do Hefesto.
 *
 * <ul>
 *   <li>{@link #hefestoTools} publica os métodos {@code @Tool} de
 *       {@link HefestoMcpTools} como ferramentas MCP (o starter webmvc serve
 *       em GET /sse + POST /mcp/message).</li>
 *   <li>{@link #localModelsRegistrar} é {@code static} de propósito: um
 *       BeanDefinitionRegistryPostProcessor precisa ser instanciado bem cedo,
 *       antes da resolução normal de beans.</li>
 * </ul>
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider hefestoTools(HefestoMcpTools tools, WorkflowMcpTools workflowTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools, workflowTools)
                .build();
    }

    @Bean
    public static LocalModelsRegistrar localModelsRegistrar() {
        return new LocalModelsRegistrar();
    }
}
