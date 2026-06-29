package com.hefesto.story;

import java.util.List;

/**
 * Matriz de rastreabilidade: cobertura dos critérios de aceite de uma história
 * pelos casos de teste existentes.
 *
 * @param coveragePercent 0-100 (critérios cobertos / total).
 * @param criteria        um item por critério de aceite, com sua cobertura.
 * @param raw             resposta bruta do modelo.
 */
public record CoverageReport(
    int coveragePercent,
    List<CriterionCoverage> criteria,
    String raw
) {
    /**
     * Cobertura de um critério de aceite.
     *
     * @param criterion texto do critério.
     * @param covered   se há ao menos um caso de teste cobrindo.
     * @param byTests   códigos/keys dos testes que o cobrem.
     */
    public record CriterionCoverage(String criterion, boolean covered, List<String> byTests) {}
}
