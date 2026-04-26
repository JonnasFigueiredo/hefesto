package com.hefesto.chat.dto;

import java.util.List;

public record ConversationDto(
    String id,
    String title,
    String adapterId,
    long createdAt,
    long updatedAt,
    List<MessageDto> messages
) {

    public record MessageDto(
        String role,
        String content,
        long timestamp
    ) {}
}
