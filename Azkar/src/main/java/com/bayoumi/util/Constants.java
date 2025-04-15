package com.bayoumi.util;

import io.sentry.Sentry;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Constants {
    public enum Mode {PRODUCTION, DEVELOPMENT}

    // Program characteristics
    public static String assetsPath;
    public final static String APP_NAME = "Azkar";
    public final static String VERSION = "1.3.0";
    public final static Mode RUNNING_MODE = Mode.DEVELOPMENT;

    public final static String LOCATIONS_DB_URL = "https://github.com/AbdelrahmanBayoumi/LocationsDB/releases/latest/download/locations.db";
    public static final String QURAN_FONT_FAMILY = "Noto Naskh Arabic";

    public static boolean isAssetsPathChanged = false;

    static {
        try {
            if (Files.isWritable(Paths.get(Constants.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                assetsPath = "jarFiles";
            } else {
                assetsPath = System.getenv("LOCALAPPDATA") + "/" + Constants.APP_NAME + "/jarFiles";
                isAssetsPathChanged = true;
            }
        } catch (Exception ex) {
            Sentry.captureException(ex);
            // TODO is Logger valid here or its not initialized yet ?
            Logger.error(ex.getLocalizedMessage(), ex, Constants.class.getName() + " -> static init");
        }
    }
}
