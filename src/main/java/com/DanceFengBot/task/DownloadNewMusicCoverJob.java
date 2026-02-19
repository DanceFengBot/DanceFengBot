package com.DanceFengBot.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.DanceFengBot.config.AbstractConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadNewMusicCoverJob implements Job {
    private static final Gson gson = new GsonBuilder().create();

    // 音乐索引列表，1-最新，2-国语，3-粤语，4-韩文，5-欧美，6-其他，100-网易云，101-尘音
    private static final List<Integer> MUSIC_INDEX_LIST = Arrays.asList(1);

    // 统计信息
    private static AtomicInteger totalProcessed = new AtomicInteger(0);
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("开始爬取最新歌曲封面...");
        // 创建HTTP客户端
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // 遍历所有音乐索引
        // 遍历所有音乐索引
        for (Integer musicIndex : MUSIC_INDEX_LIST) {
            // 重置当前索引的数据标记
            boolean hasDataForCurrentIndex = false;
            System.out.println("处理音乐索引: " + musicIndex);
            // 修正：循环条件仅保留页面范围（1-35），无数据时通过break终止
            for (int page = 1; page <= 35; page++) {
                String coverUrl = String.format("https://dancedemo.shenghuayule.com/Dance/api/User/GetMusicRankingNew?musicIndex=%s&page=%d&pagesize=20", musicIndex, page);
                try {
                    // 发送HTTP请求、获取响应（代码不变）
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(coverUrl))
                            .timeout(Duration.ofSeconds(30))
                            .header("User-Agent", "Mozilla/5.0")
                            .GET()
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    // 解析响应（会更新hasDataForCurrentIndex）
                    processApiResponse(response.body());

                    // 修正：如果当前页面无数据，终止该索引的后续页面爬取
                    if (!hasDataForCurrentIndex) {
                        System.out.println("当前索引" + musicIndex + "第" + page + "页无数据，跳过后续页面");
                        break; // 退出页面循环，处理下一个音乐索引
                    }

                    // 延迟避免请求过快（仅当有数据时延迟，无数据则直接break）
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
    }
    private static void processApiResponse(String jsonResponse) {
        try {
            // 解析JSON
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

            if (responseObj == null) {
                System.err.println("API响应为空");
                return;
            }

            // 获取List数组
            JsonArray listArray = responseObj.getAsJsonArray("List");
            if (listArray == null || listArray.size() == 0) {
                System.out.println("当前页面没有数据");
                return;
            }

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
                    boolean success = downloadMusicCover(musicId, coverUrl);

                    if (success) {
                        successCount.incrementAndGet();
                        System.out.println(" ✓ 下载成功: id=" + musicId +
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
    }
    private static boolean downloadMusicCover(Long musicId, String coverUrl) {
        String filePath = AbstractConfig.configPath + "Images/Cover/OfficialImage/" + musicId + ".jpg";
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(coverUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             FileChannel fileChannel = fileOutputStream.getChannel()) {
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return true;
        } catch (IOException e) {
            System.err.println("下载封面失败 (id=" + musicId + "): " + e.getMessage());
            return false;
        }
    }
}
