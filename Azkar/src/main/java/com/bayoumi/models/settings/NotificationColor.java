package com.bayoumi.models.settings;

import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.preferences.PreferencesType;
import com.bayoumi.util.Logger;

import java.util.Objects;

public class NotificationColor {
    public static final NotificationColor LIGHT_THEME = new NotificationColor("#E9C46A", "#FFFFFF", "#000000");
    public static final NotificationColor DARK_THEME = new NotificationColor("#2C3F51", "#192735", "#FFFFFF");

    public static void setLightTheme() {
        Settings.getInstance().getNotificationSettings().setBorderColor(LIGHT_THEME.borderColor);
        Settings.getInstance().getNotificationSettings().setBackgroundColor(LIGHT_THEME.backgroundColor);
        Settings.getInstance().getNotificationSettings().setTextColor(LIGHT_THEME.textColor);
    }

    public static void setDarkTheme() {
        Settings.getInstance().getNotificationSettings().setBorderColor(DARK_THEME.borderColor);
        Settings.getInstance().getNotificationSettings().setBackgroundColor(DARK_THEME.backgroundColor);
        Settings.getInstance().getNotificationSettings().setTextColor(DARK_THEME.textColor);
    }


    private String borderColor;
    private String backgroundColor;
    private String textColor;

    public NotificationColor(String borderColor, String backgroundColor, String textColor) {
        this();
        this.borderColor = borderColor;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public NotificationColor() {
        loadSettings();
    }

    private void loadSettings() {
        try {
            this.borderColor = Preferences.getInstance().get(PreferencesType.NOTIFICATION_BORDER_COLOR, PreferencesType.NOTIFICATION_BORDER_COLOR.getDefaultValue());
            this.backgroundColor = Preferences.getInstance().get(PreferencesType.NOTIFICATION_BACKGROUND_COLOR, PreferencesType.NOTIFICATION_BACKGROUND_COLOR.getDefaultValue());
            this.textColor = Preferences.getInstance().get(PreferencesType.NOTIFICATION_TEXT_COLOR, PreferencesType.NOTIFICATION_TEXT_COLOR.getDefaultValue());
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }


    public String getBorderColor() {
        if (borderColor == null || borderColor.isEmpty())
            return PreferencesType.NOTIFICATION_BORDER_COLOR.getDefaultValue();
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        if (Objects.equals(borderColor, this.borderColor)) return;
        this.borderColor = borderColor;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_BORDER_COLOR, this.borderColor);
    }

    public String getBackgroundColor() {
        if (backgroundColor == null || backgroundColor.isEmpty())
            return PreferencesType.NOTIFICATION_BACKGROUND_COLOR.getDefaultValue();
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        if (Objects.equals(backgroundColor, this.backgroundColor)) return;
        this.backgroundColor = backgroundColor;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_BACKGROUND_COLOR, this.backgroundColor);
    }

    public String getTextColor() {
        if (textColor == null || textColor.isEmpty())
            return PreferencesType.NOTIFICATION_TEXT_COLOR.getDefaultValue();
        return textColor;
    }

    public void setTextColor(String textColor) {
        if (Objects.equals(textColor, this.textColor)) return;
        this.textColor = textColor;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_TEXT_COLOR, this.textColor);
    }

    @Override
    public String toString() {
        return "NotificationColor{" +
                "borderColor='" + borderColor + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                ", textColor='" + textColor + '\'' +
                '}';
    }
}
