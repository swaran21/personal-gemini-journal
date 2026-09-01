ALTER TABLE journal_entries ALTER COLUMN ai_response DROP NOT NULL;
ALTER TABLE journal_entries ALTER COLUMN embedding DROP NOT NULL;

ALTER TABLE journal_entries
    ADD COLUMN processing_status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED'
        CHECK (processing_status IN ('PENDING', 'COMPLETED', 'FAILED')),
    ADD COLUMN processing_error VARCHAR(255);

CREATE INDEX journal_entries_user_processing_idx
    ON journal_entries(user_id, processing_status, created_at DESC);
