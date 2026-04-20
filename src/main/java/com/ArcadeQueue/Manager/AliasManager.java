package com.ArcadeQueue.Manager;

import com.ArcadeQueue.Data.Arcade;
import com.ArcadeQueue.Storage.DataStorage;

public class AliasManager {
    private final DataStorage Storage;

    public AliasManager(DataStorage Storage) {
        this.Storage = Storage;
    }

    // 添加别名
    public String addAlias(long groupId, String nameOrIndex, String alias) {
        if (!Storage.hasGroup(groupId)) {
            return "请联系群主或管理员添加群聊";
        }
        Arcade arcade = Storage.resolveArcade(groupId, nameOrIndex);
        if (arcade == null) {
            return "店名 '" + nameOrIndex + "' 不在群聊中或为机厅别名，请先添加该机厅或使用该机厅本名";
        }
        if (arcade.getAliasList().contains(alias)) {
            return "别名 '" + alias + "' 已存在，请使用其他别名";
        }
        arcade.getAliasList().add(alias);
        Storage.updateArcade(arcade);
        return "已成功为 '" + arcade.getName() + "' 添加别名 '" + alias + "'";
    }

    // 删除别名
    public String deleteAlias(long groupId, String nameOrIndex, String aliasOrIndex) {
        if (!Storage.hasGroup(groupId)) {
            return "请联系群主或管理员添加群聊";
        }
        Arcade arcade = Storage.resolveArcade(groupId, nameOrIndex);
        if (arcade == null) {
            return "店名 '" + nameOrIndex + "' 不在群聊中或为机厅别名，请先添加该机厅或使用该机厅本名";
        }
        // 解析别名（可能是序号）
        String aliasToRemove = null;
        try {
            int idx = Integer.parseInt(aliasOrIndex) - 1;
            if (idx >= 0 && idx < arcade.getAliasList().size()) {
                aliasToRemove = arcade.getAliasList().get(idx);
            }
        } catch (NumberFormatException e) {
            // 直接匹配字符串
            if (arcade.getAliasList().contains(aliasOrIndex)) {
                aliasToRemove = aliasOrIndex;
            }
        }
        if (aliasToRemove == null) {
            return "别名 '" + aliasOrIndex + "' 不存在，请检查输入的别名";
        }
        arcade.getAliasList().remove(aliasToRemove);
        Storage.updateArcade(arcade);
        return "已成功删除 '" + arcade.getName() + "' 的别名 '" + aliasToRemove + "'";
    }

    // 查询机厅别名
    public String getAliasList(long groupId, String nameOrIndex) {
        if (!Storage.hasGroup(groupId)) {
            return "本群尚未开通相关功能，请联系群主或管理员添加群聊";
        }
        Arcade arcade = Storage.resolveArcade(groupId, nameOrIndex);
        if (arcade == null) {
            return "找不到机厅或机厅别名为 '" + nameOrIndex + "' 的相关信息";
        }
        if (arcade.getAliasList().isEmpty()) {
            return "机厅 '" + arcade.getName() + "' 尚未添加别名";
        }
        StringBuilder sb = new StringBuilder("机厅 '").append(arcade.getName()).append("' 的别名列表如下：\n");
        for (int i = 0; i < arcade.getAliasList().size(); i++) {
            sb.append(i + 1).append(". ").append(arcade.getAliasList().get(i)).append("\n");
        }
        return sb.toString().trim();
    }
}