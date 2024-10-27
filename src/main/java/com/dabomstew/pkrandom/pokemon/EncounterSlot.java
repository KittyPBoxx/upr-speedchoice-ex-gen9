package com.dabomstew.pkrandom.pokemon;

public enum EncounterSlot {

    CAVE_OR_GRASS(0, "Cave/Grass", 6),
    SURFING(1, "Surfing", 5),
    ROCK_SMASH(2, "Rock Smash", 5),
    FISHING(3, "Fishing", 2),
    HEADBUTT(4, "Headbutt", 5);

    private final int index;

    private final String name;

    private final int slotEncounters;

    EncounterSlot(int index, String name, int slotEncounters) {
        this.index = index;
        this.name = name;
        this.slotEncounters = slotEncounters;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public int getSlotEncounters() {
        return slotEncounters;
    }
}
