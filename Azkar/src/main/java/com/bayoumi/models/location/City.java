package com.bayoumi.models.location;

import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.LocationsDBManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Objects;

public class City {
    private String countryCode;
    private String countryName;
    private String englishName;
    private String arabicName;
    private double latitude;
    private double longitude;
    private double timezone;

    public City(String countryCode, String englishName, String arabicName, double latitude, double longitude, double timezone) {
        this.countryCode = countryCode;
        this.englishName = englishName;
        this.arabicName = arabicName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    public City(String countryCode, String countryName, String englishName, String arabicName, double latitude, double longitude, double timezone) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.englishName = englishName;
        this.arabicName = arabicName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    /*
        CREATE TABLE "cityd" (
            "country"	VARCHAR,
            "city"	VARCHAR,
            "latitude"	DOUBLE,
            "longitude"	DOUBLE,
            "time_zone"	DOUBLE DEFAULT 0,
            "locId"	INTEGER NOT NULL,
            "Ar_Name"	VARCHAR,
            PRIMARY KEY("locId")
        )
        * */
    public static ArrayList<City> getCitiesInCountry(String code) {
        ArrayList<City> cities = new ArrayList<>();
        try {
            ResultSet res = LocationsDBManager.getInstance().con.prepareStatement("SELECT * from (SELECT * from (SELECT * from cityd WHERE country='" + code + "' GROUP BY city) " +
                    "WHERE country='" + code + "' GROUP BY longitude)  " +
                    "WHERE country='" + code + "' GROUP BY latitude ORDER BY city ASC;").executeQuery();
            while (res.next()) {
                cities.add(new City(
                        res.getString("country"), res.getString("city"),
                        res.getString("Ar_Name"), res.getDouble("latitude"),
                        res.getDouble("longitude"), res.getDouble("time_zone")
                ));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, City.class.getName() + ".getCitiesInCountry()");
        }
        return cities;
    }

    public static City getCityFromEngName(String englishName, String countryCode) {
        try {
            PreparedStatement statement = LocationsDBManager.getInstance().con.prepareStatement("SELECT * FROM cityd WHERE city = ? AND country = ?");
            statement.setString(1, englishName);
            statement.setString(2, countryCode);
            ResultSet res = statement.executeQuery();
            if (res.next()) {
                return new City(
                        res.getString("country"), res.getString("city"),
                        res.getString("Ar_Name"), res.getDouble("latitude"),
                        res.getDouble("longitude"), res.getDouble("time_zone")
                );
            }
        } catch (Exception ex) {
            Logger.error(null, ex, City.class.getName() + ".getCityFromEngName()");
        }
        return null;
    }

    public String getName() {
        if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
            return this.getArabicName() == null || this.getArabicName().equals("") ? this.getEnglishName() : this.getArabicName();
        }
        return this.getEnglishName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return Double.compare(city.latitude, latitude) == 0 &&
                Double.compare(city.longitude, longitude) == 0 &&
                Double.compare(city.timezone, timezone) == 0 &&
                countryCode.equals(city.countryCode) &&
                englishName.equals(city.englishName) &&
                Objects.equals(arabicName, city.arabicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, englishName, arabicName, latitude, longitude, timezone);
    }

    @Override
    public String toString() {
        return "City{" +
                "countryCode='" + countryCode + '\'' +
                ", countryName='" + countryName + '\'' +
                ", englishName='" + englishName + '\'' +
                ", arabicName='" + arabicName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timezone=" + timezone +
                '}';
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getArabicName() {
        return arabicName;
    }

    public void setArabicName(String arabicName) {
        this.arabicName = arabicName;
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

    public double getTimezone() {
        return timezone;
    }

    public void setTimezone(double timezone) {
        this.timezone = timezone;
    }
}
