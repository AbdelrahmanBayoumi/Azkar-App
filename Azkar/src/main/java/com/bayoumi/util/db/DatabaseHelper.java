package com.bayoumi.util.db;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseHelper {

    public static boolean checkIfTablesExist(Connection con, String tableName) throws Exception {
        final ResultSet resultSet = con.prepareStatement("SELECT EXISTS ( SELECT name FROM sqlite_schema WHERE type='table' AND name='" + tableName + "' );").executeQuery();
        resultSet.next();
        System.out.println("table: " + tableName + " = " + (resultSet.getInt(1) == 1));
        return resultSet.getInt(1) == 1;
    }
}
