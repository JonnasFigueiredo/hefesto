package com.hefesto.llm;

/**
 * Unidade incremental de uma resposta streaming.
 *
 * @param type     tipo do chunk: CONTENT (texto incremental), METADATA
 *                 (modelo/tokens), DONE (fim normal), ERROR (falha).
 * @param content  texto incremental do delta. Pode ser null em chunks
 *                 METADATA/DONE/ERROR.
 * @param meta     informações estruturadas: model, sessionId, latencyMs,
 *                 tokensIn, tokensOut, errorMessage. Pode ser null.
 */
public record ChatChunk(
    ChunkType type,
    String content,
    ChunkMeta meta
) {

    public enum ChunkType {
        CONTENT,
        METADATA,
        DONE,
        ERROR
    }

    public record ChunkMeta(
        String model,
        String sessionId,
        Long latencyMs,
        Integer tokensIn,
        Integer tokensOut,
        String errorMessage
    ) {}

    public static ChatChunk content(String text) {
        return new ChatChunk(ChunkType.CONTENT, text, null);
    }

    public static ChatChunk done(ChunkMeta meta) {
        return new ChatChunk(ChunkType.DONE, null, meta);
    }

    public static ChatChunk error(String message) {
        return new ChatChunk(ChunkType.ERROR, null,
            new ChunkMeta(null, null, null, null, null, message));
    }
}
