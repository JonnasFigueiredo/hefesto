package com.hefesto.attachments;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fachada sobre {@link AttachmentRepository}. Mantém os limites de
 * tamanho (500 KB / 5 MB / 50 arquivos) usando agregações SQL em vez de
 * estado em memória. API histórica preservada.
 */
@Component
public class AttachmentStore {

    private static final Logger log = LoggerFactory.getLogger(AttachmentStore.class);

    public static final long MAX_FILE_BYTES = 500 * 1024L;
    public static final long MAX_TOTAL_BYTES = 5 * 1024 * 1024L;
    public static final int MAX_FILES = 50;

    private final AttachmentRepository repo;

    public AttachmentStore(AttachmentRepository repo) {
        this.repo = repo;
    }

    public Attachment add(String filename, String contentType, String content) {
        if (content == null) content = "";
        long size = content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

        if (size > MAX_FILE_BYTES) {
            throw new AttachmentException(
                "Arquivo grande demais (" + formatBytes(size)
                    + "). Máximo por arquivo: " + formatBytes(MAX_FILE_BYTES) + ".");
        }
        if (repo.count() >= MAX_FILES) {
            throw new AttachmentException(
                "Limite de " + MAX_FILES + " anexos atingido. Remova alguns antes.");
        }
        long projected = repo.totalBytes() + size;
        if (projected > MAX_TOTAL_BYTES) {
            throw new AttachmentException(
                "Espaço insuficiente. Total atual: " + formatBytes(repo.totalBytes())
                    + ", limite: " + formatBytes(MAX_TOTAL_BYTES) + ".");
        }

        String id = UUID.randomUUID().toString().substring(0, 12);
        Attachment att = Attachment.of(id, filename, contentType, content);
        repo.save(att);

        log.debug("Attachment added: {} ({}, {})", id, filename, formatBytes(size));
        return att;
    }

    public Attachment get(String id) {
        return id == null ? null : repo.findById(id).orElse(null);
    }

    public boolean delete(String id) {
        boolean removed = repo.delete(id);
        if (removed) log.debug("Attachment deleted: {}", id);
        return removed;
    }

    public List<Attachment> list() {
        return repo.findAll();
    }

    public long totalBytes() {
        return repo.totalBytes();
    }

    public int count() {
        return repo.count();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
