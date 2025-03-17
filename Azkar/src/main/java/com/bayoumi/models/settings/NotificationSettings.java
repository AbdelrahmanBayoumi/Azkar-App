package com.bayoumi.models.settings;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.util.Logger;
import javafx.geometry.Pos;
import javafx.util.StringConverter;

import java.util.Objects;

public class NotificationSettings {
    private Pos position;
    private final NotificationColor notificationColor;

    protected NotificationSettings() {
        this.position = Pos.BOTTOM_RIGHT;
        this.notificationColor = new NotificationColor();
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
                    return "أعلى اليمين";
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
                    return "في المنتصف";
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
        if (Objects.equals(this.position, position)) return;
        this.position = position;
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_POS, this.position.toString());
    }

    public String getBorderColor() {
        return this.notificationColor.getBorderColor();
    }

    public void setBorderColor(String borderColor) {
        this.notificationColor.setBorderColor(borderColor);
    }

    public String getBackgroundColor() {
        return this.notificationColor.getBackgroundColor();
    }

    public void setBackgroundColor(String backgroundColor) {
        this.notificationColor.setBackgroundColor(backgroundColor);
    }

    public String getTextColor() {
        return this.notificationColor.getTextColor();
    }

    public void setTextColor(String textColor) {
        this.notificationColor.setTextColor(textColor);
    }

    @Override
    public String toString() {
        return "NotificationSettings{" +
                "position=" + position +
                ", notificationColor=" + notificationColor +
                '}';
    }
}