package com.hefesto.testcases;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/test-cases")
public class TestCaseController {

    private static final Logger log = LoggerFactory.getLogger(TestCaseController.class);

    private final TestCaseService service;

    public TestCaseController(TestCaseService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestCase> list(
        @RequestParam(value = "conversationId", required = false) String conversationId,
        @RequestParam(value = "messageId", required = false) String messageId
    ) {
        if (messageId != null && !messageId.isBlank()) {
            return service.findByMessage(messageId);
        }
        if (conversationId != null && !conversationId.isBlank()) {
            return service.findByConversation(conversationId);
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCase> get(@PathVariable String id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record StatusUpdate(String status, String notes) {}

    @PatchMapping("/{id}")
    public ResponseEntity<TestCase> updateStatus(
        @PathVariable String id,
        @RequestBody StatusUpdate update
    ) {
        if (service.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        service.updateStatus(id, update.status(), update.notes());
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Evidence ----

    @GetMapping("/{id}/evidence")
    public List<TestCaseEvidence> listEvidence(@PathVariable String id) {
        return service.evidenceOf(id);
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<?> uploadEvidence(
        @PathVariable String id,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "note", required = false) String note
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_file"));
        }
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Limite de 5MB por evidência (cabe screenshots e logs comuns).
        if (file.getSize() > 5L * 1024L * 1024L) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "file_too_large",
                "message", "Máximo 5 MB por evidência."
            ));
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        String ct = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        TestCaseEvidence saved = service.addEvidence(id, filename, ct, file.getBytes(), note);
        log.info("Evidence uploaded for {} ({}, {} bytes)", id, filename, saved.sizeBytes());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/evidence/{evId}/download")
    public ResponseEntity<byte[]> downloadEvidence(
        @PathVariable String id,
        @PathVariable String evId
    ) {
        return service.downloadEvidence(evId)
            .filter(c -> c != null && c.content() != null)
            .map(c -> {
                String contentType = c.contentType() == null ? "application/octet-stream" : c.contentType();
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + safe(c.filename()) + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(c.content());
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/evidence/{evId}")
    public ResponseEntity<Void> deleteEvidence(
        @PathVariable String id,
        @PathVariable String evId
    ) {
        boolean removed = service.deleteEvidence(evId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "bad_request");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "evidence.bin";
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return clean.length() > 120 ? clean.substring(0, 120) : clean;
    }

    private static String safe(String s) {
        return s == null ? "evidence" : s.replace("\"", "");
    }
}
