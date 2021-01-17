package com.bayoumi.util.time;

import com.bayoumi.util.Utilities;
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HijriDate {
    private int year;
    private int month;
    private int day;
    private Calendar calendar;

    public HijriDate(int year, int month, int day) {
        this.calendar = new UmmalquraCalendar(year, month, day);
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public HijriDate() {
        this.calendar = new UmmalquraCalendar();
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static HijriDate plusDay(HijriDate hijriDate, int days) {
        return new HijriDate(hijriDate.getYear(), hijriDate.getMonth(), hijriDate.getDay() + days);
    }

    public static String getHijriDateNowString(String language) {
        Calendar cal = new UmmalquraCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM y", new Locale(language));
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public String getHijriDateFormatted() {
        return this.getYear() + "-" + Utilities.formatIntToTwoDigit(this.getMonth() + 1) + "-" + Utilities.formatIntToTwoDigit(this.getDay());
    }


    private String getHijriYear(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("y");
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    private String getHijriDayOfMonth(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    private String getHijriMonth(Calendar cal, String language) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM", new Locale(language));
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    private String getHijriDateOf(HijriDate hijriDate, String language) {
        Calendar cal = new UmmalquraCalendar(hijriDate.getYear(), hijriDate.getMonth(), hijriDate.getDay());
        return getHijriYear(cal) + getHijriMonth(cal, language) + getHijriDayOfMonth(cal);
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
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
