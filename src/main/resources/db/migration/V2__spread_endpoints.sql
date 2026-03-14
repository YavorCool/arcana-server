ALTER TABLE devices ADD COLUMN IF NOT EXISTS has_completed_spread BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE daily_usage RENAME COLUMN readings_count TO spreads_count;
