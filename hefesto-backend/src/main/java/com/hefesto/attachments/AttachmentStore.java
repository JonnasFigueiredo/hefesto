package com.hefesto.attachments;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Armazenamento in-memory de anexos. Sem persistência — limpo a cada
 * restart do backend.
 *
 * <p>Limites:</p>
 * <ul>
 *   <li>500 KB por arquivo (txt suficiente, evita abuso);</li>
 *   <li>5 MB no total entre todos os arquivos vivos;</li>
 *   <li>50 arquivos no máximo.</li>
 * </ul>
 */
@Component
public class AttachmentStore {

    private static final Logger log = LoggerFactory.getLogger(AttachmentStore.class);

    public static final long MAX_FILE_BYTES = 500 * 1024L;
    public static final long MAX_TOTAL_BYTES = 5 * 1024 * 1024L;
    public static final int MAX_FILES = 50;

    private final Map<String, Attachment> byId = new ConcurrentHashMap<>();
    private final AtomicLong totalBytes = new AtomicLong(0);

    public Attachment add(String filename, String contentType, String content) {
        if (content == null) content = "";
        long size = content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

        if (size > MAX_FILE_BYTES) {
            throw new AttachmentException(
                "Arquivo grande demais (" + formatBytes(size)
                    + "). Máximo por arquivo: " + formatBytes(MAX_FILE_BYTES) + ".");
        }
        if (byId.size() >= MAX_FILES) {
            throw new AttachmentException(
                "Limite de " + MAX_FILES + " anexos atingido. Remova alguns antes.");
        }
        long projected = totalBytes.get() + size;
        if (projected > MAX_TOTAL_BYTES) {
            throw new AttachmentException(
                "Espaço insuficiente. Total atual: " + formatBytes(totalBytes.get())
                    + ", limite: " + formatBytes(MAX_TOTAL_BYTES) + ".");
        }

        String id = UUID.randomUUID().toString().substring(0, 12);
        Attachment att = Attachment.of(id, filename, contentType, content);
        byId.put(id, att);
        totalBytes.addAndGet(size);

        log.debug("Attachment added: {} ({}, {})", id, filename, formatBytes(size));
        return att;
    }

    public Attachment get(String id) {
        return id == null ? null : byId.get(id);
    }

    public boolean delete(String id) {
        Attachment removed = byId.remove(id);
        if (removed == null) return false;
        totalBytes.addAndGet(-removed.sizeBytes());
        log.debug("Attachment deleted: {} ({})", id, removed.filename());
        return true;
    }

    public List<Attachment> list() {
        return byId.values().stream()
            .sorted(Comparator.comparingLong(Attachment::uploadedAt).reversed())
            .toList();
    }

    public long totalBytes() {
        return totalBytes.get();
    }

    public int count() {
        return byId.size();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
