# Hefesto — Prompt de inicialização para Claude Code

> Cole o conteúdo abaixo (a partir de "## Contexto") no Claude Code rodando na pasta vazia do projeto. Ele foi escrito pra ser executado em **etapas**, com você validando entre cada uma.

---

## Contexto

Você está iniciando o projeto **Hefesto**: uma aplicação web que serve de interface pra interagir com chatbots de LLM (via CLIs como o Claude Code da Anthropic) e que se integra com o **Jira** pra puxar histórias/issues e usá-las como contexto da conversa. O repositório está vazio.

A interface web é parte central do produto — não um wrapper genérico em volta de uma API. O usuário vai passar a maior parte do tempo nela, conversando com o LLM e navegando issues. Trate o frontend com o mesmo cuidado que o backend.

## Objetivo do MVP

Um usuário, em modo local single-user (sem login), deve conseguir:

1. Abrir a interface web no navegador.
2. Escolher qual CLI de LLM usar (no MVP só Claude Code, mas a arquitetura permite adicionar outros).
3. Listar e buscar issues do Jira (read).
4. Selecionar uma issue e mandar seu conteúdo (descrição, comentários, critérios de aceite) como contexto inicial pro chat.
5. Conversar em streaming com o LLM.
6. Postar de volta um comentário na issue (ex: resumo da conversa, decisão técnica).

## Stack

**Backend**
- Java 18, Spring Boot 3.x, Maven
- Spring Web + Spring WebSocket (streaming do chat)
- Configuration Properties pra credenciais Jira/LLM
- Sem banco no MVP — conversas em memória

**Frontend**
- React 18 + TypeScript, Vite
- Tailwind CSS com tema custom (definido abaixo)
- TanStack Query pra estado de servidor
- Zustand pra estado de UI (conversas, adapter selecionado)
- React Router (rotas: `/chat`, `/jira`, `/settings`)
- Framer Motion pra animações de transição
- Lucide-react pra ícones (estilo line, combina com a vibe)
- Fontes via Google Fonts: **Rajdhani** (display/headings), **JetBrains Mono** (mono/data), **Inter** (body)

---

## Identidade visual — Hefesto FUI

Estética: **Fictional User Interface (FUI) / wireframe vector / cyberpunk técnico**. Pense em HUD de filme sci-fi, dashboard de Jarvis, terminal de nave espacial, painel de Linear cruzado com Cyberpunk 2077. Linhas finas, cantos com brackets, fundos escuros, acentos neon, tipografia técnica, animações sutis de pulso/scan.

### Design tokens (configurar no `tailwind.config.ts` e `index.css`)

**Cores (CSS variables em `:root`):**

```css
--bg-base: #07090f;         /* fundo profundo, quase preto */
--bg-elevated: #0d1320;     /* painéis */
--bg-overlay: #131b2e;      /* hover, modais */
--border-dim: #1a2440;      /* bordas neutras */
--border: #2a3a66;          /* bordas padrão */
--border-accent: #00d4ff;   /* bordas em foco/ativo */
--text: #e6edf9;            /* texto principal */
--text-dim: #7a8aa8;        /* texto secundário */
--text-muted: #4a5878;      /* texto terciário, labels */
--accent-cyan: #00d4ff;     /* primário, ações principais, glow */
--accent-amber: #ffb547;    /* warnings, destaque secundário */
--accent-magenta: #ff3d8a;  /* alertas, ações destrutivas */
--accent-green: #00ff9f;    /* sucesso, status online */
--scanline: rgba(0, 212, 255, 0.03);
```

**Tipografia:**
- `font-display`: Rajdhani — uppercase, letter-spacing alto, usar em títulos de página, labels de seção, KEY de issue.
- `font-mono`: JetBrains Mono — usar em código, IDs, timestamps, chunks técnicos.
- `font-sans`: Inter — corpo de texto, mensagens de chat, descrições de issue.

**Efeitos globais:**
- Fundo da app: `var(--bg-base)` + grid de pontos sutil (SVG repeating pattern, opacity 0.03).
- Texto em accent-cyan tem `text-shadow: 0 0 8px rgba(0, 212, 255, 0.4)` (glow leve).
- Bordas em foco/ativo: `box-shadow: 0 0 0 1px var(--accent-cyan), 0 0 12px rgba(0,212,255,0.2)`.
- Scanline opcional via overlay fixo `pointer-events:none` (linhas horizontais 2px translúcidas se movendo lentamente — animação CSS de 8s).
- Cantos com **corner brackets**: cada `<Frame>` tem 4 pseudo-elementos nos cantos formando `⌐`, `¬`, `L`, `J` em accent-cyan, 1px, 12px de comprimento.

**Tom geral:** muito espaço, pouco preenchimento, bordas finas (1px, sempre 1px). Nada de sombras suaves estilo Material — sombras só se forem glow neon. Border-radius pequeno (2-4px) ou zero. Cantos retos > cantos arredondados.

### Biblioteca de componentes base (criar em `src/components/ui/`)

- **`<Frame>`**: container com 1px border, corner brackets cyan, padding interno. Variantes: `default`, `accent` (border cyan), `dim` (border mais fraca). Aceita `title?` que renderiza num pequeno chip sobreposto à borda superior.
- **`<Panel>`**: bloco de fundo elevado (`--bg-elevated`) sem brackets, usado pra agrupar conteúdo dentro de um Frame.
- **`<Button>`**: variantes `primary` (fundo cyan transparente + border cyan + glow no hover), `secondary` (border-dim, hover vira border cyan), `ghost` (sem border), `danger` (magenta). Tamanhos `sm`, `md`. Sempre uppercase, font-display, letter-spacing 0.1em.
- **`<Input>` e `<Textarea>`**: fundo `--bg-overlay`, border 1px `--border`, focus vira `--accent-cyan` + glow. Font-mono pra inputs de busca/JQL, font-sans pro composer de chat.
- **`<StatusDot>`**: bolinha 6px que pulsa. Variantes `online` (verde), `offline` (vermelho), `pending` (amber, animação de blink).
- **`<Badge>`**: chip pequeno com border 1px e fundo translúcido. Variantes por cor.
- **`<Spinner>`**: anel rotativo com gap (não círculo cheio) — estilo HUD carregando.
- **`<GridBackground>`**: SVG fixed cobrindo a viewport com pontos 1px espaçados 24px, opacity baixíssima.
- **`<ScanlineOverlay>`**: opcional, ativável via setting. Overlay fullscreen com gradient repetitivo animado.
- **`<KBD>`**: tecla estilizada, mono, border, fundo escuro. Pra mostrar atalhos.

Todos os componentes em TypeScript com props tipadas.

---

## Arquitetura

### Camada de adapters de LLM (parte central do backend)

Defina a interface:

```java
public interface LlmAdapter {
    String id();               // "claude-code", "gemini-cli", etc.
    String displayName();
    boolean isAvailable();     // checa se o binário existe / API responde
    Flux<ChatChunk> chat(ChatRequest request);   // streaming
}
```

Implementação inicial:
- `ClaudeCodeCliAdapter` — faz spawn do binário `claude` como subprocesso, conversa via stdin/stdout em modo `--output-format stream-json`.

Stubs preparados (sem implementação real no MVP):
- `GeminiCliAdapter`, `CodexCliAdapter`, `AnthropicApiAdapter` — só registram-se com `isAvailable()` retornando false e mensagem clara.

`LlmAdapterRegistry` injeta todos os beans `LlmAdapter` e expõe `list()` e `get(id)`.

### Estrutura de pastas

```
hefesto/
├── PROMPT_CLAUDE_CODE.md
├── README.md
├── hefesto-backend/
│   ├── pom.xml
│   └── src/main/java/com/hefesto/
│       ├── HefestoApplication.java
│       ├── config/                  # CorsConfig, propriedades
│       ├── llm/
│       │   ├── LlmAdapter.java
│       │   ├── LlmAdapterRegistry.java
│       │   ├── ChatRequest.java
│       │   ├── ChatChunk.java
│       │   └── adapters/
│       │       ├── ClaudeCodeCliAdapter.java
│       │       ├── GeminiCliAdapter.java          # stub
│       │       ├── CodexCliAdapter.java           # stub
│       │       └── AnthropicApiAdapter.java       # stub
│       ├── jira/
│       │   ├── JiraClient.java
│       │   ├── JiraService.java
│       │   ├── JiraController.java
│       │   └── dto/
│       └── chat/
│           ├── ChatController.java
│           ├── ChatService.java
│           └── ConversationStore.java
└── hefesto-frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.ts
    ├── index.html
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── routes.tsx
        ├── theme/
        │   ├── tokens.css
        │   └── globals.css
        ├── components/
        │   ├── ui/                  # Frame, Panel, Button, Input, etc.
        │   ├── shell/               # Sidebar, TopBar, AppShell
        │   ├── chat/                # ChatWindow, MessageList, MessageBubble, Composer, AdapterSelector, ConversationList
        │   └── jira/                # IssueList, IssueCard, IssueDetail, JqlSearchBar, JiraStatusBadge
        ├── pages/
        │   ├── ChatPage.tsx
        │   ├── JiraPage.tsx
        │   └── SettingsPage.tsx
        ├── api/                     # http.ts, ws.ts, jira.ts, chat.ts, adapters.ts
        ├── store/                   # zustand: chatStore, settingsStore
        ├── hooks/
        └── types/
```

---

## Telas e componentes — especificação detalhada

### Shell (`<AppShell>`)

Layout permanente em todas as páginas. Composto por:

- **Sidebar (`240px` fixa, esquerda)**:
  - Topo: logo "HEFESTO" em font-display, uppercase, letter-spacing 0.3em, accent-cyan, com brackets `[ HEFESTO ]` e tagline embaixo "// JIRA × LLM CONSOLE" em mono pequeno text-muted.
  - Nav vertical com 3 itens: `CHAT`, `JIRA`, `SETTINGS`. Cada item tem ícone (lucide: MessageSquare, GitBranch, Settings), label em font-display uppercase, e à direita um indicador minúsculo (linha vertical de 2px, accent-cyan quando ativo). Item ativo: border-left 2px accent-cyan + fundo `--bg-elevated`.
  - Rodapé da sidebar: `<SystemStatus>` mostrando StatusDot do backend (`/api/health` polling 10s) + adapter atual + versão. Tudo em mono pequeno.

- **Main (resto da viewport)**:
  - TopBar fina (40px): breadcrumb (`HEFESTO / CHAT`) à esquerda, à direita um relógio em mono atualizando a cada 1s no formato `2026.04.25 // 14:32:07 UTC` e um indicador de latência da última request.
  - Conteúdo da página renderizado abaixo, com padding 24px.

- **Background**: `<GridBackground>` global + opcional `<ScanlineOverlay>` (toggle em settings).

### Página Chat (`/chat`) — tela principal

Layout em duas colunas:

**Esquerda (320px) — `<ConversationList>`**:
- Header com título "SESSIONS" font-display + botão `+ NEW` (Button primary sm).
- Lista de conversas (in-memory). Cada item: KEY ID (mono, 8 chars, gerado), título inferido (primeira mensagem truncada), timestamp relativo (mono dim), badge do adapter usado.
- Item ativo: border-left accent-cyan + fundo elevated.
- Vazio: `<EmptyState>` com ASCII art simples e texto "// NO ACTIVE SESSIONS".

**Direita (flex-1) — `<ChatWindow>`**:
- Header da conversa (Frame accent):
  - Esquerda: título da sessão (editável inline ao clicar).
  - Direita: `<AdapterSelector>` (dropdown FUI) + botão clear (ghost, ícone trash).
- Se houver issue Jira anexada: chip abaixo do header `[ ANEXED // PROJ-123 ]` clicável que abre slide-in com detalhe.
- `<MessageList>` (flex-1, scroll):
  - Mensagens flowem de cima pra baixo, mais recente embaixo, scroll automático no novo chunk.
  - `<MessageBubble>` variantes:
    - `user`: alinhada à direita, max-width 70%, fundo `--bg-overlay`, border-right 2px accent-cyan, font-sans, header pequeno com "USER // hh:mm:ss" mono dim.
    - `assistant`: alinhada à esquerda, max-width 80%, Frame com border, font-sans, header com nome do adapter + tokens/latência em mono dim. Conteúdo renderiza markdown (headers, listas, blocos de código com syntax highlighting via `react-syntax-highlighter` tema escuro custom).
    - `system` / `jira-context`: full-width, Frame dim com title chip "// CONTEXT INJECTED", conteúdo em mono pequeno colapsável (mostra primeiras 8 linhas + "expand").
  - Streaming: enquanto chunks chegam, último bubble assistant tem `<StreamingCursor>` (bloco piscando ▊ no fim do texto) e badge "STREAMING" no header.
  - Antes da primeira resposta: `<ThinkingIndicator>` — três barras verticais de altura variável animando (estilo equalizer) + label "// PROCESSING".
- `<Composer>` (rodapé fixo):
  - Frame com Textarea expansível (auto-resize até 200px), placeholder "// TRANSMIT MESSAGE...".
  - Footer do composer: à esquerda contador de chars/tokens estimados em mono dim; à direita botão `TRANSMIT` (primary) com KBD `⌘ ⏎` à direita.
  - Atalho: `Cmd/Ctrl + Enter` envia. `Enter` simples = nova linha.
  - Estado disabled enquanto streaming, com label "// AWAITING RESPONSE".

### Página Jira (`/jira`)

Layout em duas colunas:

**Esquerda (40% min 400px) — busca + lista**:
- `<JqlSearchBar>`: Input mono full-width, placeholder `// JQL: project = HEF AND status = "In Progress"`. Sugestões rápidas como chips abaixo: `assignee = currentUser()`, `status = Open`, `updated >= -7d`. Botão `EXECUTE` à direita.
- `<IssueList>`: scroll vertical, cada item é `<IssueCard>`:
  - Frame (default), padding 16px.
  - Linha 1: KEY (mono bold accent-cyan) + `<JiraStatusBadge>` (cor mapeada: amber=in progress, green=done, dim=todo, magenta=blocked).
  - Linha 2: título (font-sans, text).
  - Linha 3: meta em mono dim — assignee, updated relative, type icon.
  - Hover: border vira accent-cyan, glow.
  - Ativo (selecionado): border accent + corner brackets visíveis.
- Loading: skeleton de 5 cards com animação shimmer cyan.
- Erro de auth: Frame magenta com instruções pra ir em Settings.

**Direita (flex-1) — `<IssueDetail>`**:
- Header: KEY grande (font-display 32px), título abaixo, status badge.
- Action bar: `[ SEND TO CHAT ]` (primary, ícone Send) + `[ OPEN IN JIRA ]` (secondary, ícone ExternalLink).
- Tabs (FUI style — labels uppercase com underline cyan no ativo): `DESCRIPTION`, `ACCEPTANCE` (parsed se existir), `COMMENTS`, `META`.
- Conteúdo:
  - `DESCRIPTION`: markdown renderizado.
  - `ACCEPTANCE`: lista de critérios, cada um com checkbox decorativo `[ ]` em mono.
  - `COMMENTS`: timeline vertical, linha cyan à esquerda conectando pontos, cada comentário em Frame dim com autor (font-display) + timestamp (mono) + corpo.
  - `META`: tabela 2-col mono — campos do Jira (priority, labels, sprint, etc.).
- Vazio (nenhuma issue selecionada): centralizado, ASCII art + "// SELECT AN ISSUE TO INSPECT".

### Página Settings (`/settings`)

Lista de seções, cada uma num Frame com title chip:

- **`[ LLM ADAPTERS ]`**: tabela com colunas ID, NAME, AVAILABLE, ACTION. Linha por adapter. AVAILABLE = StatusDot. ACTION = botão "TEST" que dispara health check.
- **`[ JIRA CONNECTION ]`**: form com URL, EMAIL, TOKEN (input password masked). Botão "TEST CONNECTION" + "SAVE". Estado salvo no backend via `application-local.yml` ou env.
- **`[ APPEARANCE ]`**: toggles — "Scanline overlay", "Glow intensity" (slider low/medium/high), "CRT flicker" (subtle).
- **`[ ABOUT ]`**: versão, build hash, links.

### `<AdapterSelector>` (componente reusável)

Dropdown custom, **não** usar `<select>` nativo:
- Trigger: pill com `[ CLAUDE-CODE ]` em font-display + StatusDot + chevron, border 1px.
- Open: painel flutuante com Frame, lista de adapters. Cada item: ID, displayName, StatusDot, descrição curta em mono dim. Hover destaca em cyan.
- Indisponíveis aparecem disabled (opacity 0.4) com label "// UNAVAILABLE".

---

## MVP — ordem de implementação

Faça **nesta ordem**, parando ao final de cada etapa pra eu validar.

### Etapa 1 — Bootstrap + design system foundation

1. **Backend**: Spring Boot 3, Java 18, Maven. Dep: `spring-boot-starter-web`. `HealthController` com `GET /api/health` → `{"status":"ok","ts":<epoch>}`. `CorsConfig` permitindo `http://localhost:5173`.
2. **Frontend**: Vite + React + TS. Instalar Tailwind, configurar tema custom com os tokens listados acima. Importar Google Fonts (Rajdhani, JetBrains Mono, Inter) no `index.html`.
3. **Theme files**: `theme/tokens.css` com CSS variables, `theme/globals.css` com reset + base body usando `bg-base` + grid background.
4. **Componentes base** em `components/ui/`: implementar `Frame`, `Panel`, `Button`, `Input`, `Textarea`, `StatusDot`, `Badge`, `Spinner`, `KBD`, `GridBackground`, `ScanlineOverlay`. Cada um com props tipadas e variantes.
5. **AppShell**: `Sidebar` (com logo, nav, system status placeholder), `TopBar` com relógio funcional. React Router com 3 rotas (`/chat`, `/jira`, `/settings`) — páginas placeholder mostrando `<Frame title="// CHAT">CHAT PAGE STUB</Frame>` etc.
6. **Health integration**: SystemStatus na sidebar faz polling de `/api/health` a cada 10s e mostra StatusDot online/offline.
7. **README.md** na raiz com pré-requisitos (Java 18+, Maven, Node 20+, Claude Code CLI), comandos pra rodar, screenshot text-art da estrutura.
8. Vite proxy `/api` → `localhost:8080`.
9. Commit: `chore: bootstrap backend, design system and app shell`.

**Critério de aceite**: ao rodar `mvn spring-boot:run` e `npm run dev`, abrir `localhost:5173` mostra o shell completo (sidebar, topbar, grid background, brackets nos frames), navegação entre as 3 rotas funciona, StatusDot do backend está verde. **PARE AQUI.**

### Etapa 2 — Adapter de LLM (síncrono) + ChatWindow visual

1. **Backend**: definir `LlmAdapter`, `LlmAdapterRegistry`, classes `ChatRequest`, `ChatChunk`. Implementar `ClaudeCodeCliAdapter` síncrono — `chat()` executa `claude -p "<prompt>"` via `ProcessBuilder`, captura stdout completo, retorna single chunk. Path do binário em `claude.cli.path` (default `claude`).
2. Stubs `GeminiCliAdapter`, `CodexCliAdapter`, `AnthropicApiAdapter` retornando `isAvailable() = false`.
3. `ChatController` com `POST /api/chat` recebendo `{adapterId, conversationId, message}` e retornando `{response, latencyMs, tokens?}`. `ChatService` mantém `ConversationStore` em memória (Map<id, List<Message>>).
4. Endpoint `GET /api/llm/adapters` retornando lista.
5. Teste `ClaudeCodeCliAdapterTest` mockando `Process` (use `ProcessBuilder` injetável ou abstrair com interface).
6. **Frontend**: implementar `chat/` components — `ChatWindow`, `ConversationList`, `MessageList`, `MessageBubble`, `Composer`, `AdapterSelector`, `ThinkingIndicator`. Página Chat com layout em duas colunas. Estado em `chatStore` (zustand): `conversations`, `activeId`, `adapters`, `selectedAdapter`. `useChat()` hook com TanStack Query/Mutation.
7. Funcionalmente: usuário digita, clica TRANSMIT, vê ThinkingIndicator, depois mensagem do assistant chega de uma vez (sem streaming ainda).
8. Markdown rendering com `react-markdown` + `remark-gfm` + syntax highlighting custom escuro com cyan.
9. Commit: `feat: llm adapter layer and chat ui`.

**Critério de aceite**: enviar mensagem, ver resposta do Claude Code chegando completa, conversa fica na sidebar, adapter selecionável (mesmo que só 1 disponível). **PARE.**

### Etapa 3 — Streaming via WebSocket

1. **Backend**: trocar `claude -p` por `claude --output-format stream-json` lendo linhas do stdout do processo. Cada linha parseia JSON (eventos `message_start`, `content_block_delta`, `message_stop`, etc.) e emite `ChatChunk` no Flux. Configurar Spring WebSocket. Endpoint `/ws/chat` aceita mensagens JSON `{type:"start", adapterId, conversationId, message}` e envia chunks de volta.
2. **Frontend**: `api/ws.ts` com client WebSocket reconectável. Hook `useChatStream()`. Última `MessageBubble` do tipo assistant é "ativa" e recebe chunks acumulando texto. `<StreamingCursor>` aparece. Header da bubble mostra "STREAMING" + tokens incrementais.
3. Cancel: botão `STOP` aparece durante streaming (substitui TRANSMIT), envia mensagem `{type:"abort"}` que mata o subprocesso no backend.
4. Commit: `feat: websocket streaming for chat`.

**Critério de aceite**: tokens aparecem em tempo real, stop funciona. **PARE.**

### Etapa 4 — Jira (leitura)

1. **Backend**: `JiraClient` usando `RestClient` do Spring 6 ou `WebClient`. Auth básica com email + API token (Atlassian Cloud). Config em `jira.url`, `jira.email`, `jira.token` (`application-local.yml` no .gitignore + suporte a env vars). Endpoints: `GET /api/jira/issues?jql=...&maxResults=20`, `GET /api/jira/issues/{key}`, `GET /api/jira/issues/{key}/comments`. DTOs ricos (description em ADF — converter pra markdown via lib `atlassian-document-format-to-markdown` ou implementação simples).
2. **Frontend**: `pages/JiraPage.tsx` com layout 40/60. `JqlSearchBar` com chips de sugestões. `IssueList` com `IssueCard` e skeletons. `IssueDetail` com tabs (Description, Acceptance, Comments, Meta). `JiraStatusBadge` mapeando status name → cor. Estado de issue selecionada local na página. Empty states ASCII.
3. Tratamento de erro: 401/403 mostra Frame magenta linkando pra Settings.
4. Commit: `feat: jira read integration`.

**Critério de aceite**: configurar credenciais, buscar issues, ver detalhe completo com tabs. **PARE.**

### Etapa 5 — Issue → contexto do chat

1. Botão `[ SEND TO CHAT ]` na `IssueDetail` cria nova conversa, injeta primeira mensagem do tipo `jira-context` com payload formatado:
   ```
   // JIRA CONTEXT INJECTED
   KEY: PROJ-123
   TITLE: ...
   STATUS: ...
   DESCRIPTION:
   ...
   ACCEPTANCE CRITERIA:
   - ...
   RECENT COMMENTS:
   - [author, date]: ...
   ```
2. Navega pra `/chat` com a sessão nova ativa. Chip `[ ANEXED // PROJ-123 ]` aparece no header da conversa, clicável pra abrir slide-in com a issue.
3. Estado da conversa guarda `attachedIssue?: {key, url}`.
4. Commit: `feat: inject jira issue as chat context`.

**PARE.**

### Etapa 6 — Postar comentário no Jira

1. **Backend**: `POST /api/jira/issues/{key}/comments` com body `{body}`. Envia ADF mínimo (paragraph com texto plano ou markdown→ADF se trivial).
2. **Frontend**: no header do chat, se houver issue anexada, botão `[ POST SUMMARY TO JIRA ]` (ghost). Ao clicar:
   - Pede confirmação em modal Frame com textarea editável pré-preenchida com prompt enviado ao LLM ("Resuma a conversa em até 6 bullets...").
   - Após geração, mostra preview ADF→markdown e botões `[ POST ]` / `[ CANCEL ]`.
   - Sucesso: toast FUI no canto inferior direito "// COMMENT POSTED // PROJ-123".
3. Commit: `feat: post chat summary as jira comment`.

**Critério de aceite final**: fluxo completo funciona — abrir Jira, escolher issue, mandar pro chat, conversar, postar resumo de volta.

---

## Convenções

- **Testes:** JUnit 5 + Mockito no backend (mínimo: `ClaudeCodeCliAdapter` com `Process` mockado, `JiraService` com cliente mockado, `ChatController` WS com `WebSocketTestClient`). Vitest + React Testing Library no frontend (cobrir ao menos `Frame`, `Button`, `MessageBubble`, `IssueCard`).
- **Lint/format:** Spotless + google-java-format no backend. ESLint + Prettier no frontend.
- **Config sensível:** nunca hardcoded. `application.yml` só com defaults; `application-local.yml` (gitignored) ou env vars: `JIRA_URL`, `JIRA_EMAIL`, `JIRA_TOKEN`, `CLAUDE_CLI_PATH`.
- **Logs:** SLF4J no backend. No frontend, `console.debug` com prefixo `[hefesto:<modulo>]`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `chore:`, `test:`, `style:`).
- **Branch:** trabalhar em `main` no MVP; um commit limpo por etapa (squash se necessário).
- **Acessibilidade:** apesar do estilo FUI, manter contraste mínimo AA, focus rings visíveis (cyan glow), navegação por teclado funcional, `aria-label` em ícones.

## Não-objetivos (NÃO faça no MVP)

- Login, multi-usuário, RBAC.
- Banco de dados.
- Histórico persistente entre reinícios do backend.
- Implementar adapters além do Claude Code (deixe interfaces e stubs).
- CI/CD, Dockerfile, deploy.
- Editor de código no navegador, RAG, embeddings, file uploads.
- Versão mobile/responsiva (desktop-first; só não pode quebrar feio em ≥1280px).

## Primeira tarefa concreta

Execute **apenas a Etapa 1**: bootstrap backend + frontend + design system + AppShell completo + componentes UI base + 3 rotas com placeholders + health check integrado.

Pare nesse ponto, liste os arquivos criados, mostre uma descrição do que renderiza ao abrir `localhost:5173`, e me espere validar antes de seguir.
