package com.hefesto.story;

import java.util.List;

/**
 * Revisão de prontidão (INVEST) de uma história do Jira.
 *
 * @param readinessScore  0-100 (0 = nada pronta, 100 = pronta pro dev).
 * @param verdict         resumo em uma frase.
 * @param invest          notas por princípio INVEST.
 * @param gaps            lacunas encontradas.
 * @param risks           riscos identificados.
 * @param missingCriteria critérios de aceite sugeridos/faltantes.
 * @param raw             resposta bruta do modelo.
 */
public record StoryReview(
    int readinessScore,
    String verdict,
    List<String> invest,
    List<String> gaps,
    List<String> risks,
    List<String> missingCriteria,
    String raw
) {}
