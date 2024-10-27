package com.dabomstew.pkrandom.warps;

import java.util.List;

public class FlagCondition {
    private String flag;
    private List<String> condition;
    private boolean set;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public List<String> getCondition() {
        return condition;
    }

    public void setCondition(List<String> condition) {
        this.condition = condition;
    }

    public boolean isSet() {
        return set;
    }

    public void setFlag() {
        this.set = true;
    }

    @Override
    public String toString() {
        return "FlagCondition{" +
                "flag='" + flag + '\'' +
                ", condition=" + condition +
                '}';
    }
}
