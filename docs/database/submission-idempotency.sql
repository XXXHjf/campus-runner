-- Back up the database before executing. A pre-existing table must be inspected,
-- not silently accepted: this migration intentionally fails if it already exists.
CREATE TABLE tb_submission_record (
    owner_id BIGINT NOT NULL,
    operation VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    claim_token CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result_json MEDIUMTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner_id, operation, request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
