package com.hefesto.jira.dto;

public record CommentDto(
    String id,
    JiraUserDto author,
    /** Markdown convertido a partir do ADF original. */
    String body,
    long created,
    long updated
) {}
