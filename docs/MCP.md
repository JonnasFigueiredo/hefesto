# Hefesto como servidor MCP

O backend do Hefesto expõe um **servidor MCP (Model Context Protocol)** que
publica os modelos de LLM já integrados (modelos locais `.gguf` via
`llama-server`, Claude Code, Gemini, etc.) como **tools** consumíveis por
qualquer cliente MCP — Claude Code, Claude Desktop, etc.

## Arquitetura

```
Cliente MCP (Claude Code/Desktop)
        │  SSE  (GET /sse  +  POST /mcp/message)
        ▼
Hefesto backend  (Spring Boot, porta 8080)
        │  Spring AI 1.0.x — starter mcp-server-webmvc
        ▼
HefestoMcpTools  ──►  LlmAdapterRegistry
                        ├─ llama-3.1-8b / mistral-7b / qwen3-8b  (llama-server, .gguf de E:\LLM)
                        ├─ claude-code (CLI)
                        └─ gemini / codex / anthropic-api / ...
```

> Usamos o starter **webmvc** (não webflux) porque o projeto já carrega
> `spring-boot-starter-web` (stack Servlet/Tomcat). Spring AI **1.0.x** é a
> linha compatível com Spring Boot 3.4 + Java 17 (2.0.x exige Boot 4 + Java 21).

## Tools expostas

| Tool | Args | O que faz |
|---|---|---|
| `list_models` | — | Lista os adapters (`id`, `displayName`, `available`). |
| `chat` | `model`, `message` | Manda o prompt ao modelo `model` e devolve a resposta. |

## Endpoints

- `GET  http://localhost:8080/sse` — abre o stream SSE; primeiro evento traz o
  endpoint de mensagens com `sessionId`.
- `POST http://localhost:8080/mcp/message?sessionId=...` — JSON-RPC (initialize,
  tools/list, tools/call). As respostas voltam pelo canal SSE.

## Rodando os modelos locais (.gguf)

1. Baixe o `llama-server.exe` do llama.cpp (build **CPU**, já que a GPU é Intel
   HD integrada) em https://github.com/ggml-org/llama.cpp/releases e extraia
   para `E:\LLM`.
2. Suba os servidores: `scripts\start-llama-servers.bat`
   (um por modelo, portas 8081/8082/8083 — batem com `hefesto.local-models`).
3. Após "server is listening", `list_models` mostra os modelos como
   `available: true`.

> Inferência de modelos 8B Q4 em CPU é lenta; para testar, suba só um modelo.

## Conectando um cliente MCP

### Claude Code (via proxy stdio→SSE)
```bash
claude mcp add hefesto -- npx -y mcp-remote http://localhost:8080/sse
```

### Claude Desktop (`claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "hefesto": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/sse"]
    }
  }
}
```

## Configuração

`application.yml`:
- `spring.ai.mcp.server.*` — nome, versão, instructions do servidor MCP.
- `hefesto.local-models[]` — cada modelo local (`id`, `base-url`, `model`,
  `timeout-seconds`). Cada entrada vira um adapter e um destino do `chat`.
