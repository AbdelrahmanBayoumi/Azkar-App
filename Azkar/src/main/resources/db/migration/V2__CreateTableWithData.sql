CREATE TABLE IF NOT EXISTS "prayertimes_settings" (
	"country"	TEXT NOT NULL DEFAULT 'Egypt',
	"city"	TEXT NOT NULL DEFAULT 'Alexandria',
	"method"	INTEGER NOT NULL DEFAULT 5,
	"asr_juristic"	INTEGER NOT NULL DEFAULT 0,
	"summer_timing"	INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS "absolute_zekr" (
	"id"	INTEGER,
	"text"	TEXT NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "prayertimes_methods" (
	"id"	INTEGER NOT NULL UNIQUE,
	"arabic_name"	TEXT NOT NULL UNIQUE,
	"english_name"	TEXT NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS "prayertimes" (
	"date"	TEXT NOT NULL UNIQUE,
	"fajr"	TEXT NOT NULL,
	"sunrise"	TEXT NOT NULL,
	"dhuhr"	TEXT NOT NULL,
	"asr"	TEXT NOT NULL,
	"maghrib"	TEXT NOT NULL,
	"isha"	TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS "program_characteristics" (
	"version"	TEXT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS "other_settings" (
	"language"	TEXT NOT NULL DEFAULT 'عربي - Arabic',
	"enable_darkmode"	INTEGER NOT NULL DEFAULT 0,
	"enable24"	INTEGER NOT NULL DEFAULT 0,
	"hijri_offset"	INTEGER NOT NULL DEFAULT 0,
	"minimized"	INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS "azkar_settings" (
	"morning_reminder"	TEXT NOT NULL DEFAULT 'لا تذكير',
	"night_reminder"	TEXT NOT NULL DEFAULT 'لا تذكير',
	"audio_name"	TEXT NOT NULL DEFAULT 'بدون صوت',
	"high_period"	INTEGER NOT NULL DEFAULT 5,
	"mid_period"	INTEGER NOT NULL DEFAULT 10,
	"low_period"	INTEGER NOT NULL DEFAULT 20,
	"rear_period"	INTEGER NOT NULL DEFAULT 30,
	"stop_azkar"	INTEGER NOT NULL DEFAULT 0,
	"selected_period"	TEXT NOT NULL DEFAULT 'high',
	"volume"	INTEGER NOT NULL DEFAULT 50
);
CREATE TABLE IF NOT EXISTS "morning_zekr" (
	"id"	INTEGER,
	"text"	TEXT,
	"repeat"	INTEGER,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "night_zekr" (
	"id"	INTEGER,
	"text"	TEXT,
	"repeat"	INTEGER
);
CREATE TABLE IF NOT EXISTS "morning_zekr_default" (
	"id"	INTEGER,
	"text"	TEXT,
	"repeat"	INTEGER
);
CREATE TABLE IF NOT EXISTS "night_zekr_default" (
	"id"	INTEGER,
	"text"	TEXT,
	"repeat"	INTEGER
);
CREATE TABLE IF NOT EXISTS "absolute_zekr_default" (
	"id"	INTEGER,
	"text"	TEXT
);
