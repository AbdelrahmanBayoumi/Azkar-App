-- Migrate Settings to Unified Preferences Table
-- This SQL migration script consolidates settings from various application-specific tables into a single preferences table
-- that stores key-value pairs. It transfers settings from the azkar_settings, other_settings, and prayertimes_settings tables into the preferences table.
-- The script also handles the cleanup by dropping the original tables to streamline configuration management.


CREATE TABLE IF NOT EXISTS preferences
(
    key
    TEXT,
    value
    TEXT,
    PRIMARY
    KEY
(
    key
));

-- CREATE TABLE "azkar_settings" (
--                                   "morning_reminder"	TEXT NOT NULL DEFAULT 'لا تذكير',
--                                   "night_reminder"	TEXT NOT NULL DEFAULT 'لا تذكير',
--                                   "audio_name"	TEXT NOT NULL DEFAULT 'بدون صوت',
--                                   "high_period"	INTEGER NOT NULL DEFAULT 5,
--                                   "mid_period"	INTEGER NOT NULL DEFAULT 10,
--                                   "low_period"	INTEGER NOT NULL DEFAULT 20,
--                                   "rear_period"	INTEGER NOT NULL DEFAULT 30,
--                                   "stop_azkar"	INTEGER NOT NULL DEFAULT 0,
--                                   "selected_period"	TEXT NOT NULL DEFAULT 'high',
--                                   "volume"	INTEGER NOT NULL DEFAULT 50
-- )

-- Insert azkar settings into preferences
INSERT INTO preferences (key, value)
values ('morning_azkar_reminder', 60);

INSERT INTO preferences (key, value)
values ('night_azkar_reminder', 60);

INSERT INTO preferences (key, value)
SELECT 'audio_name', audio_name
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'high_period', high_period
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'mid_period', mid_period
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'low_period', low_period
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'rear_period', rear_period
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'is_stopped', CASE WHEN stop_azkar = 0 THEN 'false' ELSE 'true' END
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'selected_period', selected_period
FROM azkar_settings;

INSERT INTO preferences (key, value)
SELECT 'volume', volume
FROM azkar_settings;

-- CREATE TABLE "other_settings" (
--                                   "language"	TEXT NOT NULL DEFAULT 'عربي - Arabic',
--                                   "enable_darkmode"	INTEGER NOT NULL DEFAULT 0,
--                                   "enable24"	INTEGER NOT NULL DEFAULT 0,
--                                   "hijri_offset"	INTEGER NOT NULL DEFAULT 0,
--                                   "minimized"	INTEGER NOT NULL DEFAULT 0
--     , "automatic_check_for_updates" INTEGER DEFAULT 1)
INSERT INTO preferences (key, value)
SELECT 'language',
       CASE
           WHEN language IN ('ar', 'عربي - Arabic') THEN 'ar'
    WHEN language IN ('en', 'إنجليزية - English') THEN 'en'
    ELSE 'ar'
END
FROM other_settings;

INSERT INTO preferences (key, value)
SELECT 'enable_dark_mode', CASE WHEN enable_darkmode = 0 THEN 'false' ELSE 'true' END
FROM other_settings;

INSERT INTO preferences (key, value)
SELECT 'enable_24_format', CASE WHEN enable24 = 0 THEN 'false' ELSE 'true' END
FROM other_settings;

INSERT INTO preferences (key, value)
SELECT 'hijri_offset', hijri_offset
FROM other_settings;

INSERT INTO preferences (key, value)
SELECT 'minimized', CASE WHEN minimized = 0 THEN 'false' ELSE 'true' END
FROM other_settings;

INSERT INTO preferences (key, value)
SELECT 'automatic_check_for_updates', CASE WHEN automatic_check_for_updates = 0 THEN 'false' ELSE 'true' END
FROM other_settings;


-- CREATE TABLE "prayertimes_settings" (
--                                         "country"	TEXT NOT NULL DEFAULT 'Egypt',
--                                         "city"	TEXT NOT NULL DEFAULT 'Alexandria',
--                                         "method"	INTEGER NOT NULL DEFAULT 5,
--                                         "asr_juristic"	INTEGER NOT NULL DEFAULT 0,
--                                         "summer_timing"	INTEGER NOT NULL DEFAULT 0
--     , "latitude" DOUBLE, "longitude" DOUBLE, "adhanAudio" TEXT NOT NULL DEFAULT 'بدون صوت')
INSERT INTO preferences (key, value)
SELECT 'country', country
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'city', city
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'method', method
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'asr_juristic', asr_juristic
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'summer_timing', CASE WHEN summer_timing = 0 THEN 'false' ELSE 'true' END
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'latitude', latitude
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'longitude', longitude
FROM prayertimes_settings;

INSERT INTO preferences (key, value)
SELECT 'adhan_audio', adhanAudio
FROM prayertimes_settings;

-- CREATE TABLE "notification" (
--                                 "position"	TEXT NOT NULL DEFAULT 'BOTTOM_RIGHT'
-- )
drop table if exists "notification";
drop table if exists "azkar_settings";
drop table if exists "other_settings";
drop table if exists "prayertimes_settings";
