package com.bayoumi.util;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

public class StartupUtil {

    public static void setAutostart(boolean enable) {
        if (OSUtil.isWindows()) {
            setWindowsAutostart(enable);
        } else if (OSUtil.isLinux()) {
            setLinuxAutostart(enable);
        }
    }

    public static boolean isAutostartEnabled() {
        if (OSUtil.isWindows()) {
            return isWindowsAutostartEnabled();
        } else if (OSUtil.isLinux()) {
            return isLinuxAutostartEnabled();
        }
        return false;
    }

    private static void setWindowsAutostart(boolean enable) {
        try {
            if (enable) {
                String executablePath = getExecutablePath();
                if (executablePath == null || !isValidAutostartExecutable(executablePath)) {
                    Logger.warn("Windows Autostart disabled: Running in development mode or non-executable environment (" + executablePath + ")");
                    return;
                }

                String cmd = executablePath.endsWith(".jar")
                        ? "java -jar \"" + executablePath + "\""
                        : "\"" + executablePath + "\"";

                ProcessBuilder pb = new ProcessBuilder("reg", "add",
                        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "Azkar", "/t", "REG_SZ",
                        "/d", cmd, "/f");
                pb.start();
            } else {
                // Always allow disabling, even during development
                ProcessBuilder pb = new ProcessBuilder("reg", "delete",
                        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "Azkar", "/f");
                pb.start();
            }
        } catch (Exception e) {
            Logger.error("Error setting Windows autostart", e, StartupUtil.class.getName());
        }
    }

    private static boolean isWindowsAutostartEnabled() {
        try {
            Process process = new ProcessBuilder("reg", "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "Azkar").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            Logger.error("Error checking Windows autostart", e, StartupUtil.class.getName());
            return false;
        }
    }

    private static void setLinuxAutostart(boolean enable) {
        try {
            File autostartDir = new File(System.getProperty("user.home") + "/.config/autostart");
            File desktopFile = new File(autostartDir, "Azkar.desktop");

            if (enable) {
                String executablePath = getExecutablePath();
                if (executablePath == null || !isValidAutostartExecutable(executablePath)) {
                    Logger.warn("Autostart disabled: Running in development mode or non-executable environment (" + executablePath + ")");
                    return;
                }

                if (!autostartDir.exists()) {
                    autostartDir.mkdirs();
                }

                File execFile = new File(executablePath);
                String workingDir = execFile.isDirectory() ? execFile.getAbsolutePath() : execFile.getParent();

                String execLine = executablePath.endsWith(".jar")
                        ? "java -jar \"" + executablePath + "\""
                        : "\"" + executablePath + "\"";

                String content = "[Desktop Entry]\n" +
                        "Type=Application\n" +
                        "Version=1.0\n" +
                        "Name=Azkar\n" +
                        "Comment=Azkar Application\n" +
                        "Exec=" + execLine + "\n" +
                        "Path=" + workingDir + "\n" +
                        "Icon=Azkar\n" +
                        "Terminal=false\n" +
                        "Categories=Utility;\n";
                try (FileWriter writer = new FileWriter(desktopFile)) {
                    writer.write(content);
                }
                desktopFile.setExecutable(true);
            } else {
                if (desktopFile.exists()) {
                    Files.delete(desktopFile.toPath());
                }
            }
        } catch (Exception e) {
            Logger.error("Error setting Linux autostart", e, StartupUtil.class.getName());
        }
    }

    private static boolean isLinuxAutostartEnabled() {
        try {
            File desktopFile = new File(System.getProperty("user.home") + "/.config/autostart/Azkar.desktop");
            return desktopFile.exists();
        } catch (Exception e) {
            Logger.error("Error checking Linux autostart", e, StartupUtil.class.getName());
            return false;
        }
    }

    /**
     * Validates that the given path is acceptable for autostart registration.
     * Accepted sources:
     * 1. install4j executablepath system property
     * 2. exe4j.moduleName system property
     * 3. Existing .jar file on disk
     *
     * Rejected: directories (e.g. target/classes/), missing files, non-jar regular files.
     */
    public static boolean isValidAutostartExecutable(String executablePath) {
        if (executablePath == null || executablePath.trim().isEmpty()) {
            return false;
        }
        // Accept install4j launcher
        String install4jExe = System.getProperty("executablepath");
        if (install4jExe != null && executablePath.equals(install4jExe)) {
            return true;
        }
        // Accept exe4j module
        String exe4jModule = System.getProperty("exe4j.moduleName");
        if (exe4jModule != null && executablePath.equals(exe4jModule)) {
            return true;
        }
        // Accept existing JAR files only
        if (executablePath.endsWith(".jar")) {
            File jarFile = new File(executablePath);
            return jarFile.isFile();
        }
        // Reject everything else (directories, non-jar files, missing files)
        return false;
    }

    private static String getExecutablePath() {
        try {
            // If running via install4j launcher
            String install4jExe = System.getProperty("executablepath");
            if (install4jExe != null && !install4jExe.isEmpty()) {
                return install4jExe;
            }
            // Fallback for Windows
            String exe4jModule = System.getProperty("exe4j.moduleName");
            if (exe4jModule != null && !exe4jModule.isEmpty()) {
                return exe4jModule;
            }

            // Fallback to jar location
            String path = StartupUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            return path;
        } catch (Exception e) {
            Logger.error("Error getting executable path", e, StartupUtil.class.getName());
            return null;
        }
    }
}
