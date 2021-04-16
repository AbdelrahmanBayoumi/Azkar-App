UPDATE "program_characteristics" SET "version"="0.9.3";

-- Delete all prayer times because previous ones was Depend on Hijri Date
DELETE FROM prayertimes;
