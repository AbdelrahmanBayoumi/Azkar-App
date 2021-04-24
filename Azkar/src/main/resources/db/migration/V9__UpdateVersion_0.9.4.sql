UPDATE "program_characteristics" SET "version"="0.9.4";

ALTER TABLE "prayertimes_methods" ADD COLUMN "value" TEXT;

-- UPDATE enum values for com.batoulapps.adhan prayer times calculator
UPDATE "prayertimes_methods" SET "value"='KARACHI' WHERE "id"='1';
UPDATE "prayertimes_methods" SET "value"='NORTH_AMERICA' WHERE "id"='2';
UPDATE "prayertimes_methods" SET "value"='MUSLIM_WORLD_LEAGUE' WHERE "id"='3';
UPDATE "prayertimes_methods" SET "value"='UMM_AL_QURA' WHERE "id"='4';
UPDATE "prayertimes_methods" SET "value"='EGYPTIAN' WHERE "id"='5';
UPDATE "prayertimes_methods" SET "value"='KUWAIT' WHERE "id"='9';
UPDATE "prayertimes_methods" SET "value"='QATAR' WHERE "id"='10';
UPDATE "prayertimes_methods" SET "value"='SINGAPORE' WHERE "id"='11';
UPDATE "prayertimes_methods" SET "value"='MOON_SIGHTING_COMMITTEE' WHERE "id"='15';
INSERT INTO "prayertimes_methods" ("id", "arabic_name", "english_name", "value") VALUES (16, 'دبي', 'Dubai', 'DUBAI');

-- prayer times will be calculated locally
DROP TABLE prayertimes;

-- fix Spelling error in Absolute Azkar
UPDATE "absolute_zekr" SET "text"='الحمد لله' WHERE "text"='الحمدلله';

-- add latitude and longitude
ALTER TABLE "prayertimes_settings" ADD COLUMN "latitude" DOUBLE;
ALTER TABLE "prayertimes_settings" ADD COLUMN "longitude" DOUBLE;

UPDATE "onboarding" SET "isFirstTimeOpened"=1;
