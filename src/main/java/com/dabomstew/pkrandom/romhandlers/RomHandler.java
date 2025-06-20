package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  RomHandler.java - defines the functionality that each randomization   --*/
/*--                    handler must implement.                             --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.dabomstew.pkrandom.CustomConfig;
import com.dabomstew.pkrandom.CustomNamesSet;
import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.EncounterSet;
import com.dabomstew.pkrandom.pokemon.FieldTM;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.IngameTrade;
import com.dabomstew.pkrandom.pokemon.ItemList;
import com.dabomstew.pkrandom.pokemon.ItemLocation;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.Type;

public interface RomHandler {


    abstract class Factory {

        public RomHandler create(Random random) {
            return create(random, null);
        }

        public abstract RomHandler create(Random random, PrintStream log);
        public abstract boolean isLoadable(String filename);


    }
    // Basic load/save to filenames

    boolean loadRom(String filename);
    boolean saveRom(String filename);

    String loadedFilename();

    // Log stuff
    void setLog(PrintStream logStream);

    // Get a List of Pokemon objects in this game.
    // 0 = null 1-whatever = the Pokemon.

    List<Pokemon> getPokemon();
    // Setup Gen Restrictions.

    void setPokemonPool(GenRestrictions restrictions);
    void removeEvosForPokemonPool();

    // Randomizer: Starters
    // Get starters, they should be ordered with Pokemon
    // following the one it is SE against.
    // E.g. Grass, Fire, Water or Fire, Water, Grass etc.

    List<Pokemon> getStarters();
    // Change the starter data in the ROM.
    // Optionally also change the starter used by the rival in
    // the level 5 battle, if there is one.

    boolean setStarters(List<Pokemon> newStarters);
    // Tells whether this ROM has the ability to have starters changed.
    // Was for before CUE's compressors were found and arm9 was untouchable.

    boolean canChangeStarters();

    // Randomizer: Pokemon stats
    // Run the stats shuffler on each Pokemon.

    void shufflePokemonStats(boolean evolutionSanity);
    // Randomise stats following evolutions for proportions or not (see
    // tooltips)

    void randomizePokemonStats(boolean evolutionSanity);
    //Randomizes pokemon base stats to be between existing observed base stat totals seen from other pokemon
    //(see tooltips for details)

    void randomizePokemonBaseStats(boolean evolutionSanity, boolean randomizeRatio, boolean evosBuffStats);
    //Randomizes pokemon base stats up to +or- the given percentage.
    //(see tooltips for details)

    void randomizePokemonBaseStatsPerc(boolean evolutionSanity, int percent, boolean randomizeRatio);
    //Equalizes Pokemon Stats so all pokemon will have the same base stat total
    //(see tooltips for details)

    void equalizePokemonStats(boolean evolutionSanity, boolean randomizeRatio);
    // Give a random Pokemon who's in this game

    Pokemon randomPokemon(boolean isForPlayer);
    // Give a random non-legendary Pokemon who's in this game
    // Business rules for who's legendary are in Pokemon class

    Pokemon randomNonLegendaryPokemon(boolean isForPlayer);
    // Give a random legendary Pokemon who's in this game
    // Business rules for who's legendary are in Pokemon class

    Pokemon randomLegendaryPokemon(boolean isForPlayer);
    // Give a random Pokemon who has 2 evolution stages
    // Should make a good starter Pokemon

    Pokemon random2EvosPokemon(boolean isForPlayer);
    //Give a random Pokemon who has 1 evolution stage

    Pokemon random1EvosPokemon(boolean isForPlayer);
    //Give a random Pokemon who has no evolution stages

    Pokemon random0EvosPokemon(boolean banLegend, boolean onlyLegend, boolean isForPlayer);

    // Randomizer: types
    // return a random type valid in this game.
    // straightforward except for gen1 where dark&steel are excluded.

    Type randomType(boolean onlyUsePokemonTypes);

    // randomise Pokemon types, with a switch on whether evolutions
    // should follow the same types or not.
    // some evolutions dont anyway, e.g. Eeveelutions, Hitmons

    void randomizePokemonTypes(boolean evolutionSanity);
    // Randomizer: pokemon abilities

    int abilitiesPerPokemon();
    int highestAbilityIndex();

    String abilityName(int number);

    void randomizeAbilities(boolean evolutionSanity, boolean allowWonderGuard, boolean banTrappingAbilities, boolean banNegativeAbilities);

    // Randomizer: wild pokemon

    List<EncounterSet> getEncounters(boolean useTimeOfDay, boolean condenseSlots);
    void setEncounters(boolean useTimeOfDay, boolean condenseSlots, List<EncounterSet> encounters);

    void randomEncounters(boolean useTimeOfDay, boolean catchEmAll, boolean ceaReasonableOnly, boolean typeThemed, boolean usePowerLevels,
                          boolean noLegendaries, boolean condenseSlots);

    void area1to1Encounters(boolean useTimeOfDay, boolean catchEmAll, boolean ceaReasonableOnly, boolean typeThemed,
                            boolean usePowerLevels, boolean noLegendaries);

    void game1to1Encounters(boolean useTimeOfDay, boolean usePowerLevels, boolean noLegendaries);

    boolean hasTimeBasedEncounters();

    List<Pokemon> bannedForWildEncounters();

    boolean canCondenseEncounterSlots();

    // Randomizer: trainer pokemon

    List<Trainer> getTrainers();
    void setTrainers(List<Trainer> trainerData);

    void randomizeTrainerPokes(boolean usePowerLevels, boolean noLegendaries, boolean noEarlyWonderGuard,
                               int levelModifier, boolean fillBossTeams);

    void typeThemeTrainerPokes(boolean usePowerLevels, boolean weightByFrequency, boolean noLegendaries,
                               boolean noEarlyWonderGuard, int levelModifier, boolean fillBossTeams);

    void typeMatchTrainerPokes(boolean usePowerLevels, boolean noLegendaries, boolean noEarlyWonderGuard,
                                int levelModifier, boolean fillBossTeams);

    void writeTrainerLevelModifier(int trainersLevelModifier);

    void rivalCarriesStarter();

    void forceFullyEvolvedTrainerPokes(int minLevel);

    // Randomizer: moves

    void randomizeMovePowers();

    void randomizeMovePPs();

    void randomizeMoveAccuracies();

    void randomizeMoveTypes();

    boolean hasPhysicalSpecialSplit();

    void randomizeMoveCategory();

    // return all the moves valid in this game.
    List<Move> getMoves();

    // Randomizer: moves learnt

    void writeMovesLearnt();

    List<Integer> getMovesBannedFromLevelup();

    void setMoveTutorCompatibility(int seed);

    void randomizeMovesLearnt(boolean typeThemed, boolean noBroken, boolean forceFourStartingMoves,
            double goodDamagingProbability);

    void orderDamagingMovesByDamage();

    void metronomeOnlyMode();

    boolean supportsFourStartingMoves();

    // Randomizer: static pokemon (except starters)

    List<Pokemon> getStaticPokemon();

    boolean setStaticPokemon(List<Pokemon> staticPokemon);

    void randomizeStaticPokemon(boolean legendForLegend);

    boolean canChangeStaticPokemon();

    boolean hasFrontier();

    List<Pokemon> bannedForStaticPokemon();

    // Randomizer: Frontier
    void randomizeFrontier(boolean randomMoves);

    // Randomizer: TMs/HMs

    List<Integer> getTMMoves();

    List<Integer> getHMMoves();

    void setTMMoves(List<Integer> moveIndexes);

    void randomizeTMMoves(boolean noBroken, boolean preserveField, double goodDamagingProbability);

    int getTMCount();

    int getHMCount();

    /**
     * Get TM/HM compatibility data from this rom. The result should contain a
     * boolean array for each Pokemon indexed as such:
     * 
     * 0: blank (false) / 1 - (getTMCount()) : TM compatibility /
     * (getTMCount()+1) - (getTMCount()+getHMCount()) - HM compatibility
     * 
     * @return
     */

    Map<Pokemon, boolean[]> getTMHMCompatibility();

    void setTMHMCompatibility(int seed);

    void randomizeTMHMCompatibility(Settings.TMsHMsCompatibilityMod mode);

    // Randomizer: move tutors

    boolean hasMoveTutors();

    List<Integer> getMoveTutorMoves();

    void setMoveTutorMoves(List<Integer> moves);

    void randomizeMoveTutorMoves(boolean noBroken, boolean preserveField, double goodDamagingProbability);

    void randomizeMoveTutorCompatibility(Settings.MoveTutorsCompatibilityMod mode);

    // mt/moveset sanity

    // Randomizer: trainer names

    boolean canChangeTrainerText();

    List<String> getTrainerNames();

    void setTrainerNames(List<String> trainerNames);

    enum TrainerNameMode {
        SAME_LENGTH, MAX_LENGTH, MAX_LENGTH_WITH_CLASS
    };

    TrainerNameMode trainerNameMode();
    
    // Banned characters in trainer names
    List<Character> getBannedTrainerNameCharacters();

    // Returns this with or without the class
    int maxTrainerNameLength();

    // Only relevant for gen2, which has fluid trainer name length but
    // only a certain amount of space in the ROM bank.
    int maxSumOfTrainerNameLengths();

    // Only needed if above mode is "MAX LENGTH WITH CLASS"
    List<Integer> getTCNameLengthsByTrainer();

    void randomizeTrainerNames(CustomNamesSet customNames);

    // Randomizer: trainer class names

    List<String> getTrainerClassNames();

    void setTrainerClassNames(List<String> trainerClassNames);

    boolean fixedTrainerClassNamesLength();

    int maxTrainerClassNameLength();

    void randomizeTrainerClassNames(CustomNamesSet customNames);

    List<Integer> getDoublesTrainerClasses();

    // Items

    ItemList getAllowedItems();

    ItemList getNonBadItems();

    void randomizeWildHeldItems(boolean banBadItems);

    String[] getItemNames();

    List<Integer> getStarterHeldItems();

    void setStarterHeldItems(List<Integer> items);

    void randomizeStarterHeldItems(boolean banBadItems);

    // Field Items

    // TMs on the field

    List<Integer> getRequiredFieldTMs();

    List<FieldTM> getCurrentFieldTMs();

    void setFieldTMs(List<Integer> fieldTMs);

    // Everything else

    List<ItemLocation> getRegularFieldItems();

    void setRegularFieldItems(List<Integer> items);

    // Randomizer methods

    void shuffleFieldItems();

    void randomizeFieldItems(boolean banBadItems);

    void randomizeGivenItems(boolean banBadRandomFieldItems);

    void randomizeBerryTrees(boolean banBadRandomFieldItems);

    void randomizePickupTime(boolean randomizePickupTables);

    void randomizeMarts(boolean banBadRandomFieldItems, boolean allMartsHaveBallAndRepel);

    void randomizeItemPrices(boolean allMartsHaveBallAndRepel);

    // Trades

    List<IngameTrade> getIngameTrades();

    void setIngameTrades(List<IngameTrade> trades);

    void randomizeIngameTrades(boolean randomizeRequest, boolean randomNickname, boolean randomOT,
                               boolean randomStats, boolean randomItem, CustomNamesSet customNames);

    boolean hasDVs();

    int maxTradeNicknameLength();

    int maxTradeOTNameLength();

    // Evos

    void removeTradeEvolutions(boolean changeMoveEvos);

    void condenseLevelEvolutions(int maxLevel, int maxIntermediateLevel);

    void randomizeEvolutions(boolean similarStrength, boolean sameType, boolean limitToThreeStages,
                             boolean forceChange);

    // stats stuff
    void minimumCatchRate(int rateNonLegendary, int rateLegendary);

    void standardizeEXPCurves();

    // (Mostly) unchanging lists of moves

    List<Integer> getGameBreakingMoves();

    // includes game or gen-specific moves like Secret Power
    // but NOT healing moves (Softboiled, Milk Drink)
    List<Integer> getFieldMoves();

    // any HMs required to obtain 4 badges
    // (excluding Gameshark codes or early drink in RBY)
    List<Integer> getEarlyRequiredHMMoves();

    // Misc

    String getROMName();

    String getROMCode();

    String getSupportLevel();

    String getDefaultExtension();

    int internalStringLength(String string);

    void applySignature();

    BufferedImage getMascotImage();

    int generationOfPokemon();

    void writeCheckValueToROM(int value);

    // code tweaks

    int miscTweaksAvailable();

    void applyMiscTweak(MiscTweak tweak);

    Long getSeedUsed();

    void setSeedUsed(Long seedUsed);

    void randomizeWarps(int warpRandoLevel, boolean extraDeadendRemoval, boolean inGymOrder);

    void randomizeTypeCharts(Settings.TypeChartMod mode);

    String getTypeInteractionsLog(Settings.TypeChartMod typeChartMod);

    void saveGenRestrictionsToRom(GenRestrictions currentRestrictions);

    void setCustomConfig(CustomConfig config);

}
