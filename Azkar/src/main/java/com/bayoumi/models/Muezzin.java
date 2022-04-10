package com.bayoumi.models;

import javafx.util.StringConverter;

public enum Muezzin {
    ABDULBASIT_ABDUSAMAD("Abdulbasit Abdusamad", "عبد الباسط عبد الصمد", "adhan-abdulbasit-abdusamad.mp3"),
    ABUL_AINAIN_SHUAISHA("Abul Ainain Shuaisha", "أبو العنين شعيشع", "adhan-abul-ainain-shuaisha.mp3"),
    ALI_IBN_AHMAD_MALA("Ali Ibn Ahmad Mala", "علي بن أحمد ملا", "adhan-ali-ibn-ahmad-mala.mp3"),
    MAHMOUD_ALI_ALBANNA("Mahmoud Ali Al Banna", "محمود علي البنا", "adhan-mahmoud-ali-al-banna.mp3"),
    MUHAMMAD_REFAAT("Muhammad Refaat", "محمد رفعت", "adhan-muhammad-refaat.mp3"),
    MUSTAFA_ISMAIL("Mustafa Ismail", "مصطفى إسماعيل", "adhan-mustafa-ismail.mp3"),
    NASSER_ALQATAMI("Nasser Al Qatami", "ناصر القطامي", "adhan-nasser-al-qatami.mp3"),
    NO_SOUND("Silent", "بدون صوت", "");
//    SHARAWY_DOAA("Sharawy Doaa", "دعاء الشعراوي", "sharawy_doaa.mp3");

    public final static String PARENT_PATH = "jarFiles/audio/adhan/";
    private final String fileName;
    private final String EnglishName;
    private final String ArabicName;

    Muezzin(String englishName, String arabicName, String fileName) {
        this.fileName = fileName;
        EnglishName = englishName;
        ArabicName = arabicName;
    }

    @Override
    public String toString() {
        return "Muezzin{" +
                "fileName='" + fileName + '\'' +
                ", EnglishName='" + EnglishName + '\'' +
                ", ArabicName='" + ArabicName + '\'' +
                '}';
    }

    public String getPath() {
        return PARENT_PATH + getFileName();
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

    public static Muezzin getFromFileName(String fileName) {
        for (Muezzin muezzin : Muezzin.values()) {
            if (fileName.equals(muezzin.getFileName())) {
                return muezzin;
            }
        }
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
}
