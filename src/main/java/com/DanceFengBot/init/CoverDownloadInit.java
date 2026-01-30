package com.DanceFengBot.init;
import com.Tools.DatabaseConnection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CoverDownloadInit {
    static Long musicId = null;
    static String coverUrl = null;
    private static final List<String> coverUrls = new ArrayList<>();
    private static final List<Long> coverIds = new ArrayList<>();

    public static void Downloader() throws IOException {
        patcher();

        for (int i = 0; i < coverUrls.size(); ++i) {
            String fileUrl = coverUrls.get(i);
            Long id = coverIds.get(i);
            String savePath = "./OfficialCover" + id + ".jpg";
            Path path = Path.of(savePath);
            if (!Files.exists(path)) {
                System.out.println("savePath Not Found, Creating Directory: " + savePath);
                Files.createDirectory(path.getParent());
                break;
            }

            try {
                URL url = new URL(fileUrl);
                URLConnection connection = url.openConnection();
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(savePath);
                byte[] buffer = new byte[1024];

                while (inputStream.read(buffer) != -1) {
                }

                inputStream.close();
                outputStream.close();
                System.out.println("File Download Complete: " + savePath);
            } catch (IOException var10) {
                System.err.println("Download Failed: " + var10.getMessage());
            }
        }
    }

    private static void patcher() {
        try {
            if (!DatabaseConnection.isConnected()) {
                System.err.println("Database not connected. Please connect first.");
            }

            String query = "SELECT coverLink,id FROM `officialmusics` WHERE 1;"; // Replace with your actual table and column names
            if (!DatabaseConnection.executeSql(query)) {
                System.err.println("Failed to execute query.");
                return;
            }

            ResultSet resultSet = DatabaseConnection.executeResult;
            while (resultSet.next()) {
                musicId = resultSet.getLong("id");
                coverUrl = resultSet.getString("coverLink");

                if (musicId != null && coverUrl != null) {

                    coverUrls.add(coverUrl);
                    coverIds.add(musicId);
                    System.out.println("Added Cover URL: " + coverUrl);
                } else {
                    System.err.println("Invalid data: MusicID=" + musicId + ", Cover=" + coverUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}