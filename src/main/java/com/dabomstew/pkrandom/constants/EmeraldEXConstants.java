package com.dabomstew.pkrandom.constants;

import com.dabomstew.pkrandom.pokemon.ItemList;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO: A lot of these 'constants' seem like they can (and will) change with updates
// I think more stuff needs to come from the inigen, but also players need more control over the config (so maybe extra config files)
public class EmeraldEXConstants {

    public static final int romNameOffset = 0xA0;
    public static final int romCodeOffset = 0xAC;
    public static final int romVersionOffset = 0xBC;

    // TODO: whe are these being read from hard coded ptrs? We could pull these values from the .map
    // For example use gItemsInfo father than going directly to efrlgMoveDataPointer
    public static final int efrlgMoveNamesPointer = 0x148;
    public static final int efrlgAbilityNamesPointer = 0x1C0;
    public static final int efrlgItemDataPointer = 0x1C8;
    public static final int efrlgPokemonStatsPointer = 0x1BC;
    public static final int efrlgFrontSpritesPointer = 0x128;
    public static final int efrlgPokemonPalettesPointer = 0x130;

    public static final int baseStatsEntrySize = 0x24;

    public static final int bsHPOffset = 0;
    public static final int bsAttackOffset = 1;
    public static final int bsDefenseOffset = 2;
    public static final int bsSpeedOffset = 3;
    public static final int bsSpAtkOffset = 4;
    public static final int bsSpDefOffset = 5;
    public static final int bsPrimaryTypeOffset = 6;
    public static final int bsSecondaryTypeOffset = 7;
    public static final int bsCatchRateOffset = 8;
    public static final int bsCommonHeldItemOffset = 14;
    public static final int bsRareHeldItemOffset = 16;
    public static final int bsGenderRatioOffset = 18;
    public static final int bsGrowthCurveOffset = 21;
    public static final int bsAbility1Offset = 24;
    public static final int bsAbility2Offset = 26;
    public static final int bsAbility3Offset = 28;

    public static final int learnsetPtrOffset = 0x90;
    public static final int evolutionsPtrOffset = 0x9C;

    public static final int textTerminator = 0xFF;
    public static final int textVariable = 0xFD;

    public static final byte freeSpaceByte = (byte) 0xFF;

    public static final int rseStarter2Offset = 2, rseStarter3Offset = 4;

    public static final Type[] typeTable = constructTypeTable();

    public static final int slowpokeIndex = 79, mewIndex = 151;

    public static final int tmCount = (ItemConstants.TMS_END - ItemConstants.TMS_START) + 1;
    public static final int hmCount = (ItemConstants.HMS_END - ItemConstants.HMS_START) + 1;

    public static final List<Integer> hmMoves = Arrays.asList(0xf, 0x13, 0x39, 0x46, 0x94, 0xf9, 0x7f, 0x123);

    public static final int tmItemOffset = 582;

    public static final int rseItemDescCharsPerLine = 18;

    public static final int regularTextboxCharsPerLine = 36;

    public static final int pointerSearchRadius = 500;

    public static final int itemDataDescriptionOffset = 0x14;

    public static final String deoxysObeyCode = "CD21490088420FD0";

    public static final int mewObeyOffsetFromDeoxysObey = 0x16;

    public static final String ePokedexScriptIdentifier = "3229610825F00129E40816CD40010003";

    public static final String eNatDexScriptPart1 = "31720167";

    public static final String eNatDexScriptPart2 = "3229610825F00129E40825F30116CD40010003";

    public static final int waterStoneIndex = 212;

    public static final int highestAbilityIndex = 310;

    public static final List<Integer> eRequiredFieldTMs = Arrays.asList(2, 6, 7, 11, 18, 22, 23, 30, 37, 48);

    public static final List<Integer> rseFieldMoves = Arrays.asList(15, 19, 29, 57, 70, 148, 91, 100, 127, 249, 230, 291, 290);

    public static final List<Integer> rseEarlyRequiredHMMoves = Collections.singletonList(249);

    // Shadow lugia has no data
    public static final List<Integer> unimplementedPokemon = Collections.singletonList(1435);

    public static final int luckyEggIndex = 470;

    // Max should really be like 20, but this is way higher than any pokemon actually learns
    public static final int MAX_LEVEL_UP_LEARNSET = 51;

    public static final int SHEDINJA_NUMBER = 292;

    public static final int BIRCH_INTRO_MON_INDEX = 0;
    public static final int STARTER_ITEM_INDEX = 1;
    public static final int BATTLE_TUTORIAL_OPPONENT_INDEX = 2;
    public static final int WALLY_CATCH_TUTORIAL_MON_INDEX = 3;
    public static final int WALLY_CATCH_TUTORIAL_OPPONENT_INDEX = 4;
    public static final int PC_START_ITEM_INDEX = 5;
    public static final int TUTOR_COMPATIBILITY_INDEX = 6;
    public static final int TMHM_COMPATIBILITY_INDEX = 7;
    public static final int MART_PROMO_ITEM_INDEX = 8;
    public static final int TRAINER_LEVEL_BOOST_PERCENT_INDEX = 9;
    public static final int GEN_RESTRICTIONS_INDEX = 10;

    public static final int BERRY_TREE_COUNT = 178;
    public static final int PICKUP_ITEM_COUNT = 30;
    public static final int PICKUP_ITEM_STRUCT_SIZE = 12;

    private static Type[] constructTypeTable() {
        Type[] table = new Type[256];
        table[0x00] = Type.NONE;
        table[0x01] = Type.NORMAL;
        table[0x02] = Type.FIGHTING;
        table[0x03] = Type.FLYING;
        table[0x04] = Type.POISON;
        table[0x05] = Type.GROUND;
        table[0x06] = Type.ROCK;
        table[0x07] = Type.BUG;
        table[0x08] = Type.GHOST;
        table[0x09] = Type.STEEL;
        table[0x0A] = Type.MYSTERY;
        table[0x0B] = Type.FIRE;
        table[0x0C] = Type.WATER;
        table[0x0D] = Type.GRASS;
        table[0x0E] = Type.ELECTRIC;
        table[0x0F] = Type.PSYCHIC;
        table[0x10] = Type.ICE;
        table[0x11] = Type.DRAGON;
        table[0x12] = Type.DARK;
        table[0x13] = Type.FAIRY;
        table[0x14] = Type.STELLAR;
        return table;
    }

    public static byte typeToByte(Type type) {
        if (type == null) {
            return 0x09; // ???-type
        }
        switch (type) {
            case NONE:
                return 0;
            case NORMAL:
                return 1;
            case FIGHTING:
                return 2;
            case FLYING:
                return 3;
            case POISON:
                return 4;
            case GROUND:
                return 5;
            case ROCK:
                return 6;
            case BUG:
                return 7;
            case GHOST:
                return 8;
            case STEEL:
                return 9;
            case MYSTERY:
                return 10;
            case FIRE:
                return 11;
            case WATER:
                return 12;
            case GRASS:
                return 13;
            case ELECTRIC:
                return 14;
            case PSYCHIC:
                return 15;
            case ICE:
                return 16;
            case DRAGON:
                return 17;
            case DARK:
                return 18;
            case FAIRY:
                return 19;
            case STELLAR:
                return 20;
            default:
                return 1; // normal by default
        }
    }

    public static byte typeToPalIndex(Type type) {
        if (type == null) {
            return 0x09; // ???-type
        }
        switch (type) {
            case NORMAL:
                return 0;
            case FIGHTING:
                return 1;
            case FLYING:
                return 2;
            case POISON:
                return 3;
            case GROUND:
                return 4;
            case ROCK:
                return 5;
            case GRASS:
            case BUG:
                return 6;
            case MYSTERY:
            case GHOST:
                return 7;
            case STEEL:
                return 8;
            case FIRE:
                return 9;
            case WATER:
                return 10;
            case ELECTRIC:
                return 11;
            case PSYCHIC:
                return 12;
            case ICE:
                return 13;
            case DRAGON:
                return 14;
            case DARK:
                return 15;
            case FAIRY:
                return 16;
            case STELLAR:
            case NONE:
            default:
                return 1;
        }
    }

    public static Type byteToType(int typeIndexToType) {

        switch (typeIndexToType) {
            case 0:
                return Type.NONE;
            case 1:
                return Type.NORMAL;
            case 2:
                return Type.FIGHTING;
            case 3:
                return Type.FLYING;
            case 4:
                return Type.POISON;
            case 5:
                return Type.GROUND;
            case 6:
                return Type.ROCK;
            case 7:
                return Type.BUG;
            case 8:
                return Type.GHOST;
            case 9:
                return Type.STEEL;
            case 10:
                return Type.MYSTERY;
            case 11:
                return Type.FIRE;
            case 12:
                return Type.WATER;
            case 13:
                return Type.GRASS;
            case 14:
                return Type.ELECTRIC;
            case 15:
                return Type.PSYCHIC;
            case 16:
                return Type.ICE;
            case 17:
                return Type.DRAGON;
            case 18:
                return Type.DARK;
            case 19:
                return Type.FAIRY;
            case 20:
                return Type.STELLAR;
            default:
                return Type.MYSTERY;
        }
    }

    public static ItemList allowedItems;
    public static ItemList nonBadItems;

    static {
        setupAllowedItems();
    }

    private static void setupAllowedItems() {
        allowedItems = new ItemList(833); // TODO: this should come from item count

        allowedItems.banSingles(ItemConstants.ITEM_NONE, ItemConstants.RED_ORB, ItemConstants.BLUE_ORB, ItemConstants.TERA_ORB);
        allowedItems.banRange(ItemConstants.TMS_START, (ItemConstants.TMS_END - ItemConstants.TMS_START) + 1);
        allowedItems.banRange(ItemConstants.HMS_START, (ItemConstants.HMS_END - ItemConstants.HMS_START) + 1);
        allowedItems.banRange(ItemConstants.UNUSED_TM_START, (ItemConstants.UNUSED_TM_END - ItemConstants.UNUSED_TM_START) + 1);
        allowedItems.banRange(ItemConstants.GENERAL_KEY_ITEMS_START, (ItemConstants.GENERAL_KEY_ITEMS_END - ItemConstants.GENERAL_KEY_ITEMS_START) + 1);
        allowedItems.banRange(ItemConstants.SPEEDCHOICE_START, (ItemConstants.SPEEDCHOICE_END - ItemConstants.SPEEDCHOICE_START) + 1);

        nonBadItems = allowedItems.copy();
        nonBadItems.banSingles(ItemConstants.DYNAMAX_CANDY);
        nonBadItems.banRange(ItemConstants.MULCH_START, (ItemConstants.MULCH_END - ItemConstants.MULCH_START) + 1);
        nonBadItems.banRange(ItemConstants.APRICORN_START, (ItemConstants.APRICORN_END - ItemConstants.APRICORN_START) + 1);
        nonBadItems.banRange(ItemConstants.MISC_SPECIFIC_START, (ItemConstants.MISC_SPECIFIC_END - ItemConstants.MISC_SPECIFIC_START) + 1);
        nonBadItems.banRange(ItemConstants.MAIL_START, (ItemConstants.MAIL_END - ItemConstants.MAIL_START) + 1);
        nonBadItems.banRange(ItemConstants.NECTARS_START, (ItemConstants.NECTARS_END - ItemConstants.NECTARS_START) + 1);
        nonBadItems.banRange(ItemConstants.CONTEST_SCARVES_START, (ItemConstants.CONTEST_SCARVES_END - ItemConstants.CONTEST_SCARVES_START) + 1);
        // nonBadItems.banRange(ItemConstants.BERRIES_START, (ItemConstants.BERRIES_END - ItemConstants.BERRIES_START) + 1);
        // Berries that have no effect in battle
        nonBadItems.banSingles(ItemConstants.CORNN_BERRY, ItemConstants.WEPEAR_BERRY, ItemConstants.MAGOST_BERRY,
                ItemConstants.RABUTA_BERRY, ItemConstants.NOMEL_BERRY, ItemConstants.SPELON_BERRY,
                ItemConstants.PAMTRE_BERRY, ItemConstants.WATMEL_BERRY, ItemConstants.DURIN_BERRY,
                ItemConstants.BELUE_BERRY);
        // Ban items where the effect doesn't work
        nonBadItems.banSingles(ItemConstants.AUX_EVASION, ItemConstants.AUX_GUARD, ItemConstants.AUX_POWER,
                ItemConstants.POWERGUARD, ItemConstants.CHOICE_DUMPLING, ItemConstants.SWAP_SNACK, ItemConstants.SPICED_RADISH);

        // I'm not sure that cap pikachu will even be generated
        nonBadItems.banSingles(ItemConstants.PIKASHUNIUM_Z);

        // Some items (like z items or berries) are super common which means randomly selecting from the items list
        // will usually return those, By dividing items into categories we can get a better distrobution
        List<int[]> itemTypeRanges = new ArrayList<>();
        itemTypeRanges.add(new int[]{ItemConstants.BALLS_START, ItemConstants.BALLS_END});
        itemTypeRanges.add(new int[]{ItemConstants.MEDICINE_START, ItemConstants.SPECIALTIES_END});
        itemTypeRanges.add(new int[]{ItemConstants.VITAMINS_START, ItemConstants.FEATHERS_END});
        itemTypeRanges.add(new int[]{ItemConstants.ABILITY_MODIFIERS_START, ItemConstants.ABILITY_MODIFIERS_END});
        itemTypeRanges.add(new int[]{ItemConstants.MINTS_START, ItemConstants.MINTS_END});
        itemTypeRanges.add(new int[]{ItemConstants.CANDY_START, ItemConstants.CANDY_END});
        itemTypeRanges.add(new int[]{ItemConstants.FLUTES_START, ItemConstants.FLUTES_END});
        itemTypeRanges.add(new int[]{ItemConstants.REPEL_START, ItemConstants.ESCAPE_ROPE});
        itemTypeRanges.add(new int[]{ItemConstants.X_ITEMS_START, ItemConstants.X_ITEMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.TREASURE_START, ItemConstants.TREASURE_END});
        itemTypeRanges.add(new int[]{ItemConstants.FOSSIL_START, ItemConstants.FOSSIL_END});
        itemTypeRanges.add(new int[]{ItemConstants.MULCH_START, ItemConstants.MULCH_END});
        itemTypeRanges.add(new int[]{ItemConstants.APRICORN_START, ItemConstants.APRICORN_END});
        itemTypeRanges.add(new int[]{ItemConstants.MISC_SPECIFIC_START, ItemConstants.MISC_SPECIFIC_END});
        itemTypeRanges.add(new int[]{ItemConstants.MAIL_START, ItemConstants.MAIL_END});
        itemTypeRanges.add(new int[]{ItemConstants.EVO_START, ItemConstants.EVO_END});
        itemTypeRanges.add(new int[]{ItemConstants.NECTARS_START, ItemConstants.NECTARS_END});
        itemTypeRanges.add(new int[]{ItemConstants.PLATES_START, ItemConstants.PLATES_END});
        itemTypeRanges.add(new int[]{ItemConstants.DRIVES_START, ItemConstants.RUSTED_SHIELD});
        itemTypeRanges.add(new int[]{ItemConstants.MEGA_STONES_START, ItemConstants.MEGA_STONES_END});
        itemTypeRanges.add(new int[]{ItemConstants.GEMS_START, ItemConstants.GEMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.Z_CRYSTALS_START, ItemConstants.Z_CRYSTALS_END});
        itemTypeRanges.add(new int[]{ItemConstants.MON_SPECIFIC_HELD_ITEMS_START, ItemConstants.MON_SPECIFIC_HELD_ITEMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.INCENSE_START, ItemConstants.INCENSE_END});
        itemTypeRanges.add(new int[]{ItemConstants.CONTEST_SCARVES_START, ItemConstants.CONTEST_SCARVES_END});
        itemTypeRanges.add(new int[]{ItemConstants.EV_MODIFIERS_START, ItemConstants.EV_MODIFIERS_END});
        itemTypeRanges.add(new int[]{ItemConstants.HELD_BATTLE_ITEMS_START, ItemConstants.HELD_BATTLE_ITEMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.BERRIES_START, ItemConstants.BERRIES_END});
        itemTypeRanges.add(new int[]{ItemConstants.TMS_START, ItemConstants.TMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.HMS_START, ItemConstants.HMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.CHARMS_START, ItemConstants.CHARMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.FORM_CHANGE_START, ItemConstants.FORM_CHANGE_END});
        itemTypeRanges.add(new int[]{ItemConstants.GENERAL_KEY_ITEMS_START, ItemConstants.GENERAL_KEY_ITEMS_END});
        itemTypeRanges.add(new int[]{ItemConstants.GEN_9_BATTLE_START, ItemConstants.GEN_9_BATTLE_END});
        itemTypeRanges.add(new int[]{ItemConstants.TERA_SHARD_START, ItemConstants.TERA_SHARD_END});
        itemTypeRanges.add(new int[]{ItemConstants.GEN_9_EVO_FORM_START, ItemConstants.GEN_9_EVO_FORM_END});
        itemTypeRanges.add(new int[]{ItemConstants.GEN_9_STAT_START, ItemConstants.GEN_9_STAT_END});
        itemTypeRanges.add(new int[]{ItemConstants.GEN_9_MEDICINE_START, ItemConstants.GEN_9_MEDICINE_END});

        allowedItems.configureGroups(itemTypeRanges);
        nonBadItems.configureGroups(itemTypeRanges);
    }

    public static void trainerTagsE(List<Trainer> trs) {
        // Gym Trainers
        tag(trs, "GYM1", 0x140, 0x141, 0x23B);
        tag(trs, "GYM2", 0x1AA, 0x1A9, 0xB3, 0x23C, 0x23D, 0x23E);
        tag(trs, "GYM3", 0xBF, 0x143, 0xC2, 0x289, 0x322);
        tag(trs, "GYM4", 0x288, 0xC9, 0xCB, 0x28A, 0xCA, 0xCC, 0x1F5, 0xCD);
        tag(trs, "GYM5", 0x47, 0x59, 0x49, 0x5A, 0x48, 0x5B, 0x4A);
        tag(trs, "GYM6", 0x192, 0x28F, 0x191, 0x28E, 0x194, 0x323);
        tag(trs, "GYM7", 0xE9, 0xEA, 0xEB, 0xF4, 0xF5, 0xF6, 0x24F, 0x248, 0x247, 0x249, 0x246, 0x23F);
        tag(trs, "GYM8", 0x265, 0x80, 0x1F6, 0x73, 0x81, 0x76, 0x82, 0x12D, 0x83, 0x266);

        // Gym Leaders + Emerald Rematches!
        tag(trs, "GYM1", 0x109, 0x302, 0x303, 0x304, 0x305);
        tag(trs, "GYM2", 0x10A, 0x306, 0x307, 0x308, 0x309);
        tag(trs, "GYM3", 0x10B, 0x30A, 0x30B, 0x30C, 0x30D);
        tag(trs, "GYM4", 0x10C, 0x30E, 0x30F, 0x310, 0x311);
        tag(trs, "GYM5", 0x10D, 0x312, 0x313, 0x314, 0x315);
        tag(trs, "GYM6", 0x10E, 0x316, 0x317, 0x318, 0x319);
        tag(trs, "GYM7", 0x10F, 0x31A, 0x31B, 0x31C, 0x31D);
        tag(trs, "GYM8", 0x110, 0x31E, 0x31F, 0x320, 0x321);

        // Elite 4
        tag(trs, 0x105, "ELITE1");
        tag(trs, 0x106, "ELITE2");
        tag(trs, 0x107, "ELITE3");
        tag(trs, 0x108, "ELITE4");
        tag(trs, 0x14F, "CHAMPION");

        // Brendan
        tag(trs, 0x208, "RIVAL1-2");
        tag(trs, 0x20B, "RIVAL1-0");
        tag(trs, 0x20E, "RIVAL1-1");

        tag(trs, 0x251, "RIVAL2-2");
        tag(trs, 0x250, "RIVAL2-0");
        tag(trs, 0x257, "RIVAL2-1");

        tag(trs, 0x209, "RIVAL3-2");
        tag(trs, 0x20C, "RIVAL3-0");
        tag(trs, 0x20F, "RIVAL3-1");

        tag(trs, 0x20A, "RIVAL4-2");
        tag(trs, 0x20D, "RIVAL4-0");
        tag(trs, 0x210, "RIVAL4-1");

        tag(trs, 0x295, "RIVAL5-2");
        tag(trs, 0x296, "RIVAL5-0");
        tag(trs, 0x297, "RIVAL5-1");

        // May
        tag(trs, 0x211, "RIVAL1-2");
        tag(trs, 0x214, "RIVAL1-0");
        tag(trs, 0x217, "RIVAL1-1");

        tag(trs, 0x258, "RIVAL2-2");
        tag(trs, 0x300, "RIVAL2-0");
        tag(trs, 0x301, "RIVAL2-1");

        tag(trs, 0x212, "RIVAL3-2");
        tag(trs, 0x215, "RIVAL3-0");
        tag(trs, 0x218, "RIVAL3-1");

        tag(trs, 0x213, "RIVAL4-2");
        tag(trs, 0x216, "RIVAL4-0");
        tag(trs, 0x219, "RIVAL4-1");

        tag(trs, 0x298, "RIVAL5-2");
        tag(trs, 0x299, "RIVAL5-0");
        tag(trs, 0x29A, "RIVAL5-1");

        // Themed
        tag(trs, "THEMED:MAXIE", 0x259, 0x25A, 0x2DE);
        tag(trs, "THEMED:TABITHA", 0x202, 0x255, 0x2DC);
        tag(trs, "THEMED:ARCHIE", 0x22);
        tag(trs, "THEMED:MATT", 0x1E);
        tag(trs, "THEMED:SHELLY", 0x20, 0x21);

        // Steven
        tag(trs, 0x324, "UBER");

    }

    private static void tag(List<Trainer> trainers, int trainerNum, String tag) {
        trainers.get(trainerNum - 1).setTag(tag);
    }

    private static void tag(List<Trainer> allTrainers, String tag, int... numbers) {
        for (int num : numbers) {
            allTrainers.get(num - 1).setTag(tag);
        }
    }

    // Used for frontier mons
    public static List<Integer> getBattleItems()
    {
        List<Integer> items = new ArrayList<>();

        items.add(339);
        items.add(340);
        items.add(341);
        items.add(342);
        items.add(343);
        items.add(344);
        items.add(345);
        items.add(346);
        items.add(347);
        items.add(348);
        items.add(349);
        items.add(350);
        items.add(351);
        items.add(352);
        items.add(353);
        items.add(354);
        items.add(355);
        items.add(356);
        items.add(404);
        items.add(405);
        items.add(406);
        items.add(407);
        items.add(408);
        items.add(409);
        items.add(410);
        items.add(411);
        items.add(412);
        items.add(425);
        items.add(426);
        items.add(427);
        items.add(428);
        items.add(429);
        items.add(430);
        items.add(431);
        items.add(432);
        items.add(433);
        items.add(434);
        items.add(435);
        items.add(436);
        items.add(437);
        items.add(438);
        items.add(439);
        items.add(440);
        items.add(441);
        items.add(442);
        items.add(443);
        items.add(444);
        items.add(445);
        items.add(446);
        items.add(447);
        items.add(448);
        items.add(449);
        items.add(450);
        items.add(451);
        items.add(452);
        items.add(453);
        items.add(454);
        items.add(455);
        items.add(456);
        items.add(457);
        items.add(458);
        items.add(459);
        items.add(460);
        items.add(462);
        items.add(463);
        items.add(464);
        items.add(465);
        items.add(466);
        items.add(467);
        items.add(468);
        items.add(469);
        items.add(470);
        items.add(471);
        items.add(472);
        items.add(473);
        items.add(474);
        items.add(475);
        items.add(476);
        items.add(477);
        items.add(478);
        items.add(479);
        items.add(480);
        items.add(481);
        items.add(482);
        items.add(483);
        items.add(484);
        items.add(485);
        items.add(486);
        items.add(487);
        items.add(488);
        items.add(489);
        items.add(490);
        items.add(491);
        items.add(492);
        items.add(493);
        items.add(494);
        items.add(495);
        items.add(496);
        items.add(497);
        items.add(498);
        items.add(499);
        items.add(500);
        items.add(501);
        items.add(502);
        items.add(503);
        items.add(504);
        items.add(505);
        items.add(506);
        items.add(507);
        items.add(508);
        items.add(509);
        items.add(510);
        items.add(511);
        items.add(512);
        items.add(513);

        // Berries
        items.add(514);
        items.add(515);
        items.add(516);
        items.add(517);
        items.add(518);
        items.add(519);
        items.add(520);
        items.add(521);
        items.add(522);
        items.add(523);
        items.add(524);
        items.add(525);
        items.add(526);
        items.add(527);
        items.add(528);
        items.add(529);
        items.add(530);
        items.add(531);
        items.add(533);
        items.add(534);
        items.add(535);
        items.add(536);
        items.add(537);
        items.add(538);
        items.add(539);
        items.add(549);
        items.add(550);
        items.add(551);
        items.add(552);
        items.add(553);
        items.add(554);
        items.add(555);
        items.add(556);
        items.add(557);
        items.add(558);
        items.add(559);
        items.add(560);
        items.add(561);
        items.add(562);
        items.add(563);
        items.add(564);
        items.add(565);
        items.add(566);
        items.add(567);
        items.add(568);
        items.add(569);
        items.add(570);
        items.add(571);
        items.add(572);
        items.add(573);
        items.add(574);
        items.add(575);
        items.add(576);
        items.add(577);
        items.add(578);
        items.add(579);
        items.add(580);

        // Gen 9
        items.add(758);
        items.add(759);
        items.add(760);
        items.add(761);
        items.add(762);
        items.add(764);
        items.add(769);

        return items;
    }

    @SuppressWarnings("unused")
    public static class ItemConstants {
        public static final int ITEM_NONE = 0;

        public static final int BALLS_START = 1;
        public static final int BALLS_END = 27;

        public static final int MEDICINE_START = 28;
        public static final int MEDICINE_END = 56;

        public static final int SPECIALTIES_START = 57;
        public static final int SPECIALTIES_END = 64;

        public static final int VITAMINS_START = 65;
        public static final int VITAMINS_END = 72;

        public static final int FEATHERS_START = 73;
        public static final int FEATHERS_END = 78;

        public static final int ABILITY_MODIFIERS_START = 79;
        public static final int ABILITY_MODIFIERS_END = 80;

        public static final int MINTS_START = 81;
        public static final int MINTS_END = 101;

        public static final int CANDY_START = 102;
        public static final int CANDY_END = 108;

        public static final int DYNAMAX_CANDY = 108;

        public static final int FLUTES_START = 109;
        public static final int FLUTES_END = 113;

        // This also includes lures
        public static final int REPEL_START = 114;
        public static final int REPEL_END = 119;

        public static final int MAX_REPEL = 116;

        public static final int ESCAPE_ROPE = 120;

        public static final int X_ITEMS_START = 121;
        public static final int X_ITEMS_END = 126;

        public static final int ESCAPE_ITEMS_START = 129;
        public static final int ESCAPE_ITEMS_END = 132;

        public static final int TREASURE_START = 133;
        public static final int TREASURE_END = 164;

        public static final int FOSSIL_START = 165;
        public static final int FOSSIL_END = 179;

        public static final int MULCH_START = 180;
        public static final int MULCH_END = 187;

        public static final int APRICORN_START = 188;
        public static final int APRICORN_END = 194;

        // e.g GALARICA_TWIG
        public static final int MISC_SPECIFIC_START = 195;
        public static final int MISC_SPECIFIC_END = 198;

        public static final int MAIL_START = 199;
        public static final int MAIL_END = 210;

        public static final int EVO_START = 211;
        public static final int EVO_END = 245;

        public static final int NECTARS_START = 246;
        public static final int NECTARS_END = 249;

        public static final int PLATES_START = 250;
        public static final int PLATES_END = 266;

        public static final int DRIVES_START = 267;
        public static final int DRIVES_END = 270;

        public static final int MEMORIES_START = 271;
        public static final int MEMORIES_END = 287;

        public static final int RUSTED_SWORD = 288;
        public static final int RUSTED_SHIELD = 289;

        public static final int RED_ORB = 290;
        public static final int BLUE_ORB = 291;

        public static final int MEGA_STONES_START = 292;
        public static final int MEGA_STONES_END = 338;

        public static final int GEMS_START = 339;
        public static final int GEMS_END = 356;

        public static final int Z_CRYSTALS_START = 357;
        public static final int Z_CRYSTALS_END = 391;

        public static final int PIKASHUNIUM_Z = 390;

        public static final int MON_SPECIFIC_HELD_ITEMS_START = 392;
        public static final int MON_SPECIFIC_HELD_ITEMS_END = 403;

        public static final int INCENSE_START = 404;
        public static final int INCENSE_END = 412;

        public static final int CONTEST_SCARVES_START = 413;
        public static final int CONTEST_SCARVES_END = 417;

        public static final int EV_MODIFIERS_START = 418;
        public static final int EV_MODIFIERS_END = 424;

        public static final int HELD_BATTLE_ITEMS_START = 425;
        public static final int HELD_BATTLE_ITEMS_END = 513;

        public static final int BERRIES_START = 514;
        public static final int BERRIES_END = 581;

        // No battle effect berries
        public static final int CORNN_BERRY = 581;
        public static final int WEPEAR_BERRY = 532;
        public static final int MAGOST_BERRY = 541;
        public static final int RABUTA_BERRY = 542;
        public static final int NOMEL_BERRY = 543;
        public static final int SPELON_BERRY = 544;
        public static final int PAMTRE_BERRY = 545;
        public static final int WATMEL_BERRY = 546;
        public static final int DURIN_BERRY = 547;
        public static final int BELUE_BERRY = 548;

        public static final int TMS_START = 582;
        public static final int TMS_END = 681;

        public static final int UNUSED_TM_START = 632;
        public static final int UNUSED_TM_END = 681;

        public static final int HMS_START = 682;
        public static final int HMS_END = 689;

        public static final int CHARMS_START = 690;
        public static final int CHARMS_END = 693;

        public static final int FORM_CHANGE_START = 694;
        public static final int FORM_CHANGE_END = 702;

        public static final int GENERAL_KEY_ITEMS_START = 703;
        public static final int GENERAL_KEY_ITEMS_END = 757;

        public static final int GEN_9_BATTLE_START = 758;
        public static final int GEN_9_BATTLE_END = 773;

        public static final int TERA_SHARD_START = 774;
        public static final int TERA_SHARD_END = 791;

        public static final int GEN_9_EVO_FORM_START = 795;
        public static final int GEN_9_EVO_FORM_END = 805;

        public static final int GEN_9_STAT_START = 795;
        public static final int GEN_9_STAT_END = 815;

        public static final int GEN_9_MEDICINE_START = 816;
        public static final int GEN_9_MEDICINE_END = 827;

        public static final int TERA_ORB = 772;

        // Items with effects currently missing
        public static final int AUX_EVASION = 820;
        public static final int AUX_GUARD = 821;
        public static final int AUX_POWER = 822;
        public static final int POWERGUARD = 823;
        public static final int CHOICE_DUMPLING = 824;
        public static final int SWAP_SNACK = 825;
        public static final int SPICED_RADISH = 826;

        public static final int SPEEDCHOICE_START = 828;
        public static final int SPEEDCHOICE_END = 833;
    }

    /**
     * These mons arn't removed from the game we just want to make sure they don't show up in the wild
     * Normally because they should only show up as an effect in battle
     */
    public enum BannedEncounterMons {
        VENUSAUR_MEGA(906),
        CHARIZARD_MEGA_X(907),
        CHARIZARD_MEGA_Y(908),
        BLASTOISE_MEGA(909),
        BEEDRILL_MEGA(910),
        PIDGEOT_MEGA(911),
        ALAKAZAM_MEGA(912),
        SLOWBRO_MEGA(913),
        GENGAR_MEGA(914),
        KANGASKHAN_MEGA(915),
        PINSIR_MEGA(916),
        GYARADOS_MEGA(917),
        AERODACTYL_MEGA(918),
        MEWTWO_MEGA_X(919),
        MEWTWO_MEGA_Y(920),
        AMPHAROS_MEGA(921),
        STEELIX_MEGA(922),
        SCIZOR_MEGA(923),
        HERACROSS_MEGA(924),
        HOUNDOOM_MEGA(925),
        TYRANITAR_MEGA(926),
        SCEPTILE_MEGA(927),
        BLAZIKEN_MEGA(928),
        SWAMPERT_MEGA(929),
        GARDEVOIR_MEGA(930),
        SABLEYE_MEGA(931),
        MAWILE_MEGA(932),
        AGGRON_MEGA(933),
        MEDICHAM_MEGA(934),
        MANECTRIC_MEGA(935),
        SHARPEDO_MEGA(936),
        CAMERUPT_MEGA(937),
        ALTARIA_MEGA(938),
        BANETTE_MEGA(939),
        ABSOL_MEGA(940),
        GLALIE_MEGA(941),
        SALAMENCE_MEGA(942),
        METAGROSS_MEGA(943),
        LATIAS_MEGA(944),
        LATIOS_MEGA(945),
        LOPUNNY_MEGA(946),
        GARCHOMP_MEGA(947),
        LUCARIO_MEGA(948),
        ABOMASNOW_MEGA(949),
        GALLADE_MEGA(950),
        AUDINO_MEGA(951),
        DIANCIE_MEGA(952),
        RAYQUAZA_MEGA(953),
        KYOGRE_PRIMAL(954),
        GROUDON_PRIMAL(955),
        DARMANITAN_ZEN_MODE(1092),
        DARMANITAN_GALARIAN_ZEN_MODE(1093),
        WISHIWASHI_SCHOOL(1175),
        EISCUE_NOICE_FACE(1224),
        SPECIES_MORPEKO_HANGRY(1226),
        PALAFIN_HERO(1353),
        TERAPAGOS_TERASTAL(1432),
        TERAPAGOS_STELLAR(1433),
        LUGIA_SHADOW(1435),
        OGERPON_TEAL_MASK_TERA(1420),
        OGERPON_WELLSPRING_MASK_TERA(1421),
        OGERPON_HEARTHFLAME_MASK_TERA(1422),
        OGERPON_CORNERSTONE_MASK_TERA(1423),
        RATICATE_ALOLAN_TOTEM(1476),
        GUMSHOOS_TOTEM(1477),
        VIKAVOLT_TOTEM(1478),
        LURANTIS_TOTEM(1479),
        SALAZZLE_TOTEM(1480),
        MIMIKYU_TOTEM_DISGUISED(1481),
        KOMMO_O_TOTEM(1482),
        MAROWAK_ALOLAN_TOTEM(1483),
        RIBOMBEE_TOTEM(1484),
        ARAQUANID_TOTEM(1485),
        TOGEDEMARU_TOTEM(1486),
        PIKACHU_PARTNER(1487),
        EEVEE_PARTNER(1488),
        VENUSAUR_GIGANTAMAX(1489),
        BLASTOISE_GIGANTAMAX(1490),
        CHARIZARD_GIGANTAMAX(1491),
        BUTTERFREE_GIGANTAMAX(1492),
        PIKACHU_GIGANTAMAX(1493),
        MEOWTH_GIGANTAMAX(1494),
        MACHAMP_GIGANTAMAX(1495),
        GENGAR_GIGANTAMAX(1496),
        KINGLER_GIGANTAMAX(1497),
        LAPRAS_GIGANTAMAX(1498),
        EEVEE_GIGANTAMAX(1499),
        SNORLAX_GIGANTAMAX(1500),
        GARBODOR_GIGANTAMAX(1501),
        MELMETAL_GIGANTAMAX(1502),
        RILLABOOM_GIGANTAMAX(1503),
        CINDERACE_GIGANTAMAX(1504),
        INTELEON_GIGANTAMAX(1505),
        CORVIKNIGHT_GIGANTAMAX(1506),
        ORBEETLE_GIGANTAMAX(1507),
        DREDNAW_GIGANTAMAX(1508),
        COALOSSAL_GIGANTAMAX(1509),
        FLAPPLE_GIGANTAMAX(1510),
        APPLETUN_GIGANTAMAX(1511),
        SANDACONDA_GIGANTAMAX(1512),
        TOXTRICITY_AMPED_GIGANTAMAX(1513),
        TOXTRICITY_LOW_KEY_GIGANTAMAX(1514),
        CENTISKORCH_GIGANTAMAX(1515),
        HATTERENE_GIGANTAMAX(1516),
        GRIMMSNARL_GIGANTAMAX(1517),
        ALCREMIE_GIGANTAMAX(1518),
        COPPERAJAH_GIGANTAMAX(1519),
        DURALUDON_GIGANTAMAX(1520),
        URSHIFU_SINGLE_STRIKE_STYLE_GIGANTAMAX(1521),
        URSHIFU_RAPID_STRIKE_STYLE_GIGANTAMAX(1522),
        MIMIKYU_TOTEM_BUSTED(1523);

        private final Integer internalSpeciesValue;

        BannedEncounterMons(Integer internalSpeciesValue) {
            this.internalSpeciesValue = internalSpeciesValue;
        }

        public Integer getInternalSpeciesValue() {
            return internalSpeciesValue;
        }
    }

}
