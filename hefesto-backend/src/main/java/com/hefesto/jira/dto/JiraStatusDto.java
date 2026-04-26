package com.hefesto.jira.dto;

/**
 * Status de uma issue. {@code category} mapeia pra cor no frontend:
 * "new" (cinza/dim), "indeterminate" (amber), "done" (verde).
 */
public record JiraStatusDto(
    String name,
    String category
) {}
