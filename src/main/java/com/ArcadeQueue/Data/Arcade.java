package com.ArcadeQueue.Data;

import java.util.ArrayList;
import java.util.List;

public class Arcade {
    private String id;                  // 唯一标识 (UUID)
    private long groupId;             // 所属群聊ID
    private String name;                // 机厅本名
    private List<String> aliasList;     // 别名列表
    private List<Integer> num;          // 人数增量历史
    private String lastUpdatedBy;       // 最后更新者昵称
    private String lastUpdatedAt;       // 最后更新时间 (HH:MM)
    private int coutnum;                // 机台数量
    private List<String> map;           // 地图链接列表 (保留但不再使用网络)

    public Arcade() {
        this.aliasList = new ArrayList<>();
        this.num = new ArrayList<>();
        this.map = new ArrayList<>();
    }

    // getters and setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getAliasList() { return aliasList; }
    public void setAliasList(List<String> aliasList) { this.aliasList = aliasList; }
    public List<Integer> getNum() { return num; }
    public void setNum(List<Integer> num) { this.num = num; }
    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }
    public String getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(String lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    public int getCoutnum() { return coutnum; }
    public void setCoutnum(int coutnum) { this.coutnum = coutnum; }
    public List<String> getMap() { return map; }
    public void setMap(List<String> map) { this.map = map; }

    // 获取当前总人数
    public int getCurrentNum() {
        int sum = 0;
        for (int n : num) sum += n;
        return sum;
    }

    // 更新人数（增量模式）
    public void updateNum(int delta, String updater, String time) {
        num.add(delta);
        lastUpdatedBy = updater;
        lastUpdatedAt = time;
    }

    // 设置绝对人数（清空历史）
    public void setAbsoluteNum(int value, String updater, String time) {
        num.clear();
        num.add(value);
        lastUpdatedBy = updater;
        lastUpdatedAt = time;
    }
}