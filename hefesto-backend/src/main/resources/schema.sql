-- =========================================================================
-- Hefesto :: schema SQLite
-- Carregado automaticamente pelo Spring Boot via spring.sql.init.schema-locations
-- "IF NOT EXISTS" garante idempotência entre restarts.
-- =========================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id              TEXT PRIMARY KEY,
    title           TEXT,
    adapter_id      TEXT NOT NULL,
    agent_id        TEXT NOT NULL DEFAULT 'default',
    jira_issue_key  TEXT,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    archived        INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS messages (
    id               TEXT PRIMARY KEY,
    conversation_id  TEXT NOT NULL,
    role             TEXT NOT NULL,
    content          TEXT NOT NULL,
    timestamp        INTEGER NOT NULL,
    meta_json        TEXT,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attachments (
    id            TEXT PRIMARY KEY,
    filename      TEXT NOT NULL,
    content_type  TEXT,
    content       TEXT NOT NULL,
    size_bytes    INTEGER NOT NULL,
    uploaded_at   INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS conversation_attachments (
    conversation_id  TEXT NOT NULL,
    attachment_id    TEXT NOT NULL,
    PRIMARY KEY (conversation_id, attachment_id),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (attachment_id)   REFERENCES attachments(id)   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usage_events (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp        INTEGER NOT NULL,
    event_type       TEXT NOT NULL,
    conversation_id  TEXT,
    payload_json     TEXT,
    duration_ms      INTEGER
);

CREATE INDEX IF NOT EXISTS idx_messages_conv  ON messages(conversation_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_conv_updated   ON conversations(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_time    ON usage_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_events_type    ON usage_events(event_type, timestamp);

-- =========================================================================
-- Etapa 9: Test Cases + Evidence
-- =========================================================================

CREATE TABLE IF NOT EXISTS test_cases (
    id                 TEXT PRIMARY KEY,
    conversation_id    TEXT NOT NULL,
    message_id         TEXT,
    code               TEXT,                   -- "TC-001", "TC-002"...
    category           TEXT,                   -- POSITIVO / NEGATIVO / EDGE
    title              TEXT NOT NULL,
    preconditions      TEXT,
    steps_json         TEXT NOT NULL,          -- JSON array de strings
    expected_result    TEXT,
    priority           TEXT,                   -- P1 / P2 / P3
    status             TEXT NOT NULL DEFAULT 'PENDING', -- PENDING/PASSED/FAILED/BLOCKED
    notes              TEXT,
    position           INTEGER NOT NULL DEFAULT 0, -- ordem dentro da mensagem
    created_at         INTEGER NOT NULL,
    updated_at         INTEGER NOT NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS test_case_evidence (
    id              TEXT PRIMARY KEY,
    test_case_id    TEXT NOT NULL,
    filename        TEXT NOT NULL,
    content_type    TEXT,
    content         BLOB NOT NULL,
    size_bytes      INTEGER NOT NULL,
    note            TEXT,
    uploaded_at     INTEGER NOT NULL,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tc_conv     ON test_cases(conversation_id, position);
CREATE INDEX IF NOT EXISTS idx_tc_message  ON test_cases(message_id);
CREATE INDEX IF NOT EXISTS idx_evidence_tc ON test_case_evidence(test_case_id, uploaded_at DESC);
