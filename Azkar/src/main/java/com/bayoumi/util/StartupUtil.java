package com.bayoumi.util;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

public class StartupUtil {

    public static void setAutostart(boolean enable) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            setWindowsAutostart(enable);
        } else if (os.contains("linux")) {
            setLinuxAutostart(enable);
        }
    }

    public static boolean isAutostartEnabled() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return isWindowsAutostartEnabled();
        } else if (os.contains("linux")) {
            return isLinuxAutostartEnabled();
        }
        return false;
    }

    private static void setWindowsAutostart(boolean enable) {
        try {
            String executablePath = getExecutablePath();
            if (executablePath == null) return;

            String cmd = executablePath.endsWith(".jar") 
                    ? "java -jar \"" + executablePath + "\"" 
                    : "\"" + executablePath + "\"";

            ProcessBuilder pb;
            if (enable) {
                pb = new ProcessBuilder("reg", "add",
                        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "Azkar", "/t", "REG_SZ",
                        "/d", cmd, "/f");
            } else {
                pb = new ProcessBuilder("reg", "delete",
                        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "Azkar", "/f");
            }
            pb.start();
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
            if (!autostartDir.exists()) {
                autostartDir.mkdirs();
            }
            File desktopFile = new File(autostartDir, "Azkar.desktop");

            if (enable) {
                String executablePath = getExecutablePath();
                if (executablePath == null) return;

                String execLine = executablePath.endsWith(".jar")
                        ? "java -jar \"" + executablePath + "\""
                        : "\"" + executablePath + "\"";

                String content = "[Desktop Entry]\n" +
                        "Type=Application\n" +
                        "Version=1.0\n" +
                        "Name=Azkar\n" +
                        "Comment=Azkar Application\n" +
                        "Exec=" + execLine + "\n" +
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
            if (path.endsWith(".jar")) {
                return path;
            }
            return path;
        } catch (Exception e) {
            Logger.error("Error getting executable path", e, StartupUtil.class.getName());
            return null;
        }
    }
}
