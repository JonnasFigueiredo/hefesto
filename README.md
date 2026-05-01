# Hefesto

> **Console de QA com IA — open source.**
> Conecta múltiplos modelos de LLM, integra Jira, gera casos de teste estruturados a partir de manuais e histórias, e produz relatórios em PDF com evidências anexadas.

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

---

## Sumário

- [Pra quem é](#pra-quem-é)
- [O que faz](#o-que-faz)
- [Demo: o fluxo do QA](#demo-o-fluxo-do-qa)
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

Hefesto foi feito pensando em **QA's e devs que usam IA no dia a dia**
mas perdem tempo costurando contexto manualmente entre ferramentas:
copiando manual, colando no chat, abrindo o Jira, copiando história,
gerando casos, executando, anexando screenshot, gerando documento, etc.

Se você já passou por isso, esse projeto é pra você.

## O que faz

- 🤖 **Multi-LLM**: Claude Code (oficial), GitHub Copilot via VS Code, Gemini, Codex — você escolhe no dropdown
- 🎭 **Agentes em arquivos**: cada `.md` na pasta `agentes/` vira um especialista. QA Sênior, Tech Writer, Arquiteto, Analista de Negócios, Code Reviewer já vêm prontos. Edita ou cria mais sem mexer no código.
- 📎 **Anexos como contexto**: arrasta `.txt`, `.md`, `.json`, `.yml` e o conteúdo entra na conversa
- 🎫 **Jira integrado**: busca por JQL, anexa issue como contexto, lê descrição/critérios/comentários
- 🧪 **Casos de teste estruturados**: agentes marcados com `extractsTestCases: true` produzem casos no formato `TC-NNN`. O sistema parseia e transforma em **cards interativos** abaixo da resposta
- 📸 **Evidências por caso**: drag-and-drop ou click pra anexar screenshots, logs, JSONs em cada caso individualmente — tudo persistido em SQLite
- 📄 **Relatório PDF**: 1 clique gera HTML formatado com cabeçalho (Jira KEY, data, escopo), resumo (X passou / Y falhou), cada caso com status colorido + screenshots inline. `Ctrl+P` exporta como PDF
- 📊 **Analytics**: dashboard com KPIs, latência, taxa de erro, série temporal — telemetria automática
- 💾 **Persistência local**: SQLite num arquivo único, sem infra

## Demo: o fluxo do QA

1. Abre Hefesto
2. Cria sessão e seleciona agente **🧪 QAseniorAgent.md**
3. Anexa o manual da funcionalidade (`manual.txt`)
4. Anexa issue Jira `PROJ-123` (busca pelo JQL ou cola a KEY)
5. Manda: *"Gere casos de teste cobrindo essa funcionalidade"*
6. Resposta chega — **embaixo da bubble aparece painel com cards de cada caso**
7. Pra cada caso:
   - Lê passos, executa manualmente
   - Marca **✓ APROVADO** ou **✗ REPROVADO**
   - Anexa screenshot da execução (drag-and-drop)
8. Clica **📄 GERAR RELATÓRIO**
9. Nova aba abre com PDF-ready: header com Jira KEY destacado, sumário, cada caso com evidências
10. **Ctrl+P → Salvar como PDF** → anexa no ticket Jira

Ciclo completo em minutos, tudo no navegador.

## Requisitos

- **Java 17+** — backend
- **Maven 3.9+** — build do backend
- **Node.js 20 LTS+** — frontend
- **Claude Code CLI** instalado e logado (`claude --version`) — pra usar o adapter padrão
- *(Opcional)* **Conta Atlassian Cloud** com API token, pra integração Jira
- *(Opcional)* **VS Code com GitHub Copilot** + extensão Hefesto Bridge, pra usar Copilot da empresa

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

A pasta [`agentes/`](./agentes) na raiz vem com 6 agentes prontos.
Edite os `.md` ou crie novos — recarrega com
`curl -X POST http://localhost:8080/api/agents/reload` (sem precisar reiniciar).

### (Opcional) Extensão VS Code pra Copilot

```bash
cd hefesto-bridge-extension
npm install
npm run package
code --install-extension hefesto-bridge-0.1.0.vsix
```

Veja [hefesto-bridge-extension/README.md](./hefesto-bridge-extension/README.md)
pra distribuição interna na empresa.

## Configuração

Credenciais sensíveis ficam em
`hefesto-backend/src/main/resources/application-local.yml` (gitignored).

Crie esse arquivo se quiser usar Jira:

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

Cada agente é um **arquivo `.md` em [`agentes/`](./agentes)**. O nome do
arquivo aparece direto no dropdown — ex: `QAseniorAgent.md` é o agente.

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
| `AnalistaDeNegocios.md` | Avalia histórias do Jira (clareza, completude, gaps) |
| `TechWriter.md` | Documentação técnica em formato padrão |
| `Arquiteto.md` | Análise técnica com 2-3 abordagens e trade-offs |
| `CodeReviewer.md` | Revisão de código (bugs, segurança, performance) |

## Arquitetura

```
[ Browser ]                 [ Backend ]
localhost:5173   ─ WS/REST ─ localhost:8080      [ SQLite ]
   FUI UI                       │                hefesto.db
   React 18                     ├── Adapters (LLM)
   Tailwind                     │     ├── Claude Code CLI (subprocess)
                                │     ├── GitHub Copilot (via VS Code extension)
                                │     ├── Gemini, Codex, Anthropic API (stubs)
                                │
                                ├── JiraClient (REST API v3)
                                ├── PromptBuilder (agente + anexos + Jira)
                                ├── AgentRegistry (lê pasta agentes/*.md)
                                ├── TestCase Extractor (regex sobre markdown)
                                ├── Telemetry (eventos em SQLite)
                                └── Report Service (HTML → PDF)
```

Toda a camada de adapters é plugável — adicionar novo modelo é
implementar a interface `LlmAdapter`. Detalhes em
[hefesto-backend/README.md](./hefesto-backend/README.md).

## Stack

**Backend** — Java 17, Spring Boot 3.4, Spring Data JDBC, SQLite, WebSocket, Jackson, JUnit 5.

**Frontend** — React 18, TypeScript strict, Vite, Tailwind CSS, TanStack Query, Zustand, react-markdown, Framer Motion, Lucide.

**Extensão VS Code** — TypeScript, VS Code Language Model API.

## Estrutura do repositório

```
hefesto/
├── README.md                      # você está aqui
├── APRESENTACAO.md                # pitch resumido pra apresentações
├── CONTRIBUTING.md                # como contribuir
├── LICENSE                        # MIT
│
├── agentes/                       # arquivos .md dos agentes
│   ├── README.md                  # como criar/editar agentes
│   ├── Default.md
│   ├── QAseniorAgent.md
│   ├── AnalistaDeNegocios.md
│   ├── TechWriter.md
│   ├── Arquiteto.md
│   └── CodeReviewer.md
│
├── hefesto-backend/               # Spring Boot 3 + SQLite
│   └── README.md                  # docs específicas
│
├── hefesto-frontend/              # React + Vite + Tailwind
│   └── README.md                  # docs específicas
│
└── hefesto-bridge-extension/      # extensão VS Code (Copilot bridge)
    └── README.md                  # docs específicas
```

## Roadmap

Já implementado:

- ✅ Chat com streaming via WebSocket, persistido em SQLite
- ✅ Multi-adapter de LLM com Claude Code rodando + Copilot via extensão
- ✅ Agentes em arquivos `.md` (hot reload)
- ✅ Upload de anexos como contexto
- ✅ Integração Jira (busca, leitura, vínculo)
- ✅ Extração automática de casos de teste estruturados
- ✅ Anexar evidências (screenshots, logs) por caso
- ✅ Relatório HTML/PDF com evidências inline
- ✅ Dashboard de Analytics (KPIs, latência, série temporal)

Em planejamento:

- 🔜 Postar resumo de execução no Jira automaticamente
- 🔜 Templates de export (Zephyr, Xray, TestRail)
- 🔜 Streaming token-a-token via API direta
- 🔜 Hub central pra agregar uso de múltiplas instâncias
- 🔜 Suporte a anexos binários (PDF, imagens) como contexto

## Contribuindo

Veja [CONTRIBUTING.md](./CONTRIBUTING.md). Resumo: branches com
prefixo `feat/`/`fix/`/`docs/`, commits convencionais, testes pra
adapters e parsers, sem credenciais comitadas.

Issues e PRs bem-vindos. Pra agentes novos, abra um PR adicionando
`.md` na pasta `agentes/` — fica versionado junto com o projeto.

## Licença

[MIT](./LICENSE) — use, modifique, distribua. Atribuição apreciada
mas não obrigatória.

---

> Para detalhes técnicos por módulo:
> - [hefesto-backend/README.md](./hefesto-backend/README.md) — pacotes Java, contratos de adapter, persistência, testes
> - [hefesto-frontend/README.md](./hefesto-frontend/README.md) — design system, componentes, theming, estado
> - [hefesto-bridge-extension/README.md](./hefesto-bridge-extension/README.md) — build/install/distribuição interna
> - [agentes/README.md](./agentes/README.md) — sistema de agentes em arquivos
