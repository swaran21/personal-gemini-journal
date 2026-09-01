ALTER TABLE journal_entries
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN location_label VARCHAR(200),
    ADD CONSTRAINT journal_entries_location_pair CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    );

CREATE INDEX journal_entries_user_page_idx ON journal_entries(user_id, created_at DESC, id DESC);
CREATE INDEX action_items_user_page_idx ON action_items(user_id, created_at DESC, id DESC);
