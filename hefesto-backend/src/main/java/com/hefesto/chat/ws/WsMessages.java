package com.hefesto.chat.ws;

/**
 * DTOs do protocolo do WebSocket /ws/chat.
 *
 * <p>Cliente → Servidor:</p>
 * <ul>
 *   <li>{@code {"type":"start","adapterId":"...","conversationId":"...","message":"..."}}</li>
 *   <li>{@code {"type":"abort","conversationId":"..."}}</li>
 * </ul>
 *
 * <p>Servidor → Cliente:</p>
 * <ul>
 *   <li>{@code {"type":"chunk","conversationId":"...","content":"..."}}</li>
 *   <li>{@code {"type":"done","conversationId":"...","content":"...","model":"...","latencyMs":1234}}</li>
 *   <li>{@code {"type":"error","conversationId":"...","message":"..."}}</li>
 *   <li>{@code {"type":"started","conversationId":"...","adapterId":"..."}}</li>
 * </ul>
 */
public final class WsMessages {

    private WsMessages() {}

    public record In(
        String type,
        String adapterId,
        String conversationId,
        String message,
        String agentId,
        java.util.List<String> attachmentIds,
        String jiraIssueKey
    ) {}

    public record OutChunk(
        String type,
        String conversationId,
        String content
    ) {
        public static OutChunk of(String conversationId, String content) {
            return new OutChunk("chunk", conversationId, content);
        }
    }

    public record OutDone(
        String type,
        String conversationId,
        String messageId,
        String content,
        String adapterId,
        String model,
        long latencyMs
    ) {
        public static OutDone of(String conversationId, String messageId, String content, String adapterId, String model, long latencyMs) {
            return new OutDone("done", conversationId, messageId, content, adapterId, model, latencyMs);
        }
    }

    public record OutError(
        String type,
        String conversationId,
        String message
    ) {
        public static OutError of(String conversationId, String message) {
            return new OutError("error", conversationId, message);
        }
    }

    public record OutStarted(
        String type,
        String conversationId,
        String adapterId
    ) {
        public static OutStarted of(String conversationId, String adapterId) {
            return new OutStarted("started", conversationId, adapterId);
        }
    }
}
