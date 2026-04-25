# hefesto-frontend

> Frontend React + TypeScript do Hefesto. Estética **FUI / wireframe / cyberpunk técnico**, single-page com 3 rotas (Chat, Jira, Settings), comunicação com o backend via REST e WebSocket.

## Sumário

- [Stack](#stack)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Como rodar](#como-rodar)
- [Design system](#design-system)
- [Biblioteca de componentes](#biblioteca-de-componentes)
- [App shell e navegação](#app-shell-e-navegação)
- [Estado da aplicação](#estado-da-aplicação)
- [Comunicação com o backend](#comunicação-com-o-backend)
- [Convenções](#convenções)
- [Testes](#testes)

---

## Stack

| Pacote | Versão | Uso |
|--------|--------|-----|
| `react` | 18.3 | Framework UI. |
| `react-dom` | 18.3 | Renderização no DOM. |
| `typescript` | 5.6 | Tipagem estrita. |
| `vite` | 5.4 | Bundler dev/prod. |
| `@vitejs/plugin-react` | 4.3 | Plugin React do Vite. |
| `tailwindcss` | 3.4 | Estilo utility-first. |
| `react-router-dom` | 6.27 | Roteamento client-side. |
| `@tanstack/react-query` | 5.59 | Estado de servidor (cache, refetch). |
| `zustand` | 5.0 | Estado de UI local (chat, settings). |
| `framer-motion` | 11.11 | Animações de transição. |
| `lucide-react` | 0.453 | Ícones em linha fina. |

---

## Estrutura de pastas

```
hefesto-frontend/
├── package.json
├── vite.config.ts             # alias @/ + proxy /api e /ws
├── tsconfig.json              # strict TypeScript
├── tsconfig.node.json
├── tailwind.config.ts         # tema custom: cores via CSS vars, fontes, animações
├── postcss.config.js
├── index.html                 # imports de Google Fonts (Rajdhani, Mono, Inter)
└── src/
    ├── main.tsx               # entrada React
    ├── App.tsx                # QueryClient + Router + AppShell
    ├── routes.tsx             # rotas / → /chat
    ├── vite-env.d.ts
    │
    ├── theme/
    │   ├── tokens.css         # CSS variables de design tokens
    │   └── globals.css        # reset, scrollbar, glow utils, keyframes
    │
    ├── lib/
    │   └── cn.ts              # helper de classnames
    │
    ├── api/
    │   ├── http.ts            # fetch wrapper com tracking de latência
    │   ├── health.ts          # GET /api/health
    │   ├── chat.ts            # [Etapa 2+] HTTP do chat
    │   ├── ws.ts              # [Etapa 3+] cliente WebSocket
    │   ├── jira.ts            # [Etapa 4+] endpoints Jira
    │   └── adapters.ts        # [Etapa 4+] lista de adapters
    │
    ├── hooks/
    │   ├── useHealth.ts       # polling do /api/health
    │   ├── useClock.ts        # relógio que atualiza por segundo
    │   ├── useChat.ts         # [Etapa 2+]
    │   └── useChatStream.ts   # [Etapa 3+]
    │
    ├── store/                 # [Etapa 2+] zustand
    │   ├── chatStore.ts
    │   └── settingsStore.ts
    │
    ├── components/
    │   ├── ui/                # primitivos do design system
    │   │   ├── Frame.tsx
    │   │   ├── Panel.tsx
    │   │   ├── Button.tsx
    │   │   ├── Input.tsx
    │   │   ├── Textarea.tsx
    │   │   ├── StatusDot.tsx
    │   │   ├── Badge.tsx
    │   │   ├── Spinner.tsx
    │   │   ├── KBD.tsx
    │   │   ├── GridBackground.tsx
    │   │   ├── ScanlineOverlay.tsx
    │   │   └── index.ts
    │   ├── shell/             # layout permanente
    │   │   ├── AppShell.tsx
    │   │   ├── Sidebar.tsx
    │   │   ├── TopBar.tsx
    │   │   └── SystemStatus.tsx
    │   ├── chat/              # [Etapa 2+]
    │   │   ├── ChatWindow.tsx
    │   │   ├── MessageList.tsx
    │   │   ├── MessageBubble.tsx
    │   │   ├── Composer.tsx
    │   │   ├── AdapterSelector.tsx
    │   │   ├── ConversationList.tsx
    │   │   ├── ThinkingIndicator.tsx
    │   │   └── StreamingCursor.tsx
    │   └── jira/              # [Etapa 4+]
    │       ├── JqlSearchBar.tsx
    │       ├── IssueList.tsx
    │       ├── IssueCard.tsx
    │       ├── IssueDetail.tsx
    │       └── JiraStatusBadge.tsx
    │
    └── pages/
        ├── ChatPage.tsx
        ├── JiraPage.tsx
        └── SettingsPage.tsx
```

Pastas/arquivos com `[Etapa X]` ainda não existem.

---

## Como rodar

```bash
npm install     # primeira vez ou após mudar package.json
npm run dev     # http://localhost:5173 com HMR
npm run build   # build de produção em dist/
npm run preview # serve o dist/ pra teste
```

O Vite proxy intermedia chamadas pra `/api/*` e `/ws/*` direto pro backend em `localhost:8080` (configurado em `vite.config.ts`), então no dia a dia você acessa só `localhost:5173`.

> Em produção, o `dist/` deve ser servido por um servidor estático (nginx, ou pelo próprio Spring via `frontend-maven-plugin` na futura unificação). As rotas SPA precisam de fallback pra `index.html`.

---

## Design system

A estética do Hefesto é **FUI** (Fictional User Interface) — visual de HUD de filme sci-fi, terminal de nave, dashboard cyberpunk. Linhas finas, acentos neon, tipografia técnica, animações sutis de pulso.

### Tokens

Definidos em `src/theme/tokens.css` como CSS variables:

```css
--bg-base: #07090f;          /* fundo profundo */
--bg-elevated: #0d1320;      /* painéis */
--bg-overlay: #131b2e;       /* hover, inputs */

--border-dim: #1a2440;
--border: #2a3a66;
--border-accent: #00d4ff;

--text: #e6edf9;
--text-dim: #7a8aa8;
--text-muted: #4a5878;

--accent-cyan: #00d4ff;      /* primário, ações principais, glow */
--accent-amber: #ffb547;     /* warnings */
--accent-magenta: #ff3d8a;   /* erros, destrutivo */
--accent-green: #00ff9f;     /* sucesso, online */
```

**Por que CSS variables?** Permitem temas (light/dark, intensidade do glow, alternativas de paleta) sem rebuild do Tailwind. Toda cor de componente é referenciada via `var(--...)` ou `bg-[var(--...)]`.

### Tipografia

Três famílias, importadas do Google Fonts no `index.html`:

| Família | Uso | Classe Tailwind |
|---------|-----|-----------------|
| Rajdhani | Títulos, labels uppercase, KEYs de issue | `font-display` |
| JetBrains Mono | IDs, timestamps, dados técnicos, código | `font-mono` |
| Inter | Corpo de texto, mensagens de chat, descrições | `font-sans` (default) |

Convenção: `font-display` quase sempre vem com `uppercase tracking-[0.15em]` ou maior. `font-mono` em tamanhos pequenos (10–12px) pra meta-info.

### Efeitos

- **Glow**: aplicado em texto importante via classe `.glow-cyan` (ver `globals.css`).
- **Box glow**: `box-glow-cyan` adiciona shadow neon em focos.
- **Corner brackets**: o componente `Frame` desenha 4 brackets cyan nos cantos via pseudo-elementos absolutos.
- **Grid background**: `GridBackground` renderiza pontos sutis em radial-gradient, cobertos por uma máscara radial pra desvanecer nas bordas.
- **Scanline opcional**: `ScanlineOverlay` (off por padrão, toggle em settings futuro).

### Animações (definidas em `tailwind.config.ts`)

| Classe | Uso |
|--------|-----|
| `animate-pulse-dot` | StatusDot piscando |
| `animate-spin-slow` | Spinner HUD |
| `animate-scanline` | Linha cyan deslizando |
| `animate-shimmer` | Skeleton de carregamento |

Animações custom em `globals.css`: `crt-flicker` (opcional, intenso), `eq-bar` (equalizer pro ThinkingIndicator), `blink-cursor` (cursor de streaming).

---

## Biblioteca de componentes

Todos em `src/components/ui/`. Cada um aceita `className` pra extensão e tem variantes claras.

### `<Frame>`

Container com border 1px e 4 corner brackets. Aceita `title` (chip flutuante na borda superior) e `variant` (`default` cyan, `accent` cyan forte, `dim` mais apagado, `danger` magenta).

```tsx
<Frame title="// SESSION" variant="default">
  <p>Conteúdo</p>
</Frame>
```

### `<Panel>`

Bloco elevado sem brackets. Usado pra agrupar conteúdo dentro de um `Frame` ou como standalone neutro.

### `<Button>`

Variantes: `primary` (cyan com glow no hover), `secondary` (border-dim → cyan), `ghost`, `danger` (magenta). Tamanhos: `sm`, `md`. Sempre uppercase com letter-spacing alto.

```tsx
<Button variant="primary" icon={<Send size={14} />}>TRANSMIT</Button>
```

### `<Input>` e `<Textarea>`

Fundo `bg-overlay`, border 1px, glow cyan no focus. Prop `mono` troca pra JetBrains Mono (útil pra JQL, IDs).

### `<StatusDot>`

Bolinha 6px que pulsa quando `status="online"` ou `"pending"`. Variantes: `online`, `offline`, `pending`, `warning`, `unknown`.

### `<Badge>`

Chip pequeno em mono uppercase. Variantes por cor (default, cyan, amber, magenta, green, dim).

### `<Spinner>`

Arco rotativo SVG (não círculo cheio) — visual HUD.

### `<KBD>`

Estiliza tecla de atalho. Usado em hints como `<KBD>⌘</KBD>+<KBD>⏎</KBD>`.

### `<GridBackground>`

Grid de pontos cobrindo a viewport, com mask radial pra desvanecer. Posicionado `fixed` com `z-index: -10`.

### `<ScanlineOverlay>`

Linha horizontal cyan animada cobrindo a tela. Off por padrão; toggle via prop `enabled`.

---

## App shell e navegação

### `<AppShell>` (em `components/shell/AppShell.tsx`)

Layout permanente:

```
+----------+--------------------------------+
| Sidebar  | TopBar (h-10)                  |
| (240px)  +--------------------------------+
|          |                                |
|          |   Outlet (rota atual)          |
|          |                                |
|          |                                |
+----------+--------------------------------+
```

Inclui `<GridBackground />` global e `<ScanlineOverlay />` plugado mas desligado.

### `<Sidebar>`

- **Logo**: `[ HEFESTO ]` em Rajdhani uppercase tracking 0.3em, glow cyan, com tagline mono `// JIRA × LLM CONSOLE`.
- **Nav**: 3 itens com ícone + label, indicador esquerdo (border 2px cyan) quando ativo.
- **`<SystemStatus>`** no rodapé: lista 3 linhas mono — BACKEND status (StatusDot reagindo a `useHealth`), ADAPTER atual e VERSION.

### `<TopBar>`

- Breadcrumb dinâmico: `HEFESTO / <PÁGINA>`.
- Latência da última chamada HTTP (subscription via `subscribeLatency` em `api/http.ts`).
- Relógio UTC formato `2026.04.25 // 14:32:07 UTC` atualizando 1× por segundo.

### Rotas

Configuradas em `routes.tsx`:

| Path | Componente |
|------|-----------|
| `/` | redirect → `/chat` |
| `/chat` | `<ChatPage />` |
| `/jira` | `<JiraPage />` |
| `/settings` | `<SettingsPage />` |
| `*` | redirect → `/chat` |

---

## Estado da aplicação

### Estado de servidor — TanStack Query

Queries de servidor (saúde, lista de adapters, issues do Jira, mensagens) ficam em hooks customizados em `src/hooks/`. Cada hook usa `useQuery` ou `useMutation` com configuração apropriada.

Exemplo (`useHealth.ts`):

```ts
export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    refetchInterval: 10_000,
    staleTime: 5_000,
  })
}
```

`QueryClient` é criado em `App.tsx` com defaults: `retry: 1`, `refetchOnWindowFocus: false` (ajustado por hook quando precisa).

### Estado de UI — Zustand (a partir da Etapa 2)

Stores ficam em `src/store/`. Convenção: um store por domínio.

```ts
// store/chatStore.ts (Etapa 2+)
interface ChatState {
  conversations: Map<string, Conversation>
  activeId: string | null
  selectedAdapter: string
  setActive: (id: string) => void
  appendChunk: (id: string, chunk: ChatChunk) => void
}

export const useChatStore = create<ChatState>((set) => ({ ... }))
```

Stores não persistem entre reloads no MVP (decisão consciente — sem banco). Se quiser persistir histórico de adapter selecionado, use o middleware `persist` do zustand com `sessionStorage`.

### Estado local de componente

`useState` / `useReducer` pra coisas que vivem só dentro de uma tela (input do JQL, tab ativa do IssueDetail, etc.).

---

## Comunicação com o backend

### REST — `api/http.ts`

Wrapper fino sobre `fetch` que:

- Adiciona header `Content-Type: application/json` por padrão.
- Trata erros HTTP lançando `Error` com `status statusText :: body`.
- Mede latência e notifica via `subscribeLatency` (consumido pela TopBar).
- Usa caminhos relativos (`/api/...`) pra ser intermediado pelo proxy do Vite.

```ts
import { http } from '@/api/http'
const data = await http<MyResponse>('/api/some/endpoint')
```

Cada domínio tem seu módulo:

- `api/health.ts` — health check.
- `api/chat.ts` (Etapa 2) — `POST /api/chat`.
- `api/jira.ts` (Etapa 4) — busca, detalhe, comentários.
- `api/adapters.ts` (Etapa 4) — lista de adapters.

### WebSocket — `api/ws.ts` (Etapa 3+)

Cliente WebSocket reconectável com fila de mensagens. Hook `useChatStream(conversationId)` consome o stream e atualiza o `chatStore` com cada chunk recebido.

Esquema de mensagens (proposto):

```ts
// Cliente → Servidor
{ type: 'start', adapterId: string, conversationId: string, message: string }
{ type: 'abort', conversationId: string }

// Servidor → Cliente
{ type: 'chunk', conversationId: string, content: string }
{ type: 'done', conversationId: string, meta: { latencyMs, tokens } }
{ type: 'error', conversationId: string, message: string }
```

---

## Convenções

### Imports

Use sempre o alias `@/` pra paths absolutos a partir de `src/`:

```ts
// bom
import { Frame } from '@/components/ui/Frame'
import { useHealth } from '@/hooks/useHealth'

// ruim
import { Frame } from '../../components/ui/Frame'
```

### Componentes

- Um componente por arquivo, nome do arquivo igual ao do componente em PascalCase.
- Props tipadas com `interface`. Use `type` só pra unions/intersections.
- Default export evitado — prefira named export pra refactor mais fácil.
- Componentes "burros" (UI) ficam em `components/ui/` ou `components/<domain>/`. Páginas em `pages/`.

### Estilização

- Tailwind primeiro. Arbitrary values (`bg-[var(--bg-base)]`) ok.
- CSS custom só em `theme/globals.css` pra animações e pseudo-elementos.
- Cores via CSS variables, nunca hardcoded em hex no componente.
- `cn()` em `lib/cn.ts` pra concatenar classes condicionalmente.

### TypeScript

`strict: true` no tsconfig. `noUnusedLocals`, `noUnusedParameters` ligados — código com warning não compila.

### Acessibilidade

Mesmo com estética FUI:

- Contraste mínimo AA (a paleta atual respeita).
- `focus-visible` ring em todo elemento interativo (Button já tem).
- `aria-label` em botões só com ícone.
- Navegação por teclado: tab order natural, atalhos com `KBD` visualmente indicados.

---

## Testes

> Setup inicial planejado pra Etapa 2.

```bash
npm test          # Vitest watch mode
npm run test:ci   # uma rodada, exit code
```

### O que cobrir

- Componentes UI base (`Frame`, `Button`, `MessageBubble`, etc.) — render + variants.
- Hooks customizados (`useHealth`, `useClock`, `useChatStream`).
- Stores zustand — actions e seletores.
- Páginas com lógica não-trivial — `JiraPage` com mock do `api/jira.ts`.

### O que não cobrir

- Snapshots de componentes inteiros (frágeis e baixo valor).
- Cobertura % alta como métrica — foque em comportamento crítico.
