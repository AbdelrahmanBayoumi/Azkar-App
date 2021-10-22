UPDATE "program_characteristics" SET "version"="1.0.0";

-- automatic_check_for_updates
ALTER TABLE other_settings ADD COLUMN "automatic_check_for_updates" INTEGER DEFAULT 1;

ALTER TABLE program_characteristics ADD COLUMN "ID" TEXT;