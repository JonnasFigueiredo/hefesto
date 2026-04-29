package com.hefesto.testcases;

/**
 * Arquivo anexado a um caso de teste como evidência (screenshot, log,
 * resposta de API, etc.). Conteúdo em BLOB pra suportar binário.
 */
public record TestCaseEvidence(
    String id,
    String testCaseId,
    String filename,
    String contentType,
    long sizeBytes,
    String note,
    long uploadedAt
) {}
