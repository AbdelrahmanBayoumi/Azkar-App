package com.bayoumi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Constants {
    public enum Mode {PRODUCTION, DEVELOPMENT}

    // Program characteristics
    public static String assetsPath;
    public final static String APP_NAME = "Azkar";
    public final static String VERSION = "1.2.8";
    public final static String LOCATIONS_DB_URL = "https://github.com/AbdelrahmanBayoumi/LocationsDB/releases/latest/download/locations.db";
    public static final String QURAN_FONT_FAMILY = "Noto Naskh Arabic";
    public final static Mode RUNNING_MODE = Mode.PRODUCTION;

    static {
        try {
            if (Files.isWritable(Paths.get(Constants.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                assetsPath = "jarFiles";
            } else {
                assetsPath = System.getenv("LOCALAPPDATA") + "/" + Constants.APP_NAME + "/jarFiles";
            }
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, Constants.class.getName() + " -> static init");
        }
    }

    public static void copyDatabaseToAppData() throws IOException {
        Utility.createDirectory(System.getenv("LOCALAPPDATA") + "/" + Constants.APP_NAME + "/jarFiles/db");
        Path from = Paths.get(new File("jarFiles/db/data.db").getAbsolutePath());
        Logger.debug(from);
        Path to = Paths.get(System.getenv("LOCALAPPDATA") + "/" + Constants.APP_NAME + "/jarFiles/db/data.db");
        copyIfNotExist(from, to);
    }

    public static void copyIfNotExist(Path from, Path to) throws IOException {
        File f = new File(to.toString());
        if (f.exists() && !f.isDirectory()) {
            return;
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

}
