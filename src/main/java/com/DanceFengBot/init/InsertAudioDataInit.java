package com.DanceFengBot.init;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.Tools.DatabaseConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class InsertAudioDataInit {
    private static final Gson gson = new GsonBuilder().create();

    // 统计信息
    private static AtomicInteger totalProcessed = new AtomicInteger(0);
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * 执行补丁任务
     */
    public static void init() {
        System.out.println("开始爬取音频数据...");

        // 连接数据库
        if (!DatabaseConnection.connect()) {
            System.err.println("数据库连接失败");
            return;
        }
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.err.println("数据库连接为空");
            return;
        }

        // 创建HTTP客户端
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        String audioUrl = "https://dancedemo.shenghuayule.com/Dance/Music/GetMusicList?getAdvanced=true&getNotDisplay=true&getaudio=true&getitem=true";
        System.out.println("请求URL: " + audioUrl);

        try {
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(audioUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("HTTP请求失败，状态码: " + response.statusCode());
                return;
            }

            String responseBody = response.body();
            System.out.println("响应长度: " + responseBody.length());

            processApiResponse(responseBody, conn);

        } catch (IOException e) {
            System.err.println("HTTP请求失败: " + e.getMessage());
            failureCount.incrementAndGet();
        } catch (InterruptedException e) {
            System.err.println("请求被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("处理数据失败: " + e.getMessage());
            e.printStackTrace();
            failureCount.incrementAndGet();
        } finally {
            DatabaseConnection.disconnect();
            System.out.println("数据处理完成");
        }
    }

    /**
     * 处理API响应数据
     */
    private static void processApiResponse(String jsonResponse, Connection conn) {
        try {
            // 直接解析为JsonArray，因为API返回的是数组格式
            JsonArray musicArray = gson.fromJson(jsonResponse, JsonArray.class);

            if (musicArray == null) {
                System.err.println("JSON解析失败: 返回的JSON为null");
                return;
            }

            if (musicArray.size() == 0) {
                System.out.println("没有找到音乐数据");
                return;
            }

            System.out.println("找到 " + musicArray.size() + " 首音乐");

            // 遍历音乐数组
            for (int i = 0; i < musicArray.size(); i++) {
                try {
                    JsonObject musicItem = musicArray.get(i).getAsJsonObject();

                    // 提取字段
                    Long musicId = musicItem.has("MusicID") ? musicItem.get("MusicID").getAsLong() : null;
                    String musicName = musicItem.has("Name") ? musicItem.get("Name").getAsString() : null;
                    String audioUrl = musicItem.has("AudioUrl") ? musicItem.get("AudioUrl").getAsString() : null;

                    // 验证必要字段
                    if (musicId == null || audioUrl == null || audioUrl.isEmpty()) {
                        System.err.println("缺少必要字段: MusicID=" + musicId + ", AudioUrl=" + audioUrl);
                        failureCount.incrementAndGet();
                        continue;
                    }

                    // 插入或更新数据库
                    boolean success = insertOrUpdateMusic(conn, musicId, musicName, audioUrl);

                    if (success) {
                        successCount.incrementAndGet();
                        System.out.printf("✓ [%d/%d] 插入成功: id=%d, name=%s%n",
                                (i + 1), musicArray.size(), musicId,
                                musicName != null ? musicName : "未知");
                    } else {
                        failureCount.incrementAndGet();
                        System.err.printf("✗ [%d/%d] 插入失败: id=%d%n",
                                (i + 1), musicArray.size(), musicId);
                    }

                    totalProcessed.incrementAndGet();

                } catch (Exception e) {
                    System.err.printf("处理第 %d 首音乐失败: %s%n", (i + 1), e.getMessage());
                    failureCount.incrementAndGet();
                }
            }

        } catch (Exception e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            System.err.println("响应前200字符: " +
                    jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            e.printStackTrace();
        }
    }

    /**
     * 插入或更新音乐信息到数据库
     */
    private static boolean insertOrUpdateMusic(Connection conn, long musicId, String musicName, String audioUrl) {
        String sql = "INSERT INTO officialmusics (id, name, audioLink) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), audioLink = VALUES(audioLink)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, musicId);
            pstmt.setString(2, musicName != null ? musicName : "");
            pstmt.setString(3, audioUrl);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("数据库操作失败 (id=" + musicId + "): " + e.getMessage());
            return false;
        }
    }

    @Test
    public void testInit() {
        init();
    }

}