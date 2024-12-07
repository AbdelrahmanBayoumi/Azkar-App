package com.bayoumi.models.settings;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.util.Logger;
import javafx.geometry.Pos;
import javafx.util.StringConverter;

public class NotificationSettings {
    private Pos position;
    private String borderColor;
    private String backgroundColor;
    private String textColor;

    protected NotificationSettings() {
        this.position = Pos.BOTTOM_RIGHT;
        this.borderColor = PreferencesType.NOTIFICATION_BORDER_COLOR.getDefaultValue();
        this.backgroundColor = PreferencesType.NOTIFICATION_BACKGROUND_COLOR.getDefaultValue();
        this.textColor = PreferencesType.NOTIFICATION_TEXT_COLOR.getDefaultValue();
        loadSettings();
    }

    public static StringConverter<Pos> posEnglishConverter() {
        return new StringConverter<Pos>() {
            @Override
            public String toString(Pos object) {
                if (object.equals(Pos.TOP_RIGHT)) {
                    return "Top Right";
                }
                if (object.equals(Pos.BOTTOM_RIGHT)) {
                    return "Bottom Right";
                }
                if (object.equals(Pos.TOP_LEFT)) {
                    return "Top Left";
                }
                if (object.equals(Pos.BOTTOM_LEFT)) {
                    return "Bottom Left";
                }
                if (object.equals(Pos.CENTER)) {
                    return "Center";
                }
                return object.toString();
            }

            @Override
            public Pos fromString(String string) {
                return null;
            }
        };
    }

    public static StringConverter<Pos> posArabicConverter() {
        return new StringConverter<Pos>() {
            @Override
            public String toString(Pos object) {
                if (object.equals(Pos.TOP_RIGHT)) {
                    return "فوق على اليمين";
                }
                if (object.equals(Pos.BOTTOM_RIGHT)) {
                    return "أسفل اليمين";
                }
                if (object.equals(Pos.TOP_LEFT)) {
                    return "أعلى اليسار";
                }
                if (object.equals(Pos.BOTTOM_LEFT)) {
                    return "أسفل اليسار";
                }
                if (object.equals(Pos.CENTER)) {
                    return "المنتصف";
                }
                return object.toString();
            }

            @Override
            public Pos fromString(String string) {
                return null;
            }
        };
    }

    private void loadSettings() {
        try {
            this.position = Pos.valueOf(Preferences.getInstance().get(PreferencesType.NOTIFICATION_POS, PreferencesType.NOTIFICATION_POS.getDefaultValue()));
            this.borderColor = Preferences.getInstance().get(PreferencesType.NOTIFICATION_BORDER_COLOR, PreferencesType.NOTIFICATION_BORDER_COLOR.getDefaultValue());
            this.backgroundColor = Preferences.getInstance().get(PreferencesType.NOTIFICATION_BACKGROUND_COLOR, PreferencesType.NOTIFICATION_BACKGROUND_COLOR.getDefaultValue());
            this.textColor = Preferences.getInstance().get(PreferencesType.NOTIFICATION_TEXT_COLOR, PreferencesType.NOTIFICATION_TEXT_COLOR.getDefaultValue());
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    public Pos getPosition() {
        if (position == null)
            return Pos.BOTTOM_RIGHT;
        return position;
    }

    public void setPosition(Pos position) {
        this.position = position;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_POS, this.position.toString());
    }

    public String getBorderColor() {
        if (borderColor == null || borderColor.isEmpty())
            return PreferencesType.NOTIFICATION_BORDER_COLOR.getDefaultValue();
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_BORDER_COLOR, this.borderColor);
    }

    public String getBackgroundColor() {
        if (backgroundColor == null || backgroundColor.isEmpty())
            return PreferencesType.NOTIFICATION_BACKGROUND_COLOR.getDefaultValue();
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_BACKGROUND_COLOR, this.backgroundColor);
    }

    public String getTextColor() {
        if (textColor == null || textColor.isEmpty())
            return PreferencesType.NOTIFICATION_TEXT_COLOR.getDefaultValue();
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_TEXT_COLOR, this.textColor);
    }

    @Override
    public String toString() {
        return "NotificationSettings{" +
                "position=" + position +
                ", borderColor='" + borderColor + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                ", textColor='" + textColor + '\'' +
                '}';
    }
}
