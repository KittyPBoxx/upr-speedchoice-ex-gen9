package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  Randomizer.java - Can randomize a file based on settings.             --*/
/*--                    Output varies by seed.                              --*/
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.dabomstew.pkrandom.constants.EmeraldEXConstants;
import com.dabomstew.pkrandom.pokemon.Encounter;
import com.dabomstew.pkrandom.pokemon.EncounterSet;
import com.dabomstew.pkrandom.pokemon.FieldTM;
import com.dabomstew.pkrandom.pokemon.IngameTrade;
import com.dabomstew.pkrandom.pokemon.ItemLocation;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.MoveLearnt;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.TrainerPokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

// Can randomize a file based on settings. Output varies by seed.
public class Randomizer {

    private static final String NEWLINE = System.lineSeparator();

    private final Settings settings;
    private final RomHandler romHandler;

    public Randomizer(Settings settings, RomHandler romHandler) {
        this.settings = settings;
        this.romHandler = romHandler;
    }

    public int randomize(final String filename, final PrintStream log, long seed, Consumer<String> progressCallback) {
        final long startTime = System.currentTimeMillis();
        RandomSource.seed(seed);

        int checkValue = 0;

        // limit pokemon?
        if (settings.isLimitPokemon()) {
            romHandler.setPokemonPool(settings.getCurrentRestrictions());
            romHandler.removeEvosForPokemonPool();
        } else {
            romHandler.setPokemonPool(null);
        }

        if (settings.isRandomizeMovePowers()) {
            romHandler.randomizeMovePowers();
        }

        if (settings.isRandomizeMoveAccuracies()) {
            romHandler.randomizeMoveAccuracies();
        }

        if (settings.isRandomizeMovePPs()) {
            romHandler.randomizeMovePPs();
        }

        if (settings.isRandomizeMoveTypes()) {
            romHandler.randomizeMoveTypes();
        }

        if (settings.isRandomizeMoveCategory() && romHandler.hasPhysicalSpecialSplit()) {
            romHandler.randomizeMoveCategory();
        }

        List<Move> moves = romHandler.getMoves();

        // Misc Tweaks?
        int currentMiscTweaks = settings.getCurrentMiscTweaks();
        if (romHandler.miscTweaksAvailable() != 0) {
            int codeTweaksAvailable = romHandler.miscTweaksAvailable();
            List<MiscTweak> tweaksToApply = new ArrayList<>();

            for (MiscTweak mt : MiscTweak.allTweaks) {
                if ((codeTweaksAvailable & mt.getValue()) > 0 && (currentMiscTweaks & mt.getValue()) > 0) {
                    tweaksToApply.add(mt);
                }
            }

            // Sort so priority is respected in tweak ordering.
            Collections.sort(tweaksToApply);

            // Now apply in order.
            for (MiscTweak mt : tweaksToApply) {
                romHandler.applyMiscTweak(mt);
            }
        }

        // Base stats changing
        switch (settings.getBaseStatisticsMod()) {
        case SHUFFLE:
            romHandler.shufflePokemonStats(settings.isBaseStatsFollowEvolutions());
            break;
        case RANDOM:
            romHandler.randomizePokemonStats(settings.isBaseStatsFollowEvolutions());
            break;
        case RANDOMBST:
            romHandler.randomizePokemonBaseStats(settings.isBaseStatsFollowEvolutions(),
                    settings.isDontRandomizeRatio(), settings.isEvosBuffStats());
            break;
        case RANDOMBSTPERC:
            romHandler.randomizePokemonBaseStatsPerc(settings.isBaseStatsFollowEvolutions(),
                    settings.getBaseStatRange(), settings.isDontRandomizeRatio());
            break;
        case EQUALIZE:
            romHandler.equalizePokemonStats(settings.isBaseStatsFollowEvolutions(), settings.isDontRandomizeRatio());
            break;
        default:
            break;
        }

        if (settings.isStandardizeEXPCurves()) {
            romHandler.standardizeEXPCurves();
        }

        // Abilities? (new 1.0.2)
        if (romHandler.abilitiesPerPokemon() > 0 && settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE) {
            romHandler.randomizeAbilities(settings.isAbilitiesFollowEvolutions(), settings.isAllowWonderGuard(),
                    settings.isBanTrappingAbilities(), settings.isBanNegativeAbilities());
        }

        // Pokemon Types
        switch (settings.getTypesMod()) {
        case RANDOM_FOLLOW_EVOLUTIONS:
            romHandler.randomizePokemonTypes(true);
            break;
        case COMPLETELY_RANDOM:
            romHandler.randomizePokemonTypes(false);
            break;
        default:
            break;
        }

        // Wild Held Items?
        if (settings.isRandomizeWildPokemonHeldItems()) {
            romHandler.randomizeWildHeldItems(settings.isBanBadRandomWildPokemonHeldItems());
        }

        maybeLogBaseStatAndTypeChanges(log, romHandler);
        for (Pokemon pkmn : romHandler.getPokemon()) {
            if (pkmn != null) {
                checkValue = addToCV(checkValue, pkmn.getHp(), pkmn.getAttack(), pkmn.getDefense(), pkmn.getSpeed(), pkmn.getSpatk(), pkmn.getSpdef(),
                        pkmn.getAbility1(), pkmn.getAbility2(), pkmn.getAbility3());
            }
        }

        // Random Evos
        // Applied after type to pick new evos based on new types.
        if (settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM) {
            romHandler.randomizeEvolutions(settings.isEvosSimilarStrength(), settings.isEvosSameTyping(),
                    settings.isEvosMaxThreeStages(), settings.isEvosForceChange());

            log.println("--Randomized Evolutions--");
            List<Pokemon> allPokes = romHandler.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    int numEvos = pk.getEvolutionsFrom().size();
                    if (numEvos > 0) {
                        StringBuilder evoStr = new StringBuilder(pk.getEvolutionsFrom().get(0).getTo().getName());
                        for (int i = 1; i < numEvos; i++) {
                            if (i == numEvos - 1) {
                                evoStr.append(" and ").append(pk.getEvolutionsFrom().get(i).getTo().getName());
                            } else {
                                evoStr.append(", ").append(pk.getEvolutionsFrom().get(i).getTo().getName());
                            }
                        }
                        log.println(pk.getName() + " now evolves into " + evoStr.toString());
                    }
                }
            }

            log.println();
        }

        // Trade evolutions removal
        if (settings.isChangeImpossibleEvolutions()) {
            romHandler.removeTradeEvolutions(!(settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED));
        }

        // Easier evolutions
        if (settings.isMakeEvolutionsEasier()) {
            romHandler.condenseLevelEvolutions(40, 30);
        }

        // Starter Pokemon
        // Applied after type to update the strings correctly based on new types
        maybeChangeAndLogStarters(log, romHandler);

        // Move Data Log
        // Placed here so it matches its position in the randomizer interface
        maybeLogMoveChanges(log, romHandler);

        // Movesets
        boolean noBrokenMoves = settings.doBlockBrokenMoves();
        boolean forceFourLv1s = romHandler.supportsFourStartingMoves() && settings.isStartWithFourMoves();
        double msGoodDamagingProb = settings.isMovesetsForceGoodDamaging()
                ? settings.getMovesetsGoodDamagingPercent() / 100.0
                : 0;
        if (settings.getMovesetsMod() == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE) {
            romHandler.randomizeMovesLearnt(true, noBrokenMoves, forceFourLv1s, msGoodDamagingProb);
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.COMPLETELY_RANDOM) {
            romHandler.randomizeMovesLearnt(false, noBrokenMoves, forceFourLv1s, msGoodDamagingProb);
        }

        if (settings.isReorderDamagingMoves()) {
            romHandler.orderDamagingMovesByDamage();
        }

        // Show the new movesets if applicable
        if (settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED) {
            log.println("Pokemon Movesets: Unchanged." + NEWLINE);
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("Pokemon Movesets: Metronome Only." + NEWLINE);
        } else {
            log.println("--Pokemon Movesets--");
            List<String> movesets = new ArrayList<>();

            for (Pokemon pkmn : romHandler.getPokemon()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%03d %-10s : ", pkmn.getNumber(), pkmn.getName()));
                List<MoveLearnt> data = pkmn.getLearnset();
                boolean first = true;
                for (MoveLearnt ml : data) {
                    if (!first) {
                        sb.append(", ");
                    }
                    try {
                        int moveIndex = ml.getMove();
                        Move move = moves.get(moveIndex - 1);
                        sb.append(move.getName()).append(" at level ").append(ml.getLevel());
                    } catch (NullPointerException ex) {
                        sb.append("invalid move at level").append(ml.getLevel());
                    }
                    first = false;
                }
                movesets.add(sb.toString());
            }
            Collections.sort(movesets);
            for (String moveset : movesets) {
                log.println(moveset);
            }
            log.println();
        }

        // Trainer Pokemon
        if (settings.getTrainersMod() == Settings.TrainersMod.RANDOM) {
            romHandler.randomizeTrainerPokes(settings.isTrainersUsePokemonOfSimilarStrength(),
                    settings.isTrainersBlockLegendaries(), settings.isTrainersBlockEarlyWonderGuard(),
                    settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0,
                    settings.isFillBossTeams());
        } else if (settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED) {
            romHandler.typeThemeTrainerPokes(settings.isTrainersUsePokemonOfSimilarStrength(),
                    settings.isTrainersMatchTypingDistribution(), settings.isTrainersBlockLegendaries(),
                    settings.isTrainersBlockEarlyWonderGuard(),
                    settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0);
        }

        if (settings.isTrainersLevelModified() && settings.getTrainersLevelModifier() != 0) {
            romHandler.writeTrainerLevelModifier(settings.getTrainersLevelModifier());
        }

        if ((settings.getTrainersMod() != Settings.TrainersMod.UNCHANGED
                || settings.getStartersMod() != Settings.StartersMod.UNCHANGED)
                && settings.isRivalCarriesStarterThroughout()) {
            romHandler.rivalCarriesStarter();
        }

        if (settings.isTrainersForceFullyEvolved()) {
            romHandler.forceFullyEvolvedTrainerPokes(settings.getTrainersForceFullyEvolvedLevel());
        }

        // Trainer names & class names randomization
        // done before trainer log to add proper names

        if (romHandler.canChangeTrainerText()) {
            if (settings.isRandomizeTrainerClassNames()) {
                romHandler.randomizeTrainerClassNames(settings.getCustomNames());
            }

            if (settings.isRandomizeTrainerNames()) {
                romHandler.randomizeTrainerNames(settings.getCustomNames());
            }
        }

        maybeLogTrainerChanges(log, romHandler);

        // Apply metronome only mode now that trainers have been dealt with
        if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            romHandler.metronomeOnlyMode();
        }

        List<Trainer> trainers = romHandler.getTrainers();
        for (Trainer t : trainers) {
            for (TrainerPokemon tpk : t.getPokemon()) {
                if (tpk.getPokemon() != null) {
                    checkValue = addToCV(checkValue, tpk.getLevel(), tpk.getPokemon().getNumber());
                }
            }
        }

        // Static Pokemon
        checkValue = maybeChangeAndLogStaticPokemon(log, romHandler, checkValue);

        // Wild Pokemon
        if (settings.isUseMinimumCatchRate()) {
            int normalMin, legendaryMin;
            switch (settings.getMinimumCatchRateLevel()) {
            case 1:
            default:
                normalMin = 75;
                legendaryMin = 37;
                break;
            case 2:
                normalMin = 128;
                legendaryMin = 64;
                break;
            case 3:
                normalMin = 200;
                legendaryMin = 100;
                break;
            case 4:
                normalMin = legendaryMin = 255;
                break;
            }
            romHandler.minimumCatchRate(normalMin, legendaryMin);
        }

        switch (settings.getWildPokemonMod()) {
        case RANDOM:
            romHandler.randomEncounters(settings.isUseTimeBasedEncounters(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL,
                    settings.isCatchEmAllReasonableSlotsOnly(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.TYPE_THEME_AREAS,
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH,
                    settings.isBlockWildLegendaries(),
                    settings.isCondenseEncounterSlots());
            break;
        case AREA_MAPPING:
            romHandler.area1to1Encounters(settings.isUseTimeBasedEncounters(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL,
                    settings.isCatchEmAllReasonableSlotsOnly(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.TYPE_THEME_AREAS,
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH,
                    settings.isBlockWildLegendaries());
            break;
        case GLOBAL_MAPPING:
            romHandler.game1to1Encounters(settings.isUseTimeBasedEncounters(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH,
                    settings.isBlockWildLegendaries());
            break;
        default:
            break;
        }

        maybeLogWildPokemonChanges(log, romHandler);
        List<EncounterSet> encounters = romHandler.getEncounters(settings.isUseTimeBasedEncounters(), settings.isCondenseEncounterSlots());
        for (EncounterSet es : encounters) {
            for (Encounter e : es.getEncounters()) {
                checkValue = addToCV(checkValue, e.getLevel(), e.getPokemon().getNumber());
            }
        }

        // Frontier Pokemon
        if (settings.isRandomizeFrontier()) {
            romHandler.randomizeFrontier(settings.getMovesetsMod() == Settings.MovesetsMod.COMPLETELY_RANDOM);
        }

        // TMs
        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getTmsMod() == Settings.TMsMod.RANDOM) {
            double goodDamagingProb = settings.isTmsForceGoodDamaging() ? settings.getTmsGoodDamagingPercent() / 100.0
                    : 0;
            romHandler.randomizeTMMoves(noBrokenMoves, settings.isKeepFieldMoveTMs(), goodDamagingProb);
            log.println("--TM Moves--");
            List<Integer> tmMoves = romHandler.getTMMoves();
            for (int i = 0; i < tmMoves.size(); i++) {
                log.printf("TM%02d %s" + NEWLINE, i + 1, moves.get(tmMoves.get(i) - 1).getName());
                checkValue = addToCV(checkValue, tmMoves.get(i));
            }
            log.println();
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("TM Moves: Metronome Only." + NEWLINE);
        } else {
            log.println("TM Moves: Unchanged." + NEWLINE);
        }

        // TM/HM compatibility
        romHandler.randomizeTMHMCompatibility(settings.getTmsHmsCompatibilityMod());

        // Move Tutors
        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.RANDOM) {
            List<Integer> oldMtMoves = romHandler.getMoveTutorMoves();
            double goodDamagingProb = settings.isTutorsForceGoodDamaging()
                    ? settings.getTutorsGoodDamagingPercent() / 100.0
                    : 0;
            romHandler.randomizeMoveTutorMoves(noBrokenMoves, settings.isKeepFieldMoveTutors(), goodDamagingProb);
            log.println("--Move Tutor Moves--");
            List<Integer> newMtMoves = romHandler.getMoveTutorMoves();
            for (int i = 0; i < newMtMoves.size(); i++) {
                log.printf("%s => %s" + NEWLINE, moves.get(oldMtMoves.get(i) - 1).getName(),
                        moves.get(newMtMoves.get(i) - 1).getName());
                checkValue = addToCV(checkValue, newMtMoves.get(i));
            }
            log.println();
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("Move Tutor Moves: Metronome Only." + NEWLINE);
        } else {
            log.println("Move Tutor Moves: Unchanged." + NEWLINE);
        }

        // Compatibility
        romHandler.randomizeMoveTutorCompatibility(settings.getMoveTutorsCompatibilityMod());

        // In-game trades
        List<IngameTrade> oldTrades = romHandler.getIngameTrades();
        if (settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN) {
            romHandler.randomizeIngameTrades(false, settings.isRandomizeInGameTradesNicknames(),
                    settings.isRandomizeInGameTradesOTs(), settings.isRandomizeInGameTradesIVs(),
                    settings.isRandomizeInGameTradesItems(), settings.getCustomNames());
        } else if (settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN_AND_REQUESTED) {
            romHandler.randomizeIngameTrades(true, settings.isRandomizeInGameTradesNicknames(),
                    settings.isRandomizeInGameTradesOTs(), settings.isRandomizeInGameTradesIVs(),
                    settings.isRandomizeInGameTradesItems(), settings.getCustomNames());
        }

        if (!(settings.getInGameTradesMod() == Settings.InGameTradesMod.UNCHANGED)) {
            log.println("--In-Game Trades--");
            List<IngameTrade> newTrades = romHandler.getIngameTrades();
            int size = oldTrades.size();
            for (int i = 0; i < size; i++) {
                IngameTrade oldT = oldTrades.get(i);
                IngameTrade newT = newTrades.get(i);
                log.printf("Trading %s for %s the %s has become trading %s for %s the %s" + NEWLINE,
                        oldT.getRequestedPokemon().getName(), oldT.getNickname(), oldT.getGivenPokemon().getName(), newT.getRequestedPokemon().getName(),
                        newT.getNickname(), newT.getGivenPokemon().getName());
            }
            log.println();
        }

        // Field Items
        maybeChangeAndLogFieldItems(log, romHandler);

        if (settings.getTypeChartMod() != Settings.TypeChartMod.UNCHANGED) {
            log.println("--Type Chart--");
            romHandler.randomizeTypeCharts(settings.getTypeChartMod());
            log.println(romHandler.getTypeInteractionsLog(settings.getTypeChartMod()));
            log.println();
        }

        if (settings.isRandomWarps()) {
            // How much of the game to randomize (broken down by gym progression), 10 is all of it
            int warpRandoLevel = settings.getWarpRandoLevel() == 0 ? 10 : settings.getWarpRandoLevel();
            romHandler.randomizeWarps(warpRandoLevel, !settings.isKeepUselessDeadends(), !settings.isRemoveGymOrderLogic());
        }


        if (settings.getCurrentRestrictions() != null && !settings.getCurrentRestrictions().nothingSelected()) {
            romHandler.saveGenRestrictionsToRom(settings.getCurrentRestrictions());
        }

        // Signature...
        romHandler.applySignature();

        // Record check value?
        romHandler.writeCheckValueToROM(checkValue);

        // Save
        romHandler.saveRom(filename);

        // Log tail
        log.println("------------------------------------------------------------------");
        log.println("Randomization of " + romHandler.getROMName() + " completed.");
        log.println("Time elapsed: " + (System.currentTimeMillis() - startTime) + "ms");
        log.println("RNG Calls: " + RandomSource.callsSinceSeed());
        log.println("------------------------------------------------------------------");

        return checkValue;
    }

    private void maybeLogBaseStatAndTypeChanges(final PrintStream log, final RomHandler romHandler) {
        List<Pokemon> allPokes = romHandler.getPokemon();
        String[] itemNames = romHandler.getItemNames();
        // Log base stats & types if changed at all
        if (settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.UNCHANGED
                && settings.getTypesMod() == Settings.TypesMod.UNCHANGED
                && settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED
                && !settings.isRandomizeWildPokemonHeldItems()) {
            log.println("Pokemon base stats & type: unchanged" + NEWLINE);
        } else {
            log.println("--Pokemon Base Stats & Types--");
            log.print("NUM|NAME      |TYPE             |  HP| ATK| DEF| SPE|SATK|SDEF");
            int abils = romHandler.abilitiesPerPokemon();
            for (int i = 0; i < abils; i++) {
                log.print("|ABILITY" + (i + 1) + "    ");
            }
            log.print("|ITEM");
            log.println();
            for (Pokemon pkmn : allPokes) {
                if (pkmn != null) {
                    String typeString = pkmn.getPrimaryType() == null ? "???" : pkmn.getPrimaryType().toString();
                    if (pkmn.getSecondaryType() != null) {
                        typeString += "/" + pkmn.getSecondaryType().toString();
                    }
                    log.printf("%3d|%-10s|%-17s|%4d|%4d|%4d|%4d|%4d|%4d", pkmn.getNumber(), pkmn.getName(), typeString,
                            pkmn.getHp(), pkmn.getAttack(), pkmn.getDefense(), pkmn.getSpeed(), pkmn.getSpatk(), pkmn.getSpdef());
                    if (abils > 0) {
                        log.printf("|%-12s|%-12s", romHandler.abilityName(pkmn.getAbility1()),
                                romHandler.abilityName(pkmn.getAbility2()));
                        if (abils > 2) {
                            log.printf("|%-12s", romHandler.abilityName(pkmn.getAbility3()));
                        }
                    }
                    log.print("|");
                    if (pkmn.getGuaranteedHeldItem() > 0) {
                        log.print(itemNames[pkmn.getGuaranteedHeldItem()] + " (100%)");
                    } else {
                        int itemCount = 0;
                        if (pkmn.getCommonHeldItem() > 0) {
                            itemCount++;
                            log.print(itemNames[pkmn.getCommonHeldItem()] + " (common)");
                        }
                        if (pkmn.getRareHeldItem() > 0) {
                            if (itemCount > 0) {
                                log.print(", ");
                            }
                            itemCount++;
                            log.print(itemNames[pkmn.getRareHeldItem()] + " (rare)");
                        }
                        if (pkmn.getDarkGrassHeldItem() > 0) {
                            if (itemCount > 0) {
                                log.print(", ");
                            }
                            log.print(itemNames[pkmn.getDarkGrassHeldItem()] + " (dark grass only)");
                        }
                    }
                    log.println();
                }
            }
            log.println();
        }
    }

    private void maybeChangeAndLogStarters(final PrintStream log, final RomHandler romHandler) {
        if (romHandler.canChangeStarters()) {
            if (settings.getStartersMod() == Settings.StartersMod.CUSTOM) {
                log.println("--Custom Starters--");
                List<Pokemon> romPokemon = romHandler.getPokemon();
                int[] customStarters = settings.getCustomStarters();
                Pokemon pkmn1 = romPokemon.get(customStarters[0]);
                log.println("Set starter 1 to " + pkmn1.getName());
                Pokemon pkmn2 = romPokemon.get(customStarters[1]);
                log.println("Set starter 2 to " + pkmn2.getName());
                    Pokemon pkmn3 = romPokemon.get(customStarters[2]);
                    log.println("Set starter 3 to " + pkmn3.getName());
                    romHandler.setStarters(Arrays.asList(pkmn1, pkmn2, pkmn3));
                log.println();

            } else if (settings.getStartersMod() == Settings.StartersMod.COMPLETELY_RANDOM) {
                // Randomise
                log.println("--Random Starters--");
                int starterCount = 3;
                List<Pokemon> starters = new ArrayList<>();
                for (int i = 0; i < starterCount; i++) {
                    Pokemon pkmn = romHandler.randomPokemon(true);
                    int currentTry = 0;
                    while (starters.contains(pkmn) && !(currentTry >= 100)) {
                        pkmn = romHandler.randomPokemon(true);
                        currentTry++;
                    }
                    log.println("Set starter " + (i + 1) + " to " + pkmn.getName());
                    starters.add(pkmn);
                }
                romHandler.setStarters(starters);
                log.println();
            } else if (settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_TWO_EVOLUTIONS) {
                // Randomise
                log.println("--Random 2-Evolution Starters--");
                int starterCount = 3;
                List<Pokemon> starters = new ArrayList<>();
                for (int i = 0; i < starterCount; i++) {
                    Pokemon pkmn = romHandler.random2EvosPokemon(true);
                    while (starters.contains(pkmn)) {
                        pkmn = romHandler.random2EvosPokemon(true);
                    }
                    log.println("Set starter " + (i + 1) + " to " + pkmn.getName());
                    starters.add(pkmn);
                }
                romHandler.setStarters(starters);
                log.println();
            } else if (settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_ONE_EVOLUTION) {
                // Randomise
                log.println("--Random 1-Evolution Starters--");
                int starterCount = 3;
                List<Pokemon> starters = new ArrayList<>();
                for (int i = 0; i < starterCount; i++) {
                    Pokemon pkmn = romHandler.random1EvosPokemon(true);
                    while (starters.contains(pkmn)) {
                        pkmn = romHandler.random1EvosPokemon(true);
                    }
                    log.println("Set starter " + (i + 1) + " to " + pkmn.getName());
                    starters.add(pkmn);
                }
                romHandler.setStarters(starters);
                log.println();
            } else if (settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_NO_EVOLUTIONS) {
                // Randomise
                log.println("--Random 0-Evolution Starters--");
                int starterCount = 3;
                List<Pokemon> starters = new ArrayList<>();
                for (int i = 0; i < starterCount; i++) {
                    Pokemon pkmn = romHandler.random0EvosPokemon(settings.isBanLegendaryStarters(),
                            settings.isOnlyLegendaryStarters(), true);
                    while (starters.contains(pkmn)) {
                        pkmn = romHandler.random0EvosPokemon(settings.isBanLegendaryStarters(),
                                settings.isOnlyLegendaryStarters(), true);
                    }
                    log.println("Set starter " + (i + 1) + " to " + pkmn.getName());
                    starters.add(pkmn);
                }
                romHandler.setStarters(starters);
                log.println();
            }
            if (settings.isRandomizeStartersHeldItems()) {
                romHandler.randomizeStarterHeldItems(settings.isBanBadRandomStarterHeldItems());
            }
        }
    }

    private void maybeLogWildPokemonChanges(final PrintStream log, final RomHandler romHandler) {
        if (settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED) {
            log.println("Wild Pokemon: Unchanged." + NEWLINE);
        } else {
            log.println("--Wild Pokemon--");
            List<EncounterSet> encounters = romHandler.getEncounters(settings.isUseTimeBasedEncounters(), settings.isCondenseEncounterSlots());
            int idx = 0;
            for (EncounterSet es : encounters) {
                idx++;
                log.print("Set #" + idx + " ");
                if (es.getDisplayName() != null) {
                    log.print("- " + es.getDisplayName() + " ");
                }
                log.print("(rate=" + es.getRate() + ")");
                log.print(" - ");
                boolean first = true;
                for (Encounter e : es.getEncounters()) {
                    if (!first) {
                        log.print(", ");
                    }
                    log.print(e.getPokemon().getName() + " Lv");
                    if (e.getMaxLevel() > 0 && e.getMaxLevel() != e.getLevel()) {
                        log.print("s " + e.getLevel() + "-" + e.getMaxLevel());
                    } else {
                        log.print(e.getLevel());
                    }
                    first = false;
                }
                log.println();
            }
            log.println();
        }
    }

    private void maybeLogTrainerChanges(final PrintStream log, final RomHandler romHandler) {
        if (settings.getTrainersMod() == Settings.TrainersMod.UNCHANGED
                && !settings.isRivalCarriesStarterThroughout()) {
            log.println("Trainers: Unchanged." + NEWLINE);
        } else {
            log.println("--Trainers Pokemon--");
            List<Trainer> trainers = romHandler.getTrainers();
            int idx = 0;
            for (Trainer t : trainers) {
                idx++;
                log.print("#" + idx + " ");
                if (t.getFullDisplayName() != null) {
                    log.print("(" + t.getFullDisplayName() + ")");
                } else if (t.getName() != null) {
                    log.print("(" + t.getName() + ")");
                }
                if (t.getOffset() != idx && t.getOffset() != 0) {
                    log.printf("@%X", t.getOffset());
                }
                log.print(" - ");
                boolean first = true;
                for (TrainerPokemon tpk : t.getPokemon()) {
                    if (tpk.getPokemon() == null) {
                        continue;
                    }
                    if (!first) {
                        log.print(", ");
                    }
                    log.print(tpk.getPokemon().getName() + " Lv" + tpk.getLevel());
                    first = false;
                }
                log.println();
            }
            log.println();
        }
    }

    private int maybeChangeAndLogStaticPokemon(final PrintStream log, final RomHandler romHandler,
            int checkValue) {
        if (romHandler.canChangeStaticPokemon()) {
            List<Pokemon> oldStatics = romHandler.getStaticPokemon();
            if (settings.getStaticPokemonMod() == Settings.StaticPokemonMod.RANDOM_MATCHING) {
                romHandler.randomizeStaticPokemon(true);
            } else if (settings.getStaticPokemonMod() == Settings.StaticPokemonMod.COMPLETELY_RANDOM) {
                romHandler.randomizeStaticPokemon(false);
            }
            List<Pokemon> newStatics = romHandler.getStaticPokemon();
            if (settings.getStaticPokemonMod() == Settings.StaticPokemonMod.UNCHANGED) {
                log.println("Static Pokemon: Unchanged." + NEWLINE);
            } else {
                log.println("--Static Pokemon--");
                Map<Pokemon, Integer> seenPokemon = new TreeMap<>();
                for (int i = 0; i < oldStatics.size(); i++) {
                    Pokemon oldP = oldStatics.get(i);
                    Pokemon newP = newStatics.get(i);
                    checkValue = addToCV(checkValue, newP.getNumber());
                    log.print(oldP.getName());
                    if (seenPokemon.containsKey(oldP)) {
                        int amount = seenPokemon.get(oldP);
                        log.print("(" + (++amount) + ")");
                        seenPokemon.put(oldP, amount);
                    } else {
                        seenPokemon.put(oldP, 1);
                    }
                    log.println(" => " + newP.getName());
                }
                log.println();
            }
        }
        return checkValue;
    }

    private void maybeLogMoveChanges(final PrintStream log, final RomHandler romHandler) {
        if (!settings.isRandomizeMoveAccuracies() && !settings.isRandomizeMovePowers() && !settings.isRandomizeMovePPs()
                && !settings.isRandomizeMoveCategory() && !settings.isRandomizeMoveTypes()) {
            if (!settings.isUpdateMoves()) {
                log.println("Move Data: Unchanged." + NEWLINE);
            }
        } else {
            log.println("--Move Data--");
            log.print("NUM|NAME           |TYPE    |POWER|ACC.|PP");
            if (romHandler.hasPhysicalSpecialSplit()) {
                log.print(" |CATEGORY");
            }
            log.println();
            List<Move> allMoves = romHandler.getMoves();
            for (Move mv : allMoves) {
                if (mv != null) {
                    String mvType = (mv.getType() == null) ? "???" : mv.getType().toString();
                    log.printf("%3d|%-15s|%-8s|%5d|%4d|%3d", mv.getInternalId(), mv.getName(), mvType, mv.getPower(),
                            (int) mv.getHitratio(), mv.getPp());
                    if (romHandler.hasPhysicalSpecialSplit()) {
                        log.printf("| %s", mv.getCategory().toString());
                    }
                    log.println();
                }
            }
            log.println();
        }
    }

    private void maybeChangeAndLogFieldItems(final PrintStream log, final RomHandler romHandler) {
        
        if(settings.getFieldItemsMod() == Settings.FieldItemsMod.UNCHANGED) {
            log.println("Field Items: Unchanged." + NEWLINE);
        }
        else {
            List<ItemLocation> oldItems = romHandler.getRegularFieldItems();
            List<FieldTM> oldTMs = romHandler.getCurrentFieldTMs();
            String[] itemNames = romHandler.getItemNames();
            
            if (settings.getFieldItemsMod() == Settings.FieldItemsMod.SHUFFLE) {
                romHandler.shuffleFieldItems();
            } else if (settings.getFieldItemsMod() == Settings.FieldItemsMod.RANDOM) {
                romHandler.randomizeFieldItems(settings.isBanBadRandomFieldItems());

                if (settings.isRandomizeGivenItems()) {
                    romHandler.randomizeGivenItems(settings.isBanBadRandomFieldItems());
                }

                if (settings.isRandomizeBerryTrees()) {
                    romHandler.randomizeBerryTrees(settings.isBanBadRandomFieldItems());
                }

                if (settings.isRandomizePickupTables()) {
                    romHandler.randomizePickupTime(settings.isBanBadRandomFieldItems());
                }

                if (settings.isRandomItemPrices()) {
                    romHandler.randomizeItemPrices(settings.isAllMartsHaveBallAndRepel());
                }

                if (settings.isRandomizeMarts()) {
                    romHandler.randomizeMarts(settings.isBanBadRandomFieldItems(), settings.isAllMartsHaveBallAndRepel());
                }

            }
            
            List<ItemLocation> newItems = romHandler.getRegularFieldItems();
            List<FieldTM> newTMs = romHandler.getCurrentFieldTMs();
            
            log.println("--Field Items--");
            Iterator<ItemLocation> niIter = newItems.iterator();
            
            for(ItemLocation loc : oldItems) {
                ItemLocation newLoc = niIter.next();
                log.printf("%s: %s => %s", loc.getDescription(), itemNames[loc.getItem()], itemNames[newLoc.getItem()]);
                log.println();
            }
            
            Iterator<FieldTM> ntIter = newTMs.iterator();
            
            for(FieldTM loc : oldTMs) {
                FieldTM newLoc = ntIter.next();
                log.printf("%s: TM%02d => TM%02d", loc.getDescription(), loc.getTm(), newLoc.getTm());
                log.println();
            }
            
            log.println();
        }
        
        

    }

    private static int addToCV(int checkValue, int... values) {
        for (int value : values) {
            checkValue = Integer.rotateLeft(checkValue, 3);
            checkValue ^= value;
        }
        return checkValue;
    }
}