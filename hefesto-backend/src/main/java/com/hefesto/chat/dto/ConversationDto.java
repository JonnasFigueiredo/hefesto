package com.hefesto.chat.dto;

import java.util.List;

public record ConversationDto(
    String id,
    String title,
    String adapterId,
    String agentId,
    String jiraIssueKey,
    List<String> attachmentIds,
    long createdAt,
    long updatedAt,
    boolean archived,
    List<MessageDto> messages
) {

    public record MessageDto(
        String id,
        String role,
        String content,
        long timestamp
    ) {}
}
