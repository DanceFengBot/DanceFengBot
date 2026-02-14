package com.DanceCube.api;

import com.DanceCube.token.Token;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.Tools.HttpUtil;
import okhttp3.Call;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Ladder {
    private int matchDefineId; // 赛季ID
    private String matchName; // 赛季名称
    private String startTime; // 赛季开始时间
    private String stopTime; // 赛季结束时间
    private String matchTimeText; // 赛季时间文本
    private int levelPoint; // 段位分
    private int levelGrade; // 段位等级
    private boolean isLast; // 是否上赛季
    private boolean isCurrent; // 是否当前赛季
    private boolean isTopest; // 是否历史最高

    public static List<Ladder> get(Token token) {
        Call call = HttpUtil.httpApiCall(
                "https://dancedemo.shenghuayule.com/Dance/api/Match/GetQuanMinSeasons",
                Map.of("Authorization", Token.getBearerToken())
        );

        try (Response response = call.execute()) {
            if (response.body() == null) {
                throw new RuntimeException("Response body is null");
            }
            String ladderInfoJson = response.body().string();
            Gson gson = new GsonBuilder()
                    .serializeNulls()
                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .create();
            Type listType = new TypeToken<List<Ladder>>() {}.getType();
            return gson.fromJson(ladderInfoJson, listType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Ladder{" +
                "matchDefineId=" + matchDefineId +
                ", matchName='" + matchName + '\'' +
                ", startTime='" + startTime + '\'' +
                ", stopTime='" + stopTime + '\'' +
                ", matchTimeText='" + matchTimeText + '\'' +
                ", levelPoint=" + levelPoint +
                ", levelGrade=" + levelGrade +
                ", isLast=" + isLast +
                ", isCurrent=" + isCurrent +
                ", isTopest=" + isTopest +
                '}';
    }

    // Getters
    public int getMatchDefineId() {
        return matchDefineId;
    }

    public String getMatchName() {
        return matchName;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public String getMatchTimeText() {
        return matchTimeText;
    }

    public int getLevelPoint() {
        return levelPoint;
    }

    public int getLevelGrade() {
        return levelGrade;
    }

    public boolean isLast() {
        return isLast;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public boolean isTopest() {
        return isTopest;
    }
}