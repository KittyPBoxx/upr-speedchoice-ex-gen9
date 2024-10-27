package com.dabomstew.pkrandom.romhandlers.emeraldex;

import java.util.List;

public class TutorMove {
    private final List<Integer> offsets;

    public TutorMove(List<Integer> offsets) {
        this.offsets = offsets;
    }

    public List<Integer> getOffsets() {
        return offsets;
    }
}
