package com.hefesto.chat.dto;

public record ChatResponseDto(
    String conversationId,
    String content,
    String adapterId,
    long latencyMs,
    String model
) {}
