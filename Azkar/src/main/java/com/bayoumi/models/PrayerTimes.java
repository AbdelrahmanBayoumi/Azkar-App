package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.time.Utilities;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.prayertimes.PrayerTimesDBManager;
import com.bayoumi.util.prayertimes.PrayerTimesValidation;
import com.bayoumi.util.time.HijriDate;
import javafx.util.StringConverter;

import java.sql.ResultSet;
import java.time.LocalTime;
import java.util.ArrayList;

public class PrayerTimes {
    private HijriDate hijriDate;
    private String fajr;
    private String sunrise;
    private String dhuhr;
    private String asr;
    private String maghrib;
    private String isha;
    private PrayerTimes.PrayerTimeSettings prayerTimeSettings;


    public PrayerTimes(HijriDate hijriDate, String fajr, String sunrise, String dhuhr, String asr, String maghrib, String isha) {
        this.hijriDate = hijriDate;
        this.fajr = fajr;
        this.sunrise = sunrise;
        this.dhuhr = dhuhr;
        this.asr = asr;
        this.maghrib = maghrib;
        this.isha = isha;
        prayerTimeSettings = new PrayerTimeSettings();
    }

    public static PrayerTimesBuilder builder() {
        return new PrayerTimesBuilder();
    }

    public void formatTime24To12(String language) {
        this.fajr = Utilities.formatTime24To12String(language, this.fajr);
        this.sunrise = Utilities.formatTime24To12String(language, this.sunrise);
        this.dhuhr = Utilities.formatTime24To12String(language, this.dhuhr);
        this.asr = Utilities.formatTime24To12String(language, this.asr);
        this.maghrib = Utilities.formatTime24To12String(language, this.maghrib);
        this.isha = Utilities.formatTime24To12String(language, this.isha);
    }

    public void enableSummerTime() {
        this.fajr = LocalTime.parse(this.fajr).plusHours(1).toString();
        this.sunrise = LocalTime.parse(this.sunrise).plusHours(1).toString();
        this.dhuhr = LocalTime.parse(this.dhuhr).plusHours(1).toString();
        this.asr = LocalTime.parse(this.asr).plusHours(1).toString();
        this.maghrib = LocalTime.parse(this.maghrib).plusHours(1).toString();
        this.isha = LocalTime.parse(this.isha).plusHours(1).toString();
    }

    public HijriDate getHijriDate() {
        return hijriDate;
    }

    public void setHijriDate(HijriDate hijriDate) {
        this.hijriDate = hijriDate;
    }

    public String getFajr() {
        return fajr;
    }

    public void setFajr(String fajr) {
        this.fajr = fajr;
    }

    public String getSunrise() {
        return sunrise;
    }

    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }

    public String getDhuhr() {
        return dhuhr;
    }

    public void setDhuhr(String dhuhr) {
        this.dhuhr = dhuhr;
    }

    public String getAsr() {
        return asr;
    }

    public void setAsr(String asr) {
        this.asr = asr;
    }

    public String getMaghrib() {
        return maghrib;
    }

    public void setMaghrib(String maghrib) {
        this.maghrib = maghrib;
    }

    public String getIsha() {
        return isha;
    }

    public void setIsha(String isha) {
        this.isha = isha;
    }

    public PrayerTimeSettings getPrayerTimeSettings() {
        return prayerTimeSettings;
    }

    public void setPrayerTimeSettings(PrayerTimeSettings prayerTimeSettings) {
        this.prayerTimeSettings = prayerTimeSettings;
    }

    @Override
    public String toString() {
        return "PrayerTimes{" +
                "hijriDate=" + hijriDate +
                ", fajr='" + fajr + '\'' +
                ", sunrise='" + sunrise + '\'' +
                ", dhuhr='" + dhuhr + '\'' +
                ", asr='" + asr + '\'' +
                ", maghrib='" + maghrib + '\'' +
                ", isha='" + isha + '\'' +
                '}';
    }

    public static class PrayerTimeSettings {
        private String country;
        private String city;
        private Method method;
        private int asrJuristic; // 0 for standard, 1 for Hanafi
        private boolean summerTiming;

        public PrayerTimeSettings() {
            loadSettings();
        }

        public boolean hasLocation(){
            return !this.getCountry().trim().equals("") && !this.getCity().trim().equals("");
        }

        public void save() {
            try {
                PrayerTimeSettings oldSettings = new PrayerTimeSettings();
                // if nothing changed
                if (oldSettings.getCountry().equals(this.getCountry()) &&
                        oldSettings.getCity().equals(this.getCity()) &&
                        oldSettings.getMethod().equals(this.getMethod()) &&
                        oldSettings.getAsrJuristic() == this.getAsrJuristic() &&
                        oldSettings.isSummerTiming() == this.isSummerTiming()) {
                    return;
                }
                // if summer timing only changed
                else if (oldSettings.getCountry().equals(this.getCountry()) &&
                        oldSettings.getCity().equals(this.getCity()) &&
                        oldSettings.getMethod().equals(this.getMethod()) &&
                        oldSettings.getAsrJuristic() == this.getAsrJuristic() &&
                        oldSettings.isSummerTiming() != this.isSummerTiming()) {
                    DatabaseManager.getInstance().
                            con.
                            prepareStatement("UPDATE prayertimes_settings set summer_timing = " + (summerTiming ? 1 : 0)).
                            executeUpdate();
                    return;
                }

                DatabaseManager databaseManager = DatabaseManager.getInstance();
                databaseManager.stat = databaseManager.con.prepareStatement("UPDATE prayertimes_settings set country = ?, city = ?, method = ?, asr_juristic = ?, summer_timing = ?");
                databaseManager.stat.setString(1, country);
                databaseManager.stat.setString(2, city);
                databaseManager.stat.setInt(3, method.getId());
                databaseManager.stat.setInt(4, asrJuristic);
                databaseManager.stat.setInt(5, summerTiming ? 1 : 0);
                databaseManager.stat.executeUpdate();

                PrayerTimesDBManager.deleteAll();
                new PrayerTimesValidation().start();
            } catch (Exception ex) {
                Logger.error(null, ex, getClass().getName() + ".save()");
            }
        }

        private void loadSettings() {
            try {
                ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_settings").executeQuery();
                if (res.next()) {
                    this.country = res.getString(1);
                    this.city = res.getString(2);
                    this.method = Method.getMethodByID(res.getInt(3));
                    this.asrJuristic = res.getInt(4);
                    this.summerTiming = res.getInt(5) == 1;
                }
            } catch (Exception ex) {
                Logger.error(null, ex, getClass().getName() + ".loadSettings()");
            }
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public boolean isSummerTiming() {
            return summerTiming;
        }

        public void setSummerTiming(boolean summerTiming) {
            this.summerTiming = summerTiming;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public int getAsrJuristic() {
            return asrJuristic;
        }

        public void setAsrJuristic(int asrJuristic) {
            this.asrJuristic = asrJuristic;
        }

        @Override
        public String toString() {
            return "PrayerTimeSettings{" +
                    "country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    ", method=" + method +
                    ", asrJuristic=" + asrJuristic +
                    ", summerTiming=" + summerTiming +
                    '}';
        }

        public static class Method {
            private int id;
            private String arabicName;
            private String englishName;

            public Method(int id, String arabicName, String englishName) {
                this.id = id;
                this.arabicName = arabicName;
                this.englishName = englishName;
            }

            public static StringConverter<Method> getStringConverter() {
                return new StringConverter<Method>() {
                    @Override
                    public String toString(Method object) {
                        return object.getArabicName();
                    }

                    @Override
                    public Method fromString(String string) {
                        return null;
                    }
                };
            }

            public static Method getMethodByID(int id) {
                try {
                    ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_methods WHERE id = " + id).executeQuery();
                    if (res.next()) {
                        return new Method(res.getInt(1), res.getString(2), res.getString(3));
                    }
                } catch (Exception ex) {
                    Logger.error(null, ex, Method.class.getName() + ".getMethodByID()");
                }
                return null;
            }

            public static ArrayList<Method> getListOfMethods() {
                ArrayList<Method> methods = new ArrayList<>();
                try {
                    ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_methods").executeQuery();
                    while (res.next()) {
                        methods.add(new Method(res.getInt(1), res.getString(2), res.getString(3)));
                    }
                } catch (Exception ex) {
                    Logger.error(null, ex, Method.class.getName() + ".getListOfMethods()");
                }
                return methods;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Method) {
                    return this.getId() == ((Method) obj).getId();
                }
                return false;
            }

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getArabicName() {
                return arabicName;
            }

            public void setArabicName(String arabicName) {
                this.arabicName = arabicName;
            }

            public String getEnglishName() {
                return englishName;
            }

            public void setEnglishName(String englishName) {
                this.englishName = englishName;
            }

            @Override
            public String toString() {
                return "Method{" +
                        "id=" + id +
                        ", arabicName='" + arabicName + '\'' +
                        ", englishName='" + englishName + '\'' +
                        '}';
            }
        }
    }
}
