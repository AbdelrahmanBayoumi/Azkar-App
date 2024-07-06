package com.bayoumi.util;

import java.nio.file.Files;
import java.nio.file.Paths;

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
}
