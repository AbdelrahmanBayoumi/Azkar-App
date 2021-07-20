package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import javafx.util.StringConverter;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Objects;

public class PrayerTimeSettings {
    public static boolean isUpdated = false;
    private String country;
    private String city;
    private Method method;
    private int asrJuristic; // 0 for standard, 1 for Hanafi
    private boolean summerTiming;
    private double latitude;
    private double longitude;

    public PrayerTimeSettings() {
        loadSettings();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrayerTimeSettings that = (PrayerTimeSettings) o;
        return asrJuristic == that.asrJuristic &&
                summerTiming == that.summerTiming &&
                Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                Objects.equals(country, that.country) &&
                Objects.equals(city, that.city) &&
                Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, city, method, asrJuristic, summerTiming, latitude, longitude);
    }

    public void save() {
        try {
            if (this.equals(new PrayerTimeSettings())) {
                // if current obj is equal the one stored in DB then => do nothing (don't save)
                return;
            }
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("UPDATE prayertimes_settings set country = ?, city = ?, method = ?, asr_juristic = ?, summer_timing = ?,latitude = ?,longitude = ?;");
            databaseManager.stat.setString(1, country);
            databaseManager.stat.setString(2, city);
            databaseManager.stat.setInt(3, method.getId());
            databaseManager.stat.setInt(4, asrJuristic);
            databaseManager.stat.setInt(5, summerTiming ? 1 : 0);
            databaseManager.stat.setDouble(6, latitude);
            databaseManager.stat.setDouble(7, longitude);
            databaseManager.stat.executeUpdate();
            isUpdated = true;
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
    }

    public void loadSettings() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_settings").executeQuery();
            if (res.next()) {
                this.country = res.getString(1);
                this.city = res.getString(2);
                this.method = Method.getMethodByID(res.getInt(3));
                this.asrJuristic = res.getInt(4);
                this.summerTiming = res.getInt(5) == 1;
                this.latitude = res.getDouble("latitude");
                this.longitude = res.getDouble("longitude");
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


    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "PrayerTimeSettings{" +
                "country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", method=" + method +
                ", asrJuristic=" + asrJuristic +
                ", summerTiming=" + summerTiming +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public static class Method {
        private int id;
        private String arabicName;
        private String englishName;
        private String value;

        public Method(int id, String arabicName, String englishName, String value) {
            this.id = id;
            this.arabicName = arabicName;
            this.englishName = englishName;
            this.value = value;
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

        public static Method getMethodByValue(String value) {
            try {
                ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_methods WHERE value = '" + value + "'").executeQuery();
                if (res.next()) {
                    return new Method(res.getInt(1), res.getString(2), res.getString(3), res.getString(4));
                }
            } catch (Exception ex) {
                Logger.error(null, ex, Method.class.getName() + ".getMethodByValue()");
            }
            return null;
        }

        public static Method getMethodByID(int id) {
            try {
                ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_methods WHERE id = " + id).executeQuery();
                if (res.next()) {
                    return new Method(res.getInt(1), res.getString(2), res.getString(3), res.getString(4));
                }
            } catch (Exception ex) {
                Logger.error(null, ex, Method.class.getName() + ".getMethodByID()");
            }
            return null;
        }

        public static ArrayList<Method> getListOfMethods() {
            ArrayList<Method> methods = new ArrayList<>();
            try {
                ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes_methods WHERE value IS NOT NULL;").executeQuery();
                while (res.next()) {
                    methods.add(new Method(res.getInt(1), res.getString(2), res.getString(3), res.getString(4)));
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

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
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
