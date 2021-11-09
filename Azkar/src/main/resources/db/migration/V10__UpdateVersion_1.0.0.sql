UPDATE "program_characteristics" SET "version"="1.0.0";
ALTER TABLE program_characteristics ADD COLUMN "ID" TEXT;
UPDATE "onboarding" SET "isFirstTimeOpened"=1;

-- automatic_check_for_updates
ALTER TABLE other_settings ADD COLUMN "automatic_check_for_updates" INTEGER DEFAULT 1;

-- add Adhan Audio column to prayertimes_settings table
ALTER TABLE prayertimes_settings ADD COLUMN "adhanAudio" TEXT NOT NULL DEFAULT 'بدون صوت';

