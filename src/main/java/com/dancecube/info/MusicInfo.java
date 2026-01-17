package com.dancecube.info;

import java.sql.*;
import com.tools.DatabaseConnection;
import org.junit.Test;

public class MusicInfo {
    public int id;
    public static String name;
    public static String Cover;

    public static MusicInfo getMusicInfo(long num) throws SQLException {
        MusicInfo musicInfo = null;
        try {
            DatabaseConnection.connect();
            String sqlExecution = "SELECT * FROM officialmusics WHERE id = ?";
            try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sqlExecution)) {
                pstmt.setLong(1, num);
                DatabaseConnection.executeResult = pstmt.executeQuery();
                if (DatabaseConnection.executeResult.next()) {
                    musicInfo = new MusicInfo();
                    musicInfo.id = DatabaseConnection.executeResult.getInt("id");
                    musicInfo.name = DatabaseConnection.executeResult.getString("name");
                    musicInfo.Cover = DatabaseConnection.executeResult.getString("coverLink");
                }
            }
        } finally {
            DatabaseConnection.disconnect();
        }
        return musicInfo;
    }

    @Test
    public void testGetMusicInfo() throws SQLException {
        MusicInfo musicInfo = getMusicInfo(6502);
        if (musicInfo != null) {
            System.out.println("ID: " + musicInfo.id);
            System.out.println("Name: " + musicInfo.name);
            System.out.println("Cover: " + musicInfo.Cover);
        }
    }
}