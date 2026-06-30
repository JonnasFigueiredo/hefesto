# Mídia do README

Imagens usadas no `README.md` da raiz (capturas da interface):

- `ui-jira-detail.png` — painel do Jira com as ações de IA (hero)
- `ui-create-story.png` — modal de nova história com assistência de IA
- `ui-chat-qa.png` — chat com o agente QA Sênior gerando casos de teste
- `ui-coverage.png` — matriz de cobertura (critérios × testes)

Para regenerar/atualizar os prints, suba o backend e o frontend e use um Chrome
headless (ex.: `puppeteer-core` apontando para o Chrome do sistema) navegando por
`/jira` e `/chat`. Vídeos podem ser adicionados via tag `<video>` ou URL
`user-images.githubusercontent.com` (arraste o arquivo numa issue/PR).
