package com.dabomstew.pkrandom.constants;

import java.util.Arrays;
import java.util.List;

public class GlobalConstants {

    public static final List<Integer> bannedRandomMoves = Arrays.asList(
            144,   // Transform, glitched in RBY
            165  // Struggle, self explanatory
    );

    public static final List<Integer> bannedForDamagingMove = Arrays.asList(
            120, // SelfDestruct
            138, // Dream Eater
            153, // Explosion
            173, // Snore
            206, // False Swipe
            248, // Future Sight
            252, // Fake Out
            264, // Focus Punch
            353, // Doom Desire
            364, // Feint
            387, // Last Resort
            389, // Sucker Punch
            132, // Constrict, overly weak
            99,// Rage, lock-in in gen1
            205, // Rollout, lock-in
            301, // Ice Ball, Rollout clone
            39,// Sonicboom
            82,// Dragon Rage
            32,// Horn Drill
            12,// Guillotine
            90,// Fissure
            329, // Sheer Cold
            621, // Hyperspace Fury, fails is anything other than hoopa uses it
            711 // Aura Wheel, as it only works properly with morpeko
    );
    
    /* @formatter:on */

    public static final List<Integer> battleTrappingAbilities = Arrays.asList(23, 42, 71);
    // Shadow Tag, Magnet Pull, Arena Trap

    public static final List<Integer> negativeAbilities = Arrays.asList(129, 112, 54, 59, 161, 103);
    // Defeatist, Slow Start, Truant, Forecast, Zen Mode, Klutz
    // To test: Illusion, Imposter

    public static final int WONDER_GUARD_INDEX = 25;

    public static final int MIN_DAMAGING_MOVE_POWER = 50;

    public static final int METRONOME_MOVE = 118;

    public static final int LEVEL_UP_MOVE_END = 0xFFFF;

}
