package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  Settings.java - encapsulates a configuration of settings used by the  --*/
/*--                  randomizer to determine how to randomize the          --*/
/*--                  target game.                                          --*/
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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("UnusedReturnValue")
/*
 *  TODO: Started reworking this into something more manageable but still needs some changes
 */
public class Settings {

    public static final int VERSION = 175;

    public static final int LENGTH_OF_SETTINGS_DATA = 41;

    private CustomNamesSet customNames;

    private String romName;
    private boolean updatedFromOldVersion = false;
    private GenRestrictions currentRestrictions;
    private int currentMiscTweaks;

    private boolean changeImpossibleEvolutions;
    private boolean makeEvolutionsEasier;
    private boolean raceMode;
    private boolean blockBrokenMoves;
    private boolean limitPokemon;

    public enum BaseStatisticsMod {
        UNCHANGED, SHUFFLE, RANDOM, RANDOMBST, RANDOMBSTPERC, EQUALIZE,
    }

    private BaseStatisticsMod baseStatisticsMod = BaseStatisticsMod.UNCHANGED;
    private boolean standardizeEXPCurves;
    private boolean baseStatsFollowEvolutions;
    private boolean updateBaseStats;
    private int baseStatRange = 0;
    private boolean dontRandomizeRatio;
    private boolean evosBuffStats;

    public enum AbilitiesMod {
        UNCHANGED, RANDOMIZE
    }

    private AbilitiesMod abilitiesMod = AbilitiesMod.UNCHANGED;
    private boolean allowWonderGuard = true;
    private boolean abilitiesFollowEvolutions;
    private boolean banTrappingAbilities;
    private boolean banNegativeAbilities;

    public enum StartersMod {
        UNCHANGED, CUSTOM, COMPLETELY_RANDOM, RANDOM_WITH_TWO_EVOLUTIONS, RANDOM_WITH_ONE_EVOLUTION, RANDOM_WITH_NO_EVOLUTIONS
    }

    private StartersMod startersMod = StartersMod.UNCHANGED;

    // index in the rom's list of pokemon
    // offset from the dropdown index from RandomizerGUI by 1
    private int[] customStarters = new int[3];
    private boolean randomizeStartersHeldItems;
    private boolean banBadRandomStarterHeldItems;
    private boolean banLegendaryStarters;
    private boolean onlyLegendaryStarters;

    public enum TypesMod {
        UNCHANGED, RANDOM_FOLLOW_EVOLUTIONS, COMPLETELY_RANDOM
    }

    private TypesMod typesMod = TypesMod.UNCHANGED;

    // Evolutions
    public enum EvolutionsMod {
        UNCHANGED, RANDOM
    }

    private EvolutionsMod evolutionsMod = EvolutionsMod.UNCHANGED;
    private boolean evosSimilarStrength;
    private boolean evosSameTyping;
    private boolean evosMaxThreeStages;
    private boolean evosForceChange;

    // Move data
    private boolean randomizeMovePowers;
    private boolean randomizeMoveAccuracies;
    private boolean randomizeMovePPs;
    private boolean randomizeMoveTypes;
    private boolean randomizeMoveCategory;
    private boolean updateMoves;
    private boolean updateMovesLegacy;

    public enum MovesetsMod {
        UNCHANGED, RANDOM_PREFER_SAME_TYPE, COMPLETELY_RANDOM, METRONOME_ONLY
    }

    private MovesetsMod movesetsMod = MovesetsMod.UNCHANGED;
    private boolean startWithFourMoves;
    private boolean reorderDamagingMoves;
    private boolean movesetsForceGoodDamaging;
    private int movesetsGoodDamagingPercent = 0;

    public enum TrainersMod {
        UNCHANGED, RANDOM, TYPE_THEMED, TYPE_MATCHED
    }

    private TrainersMod trainersMod = TrainersMod.UNCHANGED;
    private boolean rivalCarriesStarterThroughout;
    private boolean trainersUsePokemonOfSimilarStrength;
    private boolean trainersMatchTypingDistribution;
    private boolean trainersBlockLegendaries = true;
    private boolean trainersBlockEarlyWonderGuard = true;
    private boolean randomizeTrainerNames;
    private boolean randomizeTrainerClassNames;
    private boolean trainersForceFullyEvolved;
    private int trainersForceFullyEvolvedLevel = 30;
    private boolean trainersLevelModified;
    private int trainersLevelModifier = 0; // -50 ~ 50

    public enum WildPokemonMod {
        UNCHANGED, RANDOM, AREA_MAPPING, GLOBAL_MAPPING
    }

    public enum WildPokemonRestrictionMod {
        NONE, SIMILAR_STRENGTH, CATCH_EM_ALL, TYPE_THEME_AREAS
    }

    private WildPokemonMod wildPokemonMod = WildPokemonMod.UNCHANGED;
    private WildPokemonRestrictionMod wildPokemonRestrictionMod = WildPokemonRestrictionMod.NONE;
    private boolean useTimeBasedEncounters;
    private boolean blockWildLegendaries = true;
    private boolean useMinimumCatchRate;
    private int minimumCatchRateLevel = 1;
    private boolean randomizeWildPokemonHeldItems;
    private boolean banBadRandomWildPokemonHeldItems;
    private boolean condenseEncounterSlots;
    private boolean catchEmAllReasonableSlotsOnly;

    public enum StaticPokemonMod {
        UNCHANGED, RANDOM_MATCHING, COMPLETELY_RANDOM
    }

    private StaticPokemonMod staticPokemonMod = StaticPokemonMod.UNCHANGED;

    private boolean randomizeFrontier;

    private boolean fillBossTeams;

    public enum TMsMod {
        UNCHANGED, RANDOM
    }

    private TMsMod tmsMod = TMsMod.UNCHANGED;
    private boolean tmLevelUpMoveSanity;
    private boolean keepFieldMoveTMs;
    private boolean fullHMCompat;
    private boolean tmsForceGoodDamaging;
    private int tmsGoodDamagingPercent = 0;

    public enum TMsHMsCompatibilityMod {
        UNCHANGED, SAME_TYPE, FIFTY_PERCENT, FULL
    }

    private TMsHMsCompatibilityMod tmsHmsCompatibilityMod = TMsHMsCompatibilityMod.UNCHANGED;

    public enum MoveTutorMovesMod {
        UNCHANGED, RANDOM
    }

    private MoveTutorMovesMod moveTutorMovesMod = MoveTutorMovesMod.UNCHANGED;
    private boolean tutorLevelUpMoveSanity;
    private boolean keepFieldMoveTutors;
    private boolean tutorsForceGoodDamaging;
    private int tutorsGoodDamagingPercent = 0;

    public enum MoveTutorsCompatibilityMod {
        UNCHANGED, SAME_TYPE, FIFTY_PERCENT, FULL
    }

    private MoveTutorsCompatibilityMod moveTutorsCompatibilityMod = MoveTutorsCompatibilityMod.UNCHANGED;

    public enum InGameTradesMod {
        UNCHANGED, RANDOMIZE_GIVEN, RANDOMIZE_GIVEN_AND_REQUESTED
    }

    private InGameTradesMod inGameTradesMod = InGameTradesMod.UNCHANGED;
    private boolean randomizeInGameTradesNicknames;
    private boolean randomizeInGameTradesOTs;
    private boolean randomizeInGameTradesIVs;
    private boolean randomizeInGameTradesItems;

    public enum FieldItemsMod {
        UNCHANGED, SHUFFLE, RANDOM
    }

    private FieldItemsMod fieldItemsMod = FieldItemsMod.UNCHANGED;
    private boolean banBadRandomFieldItems;
    private boolean randomizeGivenItems;
    private boolean randomizePickupTables;
    private boolean randomizeBerryTrees;
    private boolean randomizeMarts;
    private boolean allMartsHaveBallAndRepel;
    private boolean randomItemPrices;

    private boolean isRandomWarps;
    private int warpRandoLevel;
    private boolean keepUselessDeadends;
    private boolean removeOrderedGymLogic;


    public enum TypeChartMod {
        UNCHANGED, SHUFFLE_ROW, SHUFFLE, RANDOM;
    }
    private TypeChartMod typeChartMod = TypeChartMod.UNCHANGED;

    // to and from strings etc
    public void write(FileOutputStream out) throws IOException {
        out.write(VERSION);
        byte[] settings = toString().getBytes(StandardCharsets.UTF_8);
        out.write(settings.length);
        out.write(settings);
    }

    public static Settings read(String settings) throws IOException, UnsupportedOperationException {
        return fromString(settings);
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    public static Settings fromString(String settingsString) throws UnsupportedEncodingException {
        return new Gson().fromJson(settingsString, Settings.class);
    }

    public static class TweakForROMFeedback {
        private boolean changedStarter;
        private boolean removedCodeTweaks;

        public boolean isChangedStarter() {
            return changedStarter;
        }

        public TweakForROMFeedback setChangedStarter(boolean changedStarter) {
            this.changedStarter = changedStarter;
            return this;
        }

        public boolean isRemovedCodeTweaks() {
            return removedCodeTweaks;
        }

        public TweakForROMFeedback setRemovedCodeTweaks(boolean removedCodeTweaks) {
            this.removedCodeTweaks = removedCodeTweaks;
            return this;
        }
    }

    public TweakForROMFeedback tweakForRom(RomHandler rh) {

        TweakForROMFeedback feedback = new TweakForROMFeedback();

        // starters
        List<Pokemon> romPokemon = rh.getPokemon();
        List<Pokemon> romStarters = rh.getStarters();
        for (int starter = 0; starter < 3; starter++) {
            if (this.customStarters[starter] < 0 || this.customStarters[starter] >= romPokemon.size()) {
                // invalid starter for this game
                feedback.setChangedStarter(true);
                if (starter >= romStarters.size()) {
                    this.customStarters[starter] = 1;
                } else {
                    this.customStarters[starter] = romPokemon.indexOf(romStarters.get(starter));
                }
            }
        }

        // gen restrictions
        if (this.currentRestrictions != null) {
            this.currentRestrictions.limitToGen(rh.generationOfPokemon());
        }

        // misc tweaks
        int oldMiscTweaks = this.currentMiscTweaks;
        this.currentMiscTweaks &= rh.miscTweaksAvailable();

        if (oldMiscTweaks != this.currentMiscTweaks) {
            feedback.setRemovedCodeTweaks(true);
        }

        if (rh.abilitiesPerPokemon() == 0) {
            this.setAbilitiesMod(AbilitiesMod.UNCHANGED);
            this.setAllowWonderGuard(false);
        }


        if (!rh.supportsFourStartingMoves()) {
            this.setStartWithFourMoves(false);
        }

        if (!rh.hasTimeBasedEncounters()) {
            this.setUseTimeBasedEncounters(false);
        }

        if (!rh.canChangeStaticPokemon()) {
            this.setStaticPokemonMod(StaticPokemonMod.UNCHANGED);
        }

        if (!rh.hasFrontier()) {
            this.setRandomizeFrontier(false);
        }

        if (!rh.hasMoveTutors()) {
            this.setMoveTutorMovesMod(MoveTutorMovesMod.UNCHANGED);
            this.setMoveTutorsCompatibilityMod(MoveTutorsCompatibilityMod.UNCHANGED);
            this.setTutorLevelUpMoveSanity(false);
            this.setKeepFieldMoveTutors(false);
        }

        if (!rh.hasPhysicalSpecialSplit()) {
            this.setRandomizeMoveCategory(false);
        }

        // done
        return feedback;
    }

    // getters and setters

    public CustomNamesSet getCustomNames() {
        return customNames;
    }

    public Settings setCustomNames(CustomNamesSet customNames) {
        this.customNames = customNames;
        return this;
    }

    public String getRomName() {
        return romName;
    }

    public Settings setRomName(String romName) {
        this.romName = romName;
        return this;
    }

    public boolean isUpdatedFromOldVersion() {
        return updatedFromOldVersion;
    }

    public Settings setUpdatedFromOldVersion(boolean updatedFromOldVersion) {
        this.updatedFromOldVersion = updatedFromOldVersion;
        return this;
    }

    public GenRestrictions getCurrentRestrictions() {
        return currentRestrictions;
    }

    public Settings setCurrentRestrictions(GenRestrictions currentRestrictions) {
        this.currentRestrictions = currentRestrictions;
        return this;
    }

    public int getCurrentMiscTweaks() {
        return currentMiscTweaks;
    }

    public Settings setCurrentMiscTweaks(int currentMiscTweaks) {
        this.currentMiscTweaks = currentMiscTweaks;
        return this;
    }

    public boolean isUpdateMoves() {
        return updateMoves;
    }

    public Settings setUpdateMoves(boolean updateMoves) {
        this.updateMoves = updateMoves;
        return this;
    }

    public boolean isUpdateMovesLegacy() {
        return updateMovesLegacy;
    }

    public Settings setUpdateMovesLegacy(boolean updateMovesLegacy) {
        this.updateMovesLegacy = updateMovesLegacy;
        return this;
    }

    public boolean isChangeImpossibleEvolutions() {
        return changeImpossibleEvolutions;
    }

    public Settings setChangeImpossibleEvolutions(boolean changeImpossibleEvolutions) {
        this.changeImpossibleEvolutions = changeImpossibleEvolutions;
        return this;
    }

    public boolean isMakeEvolutionsEasier() {
        return makeEvolutionsEasier;
    }

    public Settings setMakeEvolutionsEasier(boolean makeEvolutionsEasier) {
        this.makeEvolutionsEasier = makeEvolutionsEasier;
        return this;
    }

    public boolean isRaceMode() {
        return raceMode;
    }

    public Settings setRaceMode(boolean raceMode) {
        this.raceMode = raceMode;
        return this;
    }

    public boolean doBlockBrokenMoves() {
        return blockBrokenMoves;
    }

    public Settings setBlockBrokenMoves(boolean blockBrokenMoves) {
        this.blockBrokenMoves = blockBrokenMoves;
        return this;
    }

    public boolean isLimitPokemon() {
        return limitPokemon;
    }

    public Settings setLimitPokemon(boolean limitPokemon) {
        this.limitPokemon = limitPokemon;
        return this;
    }

    public BaseStatisticsMod getBaseStatisticsMod() {
        return baseStatisticsMod;
    }

    public Settings setBaseStatisticsMod(BaseStatisticsMod baseStatisticsMod) {
        this.baseStatisticsMod = baseStatisticsMod;
        return this;
    }

    public Settings setBaseStatisticsMod(boolean... bools) {
        return setBaseStatisticsMod(getEnum(BaseStatisticsMod.class, bools));
    }

    public boolean isBaseStatsFollowEvolutions() {
        return baseStatsFollowEvolutions;
    }

    public Settings setBaseStatsFollowEvolutions(boolean baseStatsFollowEvolutions) {
        this.baseStatsFollowEvolutions = baseStatsFollowEvolutions;
        return this;
    }

    public boolean isStandardizeEXPCurves() {
        return standardizeEXPCurves;
    }

    public Settings setStandardizeEXPCurves(boolean standardizeEXPCurves) {
        this.standardizeEXPCurves = standardizeEXPCurves;
        return this;
    }

    public boolean isUpdateBaseStats() {
        return updateBaseStats;
    }

    public Settings setUpdateBaseStats(boolean updateBaseStats) {
        this.updateBaseStats = updateBaseStats;
        return this;
    }
    
    public int getBaseStatRange() {
    	return baseStatRange;
    }
    
    public Settings setBaseStatRange(int baseStatRange) {
    	this.baseStatRange = baseStatRange;
    	return this;
    }
    
    public boolean isDontRandomizeRatio() {
    	return dontRandomizeRatio;
    }
    
    public Settings setDontRandomizeRatio(boolean DontRandomizeRatio) {
    	this.dontRandomizeRatio = DontRandomizeRatio;
    	return this;
    }

    public boolean isEvosBuffStats() {
    	return evosBuffStats;
    }
    
    public Settings setEvosBuffStats(boolean evosBuffStats) {
    	this.evosBuffStats = evosBuffStats;
    	return this;
    }
    
    public AbilitiesMod getAbilitiesMod() {
        return abilitiesMod;
    }

    public Settings setAbilitiesMod(AbilitiesMod abilitiesMod) {
        this.abilitiesMod = abilitiesMod;
        return this;
    }

    public Settings setAbilitiesMod(boolean... bools) {
        return setAbilitiesMod(getEnum(AbilitiesMod.class, bools));
    }

    public boolean isAllowWonderGuard() {
        return allowWonderGuard;
    }

    public Settings setAllowWonderGuard(boolean allowWonderGuard) {
        this.allowWonderGuard = allowWonderGuard;
        return this;
    }

    public boolean isAbilitiesFollowEvolutions() {
        return abilitiesFollowEvolutions;
    }

    public Settings setAbilitiesFollowEvolutions(boolean abilitiesFollowEvolutions) {
        this.abilitiesFollowEvolutions = abilitiesFollowEvolutions;
        return this;
    }

    public boolean isBanTrappingAbilities() {
        return banTrappingAbilities;
    }

    public Settings setBanTrappingAbilities(boolean banTrappingAbilities) {
        this.banTrappingAbilities = banTrappingAbilities;
        return this;
    }

    public boolean isBanNegativeAbilities() {
        return banNegativeAbilities;
    }

    public Settings setBanNegativeAbilities(boolean banNegativeAbilities) {
        this.banNegativeAbilities = banNegativeAbilities;
        return this;
    }

    public StartersMod getStartersMod() {
        return startersMod;
    }

    public Settings setStartersMod(StartersMod startersMod) {
        this.startersMod = startersMod;
        return this;
    }

    public Settings setStartersMod(boolean... bools) {
        return setStartersMod(getEnum(StartersMod.class, bools));
    }

    public int[] getCustomStarters() {
        return customStarters;
    }

    public Settings setCustomStarters(int[] customStarters) {
        this.customStarters = customStarters;
        return this;
    }

    public boolean isRandomizeStartersHeldItems() {
        return randomizeStartersHeldItems;
    }

    public Settings setRandomizeStartersHeldItems(boolean randomizeStartersHeldItems) {
        this.randomizeStartersHeldItems = randomizeStartersHeldItems;
        return this;
    }

    public boolean isBanBadRandomStarterHeldItems() {
        return banBadRandomStarterHeldItems;
    }

    public Settings setBanBadRandomStarterHeldItems(boolean banBadRandomStarterHeldItems) {
        this.banBadRandomStarterHeldItems = banBadRandomStarterHeldItems;
        return this;
    }

    public boolean isBanLegendaryStarters() {
    	return banLegendaryStarters;
    }
    
    public Settings setBanLegendaryStarters(boolean banLegendaryStarters) {
    	this.banLegendaryStarters = banLegendaryStarters;
    	return this;
    }
    
    public boolean isOnlyLegendaryStarters() {
    	return onlyLegendaryStarters;
    }
    
    public Settings setOnlyLegendaryStarters(boolean onlyLegendaryStarters) {
    	this.onlyLegendaryStarters = onlyLegendaryStarters;
    	return this;
    }
    
    public TypesMod getTypesMod() {
        return typesMod;
    }

    public Settings setTypesMod(TypesMod typesMod) {
        this.typesMod = typesMod;
        return this;
    }

    public Settings setTypesMod(boolean... bools) {
        return setTypesMod(getEnum(TypesMod.class, bools));
    }

    public EvolutionsMod getEvolutionsMod() {
        return evolutionsMod;
    }

    public Settings setEvolutionsMod(EvolutionsMod evolutionsMod) {
        this.evolutionsMod = evolutionsMod;
        return this;
    }

    public Settings setEvolutionsMod(boolean... bools) {
        return setEvolutionsMod(getEnum(EvolutionsMod.class, bools));
    }

    public boolean isEvosSimilarStrength() {
        return evosSimilarStrength;
    }

    public Settings setEvosSimilarStrength(boolean evosSimilarStrength) {
        this.evosSimilarStrength = evosSimilarStrength;
        return this;
    }

    public boolean isEvosSameTyping() {
        return evosSameTyping;
    }

    public Settings setEvosSameTyping(boolean evosSameTyping) {
        this.evosSameTyping = evosSameTyping;
        return this;
    }

    public boolean isEvosMaxThreeStages() {
        return evosMaxThreeStages;
    }

    public Settings setEvosMaxThreeStages(boolean evosMaxThreeStages) {
        this.evosMaxThreeStages = evosMaxThreeStages;
        return this;
    }

    public boolean isEvosForceChange() {
        return evosForceChange;
    }

    public Settings setEvosForceChange(boolean evosForceChange) {
        this.evosForceChange = evosForceChange;
        return this;
    }

    public boolean isRandomizeMovePowers() {
        return randomizeMovePowers;
    }

    public Settings setRandomizeMovePowers(boolean randomizeMovePowers) {
        this.randomizeMovePowers = randomizeMovePowers;
        return this;
    }

    public boolean isRandomizeMoveAccuracies() {
        return randomizeMoveAccuracies;
    }

    public Settings setRandomizeMoveAccuracies(boolean randomizeMoveAccuracies) {
        this.randomizeMoveAccuracies = randomizeMoveAccuracies;
        return this;
    }

    public boolean isRandomizeMovePPs() {
        return randomizeMovePPs;
    }

    public Settings setRandomizeMovePPs(boolean randomizeMovePPs) {
        this.randomizeMovePPs = randomizeMovePPs;
        return this;
    }

    public boolean isRandomizeMoveTypes() {
        return randomizeMoveTypes;
    }

    public Settings setRandomizeMoveTypes(boolean randomizeMoveTypes) {
        this.randomizeMoveTypes = randomizeMoveTypes;
        return this;
    }

    public boolean isRandomizeMoveCategory() {
        return randomizeMoveCategory;
    }

    public Settings setRandomizeMoveCategory(boolean randomizeMoveCategory) {
        this.randomizeMoveCategory = randomizeMoveCategory;
        return this;
    }

    public MovesetsMod getMovesetsMod() {
        return movesetsMod;
    }

    public Settings setMovesetsMod(MovesetsMod movesetsMod) {
        this.movesetsMod = movesetsMod;
        return this;
    }

    public Settings setMovesetsMod(boolean... bools) {
        return setMovesetsMod(getEnum(MovesetsMod.class, bools));
    }

    public boolean isStartWithFourMoves() {
        return startWithFourMoves;
    }

    public Settings setStartWithFourMoves(boolean startWithFourMoves) {
        this.startWithFourMoves = startWithFourMoves;
        return this;
    }

    public boolean isReorderDamagingMoves() {
        return reorderDamagingMoves;
    }

    public Settings setReorderDamagingMoves(boolean reorderDamagingMoves) {
        this.reorderDamagingMoves = reorderDamagingMoves;
        return this;
    }

    public boolean isMovesetsForceGoodDamaging() {
        return movesetsForceGoodDamaging;
    }

    public Settings setMovesetsForceGoodDamaging(boolean movesetsForceGoodDamaging) {
        this.movesetsForceGoodDamaging = movesetsForceGoodDamaging;
        return this;
    }

    public int getMovesetsGoodDamagingPercent() {
        return movesetsGoodDamagingPercent;
    }

    public Settings setMovesetsGoodDamagingPercent(int movesetsGoodDamagingPercent) {
        this.movesetsGoodDamagingPercent = movesetsGoodDamagingPercent;
        return this;
    }

    public TrainersMod getTrainersMod() {
        return trainersMod;
    }

    public Settings setTrainersMod(TrainersMod trainersMod) {
        this.trainersMod = trainersMod;
        return this;
    }

    public Settings setTrainersMod(boolean... bools) {
        return setTrainersMod(getEnum(TrainersMod.class, bools));
    }

    public boolean isRivalCarriesStarterThroughout() {
        return rivalCarriesStarterThroughout;
    }

    public Settings setRivalCarriesStarterThroughout(boolean rivalCarriesStarterThroughout) {
        this.rivalCarriesStarterThroughout = rivalCarriesStarterThroughout;
        return this;
    }

    public boolean isTrainersUsePokemonOfSimilarStrength() {
        return trainersUsePokemonOfSimilarStrength;
    }

    public Settings setTrainersUsePokemonOfSimilarStrength(boolean trainersUsePokemonOfSimilarStrength) {
        this.trainersUsePokemonOfSimilarStrength = trainersUsePokemonOfSimilarStrength;
        return this;
    }

    public boolean isTrainersMatchTypingDistribution() {
        return trainersMatchTypingDistribution;
    }

    public Settings setTrainersMatchTypingDistribution(boolean trainersMatchTypingDistribution) {
        this.trainersMatchTypingDistribution = trainersMatchTypingDistribution;
        return this;
    }

    public boolean isTrainersBlockLegendaries() {
        return trainersBlockLegendaries;
    }

    public Settings setTrainersBlockLegendaries(boolean trainersBlockLegendaries) {
        this.trainersBlockLegendaries = trainersBlockLegendaries;
        return this;
    }

    public boolean isTrainersBlockEarlyWonderGuard() {
        return trainersBlockEarlyWonderGuard;
    }

    public Settings setTrainersBlockEarlyWonderGuard(boolean trainersBlockEarlyWonderGuard) {
        this.trainersBlockEarlyWonderGuard = trainersBlockEarlyWonderGuard;
        return this;
    }

    public boolean isRandomizeTrainerNames() {
        return randomizeTrainerNames;
    }

    public Settings setRandomizeTrainerNames(boolean randomizeTrainerNames) {
        this.randomizeTrainerNames = randomizeTrainerNames;
        return this;
    }

    public boolean isRandomizeTrainerClassNames() {
        return randomizeTrainerClassNames;
    }

    public Settings setRandomizeTrainerClassNames(boolean randomizeTrainerClassNames) {
        this.randomizeTrainerClassNames = randomizeTrainerClassNames;
        return this;
    }

    public boolean isTrainersForceFullyEvolved() {
        return trainersForceFullyEvolved;
    }

    public Settings setTrainersForceFullyEvolved(boolean trainersForceFullyEvolved) {
        this.trainersForceFullyEvolved = trainersForceFullyEvolved;
        return this;
    }

    public int getTrainersForceFullyEvolvedLevel() {
        return trainersForceFullyEvolvedLevel;
    }

    public Settings setTrainersForceFullyEvolvedLevel(int trainersForceFullyEvolvedLevel) {
        this.trainersForceFullyEvolvedLevel = trainersForceFullyEvolvedLevel;
        return this;
    }

    public boolean isTrainersLevelModified() {
        return trainersLevelModified;
    }

    public Settings setTrainersLevelModified(boolean trainersLevelModified) {
        this.trainersLevelModified = trainersLevelModified;
        return this;
    }

    public int getTrainersLevelModifier() {
        return trainersLevelModifier;
    }

    public Settings setTrainersLevelModifier(int trainersLevelModifier) {
        this.trainersLevelModifier = trainersLevelModifier;
        return this;
    }

    public WildPokemonMod getWildPokemonMod() {
        return wildPokemonMod;
    }

    public Settings setWildPokemonMod(WildPokemonMod wildPokemonMod) {
        this.wildPokemonMod = wildPokemonMod;
        return this;
    }

    public Settings setWildPokemonMod(boolean... bools) {
        return setWildPokemonMod(getEnum(WildPokemonMod.class, bools));
    }

    public WildPokemonRestrictionMod getWildPokemonRestrictionMod() {
        return wildPokemonRestrictionMod;
    }

    public Settings setWildPokemonRestrictionMod(WildPokemonRestrictionMod wildPokemonRestrictionMod) {
        this.wildPokemonRestrictionMod = wildPokemonRestrictionMod;
        return this;
    }

    public Settings setWildPokemonRestrictionMod(boolean... bools) {
        return setWildPokemonRestrictionMod(getEnum(WildPokemonRestrictionMod.class, bools));
    }

    public boolean isUseTimeBasedEncounters() {
        return useTimeBasedEncounters;
    }

    public Settings setUseTimeBasedEncounters(boolean useTimeBasedEncounters) {
        this.useTimeBasedEncounters = useTimeBasedEncounters;
        return this;
    }

    public boolean isBlockWildLegendaries() {
        return blockWildLegendaries;
    }

    public Settings setBlockWildLegendaries(boolean blockWildLegendaries) {
        this.blockWildLegendaries = blockWildLegendaries;
        return this;
    }

    public boolean isUseMinimumCatchRate() {
        return useMinimumCatchRate;
    }

    public Settings setUseMinimumCatchRate(boolean useMinimumCatchRate) {
        this.useMinimumCatchRate = useMinimumCatchRate;
        return this;
    }

    public int getMinimumCatchRateLevel() {
        return minimumCatchRateLevel;
    }

    public Settings setMinimumCatchRateLevel(int minimumCatchRateLevel) {
        this.minimumCatchRateLevel = minimumCatchRateLevel;
        return this;
    }

    public boolean isRandomizeWildPokemonHeldItems() {
        return randomizeWildPokemonHeldItems;
    }

    public Settings setRandomizeWildPokemonHeldItems(boolean randomizeWildPokemonHeldItems) {
        this.randomizeWildPokemonHeldItems = randomizeWildPokemonHeldItems;
        return this;
    }

    public boolean isBanBadRandomWildPokemonHeldItems() {
        return banBadRandomWildPokemonHeldItems;
    }

    public Settings setBanBadRandomWildPokemonHeldItems(boolean banBadRandomWildPokemonHeldItems) {
        this.banBadRandomWildPokemonHeldItems = banBadRandomWildPokemonHeldItems;
        return this;
    }

    public StaticPokemonMod getStaticPokemonMod() {
        return staticPokemonMod;
    }

    public Settings setStaticPokemonMod(StaticPokemonMod staticPokemonMod) {
        this.staticPokemonMod = staticPokemonMod;
        return this;
    }

    public Settings setStaticPokemonMod(boolean... bools) {
        return setStaticPokemonMod(getEnum(StaticPokemonMod.class, bools));
    }

    public Settings setRandomizeFrontier(boolean value) {
        this.randomizeFrontier = value;
        return this;
    }

    public boolean isRandomizeFrontier() {
        return randomizeFrontier;
    }

    public boolean isFillBossTeams() {
        return fillBossTeams;
    }

    public Settings setFillBossTeams(boolean value) {
        this.fillBossTeams = value;
        return this;
    }

    public TMsMod getTmsMod() {
        return tmsMod;
    }

    public Settings setTmsMod(TMsMod tmsMod) {
        this.tmsMod = tmsMod;
        return this;
    }

    public Settings setTmsMod(boolean... bools) {
        return setTmsMod(getEnum(TMsMod.class, bools));
    }

    public boolean isTmLevelUpMoveSanity() {
        return tmLevelUpMoveSanity;
    }

    public Settings setTmLevelUpMoveSanity(boolean tmLevelUpMoveSanity) {
        this.tmLevelUpMoveSanity = tmLevelUpMoveSanity;
        return this;
    }

    public boolean isKeepFieldMoveTMs() {
        return keepFieldMoveTMs;
    }

    public Settings setKeepFieldMoveTMs(boolean keepFieldMoveTMs) {
        this.keepFieldMoveTMs = keepFieldMoveTMs;
        return this;
    }

    public boolean isFullHMCompat() {
        return fullHMCompat;
    }

    public Settings setFullHMCompat(boolean fullHMCompat) {
        this.fullHMCompat = fullHMCompat;
        return this;
    }

    public boolean isTmsForceGoodDamaging() {
        return tmsForceGoodDamaging;
    }

    public Settings setTmsForceGoodDamaging(boolean tmsForceGoodDamaging) {
        this.tmsForceGoodDamaging = tmsForceGoodDamaging;
        return this;
    }

    public int getTmsGoodDamagingPercent() {
        return tmsGoodDamagingPercent;
    }

    public Settings setTmsGoodDamagingPercent(int tmsGoodDamagingPercent) {
        this.tmsGoodDamagingPercent = tmsGoodDamagingPercent;
        return this;
    }

    public TMsHMsCompatibilityMod getTmsHmsCompatibilityMod() {
        return tmsHmsCompatibilityMod;
    }

    public Settings setTmsHmsCompatibilityMod(TMsHMsCompatibilityMod tmsHmsCompatibilityMod) {
        this.tmsHmsCompatibilityMod = tmsHmsCompatibilityMod;
        return this;
    }

    public Settings setTmsHmsCompatibilityMod(boolean... bools) {
        return setTmsHmsCompatibilityMod(getEnum(TMsHMsCompatibilityMod.class, bools));
    }

    public MoveTutorMovesMod getMoveTutorMovesMod() {
        return moveTutorMovesMod;
    }

    public Settings setMoveTutorMovesMod(MoveTutorMovesMod moveTutorMovesMod) {
        this.moveTutorMovesMod = moveTutorMovesMod;
        return this;
    }

    public Settings setMoveTutorMovesMod(boolean... bools) {
        return setMoveTutorMovesMod(getEnum(MoveTutorMovesMod.class, bools));
    }

    public boolean isTutorLevelUpMoveSanity() {
        return tutorLevelUpMoveSanity;
    }

    public Settings setTutorLevelUpMoveSanity(boolean tutorLevelUpMoveSanity) {
        this.tutorLevelUpMoveSanity = tutorLevelUpMoveSanity;
        return this;
    }

    public boolean isKeepFieldMoveTutors() {
        return keepFieldMoveTutors;
    }

    public Settings setKeepFieldMoveTutors(boolean keepFieldMoveTutors) {
        this.keepFieldMoveTutors = keepFieldMoveTutors;
        return this;
    }

    public boolean isTutorsForceGoodDamaging() {
        return tutorsForceGoodDamaging;
    }

    public Settings setTutorsForceGoodDamaging(boolean tutorsForceGoodDamaging) {
        this.tutorsForceGoodDamaging = tutorsForceGoodDamaging;
        return this;
    }

    public int getTutorsGoodDamagingPercent() {
        return tutorsGoodDamagingPercent;
    }

    public Settings setTutorsGoodDamagingPercent(int tutorsGoodDamagingPercent) {
        this.tutorsGoodDamagingPercent = tutorsGoodDamagingPercent;
        return this;
    }

    public MoveTutorsCompatibilityMod getMoveTutorsCompatibilityMod() {
        return moveTutorsCompatibilityMod;
    }

    public Settings setMoveTutorsCompatibilityMod(MoveTutorsCompatibilityMod moveTutorsCompatibilityMod) {
        this.moveTutorsCompatibilityMod = moveTutorsCompatibilityMod;
        return this;
    }

    public Settings setMoveTutorsCompatibilityMod(boolean... bools) {
        return setMoveTutorsCompatibilityMod(getEnum(MoveTutorsCompatibilityMod.class, bools));
    }

    public InGameTradesMod getInGameTradesMod() {
        return inGameTradesMod;
    }

    public Settings setInGameTradesMod(InGameTradesMod inGameTradesMod) {
        this.inGameTradesMod = inGameTradesMod;
        return this;
    }

    public Settings setInGameTradesMod(boolean... bools) {
        return setInGameTradesMod(getEnum(InGameTradesMod.class, bools));
    }

    public boolean isRandomizeInGameTradesNicknames() {
        return randomizeInGameTradesNicknames;
    }

    public Settings setRandomizeInGameTradesNicknames(boolean randomizeInGameTradesNicknames) {
        this.randomizeInGameTradesNicknames = randomizeInGameTradesNicknames;
        return this;
    }

    public boolean isRandomizeInGameTradesOTs() {
        return randomizeInGameTradesOTs;
    }

    public Settings setRandomizeInGameTradesOTs(boolean randomizeInGameTradesOTs) {
        this.randomizeInGameTradesOTs = randomizeInGameTradesOTs;
        return this;
    }

    public boolean isRandomizeInGameTradesIVs() {
        return randomizeInGameTradesIVs;
    }

    public Settings setRandomizeInGameTradesIVs(boolean randomizeInGameTradesIVs) {
        this.randomizeInGameTradesIVs = randomizeInGameTradesIVs;
        return this;
    }

    public boolean isRandomizeInGameTradesItems() {
        return randomizeInGameTradesItems;
    }

    public Settings setRandomizeInGameTradesItems(boolean randomizeInGameTradesItems) {
        this.randomizeInGameTradesItems = randomizeInGameTradesItems;
        return this;
    }

    public FieldItemsMod getFieldItemsMod() {
        return fieldItemsMod;
    }

    public Settings setFieldItemsMod(FieldItemsMod fieldItemsMod) {
        this.fieldItemsMod = fieldItemsMod;
        return this;
    }

    public Settings setTypeChartMod(TypeChartMod typeChartMod) {
        this.typeChartMod = typeChartMod;
        return this;
    }

    public Settings setTypeChartMod(boolean... bools) {
        return setTypeChartMod(getEnum(TypeChartMod.class, bools));
    }

    public Settings setFieldItemsMod(boolean... bools) {
        return setFieldItemsMod(getEnum(FieldItemsMod.class, bools));
    }

    public TypeChartMod getTypeChartMod() {
        return typeChartMod;
    }

    public boolean isBanBadRandomFieldItems() {
        return banBadRandomFieldItems;
    }

    public Settings setBanBadRandomFieldItems(boolean banBadRandomFieldItems) {
        this.banBadRandomFieldItems = banBadRandomFieldItems;
        return this;
    }

    public boolean isRandomizeGivenItems() {
        return randomizeGivenItems;
    }

    public boolean isRandomizePickupTables() {
        return randomizePickupTables;
    }

    public boolean isRandomizeBerryTrees() {
        return randomizeBerryTrees;
    }

    public boolean isRandomizeMarts() {
        return randomizeMarts;
    }

    public Settings setRandomizeGivenItems(boolean selected) {
        randomizeGivenItems = selected;
        return this;
    }

    public boolean isAllMartsHaveBallAndRepel() {
        return allMartsHaveBallAndRepel;
    }

    public Settings setAllMartsHaveBallAndRepel(boolean selected) {
        allMartsHaveBallAndRepel = selected;
        return this;
    }

    public boolean isRandomItemPrices() {
        return randomItemPrices;
    }

    public Settings setRandomItemPrices(boolean selected) {
        randomItemPrices = selected;
        return this;
    }

    public Settings setRandomizePickupTables(boolean selected) {
        randomizePickupTables = selected;
        return this;
    }

    public Settings setRandomizeBerryTrees(boolean selected) {
        randomizeBerryTrees = selected;
        return this;
    }

    public Settings setRandomizeMarts(boolean selected) {
        randomizeMarts = selected;
        return this;
    }

    public boolean isCondenseEncounterSlots() {
        return condenseEncounterSlots;
    }

    public Settings setCondenseEncounterSlots(boolean condenseEncounterSlots) {
        this.condenseEncounterSlots = condenseEncounterSlots;
        return this;
    }

    public boolean isCatchEmAllReasonableSlotsOnly() {
        return catchEmAllReasonableSlotsOnly;
    }

    public Settings setCatchEmAllReasonableSlotsOnly(boolean catchEmAllReasonableSlotsOnly) {
        this.catchEmAllReasonableSlotsOnly = catchEmAllReasonableSlotsOnly;
        return this;
    }

    public void setRandomWarps(boolean randomWarps) {
        isRandomWarps = randomWarps;
    }

    public void setWarpRandoLevel(int warpRandoLevel) {
        this.warpRandoLevel = warpRandoLevel;
    }

    public void setKeepUselessDeadends(boolean keepUselessDeadends) {
        this.keepUselessDeadends = keepUselessDeadends;
    }

    public void setRemoveOrderedGymLogic(boolean removeOrderedGymLogic) {
        this.removeOrderedGymLogic = removeOrderedGymLogic;
    }

    public boolean isRandomWarps() {
        return isRandomWarps;
    }

    public int getWarpRandoLevel() {
        return warpRandoLevel;
    }

    public boolean isKeepUselessDeadends() {
        return keepUselessDeadends;
    }

    public boolean isRemoveGymOrderLogic() {
        return removeOrderedGymLogic;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E getEnum(Class<E> clazz, boolean... bools) {
        int index = getSetEnum(clazz.getSimpleName(), bools);
        try {
            return ((E[]) clazz.getMethod("values").invoke(null))[index];
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Unable to parse enum of type %s", clazz.getSimpleName()),
                    e);
        }
    }

    private static int getSetEnum(String type, boolean... bools) {
        int index = -1;
        for (int i = 0; i < bools.length; i++) {
            if (bools[i]) {
                if (index >= 0) {
                    throw new IllegalStateException(String.format("Only one value for %s may be chosen!", type));
                }
                index = i;
            }
        }
        // We have to return something, so return the default
        return Math.max(index, 0);
    }

}
