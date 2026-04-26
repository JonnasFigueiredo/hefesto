package com.hefesto.jira.dto;

public record JiraUserDto(
    String accountId,
    String displayName,
    String email,
    String avatarUrl
) {}
