package com.bayoumi.util.db;

import com.bayoumi.util.Logger;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseHelper {

    public static boolean checkIfTablesExist(Connection con, String tableName) throws Exception {
        final ResultSet resultSet = con.prepareStatement("SELECT EXISTS ( SELECT name FROM sqlite_schema WHERE type='table' AND name='" + tableName + "' );").executeQuery();
        resultSet.next();
        Logger.debug("table: " + tableName + " = " + (resultSet.getInt(1) == 1));
        return resultSet.getInt(1) == 1;
    }
}
