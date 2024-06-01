package com.bayoumi.models.azkar;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.button.TableViewButton;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AbsoluteZekr extends RecursiveTreeObject<AbsoluteZekr> {
    public final static ObservableList<AbsoluteZekr> absoluteZekrObservableList = FXCollections.observableArrayList();
    private int id;
    private String text;
    private TableViewButton edit;
    private TableViewButton delete;

    public AbsoluteZekr(int id, String text) {
        this.id = id;
        this.text = text;
        edit = new TableViewButton("", new FontAwesomeIconView(FontAwesomeIcon.EDIT));
        delete = new TableViewButton("", new FontAwesomeIconView(FontAwesomeIcon.TRASH));
        edit.setOnAction(this::update);
        delete.setOnAction(this::delete);
    }

    public static void returnDefault() {
        try {
            final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
            if (BuilderUI.showConfirmAlert(true, Utility.toUTF(bundle.getString("retrieveDefaultAzkarWarning")))) {
                DatabaseManager databaseManager = DatabaseManager.getInstance();
                // delete all current values
                databaseManager.con.prepareStatement("DELETE FROM absolute_zekr;").executeUpdate();
                // reset default values
                databaseManager.con.prepareStatement("INSERT INTO absolute_zekr SELECT * FROM absolute_zekr_default;").execute();
                fetchData();
            }
        } catch (Exception ex) {
            Logger.error(null, ex, AbsoluteZekr.class.getName() + ".returnDefault()");
        }
    }

    public static boolean fetchData() {
        absoluteZekrObservableList.clear();
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM absolute_zekr").executeQuery();
            while (res.next()) {
                absoluteZekrObservableList.add(new AbsoluteZekr(res.getInt(1), res.getString(2)));
            }
            return true;
        } catch (Exception ex) {
            Logger.error(null, ex, AbsoluteZekr.class.getName() + ".fetchData()");
        }
        return false;
    }

    public void update(Event event) {
        try {
            final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
            String newValue = BuilderUI.showEditTextField(Utility.toUTF(bundle.getString("zekr")), this.text);
            if (!newValue.equals("")) {
                DatabaseManager databaseManager = DatabaseManager.getInstance();
                databaseManager.stat = databaseManager.con.prepareStatement("UPDATE absolute_zekr set text = ? WHERE id =" + this.id);
                databaseManager.stat.setString(1, newValue);
                databaseManager.stat.executeUpdate();
                AbsoluteZekr.fetchData();
            }
        } catch (SQLException ex) {
            Logger.error(null, ex, getClass().getName() + ".update(id: " + this.id + ")");
        }
    }

    private void delete(Event event) {
        try {
            final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
            if (BuilderUI.showConfirmAlert(true, Utility.toUTF(bundle.getString("delete")) + " " +
                    Utility.toUTF(bundle.getString("zekr")) + " " + Utility.toUTF(bundle.getString("questionMark")))) {
                DatabaseManager.getInstance().con
                        .prepareStatement("DELETE FROM absolute_zekr WHERE id =" + this.id)
                        .executeUpdate();
                AbsoluteZekr.fetchData();
            }
        } catch (SQLException ex) {
            Logger.error(null, ex, getClass().getName() + ".delete(id: " + this.id + ")");
        }
    }

    public void insert() {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("INSERT INTO absolute_zekr (TEXT) VALUES(?)");
            databaseManager.stat.setString(1, this.text);
            databaseManager.stat.execute();
        } catch (SQLException ex) {
            Logger.error(null, ex, getClass().getName() + ".insert(text: " + this.text + ")");
        }
    }


    public TableViewButton getEdit() {
        return edit;
    }

    public void setEdit(TableViewButton edit) {
        this.edit = edit;
    }

    public TableViewButton getDelete() {
        return delete;
    }

    public void setDelete(TableViewButton delete) {
        this.delete = delete;
    }

    @Override
    public String toString() {
        return "AbsoluteZekr{" +
                "id=" + id +
                ", text='" + text + '\'' +
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
}
