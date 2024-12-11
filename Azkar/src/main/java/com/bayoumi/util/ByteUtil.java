package com.bayoumi.util;

import java.util.Locale;

public class ByteUtil {
    public static String format(long value, Locale locale) {
        if (value < 1024) {
            return value + " B";
        }
        int z = (63 - Long.numberOfLeadingZeros(value)) / 10;
        return String.format(locale, "%.1f %sB", (double) value / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}
