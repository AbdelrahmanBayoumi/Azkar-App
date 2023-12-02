package com.bayoumi.models.location;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.LocationsDBManager;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Objects;

public class Country {
    private String code;
    private String englishName;
    private String arabicName;
    private boolean summerTiming;

    public Country(String code, String englishName, String arabicName, boolean summerTiming) {
        this.code = code;
        this.englishName = englishName;
        this.arabicName = arabicName;
        this.summerTiming = summerTiming;
    }

    public static ArrayList<Country> getAll(String locale) {
        final ArrayList<Country> countries = new ArrayList<>();
        try {
            ResultSet res;
            if (locale.equals("ar")) {
                res = LocationsDBManager.getInstance().con.prepareStatement("SELECT * FROM Countries ORDER BY Ar_Name ASC").executeQuery();
            } else {
                res = LocationsDBManager.getInstance().con.prepareStatement("SELECT * FROM Countries ORDER BY En_Name ASC").executeQuery();
            }
            while (res.next()) {
                countries.add(new Country(res.getString("Code")
                        , res.getString("En_Name"), res.getString("Ar_Name"),
                        res.getInt("dls") == 1));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, Country.class.getName() + ".getAllData()");
        }
        return countries;
    }

    public static Country getCountryFormCode(String code) {
        try {
            ResultSet res = LocationsDBManager.getInstance().con.prepareStatement("SELECT * FROM Countries WHERE Code='" + code + "'").executeQuery();
            if (res.next()) {
                return new Country(res.getString("Code")
                        , res.getString("En_Name"), res.getString("Ar_Name"),
                        res.getInt("dls") == 1);
            }
        } catch (Exception ex) {
            Logger.error(null, ex, Country.class.getName() + ".getCountryFormCode()");
        }
        return null;
    }

    public static String getCountryNameFormCode(String code) {
        try {
            ResultSet res = LocationsDBManager.getInstance().con.prepareStatement("SELECT En_Name,Ar_Name FROM Countries WHERE Code='" + code + "'").executeQuery();
            if (res.next()) {
                if (res.getString("Ar_Name") == null || res.getString("Ar_Name").equals("")) {
                    return res.getString("En_Name");
                }
                return res.getString("Ar_Name");
            }
        } catch (Exception ex) {
            Logger.error(null, ex, Country.class.getName() + ".getCountryNameFormCode()");
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return summerTiming == country.summerTiming &&
                code.equals(country.code) &&
                Objects.equals(englishName, country.englishName) &&
                Objects.equals(arabicName, country.arabicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, englishName, arabicName, summerTiming);
    }

    @Override
    public String toString() {
        return "Country{" +
                "code='" + code + '\'' +
                ", englishName='" + englishName + '\'' +
                ", arabicName='" + arabicName + '\'' +
                ", summerTiming=" + summerTiming +
                '}';
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getName() {
        if (Settings.getInstance().getOtherSettings().getLanguageLocal().equals("ar")) {
            return this.getArabicName() == null || this.getArabicName().equals("") ? this.getEnglishName() : this.getArabicName();
        }
        return this.getEnglishName();
    }

    public String getArabicName() {
        return arabicName;
    }

    public void setArabicName(String arabicName) {
        this.arabicName = arabicName;
    }

    public boolean isSummerTiming() {
        return summerTiming;
    }

    public void setSummerTiming(boolean summerTiming) {
        this.summerTiming = summerTiming;
    }

}
/*
CREATE TABLE "Countries" (
	"Code"	VARCHAR NOT NULL,
	"Continent_Code"	VARCHAR,
	"En_Name"	VARCHAR,
	"iso3"	VARCHAR,
	"Number"	VARCHAR,
	"En_Full_Name"	VARCHAR,
	"mazhab"	INTEGER,
	"way"	INTEGER,
	"dls"	INTEGER,
	"Ar_Name"	VARCHAR,
	"islamic"	INTEGER,
	PRIMARY KEY("Code")
)
* */
