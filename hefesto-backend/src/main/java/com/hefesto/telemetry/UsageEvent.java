package com.hefesto.telemetry;

/**
 * Evento de uso registrado em SQLite. Persistido em {@code usage_events}.
 *
 * @param id              auto-incrementado pelo banco; null antes de save.
 * @param timestamp       epoch millis quando o evento foi gerado.
 * @param eventType       chave canônica do evento (ex: "chat.complete",
 *                        "attachment.upload"). Use ponto pra namespace.
 * @param conversationId  conversa relacionada, se aplicável.
 * @param payloadJson     JSON com detalhes específicos do evento. Pode ser null.
 * @param durationMs      duração da operação em ms (ex: latência da chamada
 *                        LLM). Pode ser null pra eventos instantâneos.
 */
public record UsageEvent(
    Long id,
    long timestamp,
    String eventType,
    String conversationId,
    String payloadJson,
    Long durationMs
) {

    public static UsageEvent now(String eventType, String conversationId, String payloadJson, Long durationMs) {
        return new UsageEvent(null, System.currentTimeMillis(), eventType, conversationId, payloadJson, durationMs);
    }

    /** Namespaces padrão de tipos de evento. */
    public static final class Type {
        public static final String CHAT_START      = "chat.start";
        public static final String CHAT_COMPLETE   = "chat.complete";
        public static final String CHAT_ERROR      = "chat.error";
        public static final String CHAT_ABORT      = "chat.abort";
        public static final String ATTACHMENT_UPLOAD = "attachment.upload";
        public static final String ATTACHMENT_DELETE = "attachment.delete";
        public static final String AGENT_SELECT    = "agent.select";
        public static final String JIRA_LINK       = "jira.link";
        public static final String JIRA_SEARCH     = "jira.search";

        private Type() {}
    }
}
