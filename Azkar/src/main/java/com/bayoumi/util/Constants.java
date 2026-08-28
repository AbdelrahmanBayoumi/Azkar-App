package com.bayoumi.util;

import com.bayoumi.util.file.AppPathManager;

public class Constants {
    public enum Mode {PRODUCTION, DEVELOPMENT}

    // Program characteristics
    public final static String APP_NAME = "Azkar";
    public final static String VERSION = "1.3.1";
    public final static Mode RUNNING_MODE = Mode.DEVELOPMENT;

    public final static String LOCATIONS_DB_URL = "https://github.com/AbdelrahmanBayoumi/LocationsDB/releases/latest/download/locations.db";
    public static final String QURAN_FONT_FAMILY = "Noto Naskh Arabic";

    public static final String assetsPath;

    static {
        AppPathManager.init();
        assetsPath = AppPathManager.getAssetsPath();
    }
}
