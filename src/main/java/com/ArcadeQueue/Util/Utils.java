package com.ArcadeQueue.Util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    // 获取当前时间 HH:MM
    public static String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // 计算等待时间（简化版，与Python逻辑一致）
    public static WaitResult calculateWaitTime(int currentNum, int coutnum) {
        int playersPerRound = Math.max(coutnum, 1) * 2;
        int queueNum = Math.max(currentNum - playersPerRound, 0);
        if (queueNum <= 0) {
            return new WaitResult(0, 0, 0, "✅ 无需等待，快去出勤吧！");
        }
        double expectedRounds = (double) queueNum / playersPerRound;
        int minRounds = queueNum / playersPerRound;
        int maxRounds = (int) Math.ceil(expectedRounds);
        int waitTimeAvg = (int) Math.round(expectedRounds * 16);
        int waitTimeMin = minRounds * 16;
        int waitTimeMax = maxRounds * 16;

        String tip;
        if (waitTimeAvg <= 20) tip = "✅ 舞萌启动！";
        else if (waitTimeAvg <= 40) tip = "🕰️ 小排队还能忍";
        else if (waitTimeAvg <= 90) tip = "💀 DBD，纯折磨，建议换店";
        else tip = "🪦 建议回家（或者明天再来）";

        return new WaitResult(waitTimeAvg, waitTimeMin, waitTimeMax, tip);
    }

    public static class WaitResult {
        public final int avg;
        public final int min;
        public final int max;
        public final String tip;

        public WaitResult(int avg, int min, int max, String tip) {
            this.avg = avg;
            this.min = min;
            this.max = max;
            this.tip = tip;
        }
    }
}

