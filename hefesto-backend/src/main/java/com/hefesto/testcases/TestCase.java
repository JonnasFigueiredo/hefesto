package com.hefesto.testcases;

import java.util.List;

/**
 * Caso de teste estruturado, extraído de uma resposta do agente
 * QA Specialist. Persistido em SQLite.
 *
 * @param status PENDING (default), PASSED, FAILED, BLOCKED.
 * @param category POSITIVO, NEGATIVO, EDGE — pode ser null se não detectado.
 * @param priority P1, P2, P3 — pode ser null.
 * @param position ordem do caso dentro da mensagem que o gerou (pra
 *                 manter sequência consistente após múltiplas leituras).
 */
public record TestCase(
    String id,
    String conversationId,
    String messageId,
    String code,
    String category,
    String title,
    String preconditions,
    List<String> steps,
    String expectedResult,
    String priority,
    String status,
    String notes,
    int position,
    long createdAt,
    long updatedAt
) {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_PASSED   = "PASSED";
    public static final String STATUS_FAILED   = "FAILED";
    public static final String STATUS_BLOCKED  = "BLOCKED";
}
