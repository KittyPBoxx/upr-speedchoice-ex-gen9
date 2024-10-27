package com.dabomstew.pkrandom.warps;

public class WarpRemapping implements Comparable<WarpRemapping> {
    
    private final int triggerMapGroup;
    private final int triggerMapNo;
    private final int triggerWarpNo;
    private final int targetMapGroup;
    private final int targetMapNo;
    private final int targetWarpNo;

    public WarpRemapping(int triggerMapGroup,
                         int triggerMapNo,
                         int triggerWarpNo,
                         int targetMapGroup,
                         int targetMapNo,
                         int targetWarpNo) {
        this.triggerMapGroup = triggerMapGroup;
        this.triggerMapNo = triggerMapNo;
        this.triggerWarpNo = triggerWarpNo;
        this.targetMapGroup = targetMapGroup;
        this.targetMapNo = targetMapNo;
        this.targetWarpNo = targetWarpNo;
    }

    public int getTriggerMapGroup() {
        return triggerMapGroup;
    }

    public int getTriggerMapNo() {
        return triggerMapNo;
    }

    public int getTriggerWarpNo() {
        return triggerWarpNo;
    }

    public int getTargetMapGroup() {
        return targetMapGroup;
    }

    public int getTargetMapNo() {
        return targetMapNo;
    }

    public int getTargetWarpNo() {
        return targetWarpNo;
    }

    @Override
    public int compareTo(WarpRemapping other) {
        if (this.triggerMapGroup != other.triggerMapGroup) {
            return Integer.compare(this.triggerMapGroup, other.triggerMapGroup);
        }

        if (this.triggerMapNo != other.triggerMapNo) {
            return Integer.compare(this.triggerMapNo, other.triggerMapNo);
        }

        return Integer.compare(this.triggerWarpNo, other.triggerWarpNo);
    }

    @Override
    public String toString() {
        return String.format("{%s,%s,%s,%s,%s,%s}",triggerMapGroup, triggerMapNo, triggerWarpNo,
                targetMapGroup, targetMapNo, targetWarpNo);
    }
}
