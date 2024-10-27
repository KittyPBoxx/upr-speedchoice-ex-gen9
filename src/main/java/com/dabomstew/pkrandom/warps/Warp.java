package com.dabomstew.pkrandom.warps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Warp {

    private String id;
    private String name;
    private Integer level;
    private String to;
    private Map<String, String> connections = new HashMap<>(); // Handles "connections" field
    private List<String> grouped = new ArrayList<>();
    private Boolean groupMain = false;
    private Boolean ignore = false;
    private List<String> tags = new ArrayList<>();
    private boolean isMapped = false;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Map<String, String> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, String> connections) {
        this.connections = connections;
    }

    public List<String> getGrouped() {
        return grouped;
    }

    public void setGrouped(List<String> grouped) {
        this.grouped = grouped;
    }

    public Boolean getGroupMain() {
        return groupMain;
    }

    public void setGroupMain(Boolean groupMain) {
        this.groupMain = groupMain;
    }

    public Boolean getIgnore() {
        return ignore;
    }

    public void setIgnore(Boolean ignore) {
        this.ignore = ignore;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isMapped() {
        return isMapped;
    }

    public void setMapped() {
        this.isMapped = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null)
            return false;

        if (!(obj instanceof Warp))
            return false;

        return name.equals(((Warp) obj).getName());
    }
}
