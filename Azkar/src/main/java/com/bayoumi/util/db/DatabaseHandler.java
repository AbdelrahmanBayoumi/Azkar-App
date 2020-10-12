package com.bayoumi.util.db;


import com.bayoumi.util.Logger;

import java.sql.*;

public class DatabaseHandler {

    private static DatabaseHandler handler = null;  // static
    public PreparedStatement stat = null;
    public Connection con = null;

    private DatabaseHandler() {
    }

    public static DatabaseHandler getInstance() {
        if (handler == null) {
            handler = new DatabaseHandler();
        }
        return handler;
    }

    public boolean connectToDatabase() {
        try {
            if (con == null) {
//                .... Connect To Server ....
//                Class.forName("com.mysql.cj.jdbc.Driver");
//                String dbName = "card_store", user = "root", pass = "";
//                con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + dbName + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=EST5EDT", user, pass);

//                .... Connect to SQlLite ....
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection("jdbc:sqlite:data.db");
                con.prepareStatement("PRAGMA foreign_keys=ON").execute();
                return true;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            con = null;
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".connectToDatabase()");
        }
        return false;
    }
}
