package com.ArcadeQueue.Manager;

import com.ArcadeQueue.Data.Arcade;
import com.ArcadeQueue.Storage.DataStorage;
import com.ArcadeQueue.Util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CountManager {
    private static DataStorage Storage = new DataStorage();

    public CountManager(DataStorage Storage) {
        CountManager.Storage = Storage;
    }

    // 更新人数（处理类似 "机厅名++" "机厅名=5" 等）
    public static String updateCount(long groupId, String input, String updaterNick) {
        // 正则匹配格式：店名[++|--|==|=\d+|\d+]
        Pattern pattern = Pattern.compile("^([\\u4e00-\\u9fa5\\w]+?)([+\\-=]{0,2})(\\d*)$");
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            return null; // 不匹配，忽略
        }

        String name = matcher.group(1);
        String op = matcher.group(2);
        String numStr = matcher.group(3);
        int num = numStr.isEmpty() ? 0 : Integer.parseInt(numStr);

        if (op.isEmpty() && num == 0) {
            return null; // 没有操作
        }

        Arcade arcade = Storage.resolveArcade(groupId, name);
        if (arcade == null) {
            return null; // 机厅不存在，忽略
        }

        int currentNum = arcade.getCurrentNum();
        int newNum;

        if ("++".equals(op) || "+".equals(op)) {
            int delta = (num == 0) ? 1 : num;
            if (Math.abs(delta) > 50) {
                return "检测到非法数值，拒绝更新";
            }
            newNum = currentNum + delta;
            if (newNum < 0 || newNum > 100) {
                return "检测到非法数值，拒绝更新";
            }
            arcade.updateNum(delta, updaterNick, Utils.getCurrentTime());
        } else if ("--".equals(op) || "-".equals(op)) {
            int delta = -(num == 0 ? 1 : num);
            if (Math.abs(delta) > 50) {
                return "检测到非法数值，拒绝更新";
            }
            newNum = currentNum + delta;
            if (newNum < 0 || newNum > 100) {
                return "检测到非法数值，拒绝更新";
            }
            arcade.updateNum(delta, updaterNick, Utils.getCurrentTime());
        } else if ("==".equals(op) || "=".equals(op) || (op.isEmpty() && num != 0)) {
            newNum = num;
            if (newNum < 0 || newNum > 100) {
                return "检测到非法数值，拒绝更新";
            }
            arcade.setAbsoluteNum(newNum, updaterNick, Utils.getCurrentTime());
        } else {
            return null;
        }

        Storage.updateArcade(arcade);

        // 生成回复消息（省略了网络同步）
        return formatCountMessage(arcade, newNum);
    }

    // 查询人数（类似 "机厅名几" "机厅名几人" "机厅名j"）
    public String queryCount(long groupId, String input) {
        String namePart = null;
        if (input.endsWith("几人")) {
            namePart = input.substring(0, input.length() - 2).trim();
        } else if (input.endsWith("几")) {
            namePart = input.substring(0, input.length() - 1).trim();
        } else if (input.endsWith("j")) {
            namePart = input.substring(0, input.length() - 1).trim();
        }
        if (namePart == null) {
            return null;
        }

        Arcade arcade = Storage.resolveArcade(groupId, namePart);
        if (arcade == null) {
            return null;
        }

        if (arcade.getNum().isEmpty()) {
            return "[" + arcade.getName() + "] 今日人数尚未更新\n你可以爽霸机了\n快去出勤吧！";
        }

        int currentNum = arcade.getCurrentNum();
        return formatCountMessage(arcade, currentNum);
    }

    private static String formatCountMessage(Arcade arcade, int num) {
        Utils.WaitResult wait = Utils.calculateWaitTime(num, arcade.getCoutnum());
        StringBuilder sb = new StringBuilder();
        sb.append("📍 ").append(arcade.getName()).append("  人数为 ").append(num).append("\n");
        sb.append("🕹️ 机台数量：").append(arcade.getCoutnum()).append(" 台（每轮 ").append(arcade.getCoutnum() * 2).append(" 人）\n\n");
        if (wait.avg > 0) {
            sb.append("⌛ 预计等待：约 ").append(wait.avg).append(" 分钟\n");
            sb.append("   ↳ 范围：").append(wait.min).append("~").append(wait.max).append(" 分钟\n\n");
            sb.append("💡 ").append(wait.tip);
        } else {
            sb.append("✅ 无需等待，快去出勤吧！");
        }
        if (arcade.getLastUpdatedAt() != null && arcade.getLastUpdatedBy() != null) {
            sb.append("\n（").append(arcade.getLastUpdatedBy()).append(" · ").append(arcade.getLastUpdatedAt()).append("）");
        }
        return sb.toString();
    }
}