package com.ArcadeQueue.Storage;

import com.DanceFengBot.config.AbstractConfig;
import com.ArcadeQueue.Data.Arcade;
import com.ArcadeQueue.Data.Group;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {
    private static final String GROUPS_FILE = AbstractConfig.configPath + "groups.json";
    private static final String ARCADES_FILE = AbstractConfig.configPath + "arcades.json";
    private final Gson gson;
    private final Map<String, Group> groups;      // groupId -> Group
    private final Map<String, Arcade> arcades;    // arcadeId -> Arcade

    public DataStorage() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.groups = new ConcurrentHashMap<>();
        this.arcades = new ConcurrentHashMap<>();
        load();
    }

    // 加载数据
    public void load() {
        // 加载群聊
        File groupsFile = new File(GROUPS_FILE);
        if (groupsFile.exists()) {
            try (Reader reader = new FileReader(groupsFile)) {
                Type type = new TypeToken<List<Group>>(){}.getType();
                List<Group> groupList = gson.fromJson(reader, type);
                if (groupList != null) {
                    for (Group g : groupList) {
                        groups.put(g.getId(), g);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 加载机厅
        File arcadesFile = new File(ARCADES_FILE);
        if (arcadesFile.exists()) {
            try (Reader reader = new FileReader(arcadesFile)) {
                Type type = new TypeToken<List<Arcade>>(){}.getType();
                List<Arcade> arcadeList = gson.fromJson(reader, type);
                if (arcadeList != null) {
                    for (Arcade a : arcadeList) {
                        arcades.put(a.getId(), a);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 保存数据
    public void save() {
        // 保存群聊
        try (Writer writer = new FileWriter(GROUPS_FILE)) {
            List<Group> groupList = new ArrayList<>(groups.values());
            gson.toJson(groupList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 保存机厅
        try (Writer writer = new FileWriter(ARCADES_FILE)) {
            List<Arcade> arcadeList = new ArrayList<>(arcades.values());
            gson.toJson(arcadeList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- 群聊操作 ----------
    public boolean hasGroup(long groupId) {
        return groups.containsKey(groupId);
    }

    public void addGroup(long groupId) {
        if (!groups.containsKey(groupId)) {
            Group group = new Group();
            group.setId(groupId);
            groups.put(String.valueOf(groupId), group);
            save();
        }
    }

    public void removeGroup(long groupId) {
        // 同时删除该群下的所有机厅
        Group group = groups.remove(groupId);
        if (group != null) {
            for (String arcadeId : group.getArcadeIds()) {
                arcades.remove(arcadeId);
            }
            save();
        }
    }

    // ---------- 机厅操作 ----------
    public List<Arcade> getArcadesByGroup(long groupId) {
        Group group = groups.get(groupId);
        if (group == null) return Collections.emptyList();
        List<Arcade> result = new ArrayList<>();
        for (String arcadeId : group.getArcadeIds()) {
            Arcade a = arcades.get(arcadeId);
            if (a != null) result.add(a);
        }
        return result;
    }

    public Arcade getArcadeById(String arcadeId) {
        return arcades.get(arcadeId);
    }

    public Arcade getArcadeByName(long groupId, String name) {
        Group group = groups.get(groupId);
        if (group == null) return null;
        for (String arcadeId : group.getArcadeIds()) {
            Arcade a = arcades.get(arcadeId);
            if (a != null && a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    // 根据名称或别名查找机厅
    public Arcade resolveArcade(long groupId, String nameOrAlias) {
        Group group = groups.get(groupId);
        if (group == null) return null;
        // 先尝试匹配本名
        for (String arcadeId : group.getArcadeIds()) {
            Arcade a = arcades.get(arcadeId);
            if (a != null && a.getName().equals(nameOrAlias)) {
                return a;
            }
        }
        // 再尝试匹配别名
        for (String arcadeId : group.getArcadeIds()) {
            Arcade a = arcades.get(arcadeId);
            if (a != null && a.getAliasList().contains(nameOrAlias)) {
                return a;
            }
        }
        // 最后尝试匹配序号 (序号从1开始)
        try {
            int index = Integer.parseInt(nameOrAlias) - 1;
            List<Arcade> arcadesInGroup = getArcadesByGroup(groupId);
            if (index >= 0 && index < arcadesInGroup.size()) {
                return arcadesInGroup.get(index);
            }
        } catch (NumberFormatException e) {
            // 不是数字，忽略
        }
        return null;
    }

    // 添加机厅
    public void addArcade(long groupId, Arcade arcade) {
        String arcadeId = UUID.randomUUID().toString();
        arcade.setId(arcadeId);
        arcade.setGroupId(groupId);
        arcades.put(arcadeId, arcade);

        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group();
            group.setId(groupId);
            groups.put(String.valueOf(groupId), group);
        }
        group.getArcadeIds().add(arcadeId);
        save();
    }

    // 删除机厅
    public void removeArcade(long groupId, String arcadeId) {
        Arcade removed = arcades.remove(arcadeId);
        if (removed != null) {
            Group group = groups.get(groupId);
            if (group != null) {
                group.getArcadeIds().remove(arcadeId);
                save();
            }
        }
    }

    // 更新机厅数据
    public void updateArcade(Arcade arcade) {
        arcades.put(arcade.getId(), arcade);
        save();
    }
}
