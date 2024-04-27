package com.bayoumi.models.settings;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.file.FileUtils;
import javafx.util.StringConverter;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Observable;

public class PrayerTimeSettings extends Observable {

    private String country;
    private String city;
    private Method method;
    private int asrJuristic; // 0 for standard, 1 for Hanafi
    private boolean summerTiming;
    private double latitude;
    private double longitude;
    private String adhanAudio;
    private boolean isManualLocationSelected;

    protected PrayerTimeSettings() {
        loadSettings();
    }

    private void loadSettings() {
        this.country = Preferences.getInstance().get(PreferencesType.COUNTRY);
        this.city = Preferences.getInstance().get(PreferencesType.CITY);
        this.method = Method.getMethodByID(Preferences.getInstance().getInt(PreferencesType.METHOD));
        this.asrJuristic = Preferences.getInstance().getInt(PreferencesType.ASR_JURISTIC);
        this.summerTiming = Preferences.getInstance().getBoolean(PreferencesType.SUMMER_TIMING);
        this.latitude = Preferences.getInstance().getDouble(PreferencesType.LATITUDE);
        this.longitude = Preferences.getInstance().getDouble(PreferencesType.LONGITUDE);
        this.adhanAudio = Preferences.getInstance().get(PreferencesType.ADHAN_AUDIO);
        this.isManualLocationSelected = Preferences.getInstance().getBoolean(PreferencesType.IS_MANUAL_LOCATION_SELECTED);
    }

    public void handleNotifyObservers() {
        // fire change event to all listeners to update their values
        setChanged();
        notifyObservers();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        // 1. set value to local variable
        this.country = country;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.COUNTRY, country);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        // 1. set value to local variable
        this.city = city;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.CITY, city);
    }

    public boolean isSummerTiming() {
        return summerTiming;
    }

    public void setSummerTiming(boolean summerTiming) {
        // 1. set value to local variable
        this.summerTiming = summerTiming;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.SUMMER_TIMING, summerTiming + "");
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        // 1. set value to local variable
        this.method = method;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.METHOD, String.valueOf(method.getId()));
    }

    public int getAsrJuristic() {
        return asrJuristic;
    }

    public void setAsrJuristic(int asrJuristic) {
        // 1. set value to local variable
        this.asrJuristic = asrJuristic;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.ASR_JURISTIC, asrJuristic + "");
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        // 1. set value to local variable
        this.latitude = latitude;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.LATITUDE, latitude + "");
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        // 1. set value to local variable
        this.longitude = longitude;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.LONGITUDE, longitude + "");
    }

    public String getAdhanAudio() {
        if (FileUtils.getAdhanFilesNames().contains(adhanAudio)) {
            return adhanAudio;
        }
        return "";
    }

    public void setAdhanAudio(String adhanAudio) {
        // 1. set value to local variable
        this.adhanAudio = adhanAudio;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.ADHAN_AUDIO, adhanAudio);
    }

    public boolean isManualLocationSelected() {
        return isManualLocationSelected;
    }

    public void setManualLocationSelected(boolean manualLocationSelected) {
        // 1. set value to local variable
        isManualLocationSelected = manualLocationSelected;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.IS_MANUAL_LOCATION_SELECTED, manualLocationSelected + "");
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
                ", adhanAudio='" + adhanAudio + '\'' +
                ", isManualLocationSelected=" + isManualLocationSelected +
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

        public static StringConverter<Method> getStringConverter(boolean isArabic) {
            return new StringConverter<Method>() {
                @Override
                public String toString(Method object) {
                    return isArabic ? object.getArabicName() : object.getEnglishName();
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
