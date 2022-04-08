package com.bayoumi.models.settings;

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
        this.resourceBundle = ResourceBundle.getBundle("bundles.language", new Locale(Settings.getInstance().getOtherSettings().getLanguageLocal()));
        // if there is a change in HijriDate offset
        Settings.getInstance().getOtherSettings().addObserver((o, arg) ->
                setResourceBundle(ResourceBundle.getBundle("bundles.language", new Locale(Settings.getInstance().getOtherSettings().getLanguageLocal()))));
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
