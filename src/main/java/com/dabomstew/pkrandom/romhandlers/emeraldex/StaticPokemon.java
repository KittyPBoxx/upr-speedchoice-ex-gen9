package com.dabomstew.pkrandom.romhandlers.emeraldex;

import com.dabomstew.pkrandom.pokemon.Pokemon;

public class StaticPokemon {
    private final int[] offsets;

    public StaticPokemon(int... offsets) {
        this.offsets = offsets;
    }

    public int getFirstOffset() {
        return offsets[0];
    }

    public int[] getOffsets() {
        return offsets;
    }
}
