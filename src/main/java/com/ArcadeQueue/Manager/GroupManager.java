package com.ArcadeQueue.Manager;

import com.ArcadeQueue.Storage.DataStorage;

public class GroupManager {
    private static DataStorage Storage = new DataStorage();

    public GroupManager(DataStorage Storage) {
        GroupManager.Storage = Storage;
    }

    // 添加群聊
    public static boolean addGroup(long groupId) {
        if (Storage.hasGroup(groupId)) {
//            return "当前群聊已在名单中";
            return false;
        }
        Storage.addGroup(groupId);
//        return "已添加当前群聊到名单中";
        return true;
    }

    // 删除群聊
    public static boolean deleteGroup(long groupId) {
        if (!Storage.hasGroup(groupId)) {
//            return "当前群聊不在名单中，无法删除";
            return false;
        }
        Storage.removeGroup(groupId);
//        return "已从名单中删除当前群聊";
        return true;
    }
}