package com.hefesto.attachments;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AttachmentRepository {

    private static final RowMapper<Attachment> ROW_MAPPER = (rs, n) -> new Attachment(
        rs.getString("id"),
        rs.getString("filename"),
        rs.getString("content_type"),
        rs.getString("content"),
        rs.getLong("size_bytes"),
        rs.getLong("uploaded_at")
    );

    private final JdbcTemplate jdbc;

    public AttachmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Attachment a) {
        jdbc.update("""
            INSERT INTO attachments
                (id, filename, content_type, content, size_bytes, uploaded_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                filename = excluded.filename,
                content_type = excluded.content_type,
                content = excluded.content,
                size_bytes = excluded.size_bytes
            """,
            a.id(),
            a.filename(),
            a.contentType(),
            a.content(),
            a.sizeBytes(),
            a.uploadedAt()
        );
    }

    public Optional<Attachment> findById(String id) {
        if (id == null) return Optional.empty();
        return jdbc.query("SELECT * FROM attachments WHERE id = ?", ROW_MAPPER, id)
            .stream().findFirst();
    }

    public List<Attachment> findAll() {
        return jdbc.query(
            "SELECT * FROM attachments ORDER BY uploaded_at DESC", ROW_MAPPER);
    }

    public boolean delete(String id) {
        int rows = jdbc.update("DELETE FROM attachments WHERE id = ?", id);
        return rows > 0;
    }

    public int count() {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM attachments", Integer.class);
        return n == null ? 0 : n;
    }

    public long totalBytes() {
        Long n = jdbc.queryForObject(
            "SELECT COALESCE(SUM(size_bytes), 0) FROM attachments", Long.class);
        return n == null ? 0L : n;
    }
}
