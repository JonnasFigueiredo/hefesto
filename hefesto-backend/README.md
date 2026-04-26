# hefesto-backend

> Backend Spring Boot 3 do Hefesto. Expõe REST + WebSocket pro frontend, orquestra adapters de LLM via subprocesso de CLI, e integra com o Jira Cloud.

## Sumário

- [Stack](#stack)
- [Estrutura de pacotes](#estrutura-de-pacotes)
- [Como rodar](#como-rodar)
- [Configuração](#configuração)
- [Endpoints](#endpoints)
- [Contrato `LlmAdapter`](#contrato-llmadapter)
- [Como adicionar um novo adapter](#como-adicionar-um-novo-adapter)
- [Testes](#testes)
- [Logs](#logs)

---

## Stack

- **Java 17** (LTS, mínimo exigido pelo Spring Boot 3).
- **Spring Boot 3.4** com `spring-boot-starter-web` e `spring-boot-starter-validation`. WebSocket entrará na Etapa 3 com `spring-boot-starter-websocket`.
- **Maven** como build tool. O `pom.xml` herda do `spring-boot-starter-parent`.
- **Jackson** (vem por dependência transitiva) pra serializar JSON.
- **JUnit 5 + Mockito** pra testes (`spring-boot-starter-test`).

---

## Estrutura de pacotes

Convenção: pacote por feature, não por camada.

```
com.hefesto/
├── HefestoApplication.java       # @SpringBootApplication, ponto de entrada
│
├── config/
│   └── CorsConfig.java           # libera localhost:5173 no dev
│
├── health/
│   └── HealthController.java     # GET /api/health → status, ts, version
│
├── llm/                          # [Etapa 2] camada de adapters
│   ├── LlmAdapter.java           # interface central
│   ├── LlmAdapterRegistry.java   # @Component que injeta todos os adapters
│   ├── ChatRequest.java          # DTO de entrada (record)
│   ├── ChatChunk.java            # DTO de chunk de saída (record)
│   └── adapters/
│       ├── ClaudeCodeCliAdapter.java
│       ├── GeminiCliAdapter.java       # stub
│       ├── CodexCliAdapter.java        # stub
│       └── AnthropicApiAdapter.java    # stub
│
├── chat/                         # [Etapa 2/3]
│   ├── ChatController.java       # REST + WebSocket endpoints
│   ├── ChatService.java          # orquestra registry + conversation store
│   └── ConversationStore.java    # in-memory Map<id, List<Message>>
│
└── jira/                         # [Etapa 4]
    ├── JiraClient.java           # wrapper do RestClient com auth básica
    ├── JiraService.java          # lógica de negócio (busca, detalhe, comentário)
    ├── JiraController.java       # endpoints REST /api/jira/*
    └── dto/
        ├── IssueDto.java
        └── CommentDto.java
```

Pacotes marcados com `[Etapa X]` ainda não existem.

---

## Como rodar

### Dev

```bash
mvn spring-boot:run
```

Sobe em `http://localhost:8080`. Recarrega quase instantaneamente quando você salva um `.java` (graças ao `spring-boot-devtools`, que será adicionado em etapa futura — por ora, restart manual).

### Build

```bash
mvn clean package
```

Gera `target/hefesto-backend-0.1.0-SNAPSHOT.jar` executável:

```bash
java -jar target/hefesto-backend-0.1.0-SNAPSHOT.jar
```

### Skipping tests

```bash
mvn clean package -DskipTests
```

---

## Configuração

### `application.yml`

Defaults seguros para todos os ambientes. Não comite nada sensível aqui.

```yaml
server:
  port: 8080

spring:
  application:
    name: hefesto-backend
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:default}

logging:
  level:
    root: INFO
    com.hefesto: DEBUG
```

### `application-local.yml` (gitignored)

Pra credenciais reais. Crie esse arquivo manualmente em `src/main/resources/application-local.yml` e ative com:

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Exemplo:

```yaml
claude:
  cli:
    path: /usr/local/bin/claude

jira:
  url: https://yourcompany.atlassian.net
  email: you@yourcompany.com
  token: ATATT3xFfGF0...
```

### Por env vars

Mesmas chaves em formato uppercase com underscores: `CLAUDE_CLI_PATH`, `JIRA_URL`, `JIRA_EMAIL`, `JIRA_TOKEN`. Útil pra rodar em container sem montar arquivo.

---

## Endpoints

### Implementados (Etapa 1)

| Método | Path | Resposta | Notas |
|--------|------|----------|-------|
| `GET` | `/api/health` | `{status, ts, service, version}` | Health check, usado pelo frontend a cada 10s. |

### Planejados

| Etapa | Método | Path | Descrição |
|-------|--------|------|-----------|
| 2 | `GET` | `/api/llm/adapters` | Lista adapters: `[{id, displayName, available}]`. |
| 2 | `POST` | `/api/chat` | Chat síncrono. Body: `{adapterId, conversationId, message}`. Resposta: `{response, latencyMs}`. |
| 3 | `WS` | `/ws/chat` | Streaming. Mensagens: `{type:"start"\|"abort", ...}` ↔ `{type:"chunk"\|"done"\|"error", ...}`. |
| 4 | `GET` | `/api/jira/issues?jql=...&maxResults=20` | Busca issues. |
| 4 | `GET` | `/api/jira/issues/{key}` | Detalhe de issue (descrição em markdown). |
| 4 | `GET` | `/api/jira/issues/{key}/comments` | Lista de comentários. |
| 6 | `POST` | `/api/jira/issues/{key}/comments` | Posta comentário. Body: `{body}`. |

---

## Contrato `LlmAdapter`

> Disponível a partir da Etapa 2.

Toda integração com LLM passa por essa interface:

```java
public interface LlmAdapter {
    /** Identificador estável usado em URLs e seleção. Ex: "claude-code". */
    String id();

    /** Nome legível mostrado no dropdown do frontend. Ex: "Claude Code". */
    String displayName();

    /**
     * Indica se o adapter está disponível agora.
     * Tipicamente verifica se um binário existe ou se uma API responde.
     * Chamado pelo frontend pra desabilitar opções indisponíveis.
     */
    boolean isAvailable();

    /**
     * Executa um chat. Retorna um Flux de chunks, que pode ter 1 (resposta completa)
     * ou N elementos (streaming token a token).
     * O Flux deve completar quando o LLM terminar e errar em caso de falha.
     */
    Flux<ChatChunk> chat(ChatRequest request);
}
```

`ChatRequest` (record):

```java
public record ChatRequest(
    String conversationId,
    List<Message> history,    // mensagens anteriores na conversa
    String userMessage,        // mensagem nova do usuário
    Map<String, Object> options // ex: {"temperature": 0.7}
) {}
```

`ChatChunk` (record):

```java
public record ChatChunk(
    ChunkType type,            // CONTENT, METADATA, DONE, ERROR
    String content,            // texto incremental (pode ser null)
    Map<String, Object> meta   // tokens, latência, etc.
) {}
```

### Implementação de referência: `ClaudeCodeCliAdapter`

```java
@Component
public class ClaudeCodeCliAdapter implements LlmAdapter {

    private final String cliPath;
    private final ProcessLauncher launcher;

    public ClaudeCodeCliAdapter(
        @Value("${claude.cli.path:claude}") String cliPath,
        ProcessLauncher launcher
    ) {
        this.cliPath = cliPath;
        this.launcher = launcher;
    }

    @Override public String id() { return "claude-code"; }
    @Override public String displayName() { return "Claude Code"; }

    @Override
    public boolean isAvailable() {
        return launcher.run(cliPath, "--version").exitCode() == 0;
    }

    @Override
    public Flux<ChatChunk> chat(ChatRequest request) {
        // Etapa 2: -p "<prompt>" e capturar stdout completo, emitir 1 chunk
        // Etapa 3: --output-format stream-json e parsear linhas JSON em chunks
    }
}
```

`ProcessLauncher` é uma abstração injetável que envolve `ProcessBuilder` — facilita mock nos testes.

---

## Como adicionar um novo adapter

1. **Crie a classe** em `com.hefesto.llm.adapters.NomeAdapter`, anotada com `@Component`.
2. **Implemente os 4 métodos** da interface.
3. **Adicione propriedade de config** se precisar (ex: `gemini.cli.path` em `application.yml`).
4. **Escreva testes** em `src/test/java/com/hefesto/llm/adapters/NomeAdapterTest.java`. Mock `ProcessLauncher` ou cliente HTTP. Não dependa do binário/serviço real estar instalado.
5. **Reinicie o backend.** O `LlmAdapterRegistry` descobre via injeção de todos os beans `LlmAdapter`. Não precisa registrar em lugar nenhum.

Exemplo mínimo de teste:

```java
@ExtendWith(MockitoExtension.class)
class GeminiCliAdapterTest {

    @Mock ProcessLauncher launcher;

    @Test
    void isAvailable_returnsFalse_whenBinaryMissing() {
        when(launcher.run("gemini", "--version"))
            .thenReturn(new ProcessResult(127, "", "command not found"));

        var adapter = new GeminiCliAdapter("gemini", launcher);

        assertThat(adapter.isAvailable()).isFalse();
    }
}
```

---

## Testes

### Rodando

```bash
mvn test                    # tudo
mvn test -Dtest=HealthControllerTest      # uma classe
mvn test -Dtest=*AdapterTest              # padrão
```

### Cobertura mínima esperada

| Área | Tipo | Cobertura mínima |
|------|------|------------------|
| Controllers | `@WebMvcTest` ou `MockMvc` | Cada endpoint com 200 e ao menos 1 caminho de erro. |
| Services | Testes unitários com Mockito | Lógica de orquestração e edge cases. |
| Adapters | Testes unitários com `ProcessLauncher` mockado | `isAvailable` true/false; `chat` com chunks simulados. |
| Jira client | `MockRestServiceServer` | Mapeamento de DTOs e tratamento de erros HTTP. |

### Relatório de cobertura (futuro)

Plug do Jacoco no `pom.xml` está planejado pra Etapa 7. Por enquanto, cobertura é informal.

---

## Logs

SLF4J com nível `DEBUG` no pacote `com.hefesto` (definido em `application.yml`). Útil pra ver o comando exato que o adapter dispara, payloads do Jira, etc.

Pra ajustar nível em runtime sem rebuild:

```bash
JAVA_TOOL_OPTIONS="-Dlogging.level.com.hefesto=TRACE" mvn spring-boot:run
```

Logs vão pro stdout. Se quiser arquivo, adicione em `application.yml`:

```yaml
logging:
  file:
    name: logs/hefesto.log
```
