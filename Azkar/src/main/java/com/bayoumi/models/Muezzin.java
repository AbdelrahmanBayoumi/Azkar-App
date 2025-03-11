package com.bayoumi.models;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.file.FileUtils;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Muezzin {
    private final String fileName;
    private final String englishName;
    private final String arabicName;

    // Static instances
    public static final Muezzin ABDULBASIT_ABDUSAMAD = new Muezzin("Abdulbasit Abdusamad", "عبد الباسط عبد الصمد", "adhan-abdulbasit-abdusamad.mp3");
    public static final Muezzin ABUL_AINAIN_SHUAISHA = new Muezzin("Abul Ainain Shuaisha", "أبو العنين شعيشع", "adhan-abul-ainain-shuaisha.mp3");
    public static final Muezzin ALI_IBN_AHMAD_MALA = new Muezzin("Ali Ibn Ahmad Mala", "علي بن أحمد ملا", "adhan-ali-ibn-ahmad-mala.mp3");
    public static final Muezzin MAHMOUD_ALI_ALBANNA = new Muezzin("Mahmoud Ali Al Banna", "محمود علي البنا", "adhan-mahmoud-ali-al-banna.mp3");
    public static final Muezzin MUHAMMAD_REFAAT = new Muezzin("Muhammad Refaat", "محمد رفعت", "adhan-muhammad-refaat.mp3");
    public static final Muezzin MUSTAFA_ISMAIL = new Muezzin("Mustafa Ismail", "مصطفى إسماعيل", "adhan-mustafa-ismail.mp3");
    public static final Muezzin NASSER_ALQATAMI = new Muezzin("Nasser Al Qatami", "ناصر القطامي", "adhan-nasser-al-qatami.mp3");
    public static final Muezzin NO_SOUND = new Muezzin("Silent", "بدون صوت", "");

    // List of all instances
    private static final List<Muezzin> VALUES;

    static {
        List<Muezzin> values = new ArrayList<>();
        values.add(ABDULBASIT_ABDUSAMAD);
        values.add(ABUL_AINAIN_SHUAISHA);
        values.add(ALI_IBN_AHMAD_MALA);
        values.add(MAHMOUD_ALI_ALBANNA);
        values.add(MUHAMMAD_REFAAT);
        values.add(MUSTAFA_ISMAIL);
        values.add(NASSER_ALQATAMI);
        values.add(NO_SOUND);
        VALUES = Collections.unmodifiableList(values);
    }

    public static String PARENT_PATH = "jarFiles/audio/adhan/";

    static {
        if (Constants.isAssetsPathChanged) {
            PARENT_PATH = Constants.assetsPath + "/audio/adhan/";
            try {
                Muezzin.copyAdhanFilesToAssetsPath();
            } catch (IOException e) {
                Logger.error(e.getLocalizedMessage(), e, Muezzin.class.getName() + ".copyAdhanFilesToAssetsPath()");
            }
        }
    }

    private static void copyAdhanFilesToAssetsPath() throws IOException {
        for (Muezzin muezzin : VALUES) {
            if (muezzin.equals(NO_SOUND)) continue;
            final Path from = Paths.get("jarFiles/audio/adhan/" + muezzin.getFileName()).toAbsolutePath();
            final Path to = Paths.get(Constants.assetsPath + "/audio/adhan/" + muezzin.getFileName()).toAbsolutePath();
            if (from.equals(to)) {
                Logger.debug("[Muezzin] Skipping from: " + from + " to: " + to);
                break;
            }
            Logger.debug("[Muezzin] Copying from: " + from + " to: " + to);
            FileUtils.copyIfNotExist(from, to);
        }
    }

    public static List<Muezzin> getAdhanList() {
        // Index Muezzin instances by fileName for quick lookup
        Map<String, Muezzin> muezzinMap = values().stream()
                .collect(Collectors.toMap(Muezzin::getFileName, muezzin -> muezzin));

        // Build the list of Muezzin objects
        return getAdhanFilesNames().stream()
                .map(fileName -> muezzinMap.getOrDefault(fileName, new Muezzin(FileUtils.removeExtension(fileName), FileUtils.removeExtension(fileName), fileName)))
                .collect(Collectors.toList());
    }

    public static List<String> getAdhanFilesNames() {
        List<String> audioFiles = new ArrayList<>();
        FileUtils.addFilesNameToList(new File(PARENT_PATH), audioFiles);
        return audioFiles;
    }

    // =========================================================================

    public Muezzin(String englishName, String arabicName, String fileName) {
        this.fileName = fileName;
        this.englishName = englishName;
        this.arabicName = arabicName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getArabicName() {
        return arabicName;
    }

    public String getPath() {
        return PARENT_PATH + getFileName();
    }

    public static List<Muezzin> values() {
        return VALUES;
    }

    public static Muezzin getFromFileName(List<Muezzin> muezzinList, String fileName) {
        for (Muezzin muezzin : muezzinList) {
            if (fileName.equals(muezzin.getFileName())) {
                return muezzin;
            }
        }
        if (!fileName.isEmpty()) return new Muezzin(fileName, fileName, fileName);
        return NO_SOUND;
    }

    public static StringConverter<Muezzin> arabicConverter() {
        return new StringConverter<Muezzin>() {
            @Override
            public String toString(Muezzin object) {
                return object.getArabicName();
            }

            @Override
            public Muezzin fromString(String string) {
                return null;
            }
        };
    }

    public static StringConverter<Muezzin> englishConverter() {
        return new StringConverter<Muezzin>() {
            @Override
            public String toString(Muezzin object) {
                return object.getEnglishName();
            }

            @Override
            public Muezzin fromString(String string) {
                return null;
            }
        };
    }

    @Override
    public String toString() {
        return "Muezzin{" +
                "fileName='" + fileName + '\'' +
                ", englishName='" + englishName + '\'' +
                ", arabicName='" + arabicName + '\'' +
                '}';
    }
}
