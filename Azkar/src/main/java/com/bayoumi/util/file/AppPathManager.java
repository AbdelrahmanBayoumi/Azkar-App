package com.bayoumi.util.file;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import io.sentry.Sentry;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPathManager {

    private static String assetsPath;
    private static boolean assetsPathChanged = false;

    static {
        init();
    }

    public static synchronized void init() {
        if (assetsPath != null) {
            return;
        }
        try {
            Path installDir = getAppInstallDir();
            Path jarFilesDir = installDir.resolve("jarFiles");

            if (Files.isWritable(installDir) || (Files.exists(jarFilesDir) && Files.isWritable(jarFilesDir))) {
                assetsPath = "jarFiles";
                assetsPathChanged = false;
            } else {
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData == null || localAppData.isEmpty()) {
                    assetsPath = System.getProperty("user.home") + "/." + Constants.APP_NAME + "/jarFiles";
                } else {
                    assetsPath = localAppData + "/" + Constants.APP_NAME + "/jarFiles";
                }
                assetsPathChanged = true;
            }
        } catch (Exception ex) {
            assetsPath = "jarFiles";
            assetsPathChanged = false;
            Sentry.captureException(ex);
            Logger.error(ex.getLocalizedMessage(), ex, AppPathManager.class.getName() + " -> init");
        }
    }

    /**
     * Gets the absolute directory path where the application JAR / binaries are located.
     * Unlike user.dir (working directory), this always points to the true installation folder.
     */
    public static Path getAppInstallDir() {
        try {
            URI uri = AppPathManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(uri);
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }
            return path;
        } catch (Exception e) {
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }

    public static String getAssetsPath() {
        if (assetsPath == null) {
            init();
        }
        return assetsPath;
    }

    public static boolean isAssetsPathChanged() {
        return assetsPathChanged;
    }
}
