package com.bayoumi.util.time;

public class Utilities {
    public static String minutesArabicPlurality(int n) {
        if (n == 1) {
            return "دقيقة";
        } else if (n == 2) {
            return "دقيقتين";
        } else if (n < 11) {
            return "دقائق";
        } else {
            return "دقيقة";
        }
    }

    public static String hoursArabicPlurality(int n) {
        if (n == 1) {
            return "ساعة";
        } else if (n == 2) {
            return "ساعتين";
        } else if (n < 11) {
            return "ساعات";
        } else {
            return "ساعة";
        }
    }

//    public static String getTimeArabicPlurality(int time) {
//        return ((time % 60 == 0) ? "" : time % 60 + " " + com.bayoumi.util.time.Utilities.minutesArabicPlurality(time % 60))
//                + ((time / 60 == 0) ? "" : (time % 60 == 0 ? "" : " و") + time / 60 + " " + com.bayoumi.util.time.Utilities.hoursArabicPlurality(time / 60));
//    }

    public static String getTimeArabicPlurality(int time) {
        String res = "";
        if (time % 60 != 0) {
            res += (isOneOrTwo(time % 60) ? "" : time % 60 + " ") + com.bayoumi.util.time.Utilities.minutesArabicPlurality(time % 60);
        }
        // ((time / 60 == 0) ? "" : (time % 60 == 0 ? "" : " و") + )
        if (time / 60 != 0) {
            if (time % 60 != 0) {
                res += " و";
            }
            res += (isOneOrTwo(time / 60) ? "" : time / 60 + " ") + com.bayoumi.util.time.Utilities.hoursArabicPlurality(time / 60);
        }

        return res;
    }

    private static boolean isOneOrTwo(int n) {
        return n == 1 || n == 2;
    }

    public static void main(String[] args) {
        System.out.println(hoursArabicPlurality(3));
        System.out.println(minutesArabicPlurality(155));
    }
}
