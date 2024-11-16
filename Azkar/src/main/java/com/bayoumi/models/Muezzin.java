package com.bayoumi.models;

import com.bayoumi.util.Constants;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Muezzin {
    public final static String PARENT_PATH = "jarFiles/audio/adhan/";
    public final static String PARENT_PATH_UPLOAD = System.getenv("LOCALAPPDATA") + "/" + Constants.APP_NAME + "/jarFiles" + "/audio/";

    private final String fileName;
    private final String EnglishName;
    private final String ArabicName;

    // Static instances (replacing enum constants)
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

    public Muezzin(String englishName, String arabicName, String fileName) {
        this.fileName = fileName;
        EnglishName = englishName;
        ArabicName = arabicName;
    }

    public String getLocalPath() {
        return PARENT_PATH + getFileName();
    }

    public String getUploadedPath() {
        return PARENT_PATH_UPLOAD + getFileName();
    }

    public String getFileName() {
        return fileName;
    }

    public String getEnglishName() {
        return EnglishName;
    }

    public String getArabicName() {
        return ArabicName;
    }

    public static List<Muezzin> values() {
        return VALUES;
    }

    public static Muezzin getFromFileName(List<Muezzin> seatchList, String fileName) {
        for (Muezzin muezzin : seatchList) {
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
                ", EnglishName='" + EnglishName + '\'' +
                ", ArabicName='" + ArabicName + '\'' +
                '}';
    }
}