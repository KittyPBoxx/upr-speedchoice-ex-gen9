package com.dabomstew.pkrandom.pokemon;

public class GenRestrictions {

    private boolean allow_gen1;
    private boolean allow_gen2;
    private boolean allow_gen3;
    private boolean allow_gen4;
    private boolean allow_gen5;
    private boolean allow_gen6;
    private boolean allow_gen7;
    private boolean allow_gen8;
    private boolean allow_gen9;

    private boolean unused1;
    private boolean unused2;
    private boolean unused3;
    private boolean unused4;
    private boolean unused5;
    private boolean unused6;

    public GenRestrictions() {
    }

    public GenRestrictions(int state) {
        allow_gen1 = (state & 1) > 0;
        allow_gen2 = (state & 2) > 0;
        allow_gen3 = (state & 4) > 0;
        allow_gen4 = (state & 8) > 0;
        allow_gen5 = (state & 16) > 0;

        allow_gen6 = (state & 32) > 0;
        allow_gen7 = (state & 64) > 0;

        allow_gen8 = (state & 128) > 0;
        allow_gen9 = (state & 256) > 0;

        unused1 = (state & 512) > 0;
        unused2 = (state & 1024) > 0;
        unused3 = (state & 2048) > 0;
        unused4 = (state & 4096) > 0;
        unused5 = (state & 8192) > 0;
        unused6 = (state & 16384) > 0;
    }

    public boolean nothingSelected() {
        return !allow_gen1 &&
                !allow_gen2 &&
                !allow_gen3 &&
                !allow_gen4 &&
                !allow_gen5 &&
                !allow_gen6 &&
                !allow_gen7 &&
                !allow_gen8 &&
                !allow_gen9;
    }

    public int toInt() {
        return makeIntSelected(allow_gen1,
                               allow_gen2,
                               allow_gen3,
                               allow_gen4,
                               allow_gen5,
                               allow_gen6,
                               allow_gen7,
                               allow_gen8,
                               allow_gen9,
                               unused1,
                               unused2,
                               unused3,
                               unused4,
                               unused5,
                               unused6);
    }

    public void limitToGen(int generation) {
        if (generation < 2) {
            allow_gen2 = false;
        }
        if (generation < 3) {
            allow_gen3 = false;
        }
        if (generation < 4) {
            allow_gen4 = false;
        }
        if (generation < 5) {
            allow_gen5 = false;
        }
        if (generation < 6) {
            allow_gen6 = false;
        }
        if (generation < 7) {
            allow_gen7 = false;
        }
        if (generation < 8) {
            allow_gen8 = false;
        }
        if (generation < 9) {
            allow_gen9 = false;
        }
    }

    public boolean isAllow_gen(int gen) {
        switch (gen) {
            case 9:
                return allow_gen9;
            case 8:
                return allow_gen8;
            case 7:
                return allow_gen7;
            case 6:
                return allow_gen6;
            case 5:
                return allow_gen5;
            case 4:
                return allow_gen4;
            case 3:
                return allow_gen3;
            case 2:
                return allow_gen2;
            case 1:
                return allow_gen1;
            default:
                return false;
        }
    }

    public void setAllow_gen(int gen, boolean value) {
        switch (gen) {
            case 9:
                allow_gen9 = value;
                break;
            case 8:
                allow_gen8 = value;
                break;
            case 7:
                allow_gen7 = value;
                break;
            case 6:
                allow_gen6 = value;
                break;
            case 5:
                allow_gen5 = value;
                break;
            case 4:
                allow_gen4 = value;
                break;
            case 3:
                allow_gen3 = value;
                break;
            case 2:
                allow_gen2 = value;
                break;
            case 1:
                allow_gen1 = value;
                break;
            default:
        }
    }

    private int makeIntSelected(boolean... switches) {
        if (switches.length > 32) {
            // No can do
            return 0;
        }
        int initial = 0;
        int state = 1;
        for (boolean b : switches) {
            initial |= b ? state : 0;
            state *= 2;
        }
        return initial;
    }

}
