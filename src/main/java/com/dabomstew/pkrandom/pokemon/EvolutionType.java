package com.dabomstew.pkrandom.pokemon;

import java.util.Arrays;

public enum EvolutionType {
    // emeraldex
    /* @formatter:off */
    EVOLUTIONS_END(0xFFFF, false),
    EVO_NONE(0xFFFE, false),
    EVO_FRIENDSHIP(1, false),
    EVO_FRIENDSHIP_DAY(2, false),
    EVO_FRIENDSHIP_NIGHT(3, false),
    EVO_LEVEL(4, true),
    EVO_TRADE(5, false),
    EVO_TRADE_ITEM(6, false),
    EVO_ITEM(7, false),
    EVO_LEVEL_ATK_GT_DEF(8, true),
    EVO_LEVEL_ATK_EQ_DEF(9, true),
    EVO_LEVEL_ATK_LT_DEF(10, true),
    EVO_LEVEL_SILCOON(11, true),
    EVO_LEVEL_CASCOON(12, true),
    EVO_LEVEL_NINJASK(13, true),
    EVO_LEVEL_SHEDINJA(14, true),
    EVO_BEAUTY(15, false),
    EVO_LEVEL_FEMALE(16, true),
    EVO_LEVEL_MALE(17, true),
    EVO_LEVEL_NIGHT(18, true),
    EVO_LEVEL_DAY(19, true),
    EVO_LEVEL_DUSK(20, true),
    EVO_ITEM_HOLD_DAY(21, false),
    EVO_ITEM_HOLD_NIGHT(22, false),
    EVO_MOVE(23, false),
    EVO_FRIENDSHIP_MOVE_TYPE(24, false),
    EVO_MAPSEC(25, false),
    EVO_ITEM_MALE(26, false),
    EVO_ITEM_FEMALE(27, false),
    EVO_LEVEL_RAIN(28, true),
    EVO_SPECIFIC_MON_IN_PARTY(29, false),
    EVO_LEVEL_DARK_TYPE_MON_IN_PARTY(30, true),
    EVO_TRADE_SPECIFIC_MON(31, false),
    EVO_SPECIFIC_MAP(32, false),
    EVO_LEVEL_NATURE_AMPED(33, true),
    EVO_LEVEL_NATURE_LOW_KEY(34, true),
    EVO_CRITICAL_HITS(35, false),
    EVO_SCRIPT_TRIGGER_DMG(36, false),
    EVO_DARK_SCROLL(37, false),
    EVO_WATER_SCROLL(38, false);
    /* @formatter:on */

    private final int evolutionValue;

    private final boolean usesLevel;

    EvolutionType(int indexes, boolean usesLevel) {
        this.evolutionValue = indexes;
        this.usesLevel = usesLevel;
    }

    public int toIndex() {
        return evolutionValue;
    }

    public static EvolutionType fromIndex(int index) {
        return Arrays.stream(EvolutionType.values())
                     .filter(e -> e.evolutionValue == index)
                     .findFirst()
                     .orElse(null);
    }

    public boolean usesLevel() {
        return usesLevel;
    }
}
