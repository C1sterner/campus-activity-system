INSERT INTO sys_config (config_key, config_value)
VALUES
  ('upload.max_size_bytes', '20971520'),
  ('upload.allowed_extensions', 'jpg,png,pdf'),
  ('export.max_rows', '50000'),
  ('registration.draft_interval_seconds', '30')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
