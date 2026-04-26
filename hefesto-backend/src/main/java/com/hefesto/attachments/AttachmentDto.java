package com.hefesto.attachments;

/**
 * DTO de listagem — não expõe conteúdo (pode ser grande). O conteúdo só é
 * usado internamente pelo PromptBuilder.
 */
public record AttachmentDto(
    String id,
    String filename,
    String contentType,
    long sizeBytes,
    long uploadedAt
) {

    public static AttachmentDto from(Attachment a) {
        return new AttachmentDto(
            a.id(),
            a.filename(),
            a.contentType(),
            a.sizeBytes(),
            a.uploadedAt()
        );
    }
}
