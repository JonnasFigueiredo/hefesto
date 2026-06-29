package com.hefesto.story;

import java.util.List;

/**
 * Rascunho de história gerado por IA a partir de requisitos/contexto, pronto pra
 * pré-preencher o formulário de criação no Jira.
 *
 * @param summary            título sugerido.
 * @param description        descrição em Markdown.
 * @param acceptanceCriteria critérios de aceite sugeridos.
 * @param raw                resposta bruta do modelo (debug/transparência).
 */
public record StoryDraft(
    String summary,
    String description,
    List<String> acceptanceCriteria,
    String raw
) {}
