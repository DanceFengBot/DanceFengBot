package com.DanceCube.info;

import java.sql.*;
import java.util.Objects;

import com.Tools.DatabaseConnection;
import org.junit.Test;
public class MusicInfo {
    public static int id = -1; //初始值
    public static String name;
    public static String Cover;
    public static String Audio;

    public static MusicInfo get(long num) throws SQLException {
        DatabaseConnection.connect();
        String sqlExecution = "SELECT * FROM officialmusics WHERE id = ?";
        try (PreparedStatement pstmt = Objects.requireNonNull(DatabaseConnection.getConnection()).prepareStatement(sqlExecution)) {
            pstmt.setLong(1, num);
            DatabaseConnection.executeResult = pstmt.executeQuery();
            if (DatabaseConnection.executeResult.next()) {
                id = DatabaseConnection.executeResult.getInt("id");
                name = DatabaseConnection.executeResult.getString("name");
                Cover = DatabaseConnection.executeResult.getString("coverLink");
                Audio = DatabaseConnection.executeResult.getString("audioLink");
            }
        } finally {
            DatabaseConnection.disconnect();
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCoverLink() {
        return Cover;
    }

    public String getAudioLink() {
        return Audio;
    }

    public String toString() {
        return "ID：" + id + "\n" +
                "歌曲名：" + name + "\n"+
                "音频链接：" + Audio;
    }

    @Test
    public void testGetMusicInfo() throws SQLException {
        MusicInfo musicInfo = get(21156);
        if(musicInfo.id == -1){
            System.out.println("No Data");
            return;
        }
        System.out.println(toString());
    }
}