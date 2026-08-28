package com.bayoumi.models.settings;

import com.bayoumi.util.SystemThemeUtil;
import com.bayoumi.util.Utility;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.ResourceBundle;

public enum Theme {
    LIGHT("light", "lightTheme"),
    DARK("dark", "darkTheme"),
    SYSTEM("system", "systemTheme");

    private final String preferenceValue;
    private final String bundleKey;

    Theme(String preferenceValue, String bundleKey) {
        this.preferenceValue = preferenceValue;
        this.bundleKey = bundleKey;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    public String getBundleKey() {
        return bundleKey;
    }

    public boolean isDark() {
        if (this == DARK) {
            return true;
        }
        if (this == LIGHT) {
            return false;
        }
        return SystemThemeUtil.isDark();
    }

    /**
     * Accepts the new values (light/dark/system) and the previous boolean
     * storage format (true/false) so existing installs keep their choice.
     */
    public static Theme from(String value) {
        if (value == null) {
            return SYSTEM;
        }
        switch (value.trim().toLowerCase()) {
            case "dark":
            case "true":
                return DARK;
            case "light":
            case "false":
                return LIGHT;
            case "system":
            default:
                return SYSTEM;
        }
    }

    public static StringConverter<Theme> stringConvertor(final ComboBox<Theme> comboBox, final ResourceBundle bundle) {
        return new StringConverter<Theme>() {
            @Override
            public String toString(Theme object) {
                return object != null ? Utility.toUTF(bundle.getString(object.getBundleKey())) : "";
            }

            @Override
            public Theme fromString(String string) {
                return comboBox.getItems().stream()
                        .filter(object -> Utility.toUTF(bundle.getString(object.getBundleKey())).equals(string))
                        .findFirst()
                        .orElse(Theme.SYSTEM);
            }
        };
    }
}
