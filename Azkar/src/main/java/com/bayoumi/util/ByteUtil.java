package com.bayoumi.util;

import java.util.Locale;

public class ByteUtil {
    public static void main(String[] args) {
        Locale locale = Locale.getDefault();
        Logger.debug(ByteUtil.format(1L, locale));
        Logger.debug(ByteUtil.format(23936, locale));
        Logger.debug(ByteUtil.format(3L * 1024 * 1024, locale));
        Logger.debug(ByteUtil.format(4L * 1024 * 1024 * 1024, locale));
        Logger.debug(ByteUtil.format(5L * 1024 * 1024 * 1024 * 1024, locale));
        Logger.debug(ByteUtil.format(6L * 1024 * 1024 * 1024 * 1024 * 1024, locale));
        Logger.debug(ByteUtil.format(Long.MAX_VALUE, locale));

    }

    public static String format(long value, Locale locale) {
        if (value < 1024) {
            return value + " B";
        }
        int z = (63 - Long.numberOfLeadingZeros(value)) / 10;
        return String.format(locale, "%.1f %sB", (double) value / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}
