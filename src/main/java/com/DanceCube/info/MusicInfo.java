package com.DanceCube.info;

import java.sql.*;
import com.Tools.DatabaseConnection;
import org.junit.Test;

public class MusicInfo {
    public static int id = -1; //初始值
    public static String name;
    public static String Cover;
    public static String Audio;

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
                    id = DatabaseConnection.executeResult.getInt("id");
                    name = DatabaseConnection.executeResult.getString("name");
                    Cover = DatabaseConnection.executeResult.getString("coverLink");
                    Audio = DatabaseConnection.executeResult.getString("audioLink");
                }
            }
        } finally {
            DatabaseConnection.disconnect();
        }
        return musicInfo;
    }

    @Test
    public void testGetMusicInfo() throws SQLException {
        MusicInfo musicInfo = getMusicInfo(4396);
        if(MusicInfo.id == -1){
            System.out.println("No Data");
            return;
        }
        System.out.println("ID: " + musicInfo.id);
        System.out.println("Name: " + musicInfo.name);
        System.out.println("Cover: " + musicInfo.Cover);

    }
}