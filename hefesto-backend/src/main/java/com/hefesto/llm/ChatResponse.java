package com.hefesto.llm;

/**
 * Resposta síncrona de um adapter de LLM (Etapa 2).
 *
 * Quando o streaming entrar (Etapa 3), o adapter expõe um método separado que
 * retorna um Flux&lt;ChatChunk&gt;; este record continua sendo usado pelo endpoint
 * REST não-streaming.
 *
 * @param content      texto completo da resposta do assistente.
 * @param adapterId    id do adapter que gerou a resposta.
 * @param latencyMs    tempo total da chamada (do envio à resposta completa).
 * @param model        modelo reportado pelo CLI/API quando disponível; pode ser null.
 */
public record ChatResponse(
    String content,
    String adapterId,
    long latencyMs,
    String model
) {}
