package com.hefesto.attachments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);

    private final AttachmentStore store;
    private final com.hefesto.telemetry.UsageEventService telemetry;

    public AttachmentController(
        AttachmentStore store,
        com.hefesto.telemetry.UsageEventService telemetry
    ) {
        this.store = store;
        this.telemetry = telemetry;
    }

    @GetMapping
    public List<AttachmentDto> list() {
        return store.list().stream().map(AttachmentDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Attachment a = store.get(id);
        if (a == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(AttachmentDto.from(a));
    }

    @PostMapping
    public AttachmentDto upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AttachmentException("Arquivo vazio ou ausente");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "text/plain" : file.getContentType();

        // Por enquanto MVP aceita só texto. Detecta heurística + extensão.
        if (!isAcceptableTextFile(filename, contentType)) {
            throw new AttachmentException(
                "Apenas arquivos de texto são suportados no MVP "
                    + "(.txt, .md, .json, .yml, .yaml). Recebido: " + filename + " (" + contentType + ")");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        Attachment att = store.add(filename, contentType, content);
        log.info("Uploaded attachment {} ({} bytes)", att.id(), att.sizeBytes());
        telemetry.record(
            com.hefesto.telemetry.UsageEvent.Type.ATTACHMENT_UPLOAD,
            null,
            java.util.Map.of(
                "attachmentId", att.id(),
                "filename", att.filename(),
                "sizeBytes", att.sizeBytes(),
                "contentType", att.contentType() == null ? "" : att.contentType()
            ),
            null
        );
        return AttachmentDto.from(att);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean removed = store.delete(id);
        if (removed) {
            telemetry.record(
                com.hefesto.telemetry.UsageEvent.Type.ATTACHMENT_DELETE,
                null,
                java.util.Map.of("attachmentId", id),
                null
            );
        }
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AttachmentException.class)
    public ResponseEntity<Map<String, Object>> handleAttachmentError(AttachmentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error", "attachment_error",
            "message", e.getMessage()
        ));
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "untitled.txt";
        // Remove path separators e caracteres perigosos.
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return clean.length() > 120 ? clean.substring(0, 120) : clean;
    }

    private static boolean isAcceptableTextFile(String filename, String contentType) {
        if (contentType != null && contentType.startsWith("text/")) return true;
        if (contentType != null && contentType.contains("json")) return true;
        if (contentType != null && contentType.contains("yaml")) return true;
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")
            || lower.endsWith(".json") || lower.endsWith(".yml") || lower.endsWith(".yaml")
            || lower.endsWith(".csv") || lower.endsWith(".log");
    }
}
