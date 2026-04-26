package com.hefesto.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Captura exceções não tratadas em qualquer @RestController, loga o stack
 * trace completo e retorna ao cliente uma resposta JSON útil em vez do
 * "Internal Server Error" genérico do Spring.
 *
 * <p>Handlers específicos em controllers individuais (ex:
 * {@code @ExceptionHandler(JiraApiException.class)} no JiraController) têm
 * prioridade sobre este — só caem aqui exceções que ninguém mais tratou.</p>
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        log.error("Unhandled exception", e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "unexpected");
        body.put("type", e.getClass().getSimpleName());
        body.put("message", e.getMessage() == null ? "(no message)" : e.getMessage());

        // Cadeia de causas — útil pra ver o que de fato quebrou.
        Throwable cause = e.getCause();
        if (cause != null) {
            body.put("causedBy", cause.getClass().getSimpleName() + ": "
                + (cause.getMessage() == null ? "(no message)" : cause.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
