package com.bayoumi.datamodel;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.ResultSet;

public class TimedZekr {
    public final static ObservableList<TimedZekr> MORNING_LIST = FXCollections.observableArrayList();
    public final static ObservableList<TimedZekr> NIGHT_LIST = FXCollections.observableArrayList();
    private int id;
    private String text;
    private int repeat;

    public TimedZekr(int id, String text, int repeat) {
        this.id = id;
        this.text = text;
        this.repeat = repeat;
    }

    public static void fetchNightAzkar() {
        NIGHT_LIST.clear();
        try {
            ResultSet res = DatabaseHandler.getInstance().con.prepareStatement("SELECT * FROM night_zekr").executeQuery();
            while (res.next()) {
                NIGHT_LIST.add(new TimedZekr(res.getInt(1), res.getString(2), res.getInt(3)));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, AbsoluteZekr.class.getName() + "fetchNightAzkar()");
        }
    }
    public static void fetchMorningAzkar() {
        MORNING_LIST.clear();
        try {
            ResultSet res = DatabaseHandler.getInstance().con.prepareStatement("SELECT * FROM morning_zekr").executeQuery();
            while (res.next()) {
                MORNING_LIST.add(new TimedZekr(res.getInt(1), res.getString(2), res.getInt(3)));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, AbsoluteZekr.class.getName() + "fetchMorningAzkar()");
        }
    }

    @Override
    public String toString() {
        return "TimedZekr{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", repeat=" + repeat +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }
}
