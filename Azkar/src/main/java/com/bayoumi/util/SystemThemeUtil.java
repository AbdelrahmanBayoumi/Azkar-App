package com.bayoumi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Detects the OS light/dark preference. Java 8 has no public API for this,
 * so each platform is queried with its native setting.
 */
public final class SystemThemeUtil {
    private static final long CACHE_TTL_MS = 2000L;

    private static volatile Boolean cachedIsDark;
    private static volatile long cachedAt;

    private SystemThemeUtil() {
    }

    public static boolean isDark() {
        final long now = System.currentTimeMillis();
        final Boolean cached = cachedIsDark;
        if (cached != null && now - cachedAt < CACHE_TTL_MS) {
            return cached;
        }
        final boolean detected = detect();
        cachedIsDark = detected;
        cachedAt = now;
        return detected;
    }

    public static void invalidateCache() {
        cachedIsDark = null;
    }

    private static boolean detect() {
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                return isWindowsDark();
            }
            if (os.contains("mac")) {
                return isMacDark();
            }
            return isLinuxDark();
        } catch (Exception ex) {
            Logger.error(null, ex, SystemThemeUtil.class.getName() + ".detect()");
            return false;
        }
    }

    private static boolean isWindowsDark() {
        String reg = "reg";
        final String windir = System.getenv("windir");
        if (windir != null && !windir.isEmpty()) {
            reg = windir + "\\System32\\reg.exe";
        }
        final String output = runCommand(reg, "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme");
        if (output == null) {
            return false;
        }
        final String[] tokens = output.trim().split("\\s+");
        if (tokens.length == 0) {
            return false;
        }
        final String last = tokens[tokens.length - 1];
        if (last.toLowerCase(Locale.ROOT).startsWith("0x")) {
            try {
                return Integer.parseInt(last.substring(2), 16) == 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return "0".equals(last);
    }

    private static boolean isMacDark() {
        final String output = runCommand("defaults", "read", "-g", "AppleInterfaceStyle");
        return output != null && output.toLowerCase(Locale.ROOT).contains("dark");
    }

    private static boolean isLinuxDark() {
        final String colorScheme = runCommand("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
        if (colorScheme != null && colorScheme.toLowerCase(Locale.ROOT).contains("dark")) {
            return true;
        }
        final String gtkTheme = runCommand("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
        if (gtkTheme != null && gtkTheme.toLowerCase(Locale.ROOT).contains("dark")) {
            return true;
        }
        final String kde5 = runCommand("kreadconfig5", "--group", "General", "--key", "ColorScheme");
        if (kde5 != null && kde5.toLowerCase(Locale.ROOT).contains("dark")) {
            return true;
        }
        final String kde6 = runCommand("kreadconfig6", "--group", "General", "--key", "ColorScheme");
        return kde6 != null && kde6.toLowerCase(Locale.ROOT).contains("dark");
    }

    private static String runCommand(String... command) {
        Process process = null;
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            final StringBuilder output = new StringBuilder();
            final Process runningProcess = process;
            final Thread reader = new Thread(() -> {
                try (BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(runningProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (output.length() > 0) {
                            output.append('\n');
                        }
                        output.append(line);
                    }
                } catch (Exception ignored) {
                }
            }, "system-theme-cmd");
            reader.setDaemon(true);
            reader.start();

            final boolean finished = process.waitFor(1500, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                return null;
            }
            reader.join(200);
            if (process.exitValue() != 0 && output.length() == 0) {
                return null;
            }
            return output.toString();
        } catch (Exception ex) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
