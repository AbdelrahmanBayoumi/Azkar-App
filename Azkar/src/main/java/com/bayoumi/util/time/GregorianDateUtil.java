package com.bayoumi.util.time;

import java.time.LocalDate;

public class GregorianDateUtil {
    // return this.getYear() + "-" + Utility.formatIntToTwoDigit(this.getMonth() + 1) + "-" + Utility.formatIntToTwoDigit(this.getDay());
    public static void main(String[] args) {
        System.out.println(LocalDate.of(2021, 4,5));
    }
}
