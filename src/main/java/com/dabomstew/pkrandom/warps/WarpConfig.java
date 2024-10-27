package com.dabomstew.pkrandom.warps;

public class WarpConfig {

    private final int level;

    private final boolean extraDeadendRemoval;

    private long seed;

    private boolean inGymOrder;

    public WarpConfig(Integer level, boolean extraDeadendRemoval, long seed, boolean inGymOrder) {
        this.level = level;
        this.extraDeadendRemoval = extraDeadendRemoval;
        this.seed = seed;
        this.inGymOrder = inGymOrder;
    }

    public int getLevel() {
        return level;
    }

    public boolean isExtraDeadendRemoval() {
        return extraDeadendRemoval;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isInGymOrder() {
        return inGymOrder;
    }

    public void incrementSeed() {
        this.seed++;
    }
}
