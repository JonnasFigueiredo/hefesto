# Hefesto Bridge — Extensão VS Code

Ponte HTTP local entre o backend [Hefesto](../README.md) e os modelos do
**GitHub Copilot** disponíveis no VS Code via [Language Model API](https://code.visualstudio.com/api/extension-guides/language-model).

A extensão **não tem UI** — ela apenas roda um servidor HTTP local em
`127.0.0.1:35421` (configurável) e expõe os modelos do Copilot que sua
licença já dá acesso. O backend Hefesto consome esses endpoints através do
adapter `copilot-bridge`.

## Por que existe

O VS Code expõe Copilot Chat só pra extensões rodando dentro dele. Pra
permitir que outras ferramentas (como o Hefesto) usem o **mesmo modelo
licenciado pela empresa**, esta extensão serve de proxy local. Sem custos
adicionais, sem reverse engineering, sem violar ToS.

## Pré-requisitos

- VS Code 1.90+
- Subscription **GitHub Copilot** ativa na conta logada no VS Code
- Backend Hefesto rodando (este repo) na mesma máquina

## Endpoints expostos

| Método | Path        | Descrição                                     |
|--------|-------------|-----------------------------------------------|
| GET    | `/health`   | Status + versão + número de modelos           |
| GET    | `/models`   | Lista detalhada dos modelos Copilot           |
| POST   | `/chat`     | Envia prompt e devolve resposta (síncrono)    |

Bind exclusivo em `127.0.0.1` — apenas processos da própria máquina
acessam. CORS aberto pra simplificar uso de dev tools.

### `POST /chat` payload

```json
{
  "prompt": "Texto inteiro do prompt (já com contexto, agente, etc.)",
  "family": "claude-sonnet-4",        // opcional
  "model": "copilot-claude-sonnet-4"  // opcional, ID exato
}
```

Resposta:

```json
{
  "content": "...",
  "model": "copilot-claude-sonnet-4",
  "family": "claude-sonnet-4",
  "latencyMs": 1234
}
```

## Configuração (Settings.json)

```jsonc
{
  "hefestoBridge.enabled": true,
  "hefestoBridge.port": 35421,
  "hefestoBridge.preferredFamily": "claude-sonnet-4"  // opcional
}
```

A configuração é reativa — mudar a porta ou habilitar/desabilitar reinicia
o servidor automaticamente, sem precisar reiniciar VS Code.

## Comandos

Disponíveis via Command Palette (`Ctrl+Shift+P`):

- **Hefesto Bridge: Mostrar status** — exibe se está rodando e em qual porta
- **Hefesto Bridge: Reiniciar servidor** — útil após mudar config manualmente
- **Hefesto Bridge: Listar modelos disponíveis** — mostra quais IDs/famílias estão acessíveis pela sua license

Status bar à direita exibe ícone:
- 📡 `Hefesto` (verde) — rodando OK
- 🚫 `Hefesto` (amarelo) — parado/erro

## Build e empacotamento

### Pré-requisitos de build

- Node.js 20 LTS+
- npm 10+

### Comandos

```bash
cd hefesto-bridge-extension

# instala dependências dev (TypeScript, vsce, types)
npm install

# compila TypeScript em out/
npm run compile

# empacota num .vsix instalável
npm run package
# gera hefesto-bridge-0.1.0.vsix
```

O resultado é um arquivo `hefesto-bridge-<version>.vsix` na raiz desta
pasta. Esse é o artefato que você distribui internamente.

## Distribuição interna na empresa

### Modo 1 — GitHub Releases (recomendado)

1. Após `npm run package`, cria release no GitHub Enterprise:
   - Tag: `v0.1.0`
   - Anexa o arquivo `hefesto-bridge-0.1.0.vsix`
2. Linka o release no README do projeto Hefesto interno ou na wiki da empresa
3. Instruí os devs:
   ```bash
   # baixa o .vsix do release e instala
   code --install-extension hefesto-bridge-0.1.0.vsix
   ```

### Modo 2 — SharePoint / Drive corporativo

Sobe o `.vsix` num caminho compartilhado, manda link via Slack/Teams.

### Modo 3 — Repositório de artefatos (Nexus, Artifactory)

Hosta o `.vsix` como artifact binário. Devs baixam via:
```bash
curl -O https://nexus.empresa.com/.../hefesto-bridge-0.1.0.vsix
code --install-extension hefesto-bridge-0.1.0.vsix
```

## Como o usuário instala (passo a passo)

```bash
# 1. Baixa o arquivo .vsix
# 2. Roda no terminal
code --install-extension hefesto-bridge-0.1.0.vsix
```

Ou via UI:
- `Ctrl+Shift+P` → digite "Extensions: Install from VSIX..."
- Seleciona o `.vsix` baixado

Recarregar o VS Code não é necessário — a extensão ativa automaticamente
após instalação.

### Validação pós-instalação

1. Abre uma janela qualquer do VS Code
2. Procura "**Hefesto**" no canto direito da status bar (deve aparecer com 📡)
3. Roda no terminal:
   ```bash
   curl http://localhost:35421/health
   ```
   Esperado:
   ```json
   {"status":"ok","version":"0.1.0","modelCount":3}
   ```
   `modelCount` > 0 significa que o Copilot está logado e expondo modelos.

## Troubleshooting

**Status bar mostra `Hefesto` em amarelo / 🚫**
Servidor não conseguiu subir. Verifica:
- Porta 35421 já está em uso? (mude em Settings → `hefestoBridge.port`)
- A configuração `hefestoBridge.enabled` está `true`?

**`modelCount: 0` no `/health`**
A extensão funciona, mas o Copilot não retornou modelos. Causas:
- Você não tem subscription Copilot ativa na conta logada
- A primeira chamada à `vscode.lm` pede consentimento — abre VS Code e tenta
  usar Copilot Chat normalmente uma vez pra disparar o popup de autorização
- Sua organização tem políticas que restringem o LM API

**Erro 502 `lm_request_failed`**
A API LM rejeitou a request. Causas comuns:
- Rate limit atingido (espera alguns minutos)
- Conteúdo bloqueado por policy do Copilot
- Modelo selecionado não está disponível no seu plano

**Backend Hefesto mostra adapter Copilot offline**
1. VS Code está aberto?
2. A extensão está instalada e ativa? (verifica status bar)
3. Backend Hefesto consegue alcançar `localhost:35421`? Tenta `curl localhost:35421/health` no terminal onde o backend roda.

## Como o backend Hefesto consome

Backend já tem um adapter `copilot-bridge` registrado. Configurar URL em
`application-local.yml` se precisar mudar:

```yaml
copilot:
  bridge:
    url: http://localhost:35421
```

Default é `http://localhost:35421`, então normalmente nem precisa setar.

## Desenvolvimento da extensão

Pra debugar localmente sem empacotar:

1. Abre **esta pasta** (`hefesto-bridge-extension`) no VS Code
2. F5 — abre uma "Extension Development Host" rodando sua extensão
3. Faça mudanças em `src/extension.ts`
4. `npm run watch` recompila automaticamente
5. `Ctrl+R` na janela de dev recarrega a extensão

Logs vão pra Output → "Hefesto Bridge" (`console.log` no código).

## Licença

UNLICENSED — uso interno restrito ao Hefesto.

## Limitações conhecidas (v0.1.0)

- **Sem streaming**: o `/chat` é síncrono. Coleta tudo do `response.text` e
  devolve de uma vez. Adequado pro caso de uso atual; streaming SSE pode
  vir em versão futura.
- **Sem histórico de conversas**: cada chamada é uma sessão isolada (igual
  o adapter do Claude Code CLI). Histórico fica no backend Hefesto.
- **Sem context-awareness do editor**: não captura arquivo aberto/seleção.
  Se quiser isso, vira feature de uma versão posterior — `vscode.workspace`
  e `vscode.window.activeTextEditor` dão acesso fácil.
