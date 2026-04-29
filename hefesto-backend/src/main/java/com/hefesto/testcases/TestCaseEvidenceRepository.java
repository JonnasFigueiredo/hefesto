package com.hefesto.testcases;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TestCaseEvidenceRepository {

    /** Versão "leve" sem o BLOB (pra listagens). */
    private static final RowMapper<TestCaseEvidence> META_MAPPER = (rs, n) -> new TestCaseEvidence(
        rs.getString("id"),
        rs.getString("test_case_id"),
        rs.getString("filename"),
        rs.getString("content_type"),
        rs.getLong("size_bytes"),
        rs.getString("note"),
        rs.getLong("uploaded_at")
    );

    private final JdbcTemplate jdbc;

    public TestCaseEvidenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TestCaseEvidence save(
        String testCaseId,
        String filename,
        String contentType,
        byte[] content,
        String note
    ) {
        String id = UUID.randomUUID().toString().substring(0, 12);
        long now = System.currentTimeMillis();
        long size = content == null ? 0 : content.length;

        jdbc.update("""
            INSERT INTO test_case_evidence
                (id, test_case_id, filename, content_type, content, size_bytes, note, uploaded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id, testCaseId, filename, contentType, content, size, note, now);

        return new TestCaseEvidence(id, testCaseId, filename, contentType, size, note, now);
    }

    public List<TestCaseEvidence> findByTestCase(String testCaseId) {
        return jdbc.query("""
            SELECT id, test_case_id, filename, content_type, size_bytes, note, uploaded_at
            FROM test_case_evidence
            WHERE test_case_id = ?
            ORDER BY uploaded_at DESC
            """, META_MAPPER, testCaseId);
    }

    public Optional<EvidenceContent> downloadById(String id) {
        if (id == null) return Optional.empty();
        return jdbc.query(
            "SELECT id, filename, content_type, content FROM test_case_evidence WHERE id = ?",
            (rs, n) -> new EvidenceContent(
                rs.getString("id"),
                rs.getString("filename"),
                rs.getString("content_type"),
                rs.getBytes("content")
            ),
            id).stream().findFirst();
    }

    public boolean delete(String id) {
        return jdbc.update("DELETE FROM test_case_evidence WHERE id = ?", id) > 0;
    }

    public record EvidenceContent(
        String id,
        String filename,
        String contentType,
        byte[] content
    ) {}
}
