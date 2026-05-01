# Agentes do Hefesto

Cada arquivo `.md` nesta pasta vira **um agente especialista** disponível no
dropdown do chat. O nome exibido no dropdown é o nome do arquivo (com
extensão), e o ID interno é o nome sem `.md`.

## Estrutura de um agente

Um arquivo de agente tem (opcionalmente) um bloco de metadados em YAML
no topo, delimitado por `---`, e o **system prompt** abaixo:

```markdown
---
description: "Descrição curta do agente (1-2 linhas)."
emoji: "🧪"
defaultPromptTemplate: "Mensagem inicial sugerida (opcional)."
extractsTestCases: true
---

Aqui vai o system prompt completo, em texto livre.
Pode ter múltiplos parágrafos, instruções, exemplos, formatos esperados...
```

### Campos do frontmatter

| Campo | Tipo | Descrição |
|---|---|---|
| `description` | string | Texto curto mostrado no card do agente. Default: vazio. |
| `emoji` | string | Ícone exibido no botão e no card. Default: `✦`. |
| `defaultPromptTemplate` | string | Sugestão de mensagem ao selecionar o agente (não preenche o composer automaticamente, mas fica disponível pra UI). |
| `extractsTestCases` | bool | Se `true`, o backend tenta extrair casos de teste estruturados das respostas (TC-NNN no template QA). Use somente em agentes que vão produzir esse formato. Default: `false`. |

Se você não quiser frontmatter, pode escrever só o system prompt no
arquivo direto. Default sensato é aplicado.

## Adicionando um agente novo

1. Crie um arquivo `.md` nesta pasta — ex: `MeuAgenteCustom.md`
2. Escreva o frontmatter (opcional) e o system prompt
3. Recarregue o backend de uma destas formas:
   - **Sem restart**: `curl -X POST http://localhost:8080/api/agents/reload`
   - **Com restart**: pare o `mvn spring-boot:run` e suba de novo
4. O agente aparece no dropdown do chat imediatamente

## Boas práticas pra prompts

- **Seja específico** sobre formato esperado da resposta. Ex: "Use exatamente este template: ..."
- **Descreva o papel** logo na primeira frase. "Você é um QA Specialist sênior..."
- **Limite o escopo** — agente focado responde melhor que generalista.
- **Inclua exemplos** quando o formato é importante.
- **Termine com regras de qualidade**: "Seja prático. Evite repetição. Use markdown."

## Agentes inclusos por padrão

| Arquivo | Função |
|---|---|
| `Default.md` | Sem persona — usa modelo direto |
| `QAseniorAgent.md` | QA sênior, gera casos com `extractsTestCases: true` |
| `AnalistaDeNegocios.md` | Avalia histórias quanto a clareza, riscos, dependências |
| `TechWriter.md` | Documentação técnica em formato padrão |
| `Arquiteto.md` | Análise técnica com 2-3 abordagens e trade-offs |
| `CodeReviewer.md` | Revisão crítica de código (bugs/segurança/performance) |

Sinta-se livre pra editar, deletar ou criar novos. Os agentes são
versionados junto com o projeto — discuta no PR antes de mudar os
defaults.

## Configurando outro caminho

Por padrão, o backend procura a pasta em `./agentes` relativa ao diretório
de execução. Pra mudar, edite `application.yml` ou rode com env var:

```bash
export AGENTS_PATH=/caminho/customizado
mvn spring-boot:run
```

Ou no `application-local.yml`:

```yaml
agents:
  path: /caminho/customizado
```
