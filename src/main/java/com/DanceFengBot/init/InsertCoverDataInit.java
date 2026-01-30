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
import java.sql.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class InsertCoverDataInit {

    private static final Gson gson = new GsonBuilder().create();

    // 音乐索引列表，1-最新，2-国语，3-粤语，4-韩文，5-欧美，6-其他，101-尘音
    private static final List<Integer> MUSIC_INDEX_LIST = Arrays.asList(1, 2, 3, 4, 5, 6, 101);
    // 移除类级hasDataForCurrentIndex，改为方法内局部变量传递
    // 统计信息
    private static AtomicInteger totalProcessed = new AtomicInteger(0);
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * 执行补丁任务
     */
    public static void init() {
        System.out.println("开始爬取数据...");

        try {

            // 连接数据库
            if (!DatabaseConnection.connect()) return;
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return;

            // 创建HTTP客户端
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            // 遍历所有音乐索引
            for (Integer musicIndex : MUSIC_INDEX_LIST) {
                // 每个索引独立的"是否有数据"标记（局部变量，避免跨索引污染）
                boolean hasDataForCurrentIndex = false;
                System.out.println("处理音乐索引: " + musicIndex);

                // 遍历页面1-35
                for (int page = 1; page <= 35; page++) {
                    String coverUrl = String.format("https://dancedemo.shenghuayule.com/Dance/api/User/GetMusicRankingNew?musicIndex=%s&page=%d&pagesize=20", musicIndex, page);
                    System.out.println("Patching " + coverUrl + "...");

                    try {
                        // 发送HTTP请求
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(coverUrl))
                                .timeout(Duration.ofSeconds(30))
                                .header("User-Agent", "Mozilla/5.0")
                                .GET()
                                .build();
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        // 处理响应并获取"当前页面是否有数据"的结果
                        hasDataForCurrentIndex = processApiResponse(response.body(), conn);

                        // 如果当前页面无数据，终止该索引的后续页面爬取
                        if (!hasDataForCurrentIndex) {
                            System.out.println("当前索引" + musicIndex + "第" + page + "页无数据，跳过后续页面");
                            break;
                        }

                        // 延迟避免请求过快
                        Thread.sleep(100);
                    } catch (IOException e) {
                        System.err.println("HTTP请求失败: " + e.getMessage());
                        failureCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        System.err.println("请求被中断: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Exception e) {
                        System.err.println("处理页面失败: " + e.getMessage());
                        failureCount.incrementAndGet();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("补丁任务执行失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 断开数据库连接
            DatabaseConnection.disconnect();
        }
    }

    /**
     * 处理API响应数据
     * @return 当前页面是否有有效数据
     */

    private static boolean processApiResponse(String jsonResponse, Connection conn) {
        // 默认当前页面无数据
        boolean hasData = false;
        try {
            // 解析JSON
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);
            if (responseObj == null) {
                System.err.println("JSON解析失败: 响应为空");
                return false;
            }

            // 获取List数组
            JsonArray listArray = responseObj.getAsJsonArray("List");
            if (listArray == null || listArray.size() == 0) {
                System.out.println("当前页面没有数据");
                return false;
            }

            // 标记当前页面有数据
            hasData = true;

            // 遍历所有音乐项
            for (int i = 0; i < listArray.size(); i++) {
                JsonObject item = listArray.get(i).getAsJsonObject();

                Long musicId = null;
                String musicName;
                String coverUrl;

                try {
                    // 提取字段
                    musicId = item.has("MusicID") ? item.get("MusicID").getAsLong() : null;
                    musicName = item.has("Name") ? item.get("Name").getAsString() : null;
                    coverUrl = item.has("Cover") ? item.get("Cover").getAsString() : null;

                    if (musicId == null || coverUrl == null) {
                        System.err.println("缺少必要字段: MusicID=" + musicId + ", Cover=" + coverUrl);
                        continue;
                    }

                    // 处理Cover URL，移除"/200"后缀
                    if (coverUrl.endsWith("/200")) {
                        coverUrl = coverUrl.substring(0, coverUrl.length() - 4);
                    }

                    // 插入或更新数据库
                    boolean success = insertOrUpdateMusic(conn, musicId, musicName, coverUrl);

                    if (success) {
                        successCount.incrementAndGet();
                        System.out.println(" ✓ 插入成功: id=" + musicId +
                                ", name=" + musicName +
                                ", link=" + coverUrl);
                    } else {
                        failureCount.incrementAndGet();
                    }

                    totalProcessed.incrementAndGet();

                } catch (Exception e) {
                    System.err.println("处理音乐项失败 (id=" + musicId + "): " + e.getMessage());
                    failureCount.incrementAndGet();
                }
            }

        } catch (Exception e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            System.err.println("原始响应: " + jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            failureCount.incrementAndGet();
        }
        return hasData;
    }

    /**
     * 插入或更新音乐信息到数据库
     */
    private static boolean insertOrUpdateMusic(Connection conn, long musicId, String musicName, String coverUrl) {
        String sql = "INSERT INTO officialmusics (id, name, coverLink) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE id = VALUES(id)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, musicId);
            pstmt.setString(2, musicName != null ? musicName : "");
            pstmt.setString(3, coverUrl);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("数据库操作失败 (id=" + musicId + "): " + e.getMessage());
            return false;
        }
    }

    @Test
    public void testInsertDataInit() {
        InsertCoverDataInit.init();
    }
}