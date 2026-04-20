package com.ArcadeQueue.Manager;

import com.ArcadeQueue.Data.Arcade;
import com.ArcadeQueue.Storage.DataStorage;

import java.util.List;
import java.util.stream.Collectors;

public class ArcadeManager {
    private static DataStorage storage = new DataStorage();

    public ArcadeManager(DataStorage storage) {
        ArcadeManager.storage = storage;
    }

    // 添加机厅（直接添加，不搜索）
    public static String addArcade(long groupId, String name) {
        if (!storage.hasGroup(groupId)) {
            return "请联系群主或管理员添加群聊";
        }
        if (storage.getArcadeByName(groupId, name) != null) {
            return "机厅已在群聊中";
        }
        Arcade arcade = new Arcade();
        arcade.setName(name);
        arcade.setCoutnum(1); // 默认1台
        storage.addArcade(groupId, arcade);
        return "已添加机厅：" + name;
    }

    // 删除机厅（支持名称或序号）
    public static String deleteArcade(long groupId, String nameOrIndex) {
        if (!storage.hasGroup(groupId)) {
            return "请联系群主或管理员添加群聊";
        }
        Arcade arcade = storage.resolveArcade(groupId, nameOrIndex);
        if (arcade == null) {
            return "机厅不在群聊中或为机厅别名，请先添加该机厅或使用该机厅本名";
        }
        storage.removeArcade(groupId, arcade.getId());
        return "已从群聊名单中删除机厅：" + arcade.getName();
    }

    // 显示机厅列表
    public static String showArcadeList(long groupId) {
        if (!storage.hasGroup(groupId)) {
            return "请联系群主或管理员添加群聊";
        }
        List<Arcade> arcades = storage.getArcadesByGroup(groupId);
        if (arcades.isEmpty()) {
            return "当前群聊暂无机厅";
        }
        StringBuilder sb = new StringBuilder("机厅列表如下：\n");
        for (int i = 0; i < arcades.size(); i++) {
            sb.append(i + 1).append("：").append(arcades.get(i).getName()).append("\n");
        }
        return sb.toString().trim();
    }

    // 查询今日已更新的机厅人数
    public static String queryUpdatedArcades(long groupId) {
        if (!storage.hasGroup(groupId)) {
            return "请联系群主或管理员添加群聊";
        }
        List<Arcade> arcades = storage.getArcadesByGroup(groupId);
        List<String> updated = arcades.stream()
                .filter(a -> a.getLastUpdatedBy() != null && !a.getNum().isEmpty())
                .map(a -> String.format("[%s] %d人 \n（%s · %s）",
                        a.getName(), a.getCurrentNum(),
                        a.getLastUpdatedBy(), a.getLastUpdatedAt()))
                .collect(Collectors.toList());
        if (updated.isEmpty()) {
            return "📋 今日机厅人数更新情况\n\n暂无更新记录\n您可以爽霸机了";
        }
        return "📋 今日机厅人数更新情况\n\n" + String.join("\n", updated);
    }
}
