package com.bayoumi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Constants {
    // Program characteristics
    public static String assetsPath;

    static {
        try {
            if (Files.isWritable(Paths.get(Constants.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                assetsPath = "jarFiles";
            } else {
                assetsPath = System.getenv("LOCALAPPDATA") + "/Azkar/jarFiles";
            }
//            assetsPath = System.getenv("LOCALAPPDATA") + "/Azkar/jarFiles";
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
    }


//    public static void main(String[] args) throws IOException {
//        Path from = Paths.get("jarFiles/db/assets.db");
//        Path to = Paths.get("D:/FFOutput/assets.db");
//        copyIfNotExist(from, to);

//        copyAssetsDBToAppData();
//    }

    public static void copyDatabaseToAppData() throws IOException {
        Utility.createDirectory(System.getenv("LOCALAPPDATA") + "/Azkar/jarFiles/db");
        Path from = Paths.get(new File("jarFiles/db/data.db").getAbsolutePath());
        System.out.println(from.toString());
        Path to = Paths.get(System.getenv("LOCALAPPDATA") + "/Azkar/jarFiles/db/data.db");
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
