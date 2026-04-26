package com.hefesto.attachments;

import java.time.Instant;

/**
 * Arquivo de texto anexado pelo usuário pra usar como contexto em conversas.
 *
 * @param id          identificador opaco gerado no upload.
 * @param filename    nome original do arquivo.
 * @param contentType MIME type detectado.
 * @param content     conteúdo bruto (UTF-8). Sempre texto no MVP.
 * @param sizeBytes   tamanho em bytes do conteúdo.
 * @param uploadedAt  timestamp de upload.
 */
public record Attachment(
    String id,
    String filename,
    String contentType,
    String content,
    long sizeBytes,
    long uploadedAt
) {

    public static Attachment of(String id, String filename, String contentType, String content) {
        return new Attachment(
            id,
            filename,
            contentType,
            content,
            content == null ? 0 : content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
            Instant.now().toEpochMilli()
        );
    }
}
