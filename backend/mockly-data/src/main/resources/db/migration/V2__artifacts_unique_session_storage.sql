-- Ensure idempotency for egress-ended webhooks:
-- keep only one artifact per (session_id, storage_url), then enforce uniqueness.

DELETE FROM artifacts a
WHERE a.ctid <> (
    SELECT MIN(b.ctid)
    FROM artifacts b
    WHERE b.session_id = a.session_id
      AND b.storage_url = a.storage_url
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_artifacts_session_storage_url
    ON artifacts(session_id, storage_url);
