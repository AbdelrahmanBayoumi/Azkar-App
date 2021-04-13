package com.bayoumi.util.time;

/**
 * Helper Methods
 */
public class ArabicNumeralDiscrimination {
    /**
     * @param minute value
     * @return Arabic String for the numerical discrimination
     * for minutes
     */
    public static String minutesArabicPlurality(int minute) {
        if (minute == 1) {
            return "دقيقة";
        } else if (minute == 2) {
            return "دقيقتين";
        } else if (minute < 11) {
            return "دقائق";
        } else {
            return "دقيقة";
        }
    }

    /**
     * @param hour value
     * @return Arabic String for the numerical discrimination
     * for hours
     */
    public static String hoursArabicPlurality(int hour) {
        if (hour == 1) {
            return "ساعة";
        } else if (hour == 2) {
            return "ساعتين";
        } else if (hour < 11) {
            return "ساعات";
        } else {
            return "ساعة";
        }
    }

    public static String getTimeArabicPlurality(int time) {
        String res = "";
        if (time % 60 != 0) {
            res += (isOneOrTwo(time % 60) ? "" : time % 60 + " ") + ArabicNumeralDiscrimination.minutesArabicPlurality(time % 60);
        }
        if (time / 60 != 0) {
            if (time % 60 != 0) {
                res += " و";
            }
            res += (isOneOrTwo(time / 60) ? "" : time / 60 + " ") + ArabicNumeralDiscrimination.hoursArabicPlurality(time / 60);
        }

        return res;
    }

    /**
     * checks if n number is equal to 1 or 2
     *
     * @param n input number
     * @return true if number is equal to 1 or 2
     * and false if not.
     */
    private static boolean isOneOrTwo(int n) {
        return n == 1 || n == 2;
    }

//    public static void main(String[] args) {
//        System.out.println(hoursArabicPlurality(3));
//        System.out.println(minutesArabicPlurality(155));
//    }
}
