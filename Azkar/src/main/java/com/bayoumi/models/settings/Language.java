package com.bayoumi.models.settings;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.util.Logger;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

public enum Language {
    Arabic("ar", "عربي - Arabic"),
    English("en", "إنجليزية - English");

    private final String locale;
    private final String name;

    Language(String locale, String name) {
        this.locale = locale;
        this.name = name;
    }

    public String getLocale() {
        return locale;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return locale;
    }

    public static StringConverter<Language> stringConvertor(ComboBox<Language> comboBox) {
        return new StringConverter<Language>() {
            @Override
            public String toString(Language object) {
                return object != null ? object.getName() : "";
            }

            @Override
            public Language fromString(String string) {
                return comboBox.getItems().stream().filter(object ->
                        object.getName().equals(string)).findFirst().orElse(null);
            }
        };
    }


    public static Language get(String locale) {
        for (Language language : Language.values()) {
            if (language.getLocale().equals(locale)) {
                return language;
            }
        }
        return Language.English;
    }

    public static String getLocalFromPreferences() {
        return getLanguageFromPreferences().getLocale();
    }

    public static Language getLanguageFromPreferences() {
        try {
            return Language.get(Preferences.getInstance().get(PreferencesType.LANGUAGE));
        } catch (Exception e) {
            Logger.error(null, e, Language.class.getName() + ".getLanguageFromPreferences()");
            return Language.English;
        }
    }
}

