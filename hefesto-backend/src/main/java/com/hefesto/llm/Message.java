package com.hefesto.llm;

import java.time.Instant;

/**
 * Uma mensagem dentro de uma conversa. Pode ter sido enviada pelo usuário,
 * gerada pelo assistente, ou injetada como contexto de sistema.
 */
public record Message(
    Role role,
    String content,
    long timestamp
) {

    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM,
        JIRA_CONTEXT
    }

    public static Message user(String content) {
        return new Message(Role.USER, content, Instant.now().toEpochMilli());
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content, Instant.now().toEpochMilli());
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content, Instant.now().toEpochMilli());
    }
}
