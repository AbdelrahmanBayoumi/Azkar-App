package com.bayoumi.util.time;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HijriDate {
    private String year;
    private String month;
    private String day;

    public HijriDate(String year, String month, String day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static String getHijriDayOfMonth(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public static String getHijriMonth(Calendar cal, String language) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM", new Locale(language));
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public static String getHijriYear(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("y");
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public static HijriDate getHijriDate(String language) {
        Calendar calendar = new UmmalquraCalendar();
        return new HijriDate(getHijriYear(calendar), getHijriMonth(calendar, language), getHijriDayOfMonth(calendar));
    }

    public static String getHijriDateString(String language) {
        Calendar cal = new UmmalquraCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM y", new Locale(language));
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    @Override
    public String toString() {
        return "HijriDate{" +
                "year='" + year + '\'' +
                ", month='" + month + '\'' +
                ", day='" + day + '\'' +
                '}';
    }
}
