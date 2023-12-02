package com.bayoumi.models.settings;

import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.BuilderUI;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.util.StringConverter;

public class NotificationSettings {
    private Pos position;

    protected NotificationSettings() {
        this.position = Pos.BOTTOM_RIGHT;
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
            this.position = Pos.valueOf(Preferences.getInstance().get(PreferencesType.NOTIFICATION_POS, "BOTTOM_LEFT"));
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    public void save() {
        try {
            Preferences.getInstance().set(PreferencesType.NOTIFICATION_POS, this.position.toString());
        } catch (Exception ex) {
            BuilderUI.showOkAlert(Alert.AlertType.ERROR, "ERROR in saving notification position", false);
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
    }

    public Pos getPosition() {
        return position;
    }

    public void setPosition(Pos position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "position=" + position +
                '}';
    }
}
