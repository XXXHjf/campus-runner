-- Back up first. Additive migration; enables no traffic policy by itself.
CREATE TABLE IF NOT EXISTS tb_request_guard (
  guard_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  window_start BIGINT NOT NULL,
  used_count BIGINT NOT NULL DEFAULT 0,
  used_bytes BIGINT NOT NULL DEFAULT 0,
  tokens INT NOT NULL DEFAULT 0,
  touched_at BIGINT NOT NULL,
  PRIMARY KEY (guard_key, window_start),
  KEY idx_request_guard_cleanup(touched_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS tb_upload_lease (
  lease_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  guard_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  expires_at BIGINT NOT NULL,
  PRIMARY KEY (lease_id),
  KEY idx_upload_lease_actor(guard_key,expires_at),
  KEY idx_upload_lease_expiry(expires_at)
) ENGINE=InnoDB;
