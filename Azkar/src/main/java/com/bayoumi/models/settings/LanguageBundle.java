package com.bayoumi.models.settings;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;

import java.util.Locale;
import java.util.Observable;
import java.util.ResourceBundle;

public class LanguageBundle extends Observable {

    private ResourceBundle resourceBundle;
    private static LanguageBundle instance = null;

    public static LanguageBundle getInstance() {
        if (instance == null) {
            instance = new LanguageBundle();
        }
        return instance;
    }

    private LanguageBundle() {
        this.resourceBundle = ResourceBundle.getBundle("bundles.language", new Locale(Language.getLocalFromPreferences()));
        Preferences.getInstance().addObserver((key, value) -> {
            if (key.equals(PreferencesType.LANGUAGE)) {
                setResourceBundle(ResourceBundle.getBundle("bundles.language", new Locale(Language.getLocalFromPreferences())));
            }
        });
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.setChanged();
        this.notifyObservers();
    }
}
