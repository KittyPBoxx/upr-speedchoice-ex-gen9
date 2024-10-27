package com.dabomstew.pkrandom.pokemon;

import java.util.Arrays;

public enum TimesOfDay {

    MORNING("Morning", 0),
    DAY("Day", 1),
    DUSK("Dusk", 2),
    NIGHT("Night", 3);

    private final String name;
    private final int index;

    TimesOfDay(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public static int getCount() {
        return TimesOfDay.values().length;
    }
}
