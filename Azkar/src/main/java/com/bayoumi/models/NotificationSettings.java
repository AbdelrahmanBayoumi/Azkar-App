package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import javafx.geometry.Pos;
import javafx.util.StringConverter;

import java.sql.ResultSet;

public class NotificationSettings {
    private Pos position;

    public NotificationSettings() {
        this.position = Pos.BOTTOM_RIGHT;
        loadSettings();
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
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM notification").executeQuery();
            if (res.next()) {
                this.position = Pos.valueOf(res.getString("position"));
            }
            System.out.println(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    public void save() {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("UPDATE notification set position = ?");
            databaseManager.stat.setString(1, this.position.toString());
            databaseManager.stat.executeUpdate();
        } catch (Exception ex) {
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
