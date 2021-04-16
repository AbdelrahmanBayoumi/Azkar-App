UPDATE "program_characteristics" SET "version"="0.9.3";

-- Delete all prayer times because previous ones was Depend on Hijri Date
DELETE FROM prayertimes;

-- add notification model
CREATE TABLE "notification" (
	"position"	TEXT NOT NULL DEFAULT 'BOTTOM_RIGHT'
);
-- add default value
INSERT INTO "notification" ("position") VALUES ('BOTTOM_RIGHT');