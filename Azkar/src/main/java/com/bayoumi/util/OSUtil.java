package com.bayoumi.util;

public class OSUtil {
    private static final String OS = System.getProperty("os.name").toLowerCase();

    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_LINUX = OS.contains("linux");
    private static final boolean IS_MAC = OS.contains("mac");

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static boolean isLinux() {
        return IS_LINUX;
    }

    public static boolean isMac() {
        return IS_MAC;
    }

    public static String getOSName() {
        return System.getProperty("os.name");
    }
}
