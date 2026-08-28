-- Migrate Settings to Unified Preferences Table

-- Add is_deleted column (boolean with default false)
ALTER TABLE absolute_zekr ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;

-- Add uuid column (string, nullable)
ALTER TABLE absolute_zekr ADD COLUMN uuid TEXT;
