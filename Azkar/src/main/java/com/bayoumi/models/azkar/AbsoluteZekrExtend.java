package com.bayoumi.models.azkar;

import com.bayoumi.storage.DatabaseManager;
import com.bayoumi.util.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AbsoluteZekrExtend {
    public final static ObservableList<AbsoluteZekr> absoluteZekrObservableListJSON = FXCollections.observableArrayList();

    public static boolean fetchDataConflictResolution() {
        absoluteZekrObservableListJSON.clear();
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM absolute_zekr WHERE uuid is null or uuid='' ").executeQuery();
            while (res.next()) {
                absoluteZekrObservableListJSON.add(new AbsoluteZekr(res.getInt(1), res.getString(2),res.getString(4)));
            }
            return true;
        } catch (Exception ex) {
            Logger.error(null, ex, AbsoluteZekr.class.getName() + ".fetchData()");
        }
        return false;
    }
    public static void update(List<AbsoluteZekr> azkar) throws SQLException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

            try {
                databaseManager.con.setAutoCommit(false);
                databaseManager.stat = databaseManager.con.prepareStatement("UPDATE absolute_zekr SET uuid=? WHERE id=?");
                for (AbsoluteZekr zekr : azkar) {
                    databaseManager.stat.setString(1, zekr.getUUID());
                    databaseManager.stat.setInt(2, zekr.getId());
                    databaseManager.stat.addBatch();
                }
                databaseManager.stat.executeBatch();
                databaseManager.con.commit(); // Commit if all succeed
                databaseManager.stat.close();
            } catch (Exception e) {
                databaseManager.con.rollback(); // Rollback if something fails
                //throw e;
            } finally {
                databaseManager.con.setAutoCommit(true); // Restore default behavior
            }

    }
    public static void  insertBatched(List<AbsoluteZekr> azkar) throws SQLException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
            try {
                databaseManager.con.setAutoCommit(false);
                databaseManager.stat = databaseManager.con.prepareStatement("INSERT INTO absolute_zekr (text, uuid) VALUES (?, ?)");
                for (AbsoluteZekr zekr : azkar) {
                    databaseManager.stat.setString(1, zekr.getText());
                    databaseManager.stat.setString(2, zekr.getUUID());

                    databaseManager.stat.addBatch();
                }
                databaseManager.stat.executeBatch();
                databaseManager.con.commit(); // Commit if all succeed
                databaseManager.stat.close();
            } catch (Exception e) {
                databaseManager.con.rollback(); // Rollback if something fails
                //throw e;
            } finally {
                databaseManager.con.setAutoCommit(true); // Restore default behavior
            }

    }
}
