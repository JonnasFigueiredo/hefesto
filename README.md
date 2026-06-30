# Hefesto

> Plataforma de QA/PO assistida por IA, exposta como servidor MCP.
> Analisa requisitos (texto ou imagem de tela), cria e revisa histórias no Jira,
> gera casos de teste e mede cobertura — pela interface web ou por linguagem
> natural via MCP. Conecta múltiplos modelos de LLM (Claude e modelos locais `.gguf`).

```
   _   _   _____   _____   _____   _____   _____   _____
  | | | | |  ___| |  ___| | ____| |  ___| |_   _| |  _  |
  | |_| | | |__   | |__   | |__   | |__     | |   | | | |
  |  _  | |  __|  |  __|  |  __|  |___ \    | |   | | | |
  | | | | | |___  | |     | |___   ___) |   | |   | |_| |
  |_| |_| |_____| |_|     |_____| |____/    |_|   |_____|
```

[![License: MIT](https://img.shields.io/badge/License-MIT-cyan)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17+-cyan)](#requisitos)
[![Node 20+](https://img.shields.io/badge/Node-20+-cyan)](#requisitos)
[![MCP server](https://img.shields.io/badge/MCP-server-cyan)](./docs/MCP.md)

<!-- ╭───────────────────────────────────────────────────────────────╮
     │  MÍDIA — vídeo de demonstração (hero)                          │
     │  Coloque o arquivo em docs/media/ e troque o bloco abaixo por: │
     │  <p align="center"><img src="docs/media/demo.gif" width="820"  │
     │     alt="Demonstração do Hefesto"></p>                         │
     ╰───────────────────────────────────────────────────────────────╯ -->
<p align="center"><sub><i>[ vídeo de demonstração — adicionar em docs/media/ ]</i></sub></p>

---

## Sumário

- [Pra quem é](#pra-quem-é)
- [O que faz](#o-que-faz)
- [Servidor MCP](#servidor-mcp)
- [Fluxos de trabalho](#fluxos-de-trabalho)
- [Requisitos](#requisitos)
- [Setup em 5 minutos](#setup-em-5-minutos)
- [Configuração](#configuração)
- [Agentes especialistas](#agentes-especialistas)
- [Arquitetura](#arquitetura)
- [Stack](#stack)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Roadmap](#roadmap)
- [Contribuindo](#contribuindo)
- [Licença](#licença)

---

## Pra quem é

Hefesto é para **QA's, devs e POs** que usam IA no dia a dia e perdem tempo
costurando contexto manualmente entre ferramentas: copiar requisito, colar no
chat, abrir o Jira, criar história, gerar casos, anexar evidência, montar
documento.

O Hefesto concentra esse ciclo em um só lugar — pela interface web ou direto do
cliente MCP (Claude Code/Desktop), por linguagem natural.

## O que faz

### Servidor MCP

O backend **é também um servidor MCP** (sobre SSE). Qualquer cliente MCP — Claude
Code, Claude Desktop — opera o Hefesto por linguagem natural: **14 tools**,
**2 resources** e **3 prompts**. Exemplo: *"revise a prontidão da PROJ-123 e gere
os casos de teste como subtarefas"*. Referência completa em
[docs/MCP.md](./docs/MCP.md).

### Fluxos de IA do ciclo QA/PO

- **Rascunho de história** a partir de requisitos em texto **ou de uma imagem de
  tela/design** (visão) — preenche título, descrição e critérios de aceite.
- **Revisão de prontidão (INVEST)** — score de 0 a 100, gaps, riscos e critérios
  faltantes; opcionalmente registra a revisão como comentário no Jira.
- **Geração de casos de teste**, criando **uma subtarefa por caso** no Jira.
- **Matriz de cobertura** — cruza os critérios de aceite com os casos de teste e
  aponta os critérios **descobertos**.

### Jira completo (ler e escrever)

Busca por JQL, leitura de descrição/critérios/comentários, e escrita: **criar,
atualizar, transicionar e comentar** issues e subtarefas (REST API v3). Projeto e
tipo de issue vêm de listas, com tipos já localizados (ex.: "História"/"Subtask").

### Multi-LLM plugável

Claude Code (oficial), **modelos locais `.gguf`** via `llama-server` (llama.cpp,
100% offline), GitHub Copilot via extensão VS Code, e stubs de Gemini/Codex/
Anthropic. A seleção é feita na interface; adicionar um modelo é implementar a
interface `LlmAdapter`.

### Agentes, casos de teste e relatórios

Cada `.md` em `agentes/` é um especialista (com hot reload). Extração automática
de casos `TC-NNN` em cards interativos, evidências (screenshots/logs) por caso em
SQLite, relatório PDF com sumário e evidências inline, e dashboard de Analytics
(KPIs, latência, série temporal). Persistência local em SQLite, sem infraestrutura.

<!-- MÍDIA — screenshots da interface (chat, Jira, analytics).
     Sugestão: docs/media/ui-chat.png, ui-jira.png, ui-analytics.png -->
<p align="center"><sub><i>[ screenshots da interface — adicionar em docs/media/ ]</i></sub></p>

## Servidor MCP

O Hefesto expõe um servidor MCP sobre SSE em `GET /sse` + `POST /mcp/message`.
Conecte um cliente MCP e opere tudo por linguagem natural:

```bash
# Claude Code (transporte SSE nativo)
claude mcp add --transport sse hefesto http://localhost:8080/sse
```

- **14 tools** — ex.: `jira_search`, `jira_create_story`, `review_story`,
  `generate_test_cases`, `create_test_subtasks`, `coverage_report`, `chat`.
- **2 resources** — `hefesto://agents`, `hefesto://models`.
- **3 prompts** — `revisar_historia`, `gerar_testes_no_jira`, `rascunhar_historia`.

Referência completa: [docs/MCP.md](./docs/MCP.md).

## Fluxos de trabalho

### Fluxo PO → QA (com IA)

Na aba **Jira**, do requisito à cobertura:

1. **Nova história** — cole o requisito (ou anexe um print da tela). A IA gera o
   rascunho (título, descrição, critérios). Selecione projeto e tipo e crie.
2. **Revisar (INVEST)** — score de prontidão, gaps, riscos e critérios faltantes;
   com opção de comentar no Jira.
3. **Gerar testes no Jira** — os casos de teste viram subtarefas da história.
4. **Cobertura** — matriz critérios × testes, apontando o que ficou descoberto.

O mesmo fluxo está disponível por linguagem natural via MCP (ver
[docs/MCP.md](./docs/MCP.md)).

<!-- MÍDIA — vídeo do fluxo PO → QA de ponta a ponta. docs/media/fluxo-po-qa.* -->
<p align="center"><sub><i>[ vídeo do fluxo PO → QA — adicionar em docs/media/ ]</i></sub></p>

### Fluxo de execução de testes (clássico)

1. Crie uma sessão de chat e selecione o agente QA Sênior.
2. Anexe o manual da funcionalidade e a issue do Jira como contexto.
3. Peça os casos de teste — eles chegam como cards interativos abaixo da resposta.
4. Execute cada caso, marque aprovado/reprovado e anexe a evidência.
5. Gere o relatório (HTML pronto para PDF) com sumário e evidências inline.

## Requisitos

- **Java 17+** — backend
- **Maven 3.9+** — build do backend
- **Node.js 20 LTS+** — frontend
- **Claude Code CLI** instalado e logado (`claude --version`) — adapter padrão e
  modelo de visão usado no rascunho a partir de imagem
- *(Opcional)* **Conta Atlassian Cloud** com API token, para integração Jira
- *(Opcional)* **`llama-server`** (do [llama.cpp](https://github.com/ggml-org/llama.cpp))
  e modelos `.gguf`, para rodar modelos locais offline
- *(Opcional)* **VS Code com GitHub Copilot** + extensão Hefesto Bridge

## Setup em 5 minutos

```bash
# 1. Clone
git clone https://github.com/SEU_USUARIO/hefesto.git
cd hefesto

# 2. Backend
cd hefesto-backend
mvn spring-boot:run
# Sobe em http://localhost:8080

# 3. Frontend (em outro terminal)
cd ../hefesto-frontend
npm install
npm run dev
# Abre http://localhost:5173 no navegador
```

A pasta [`agentes/`](./agentes) na raiz vem com 9 agentes prontos. Edite os `.md`
ou crie novos — recarrega com `curl -X POST http://localhost:8080/api/agents/reload`
(sem reiniciar).

### (Opcional) Extensão VS Code para Copilot

```bash
cd hefesto-bridge-extension
npm install
npm run package
code --install-extension hefesto-bridge-0.1.0.vsix
```

Veja [hefesto-bridge-extension/README.md](./hefesto-bridge-extension/README.md)
para distribuição interna.

## Configuração

Credenciais sensíveis ficam em
`hefesto-backend/src/main/resources/application-local.yml` (gitignored).

Crie esse arquivo para usar Jira:

```yaml
claude:
  cli:
    path: /caminho/do/claude.cmd      # Windows: C:\Users\...\npm\claude.cmd
                                       # Mac/Linux: /usr/local/bin/claude

jira:
  url: https://suaempresa.atlassian.net
  email: voce@empresa.com
  token: "API_TOKEN_GERADO_NO_ATLASSIAN_ID"
```

API token: gere em [id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens).
Reinicie o backend após salvar.

## Agentes especialistas

Cada agente é um **arquivo `.md` em [`agentes/`](./agentes)**. O nome do arquivo
aparece direto na seleção — ex.: `QAseniorAgent.md` é o agente.

```markdown
---
description: "Designer sênior de casos de teste"
emoji: "🧪"
defaultPromptTemplate: "Analise e gere casos de teste."
extractsTestCases: true
---

Você é um QA Specialist sênior. Sua missão é...
[system prompt completo]
```

Documentação completa: [agentes/README.md](./agentes/README.md).

**Inclusos por padrão:**

| Arquivo | Persona |
|---|---|
| `Default.md` | Sem persona — assistente geral |
| `QAseniorAgent.md` | QA sênior, gera casos de teste estruturados |
| `EscritorDeHistorias.md` | Escreve rascunho de história (JSON) a partir de requisito ou imagem |
| `RevisorDeHistorias.md` | Avalia prontidão (INVEST) com score, gaps e riscos |
| `AnalistaDeCobertura.md` | Cruza critérios de aceite com casos de teste (cobertura) |
| `AnalistaDeNegocios.md` | Avalia histórias do Jira (clareza, completude, gaps) |
| `TechWriter.md` | Documentação técnica em formato padrão |
| `Arquiteto.md` | Análise técnica com 2-3 abordagens e trade-offs |
| `CodeReviewer.md` | Revisão de código (bugs, segurança, performance) |

## Arquitetura

```
[ Browser ]            [ Cliente MCP ]            [ Backend :8080 ]        [ SQLite ]
localhost:5173         Claude Code/Desktop                                hefesto.db
   React 18  ─WS/REST─▶                  ─MCP/SSE─▶  ┌──────────────────┐
   Tailwind                                          │ REST + WebSocket │
                                                     │ Servidor MCP     │ 14 tools
                                                     │  (Spring AI)     │ 2 res · 3 prompts
                                                     ├──────────────────┤
                                                     │ LlmAdapterRegistry
                                                     │  ├ Claude Code (CLI, visão)
                                                     │  ├ llama-server (.gguf local)
                                                     │  ├ Copilot (extensão VS Code)
                                                     │  └ Gemini/Codex/Anthropic (stubs)
                                                     ├──────────────────┤
                                                     │ JiraService (REST v3, ler+escrever)
                                                     │ Story/Review/Coverage Services (IA)
                                                     │ PromptBuilder · AgentRegistry
                                                     │ TestCase Extractor · Telemetry
                                                     │ Report Service (HTML → PDF)
                                                     └──────────────────┘
```

A UI (REST/WS) e os clientes MCP (SSE) caem nos **mesmos serviços** — a lógica é
compartilhada. A camada de adapters é plugável: adicionar um modelo é implementar
`LlmAdapter`. Detalhes em [hefesto-backend/README.md](./hefesto-backend/README.md)
e [docs/MCP.md](./docs/MCP.md).

## Stack

**Backend** — Java 17, Spring Boot 3.4, Spring AI 1.0 (servidor MCP), Spring Data
JDBC, SQLite, WebSocket, Jackson, JUnit 5.

**Frontend** — React 18, TypeScript strict, Vite, Tailwind CSS, TanStack Query,
Zustand, react-markdown, Framer Motion, Lucide.

**Extensão VS Code** — TypeScript, VS Code Language Model API.

## Estrutura do repositório

```
hefesto/
├── README.md                      # você está aqui
├── APRESENTACAO.md                # pitch resumido para apresentações
├── CONTRIBUTING.md                # como contribuir
├── LICENSE                        # MIT
│
├── docs/
│   ├── MCP.md                     # referência do servidor MCP (tools/resources/prompts)
│   └── media/                     # imagens e vídeos do README
│
├── scripts/
│   └── start-llama-servers.bat    # sobe llama-server por modelo .gguf
│
├── agentes/                       # arquivos .md dos agentes
│   ├── README.md                  # como criar/editar agentes
│   ├── Default.md
│   ├── QAseniorAgent.md
│   ├── EscritorDeHistorias.md     # rascunho de história (texto/imagem)
│   ├── RevisorDeHistorias.md      # revisão INVEST
│   ├── AnalistaDeCobertura.md     # matriz de cobertura
│   ├── AnalistaDeNegocios.md
│   ├── TechWriter.md
│   ├── Arquiteto.md
│   └── CodeReviewer.md
│
├── hefesto-backend/               # Spring Boot 3 + Spring AI (MCP) + SQLite
│   └── README.md                  # docs específicas
│
├── hefesto-frontend/              # React + Vite + Tailwind
│   └── README.md                  # docs específicas
│
└── hefesto-bridge-extension/      # extensão VS Code (Copilot bridge)
    └── README.md                  # docs específicas
```

## Roadmap

**Entregue:**

- [x] Chat com streaming via WebSocket, persistido em SQLite
- [x] Multi-adapter de LLM: Claude Code + modelos locais `.gguf` (llama-server) + Copilot
- [x] Agentes em arquivos `.md` (hot reload)
- [x] Servidor MCP (14 tools, 2 resources, 3 prompts) sobre SSE
- [x] Jira completo: ler e escrever (criar/atualizar/transicionar/comentar/subtarefas)
- [x] Rascunho de história com IA a partir de texto ou imagem de tela (visão)
- [x] Revisão de prontidão (INVEST) com score, gaps e riscos
- [x] Geração de casos de teste em subtarefas do Jira, em um passo
- [x] Matriz de cobertura (critérios de aceite × testes)
- [x] Extração de casos `TC-NNN`, evidências por caso, relatório HTML/PDF
- [x] Dashboard de Analytics (KPIs, latência, série temporal)

**Planejado:**

- [ ] Postar resumo de execução de testes no Jira automaticamente
- [ ] Templates de export (Zephyr, Xray, TestRail)
- [ ] Tools MCP extras: `estimate_story`, `find_similar` (deduplicação)
- [ ] Streaming token-a-token via API direta
- [ ] Hub central para agregar uso de múltiplas instâncias

## Contribuindo

Veja [CONTRIBUTING.md](./CONTRIBUTING.md). Resumo: branches com prefixo
`feat/`/`fix/`/`docs/`, commits convencionais, testes para adapters e parsers,
sem credenciais comitadas.

Issues e PRs são bem-vindos. Para agentes novos, abra um PR adicionando um `.md`
na pasta `agentes/` — fica versionado junto com o projeto.

## Licença

[MIT](./LICENSE) — use, modifique, distribua. Atribuição apreciada, não obrigatória.

---

> Detalhes técnicos por módulo:
> - [docs/MCP.md](./docs/MCP.md) — servidor MCP: tools, resources, prompts, como conectar
> - [hefesto-backend/README.md](./hefesto-backend/README.md) — pacotes Java, contratos de adapter, persistência, testes
> - [hefesto-frontend/README.md](./hefesto-frontend/README.md) — design system, componentes, theming, estado
> - [hefesto-bridge-extension/README.md](./hefesto-bridge-extension/README.md) — build/install/distribuição interna
> - [agentes/README.md](./agentes/README.md) — sistema de agentes em arquivos
