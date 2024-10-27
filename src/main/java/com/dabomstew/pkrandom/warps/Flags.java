package com.dabomstew.pkrandom.warps;

import java.util.Map;

public class Flags {

    private Map<String, FlagCondition> compositeFlags;
    private Map<String, FlagCondition> compositeFlagsOutOfSeq;

    // Getters and Setters
    public Map<String, FlagCondition> getCompositeFlags() {
        return compositeFlags;
    }

    public void setCompositeFlags(Map<String, FlagCondition> compositeFlags) {
        this.compositeFlags = compositeFlags;
    }

    public Map<String, FlagCondition> getCompositeFlagsOutOfSeq() {
        return compositeFlagsOutOfSeq;
    }

    public void setCompositeFlagsOutOfSeq(Map<String, FlagCondition> compositeFlagsOutOfSeq) {
        this.compositeFlagsOutOfSeq = compositeFlagsOutOfSeq;
    }

    @Override
    public String toString() {
        return "Flags{" +
                "COMPOSITE_FLAGS=" + compositeFlags +
                ", COMPOSITE_FLAGS_OUT_OF_SEQ=" + compositeFlagsOutOfSeq +
                '}';
    }
}
