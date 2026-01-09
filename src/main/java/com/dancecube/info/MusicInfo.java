package com.dancecube.info;

import java.sql.*;
import com.tools.DatabaseConnection;
import org.junit.Test;

import javax.xml.crypto.Data;

public class MusicInfo {
    public static int id;
    public static String name;
    public static String Cover;
    public static Connection getMusicInfo(long num) throws SQLException {
        DatabaseConnection.connect();
        String sqlExecution = "SELECT * FROM officialmusics WHERE id = %s".formatted(num);
        DatabaseConnection.executeSql(sqlExecution);
        while (DatabaseConnection.executeResult.next()) {
            try {
                id = DatabaseConnection.executeResult.getInt("id");
                name = DatabaseConnection.executeResult.getString("name");
                Cover = DatabaseConnection.executeResult.getString("Cover");


            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Test
    public void testGetMusicInfo() throws SQLException {
        getMusicInfo(20401);
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Cover: " + Cover);
    }
}
