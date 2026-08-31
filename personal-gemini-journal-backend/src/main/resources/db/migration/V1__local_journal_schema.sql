CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    content VARCHAR(10000) NOT NULL CHECK (length(trim(content)) > 0),
    ai_response VARCHAR(10000) NOT NULL,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX journal_entries_user_created_idx ON journal_entries(user_id, created_at DESC);
CREATE INDEX journal_entries_embedding_idx ON journal_entries USING hnsw (embedding vector_cosine_ops);

CREATE TABLE action_items (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    source_entry_id UUID REFERENCES journal_entries(id) ON DELETE CASCADE,
    goal VARCHAR(1000) NOT NULL CHECK (length(trim(goal)) > 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, source_entry_id, goal)
);

CREATE INDEX action_items_user_created_idx ON action_items(user_id, created_at DESC);

CREATE TABLE accountability_outbox (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'DEAD')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    last_error VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE (journal_entry_id)
);

CREATE INDEX accountability_outbox_work_idx ON accountability_outbox(status, available_at);

ALTER TABLE journal_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_entries FORCE ROW LEVEL SECURITY;
CREATE POLICY journal_entries_owner_policy ON journal_entries
    USING (user_id = current_setting('app.current_user_id', true))
    WITH CHECK (user_id = current_setting('app.current_user_id', true));

ALTER TABLE action_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE action_items FORCE ROW LEVEL SECURITY;
CREATE POLICY action_items_owner_policy ON action_items
    USING (user_id = current_setting('app.current_user_id', true))
    WITH CHECK (user_id = current_setting('app.current_user_id', true));
