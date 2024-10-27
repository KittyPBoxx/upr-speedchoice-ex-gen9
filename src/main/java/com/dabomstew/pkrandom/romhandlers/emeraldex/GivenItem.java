package com.dabomstew.pkrandom.romhandlers.emeraldex;

import java.util.List;

public class GivenItem {

    // This is an array because sometimes there are multiple scripts that give the same given item
    // For example there might be on script for May and on for Brendan that are supposed to give the same thing
    private final List<Integer> offsets;

    public GivenItem(List<Integer> offsets) {
        this.offsets = offsets;
    }

    public List<Integer> getOffsets() {
        return offsets;
    }
}
