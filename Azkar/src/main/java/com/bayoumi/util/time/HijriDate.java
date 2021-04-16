package com.bayoumi.util.time;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HijriDate {
    private final int year;
    private final int month;
    private final int day;
    private final Calendar calendar;

    public HijriDate(int year, int month, int day) {
        this.calendar = new UmmalquraCalendar(year, month, day);
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public HijriDate(int dayOffset) {
        UmmalquraCalendar tempCal = new UmmalquraCalendar();
        this.calendar = new UmmalquraCalendar(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH),
                tempCal.get(Calendar.DAY_OF_MONTH) + dayOffset);
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }


    public String getString(String language) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM y", new Locale(language));
        dateFormat.setCalendar(this.calendar);
        return dateFormat.format(this.calendar.getTime());
    }



    /*

        public String getHijriDateFormatted() {
            return this.getYear() + "-" + Utility.formatIntToTwoDigit(this.getMonth() + 1) + "-" + Utility.formatIntToTwoDigit(this.getDay());
        }

        public static HijriDate plusDays(HijriDate hijriDate, int days) {
            return new HijriDate(hijriDate.getYear(), hijriDate.getMonth(), hijriDate.getDay() + days);
        }

        public static String getHijriDateNowString(String language) {
            Calendar cal = new UmmalquraCalendar();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM y", new Locale(language));
            dateFormat.setCalendar(cal);
            return dateFormat.format(cal.getTime());
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
    */

    @Override
    public String toString() {
        return "HijriDate{" +
                "year='" + year + '\'' +
                ", month='" + month + '\'' +
                ", day='" + day + '\'' +
                '}';
    }
}
