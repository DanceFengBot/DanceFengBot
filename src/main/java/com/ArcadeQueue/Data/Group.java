package com.ArcadeQueue.Data;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;                  // 群聊ID
    private List<String> arcadeIds;     // 该群下的机厅ID列表

    public Group() {
        this.arcadeIds = new ArrayList<>();
    }

    // getters and setters...
    public String getId() { return id; }
    public void setId(long id) { this.id = String.valueOf(id); }
    public List<String> getArcadeIds() { return arcadeIds; }
    public void setArcadeIds(List<String> arcadeIds) { this.arcadeIds = arcadeIds; }
}
