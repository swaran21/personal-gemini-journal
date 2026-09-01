ALTER TABLE action_items
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PROPOSED', 'PENDING', 'COMPLETED'));

UPDATE action_items SET status = CASE WHEN completed THEN 'COMPLETED' ELSE 'PENDING' END;

CREATE INDEX action_items_user_status_idx
    ON action_items(user_id, status, created_at DESC);
