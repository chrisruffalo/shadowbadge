-- add ability yo track how many times a badge is seen
ALTER TABLE badges ADD COLUMN seen bigint DEFAULT 0;
