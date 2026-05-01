# Contribuindo com o Hefesto

Obrigado por considerar contribuir! Hefesto é um projeto pra QA's e devs
que usam IA no dia a dia. Toda contribuição que torne a ferramenta mais
útil pra esse público é bem-vinda.

## Como contribuir

### Reportar bugs

Abra uma issue descrevendo:
1. O que você esperava
2. O que aconteceu
3. Passos pra reproduzir
4. Versão do Java, Node e SO
5. Stack trace ou logs relevantes (se houver)

### Sugerir features

Issues com label `enhancement` são bem-vindas. Antes de codar, abra
uma issue descrevendo a feature pra alinhar direção.

### Pull requests

1. Faça fork e crie branch a partir de `main` com prefixo:
   - `feat/...` para features
   - `fix/...` para correções
   - `docs/...` para documentação
   - `refactor/...` para refatoração
2. Mantenha PRs focados — uma mudança por PR.
3. Adicione testes quando fizer sentido (especialmente em adapters e parsers).
4. Use Conventional Commits no título do PR (ex: `feat: novo agente Linter`).
5. Referencie issues relacionadas no corpo do PR.

## Estrutura do projeto

```
hefesto/
├── agentes/                    arquivos .md dos agentes
├── hefesto-backend/            Spring Boot + SQLite
├── hefesto-frontend/           React + TS + Vite
└── hefesto-bridge-extension/   Extensão VS Code (Copilot bridge)
```

## Convenções de código

### Backend (Java)

- Java 17, Spring Boot 3
- 4 espaços, padrão google-java-format
- Pacote por feature (não por camada)
- Records pra DTOs e value objects
- `@Component`/`@Service`/`@RestController` sem mistura
- Testes em `src/test/java`, mockando dependências externas

### Frontend (TypeScript)

- TypeScript strict, sem `any` implícito
- 2 espaços, padrão Prettier
- Componentes em PascalCase, um por arquivo
- Imports absolutos via alias `@/`
- Tailwind primeiro, CSS custom só pra animações
- Cores via CSS variables, sem hex hardcoded

### Mensagens de UI

- Português (Brasil) por padrão
- Termos técnicos consagrados em inglês (backend, adapter, JQL, etc.)
- Estilo FUI / cyberpunk: uppercase em labels e botões, prefixo `//` em hints

## Adicionando um agente novo

Veja [agentes/README.md](./agentes/README.md). Resumo:

1. Crie `agentes/MeuAgente.md` com frontmatter YAML opcional + system prompt
2. `curl -X POST http://localhost:8080/api/agents/reload` (ou restart)
3. Aparece no dropdown automaticamente

Se o agente gerar casos de teste estruturados (template `## TC-NNN —
[CATEGORIA]`), use `extractsTestCases: true` no frontmatter.

## Rodando testes

### Backend

```bash
cd hefesto-backend
mvn test
```

### Frontend

```bash
cd hefesto-frontend
npm test           # quando configurado
```

## Configuração local

Não comite credenciais. `application-local.yml` é gitignored — coloque
seu token Jira e caminho do Claude CLI lá. Veja README do projeto pra
template.

## Licença

Ao contribuir, você concorda em licenciar sua contribuição sob a [MIT
License](./LICENSE).

## Código de conduta

Seja respeitoso, construtivo e paciente. Discussões técnicas focam no
código, não nas pessoas. Cancelamento de contribuições por
desalinhamento técnico é normal — não pessoal.
