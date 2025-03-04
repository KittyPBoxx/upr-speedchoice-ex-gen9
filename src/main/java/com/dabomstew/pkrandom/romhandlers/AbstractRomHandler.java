package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  AbstractRomHandler.java - a base class for all rom handlers which     --*/
/*--                            implements the majority of the actual       --*/
/*--                            randomizer logic by building on the base    --*/
/*--                            getters & setters provided by each concrete --*/
/*--                            handler.                                    --*/
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

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.constants.GlobalConstants;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.*;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractRomHandler implements RomHandler {

    private boolean restrictionsSet;
    protected List<Pokemon> mainPokemonList;
    protected List<Pokemon> noLegendaryList;
    protected List<Pokemon> onlyLegendaryList;
    protected List<Pokemon> bannedForPlayer;
    protected final Random random;
    protected PrintStream logStream;
    protected CustomConfig customConfig;

    /* Constructor */

    public AbstractRomHandler(Random random, PrintStream logStream) {
        this.random = random;
        this.logStream = logStream;
    }

    /*
     * Public Methods, implemented here for all gens. Unlikely to be overridden.
     */

    public void setLog(PrintStream logStream) {
        this.logStream = logStream;
    }

    public void setPokemonPool(GenRestrictions restrictions) {
        restrictionsSet = true;
        mainPokemonList = this.allPokemonWithoutNull();
        if (restrictions != null) {
            mainPokemonList = new ArrayList<>();
            List<Pokemon> allPokemon = this.getPokemon();

            if (restrictions.isAllow_gen(1)) {
                addPokemonGeneration(mainPokemonList, allPokemon, 1, 151);
            }

            if (restrictions.isAllow_gen(2) && allPokemon.size() > 151) {
                addPokemonGeneration(mainPokemonList, allPokemon, 152, 251);
            }

            if (restrictions.isAllow_gen(3) && allPokemon.size() > 251) {
                addPokemonGeneration(mainPokemonList, allPokemon, 252, 386);
            }

            if (restrictions.isAllow_gen(4) && allPokemon.size() > 386) {
                addPokemonGeneration(mainPokemonList, allPokemon, 387, 493);
            }

            if (restrictions.isAllow_gen(5) && allPokemon.size() > 493) {
                addPokemonGeneration(mainPokemonList, allPokemon, 494, 649);
            }

            if (restrictions.isAllow_gen(6) && allPokemon.size() > 649) {
                addPokemonGeneration(mainPokemonList, allPokemon, 650, 721);
            }

            if (restrictions.isAllow_gen(7) && allPokemon.size() > 721) {
                addPokemonGeneration(mainPokemonList, allPokemon, 722, 809);
            }

            if (restrictions.isAllow_gen(8) && allPokemon.size() > 809) {
                addPokemonGeneration(mainPokemonList, allPokemon, 810, 905);
            }

            if (restrictions.isAllow_gen(9) && allPokemon.size() > 905) {
                addPokemonGeneration(mainPokemonList, allPokemon, 1289, 1434);
            }
        }

        if (customConfig.getBannedMonNumbers() != null) {
            mainPokemonList = mainPokemonList.stream().filter(p -> !customConfig.getBannedMonNumbers().contains(p.getSpeciesNumber())).collect(Collectors.toList());
        }

        noLegendaryList = new ArrayList<>();
        onlyLegendaryList = new ArrayList<>();

        for (Pokemon p : mainPokemonList) {
            if (p != null) {
                if (p.isLegendary()) {
                    onlyLegendaryList.add(p);
                } else {
                    noLegendaryList.add(p);
                }
            }
        }
    }

    public void setCustomConfig(CustomConfig customConfig) {
        this.customConfig = customConfig;
    }

    private void addPokemonGeneration(List<Pokemon> pokemonPool, List<Pokemon> allPokemon, int firstSpeciesNo, int lastSpeciesNo) {
        Optional<Pokemon> first = getPokemon().stream().filter(p -> p.getSpeciesNumber() == firstSpeciesNo).findFirst();
        Optional<Pokemon> last = getPokemon().stream().filter(p -> p.getSpeciesNumber() == lastSpeciesNo).findFirst();

        if (first.isPresent() && last.isPresent()) {
            addPokesFromRange(pokemonPool, allPokemon, firstSpeciesNo, lastSpeciesNo);
        }
    }

    private void addPokesFromRange(List<Pokemon> pokemonPool, List<Pokemon> allPokemon, int range_min, int range_max) {

        List<Pokemon> allPokesInGen = allPokemon.stream()
                                                .filter(p -> p.getSpeciesNumber() >= range_min)
                                                .filter(p -> p.getSpeciesNumber() <= range_max)
                                                .collect(Collectors.toList());

        for (Pokemon pokemon : allPokesInGen) {
            if (!pokemonPool.contains(pokemon)) {
                pokemonPool.add(pokemon);
            }
        }
    }

    private void addEvosFromRange(List<Pokemon> pokemonPool, int first_min, int first_max, int second_min,
                                  int second_max) {
        Set<Pokemon> newPokemon = new TreeSet<>();
        for (Pokemon pk : pokemonPool) {
            if (pk.getNumber() >= first_min && pk.getNumber() <= first_max) {
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    if (ev.getTo().getNumber() >= second_min && ev.getTo().getNumber() <= second_max) {
                        if (!pokemonPool.contains(ev.getTo())) {
                            newPokemon.add(ev.getTo());
                        }
                    }
                }

                for (Evolution ev : pk.getEvolutionsTo()) {
                    if (ev.getFrom().getNumber() >= second_min && ev.getFrom().getNumber() <= second_max) {
                        if (!pokemonPool.contains(ev.getFrom())) {
                            newPokemon.add(ev.getFrom());
                        }
                    }
                }
            }
        }

        pokemonPool.addAll(newPokemon);
    }

    @Override
    public void shufflePokemonStats(boolean evolutionSanity) {
        if (evolutionSanity) {
            copyUpEvolutionsHelper(pk -> pk.shuffleStats(AbstractRomHandler.this.random),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.copyShuffledStatsUpEvolution(evFrom));
        } else {
            List<Pokemon> allPokes = this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.shuffleStats(this.random);
                }
            }
        }
    }

    @Override
    public void randomizePokemonStats(boolean evolutionSanity) {

        if (evolutionSanity) {
            copyUpEvolutionsHelper(pk -> pk.randomizeStatsWithinBST(AbstractRomHandler.this.random),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.copyRandomizedStatsUpEvolution(evFrom));
        } else {
            List<Pokemon> allPokes = this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.randomizeStatsWithinBST(this.random);
                }
            }
        }

    }

    @Override
    public void randomizePokemonBaseStats(final boolean evolutionSanity, final boolean dontRandomizeRatio, final boolean evosBuffStats) {
        if (evolutionSanity) {
            // ignore dontRandomizeRatio for evolutions - the two aren't compatible here
            copyUpEvolutionsHelper(pk -> pk.randomizeBST(AbstractRomHandler.this.random, dontRandomizeRatio),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.copyRandomizedBSTUpEvolution(random, evFrom, evosBuffStats));
        } else if (evosBuffStats) {
            copyUpEvolutionsHelper(pk -> pk.randomizeBST(AbstractRomHandler.this.random, dontRandomizeRatio),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.randomizeBSTSetAmountAbovePreevo(random, evFrom, dontRandomizeRatio));
        } else {
            // no evolution carrying at all
            List<Pokemon> allPokes = this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.randomizeBST(random, dontRandomizeRatio);
                }
            }
        }
    }

    @Override
    public void randomizePokemonBaseStatsPerc(boolean evolutionSanity, final int percent, final boolean dontRandomizeRatio) {
        if (evolutionSanity) {
            copyUpEvolutionsHelper(pk -> pk.randomizeBSTPerc(AbstractRomHandler.this.random, percent, dontRandomizeRatio),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.percentRaiseStatFloorUpEvolution(random, dontRandomizeRatio, evFrom));
        } else {
            List<Pokemon> allPokes = this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.randomizeBSTPerc(random, percent, dontRandomizeRatio);
                }
            }
        }
    }

    @Override
    public void equalizePokemonStats(boolean evolutionSanity, final boolean dontRandomizeRatio) {
        if (evolutionSanity) {
            copyUpEvolutionsHelper(pk -> pk.equalizeBST(AbstractRomHandler.this.random, dontRandomizeRatio),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.copyEqualizedStatsUpEvolution(evFrom));
        } else {
            List<Pokemon> allPokes = this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.equalizeBST(this.random, dontRandomizeRatio);
                }
            }
        }
    }

    public Pokemon randomPokemon(boolean isForPlayer) {
        checkPokemonRestrictions();
        Pokemon pokemon = null;

        List<Pokemon> selectionList = new ArrayList<>(mainPokemonList);
        if (isForPlayer) {
            selectionList.removeAll(bannedForPlayer);
        }

        while (pokemon == null) {
            pokemon = selectionList.get(this.random.nextInt(selectionList.size()));
        }

        return pokemon;
    }

    @Override
    public Pokemon randomNonLegendaryPokemon(boolean isForPlayer) {
        checkPokemonRestrictions();

        List<Pokemon> selectionList = new ArrayList<>(noLegendaryList);
        if (isForPlayer) {
            selectionList.removeAll(bannedForPlayer);
        }

        return selectionList.get(this.random.nextInt(selectionList.size()));
    }

    @Override
    public Pokemon randomLegendaryPokemon(boolean isForPlayer) {
        checkPokemonRestrictions();
        List<Pokemon> selectionList = new ArrayList<>(onlyLegendaryList);
        if (isForPlayer) {
            selectionList.removeAll(bannedForPlayer);
        }

        return selectionList.get(this.random.nextInt(selectionList.size()));
    }

    private List<Pokemon> twoEvoPokes;
    private List<Pokemon> oneEvoPokes;
    private List<Pokemon> noEvoPokes;

    @Override
    public Pokemon random2EvosPokemon(boolean isForPlayer) {
        if (twoEvoPokes == null) {
            // Prepare the list
            twoEvoPokes = new ArrayList<>();
            List<Pokemon> allPokes = new ArrayList<>(this.getPokemon());
            if (isForPlayer) {
                allPokes.removeAll(bannedForPlayer);
            }
            for (Pokemon pk : allPokes) {
                if (pk != null && pk.getEvolutionsTo().isEmpty() && !pk.getEvolutionsFrom().isEmpty()) {
                    // Potential candidate
                    for (Evolution ev : pk.getEvolutionsFrom()) {
                        // If any of the targets here evolve, the original
                        // Pokemon has 2+ stages.
                        if (!ev.getTo().getEvolutionsFrom().isEmpty()) {
                            twoEvoPokes.add(pk);
                            break;
                        }
                    }
                }
            }
        }
        return twoEvoPokes.get(this.random.nextInt(twoEvoPokes.size()));
    }

    @Override
    public Pokemon random1EvosPokemon(boolean isForPlayer) {
        if (oneEvoPokes == null) {
            // Prepare the list
            oneEvoPokes = new ArrayList<>();
            List<Pokemon> allPokes = new ArrayList<>(this.getPokemon());
            if (isForPlayer) {
                allPokes.removeAll(bannedForPlayer);
            }
            for (Pokemon pk : allPokes) {
                if (pk != null && pk.getEvolutionsTo().isEmpty() && !pk.getEvolutionsFrom().isEmpty()) {
                    // Potential candidate
                    for (Evolution ev : pk.getEvolutionsFrom()) {
                        // If any of the targets here dont evolve, the original
                        // Pokemon has 1 stage.
                        if (ev.getTo().getEvolutionsFrom().isEmpty()) {
                            oneEvoPokes.add(pk);
                            break;
                        }
                    }
                }
            }
        }
        return oneEvoPokes.get(this.random.nextInt(oneEvoPokes.size()));
    }

    @Override
    public Pokemon random0EvosPokemon(boolean banLegend, boolean onlyLegend, boolean isForPlayer) {
        if (noEvoPokes == null) {
            // Prepare the list
            noEvoPokes = new ArrayList<>();
            List<Pokemon> allPokes = new ArrayList<>(this.getPokemon());
            if (isForPlayer) {
                allPokes.removeAll(bannedForPlayer);
            }
            for (Pokemon pk : allPokes) {
                if (pk != null && pk.getEvolutionsTo().isEmpty() && pk.getEvolutionsFrom().isEmpty()) {
                    if (banLegend || onlyLegend) {
                        if (!pk.isLegendary() && banLegend) {
                            noEvoPokes.add(pk);
                        } else if (pk.isLegendary() && onlyLegend) {
                            noEvoPokes.add(pk);
                        }
                    } else {
                        noEvoPokes.add(pk);
                    }
                }
            }
        }
        return noEvoPokes.get(this.random.nextInt(noEvoPokes.size()));
    }

    @Override
    public Type randomType() {
        Type t = Type.randomType(this.random);
        return t;
    }

    @Override
    public void randomizePokemonTypes(boolean evolutionSanity) {
        List<Pokemon> allPokes = this.getPokemon();
        if (evolutionSanity) {
            // Type randomization with evolution sanity
            copyUpEvolutionsHelper(new BasePokemonAction() {
                public void applyTo(Pokemon pk) {
                    // Step 1: Basic or Excluded From Copying Pokemon
                    // A Basic/EFC pokemon has a 35% chance of a second type if
                    // it has an evolution that copies type/stats, a 50% chance
                    // otherwise
                    pk.setPrimaryType(randomType());
                    pk.setSecondaryType(null);
                    if (pk.getEvolutionsFrom().size() == 1 && pk.getEvolutionsFrom().get(0).isCarryStats()) {
                        if (AbstractRomHandler.this.random.nextDouble() < 0.35) {
                            do {
                                pk.setSecondaryType(randomType());
                            } while (pk.getSecondaryType() == pk.getPrimaryType());
                        }
                    } else {
                        if (AbstractRomHandler.this.random.nextDouble() < 0.5) {
                            do {
                                pk.setSecondaryType(randomType());
                            } while (pk.getSecondaryType() == pk.getPrimaryType());
                        }
                    }
                }
            }, new EvolvedPokemonAction() {
                public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
                    evTo.setPrimaryType(evFrom.getPrimaryType());
                    evTo.setSecondaryType(evFrom.getSecondaryType());

                    if (evTo.getSecondaryType() == null) {
                        double chance = toMonIsFinalEvo ? 0.25 : 0.15;
                        if (AbstractRomHandler.this.random.nextDouble() < chance) {
                            do {
                                evTo.setSecondaryType(randomType());
                            } while (evTo.getSecondaryType() == evTo.getPrimaryType());
                        }
                    }
                }
            });
        } else {
            // Entirely random types
            for (Pokemon pkmn : allPokes) {
                if (pkmn != null) {
                    pkmn.setPrimaryType(randomType());
                    pkmn.setSecondaryType(null);
                    if (this.random.nextDouble() < 0.5) {
                        do {
                            pkmn.setSecondaryType(randomType());
                        } while (pkmn.getSecondaryType() == pkmn.getPrimaryType());
                    }
                }
            }
        }
    }

    @Override
    public void randomizeAbilities(boolean evolutionSanity, boolean allowWonderGuard, boolean banTrappingAbilities,
                                   boolean banNegativeAbilities) {
        // Abilities don't exist in some games...
        if (this.abilitiesPerPokemon() == 0) {
            return;
        }

        final boolean hasDWAbilities = (this.abilitiesPerPokemon() == 3);

        final List<Integer> bannedAbilities = new ArrayList<>();

        if (!allowWonderGuard) {
            bannedAbilities.add(GlobalConstants.WONDER_GUARD_INDEX);
        }

        if (banTrappingAbilities) {
            bannedAbilities.addAll(GlobalConstants.battleTrappingAbilities);
        }

        if (banNegativeAbilities) {
            bannedAbilities.addAll(GlobalConstants.negativeAbilities);
        }

        final int maxAbility = this.highestAbilityIndex();

        if (evolutionSanity) {
            // copy abilities straight up evolution lines
            // still keep WG as an exception, though

            copyUpEvolutionsHelper(new BasePokemonAction() {
                public void applyTo(Pokemon pk) {
                    if (pk.getAbility1() != GlobalConstants.WONDER_GUARD_INDEX
                            && pk.getAbility2() != GlobalConstants.WONDER_GUARD_INDEX
                            && pk.getAbility3() != GlobalConstants.WONDER_GUARD_INDEX) {
                        // Pick first ability
                        pk.setAbility1(pickRandomAbility(maxAbility, bannedAbilities));

                        // Second ability?
                        if (AbstractRomHandler.this.random.nextDouble() < 0.5) {
                            // Yes, second ability
                            pk.setAbility2(pickRandomAbility(maxAbility, bannedAbilities, pk.getAbility1()));
                        } else {
                            // Nope
                            pk.setAbility2(0);
                        }

                        // Third ability?
                        if (hasDWAbilities) {
                            pk.setAbility3(pickRandomAbility(maxAbility, bannedAbilities, pk.getAbility1(), pk.getAbility2()));
                        }
                    }
                }
            }, new EvolvedPokemonAction() {
                public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
                    if (evTo.getAbility1() != GlobalConstants.WONDER_GUARD_INDEX
                            && evTo.getAbility2() != GlobalConstants.WONDER_GUARD_INDEX
                            && evTo.getAbility3() != GlobalConstants.WONDER_GUARD_INDEX) {
                        evTo.setAbility1(evFrom.getAbility1());
                        evTo.setAbility2(evFrom.getAbility2());
                        evTo.setAbility3(evFrom.getAbility3());
                    }
                }
            });
        } else {
            List<Pokemon> allPokes = this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk == null) {
                    continue;
                }

                // Don't remove WG if already in place.
                if (pk.getAbility1() != GlobalConstants.WONDER_GUARD_INDEX
                        && pk.getAbility2() != GlobalConstants.WONDER_GUARD_INDEX
                        && pk.getAbility3() != GlobalConstants.WONDER_GUARD_INDEX) {
                    // Pick first ability
                    pk.setAbility1(this.pickRandomAbility(maxAbility, bannedAbilities));

                    // Second ability?
                    if (this.random.nextDouble() < 0.5) {
                        // Yes, second ability
                        pk.setAbility2(this.pickRandomAbility(maxAbility, bannedAbilities, pk.getAbility1()));
                    } else {
                        // Nope
                        pk.setAbility2(0);
                    }

                    // Third ability?
                    if (hasDWAbilities) {
                        pk.setAbility3(pickRandomAbility(maxAbility, bannedAbilities, pk.getAbility1(), pk.getAbility2()));
                    }
                }
            }
        }
    }

    private int pickRandomAbility(int maxAbility, List<Integer> bannedAbilities, int... alreadySetAbilities) {
        int newAbility = 0;

        while (true) {
            newAbility = this.random.nextInt(maxAbility) + 1;

            if (bannedAbilities.contains(newAbility)) {
                continue;
            }

            boolean repeat = false;
            for (int alreadySetAbility : alreadySetAbilities) {
                if (alreadySetAbility == newAbility) {
                    repeat = true;
                    break;
                }
            }

            if (repeat) {
                continue;
            } else {
                break;
            }
        }

        return newAbility;
    }

    @Override
    public void randomEncounters(boolean useTimeOfDay, boolean catchEmAll, boolean ceaReasonableOnly, boolean typeThemed, boolean usePowerLevels,
                                 boolean noLegendaries, boolean condenseSlots) {
        checkPokemonRestrictions();
        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay, condenseSlots);

        // New: randomize the order encounter sets are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterSet> scrambledEncounters = new ArrayList<>(currentEncounters);
        Collections.shuffle(scrambledEncounters, this.random);

        List<Pokemon> banned = this.bannedForWildEncounters();
        // Assume EITHER catch em all OR type themed OR match strength for now
        if (catchEmAll) {

            List<Pokemon> allPokes = noLegendaries ? new ArrayList<>(noLegendaryList) : new ArrayList<>(mainPokemonList);
            allPokes.removeAll(banned);
            allPokes = allPokes.stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (ceaReasonableOnly) {
                // put all reasonable sets first so they get at least one loop of pokemon
                // (provided there are enough of them)
                scrambledEncounters.sort((o1, o2) -> {
                    if (o1.isReasonable() == o2.isReasonable()) {
                        return 0;
                    } else if (o1.isReasonable()) {
                        return -1;
                    } else {
                        return 1;
                    }
                });
            }
            for (EncounterSet area : scrambledEncounters) {
                List<Pokemon> pickablePokemon = allPokes;
                pickablePokemon = pickablePokemon.stream().filter(Objects::nonNull).collect(Collectors.toList());
                if (!area.getBannedPokemon().isEmpty()) {
                    pickablePokemon = new ArrayList<>(allPokes);
                    pickablePokemon.removeAll(area.getBannedPokemon());
                }
                for (Encounter enc : area.getEncounters()) {
                    // Pick a random pokemon
                    if (pickablePokemon.isEmpty()) {
                        // Only banned pokes are left, ignore them and pick
                        // something else for now.
                        List<Pokemon> tempPickable = noLegendaries ? new ArrayList<>(noLegendaryList) :
                                new ArrayList<>(mainPokemonList);
                        tempPickable.removeAll(banned);
                        tempPickable = tempPickable.stream().filter(Objects::nonNull).collect(Collectors.toList());
                        tempPickable.removeAll(area.getBannedPokemon());
                        if (tempPickable.isEmpty()) {
                            throw new RandomizationException("ERROR: Couldn't replace a wild Pokemon!");
                        }
                        int picked = this.random.nextInt(tempPickable.size());
                        enc.setPokemon(tempPickable.get(picked));
                    } else {
                        // Picked this Pokemon, remove it
                        int picked = this.random.nextInt(pickablePokemon.size());
                        enc.setPokemon(pickablePokemon.get(picked));
                        pickablePokemon.remove(picked);
                        if (allPokes != pickablePokemon) {
                            allPokes.remove(enc.getPokemon());
                        }
                        if (allPokes.isEmpty()) {
                            // Start again
                            allPokes.addAll(noLegendaries ? noLegendaryList : mainPokemonList);
                            allPokes.removeAll(banned);
                            allPokes = allPokes.stream().filter(Objects::nonNull).collect(Collectors.toList());
                            if (pickablePokemon != allPokes) {
                                pickablePokemon.addAll(allPokes);
                                pickablePokemon.removeAll(area.getBannedPokemon());
                            }
                        }
                    }
                }
            }
        } else if (typeThemed) {
            Map<Type, List<Pokemon>> cachedPokeLists = new TreeMap<Type, List<Pokemon>>();
            for (EncounterSet area : scrambledEncounters) {
                List<Pokemon> possiblePokemon = null;
                int iterLoops = 0;
                while (possiblePokemon == null && iterLoops < 10000) {
                    Type areaTheme = randomType();
                    if (!cachedPokeLists.containsKey(areaTheme)) {
                        List<Pokemon> pType = pokemonOfType(areaTheme, noLegendaries);
                        pType.removeAll(banned);
                        cachedPokeLists.put(areaTheme, pType);
                    }
                    possiblePokemon = cachedPokeLists.get(areaTheme);
                    if (!area.getBannedPokemon().isEmpty()) {
                        possiblePokemon = new ArrayList<Pokemon>(possiblePokemon);
                        possiblePokemon.removeAll(area.getBannedPokemon());
                    }
                    if (possiblePokemon.isEmpty()) {
                        // Can't use this type for this area
                        possiblePokemon = null;
                    }
                    iterLoops++;
                }
                if (possiblePokemon == null) {
                    throw new RandomizationException("Could not randomize an area in a reasonable amount of attempts.");
                }
                for (Encounter enc : area.getEncounters()) {
                    // Pick a random themed pokemon
                    enc.setPokemon(possiblePokemon.get(this.random.nextInt(possiblePokemon.size())));
                }
            }
        } else if (usePowerLevels) {
            List<Pokemon> allowedPokes = noLegendaries ? new ArrayList<>(noLegendaryList)
                    : new ArrayList<>(mainPokemonList);
            allowedPokes.removeAll(banned);
            for (EncounterSet area : scrambledEncounters) {
                List<Pokemon> localAllowed = allowedPokes;
                if (!area.getBannedPokemon().isEmpty()) {
                    localAllowed = new ArrayList<>(allowedPokes);
                    localAllowed.removeAll(area.getBannedPokemon());
                }
                for (Encounter enc : area.getEncounters()) {
                    enc.setPokemon(pickWildPowerLvlReplacement(localAllowed, enc.getPokemon(), false, null));
                }
            }
        } else {
            // Entirely random
            for (EncounterSet area : scrambledEncounters) {
                for (Encounter enc : area.getEncounters()) {
                    do {
                        enc.setPokemon(noLegendaries ? randomNonLegendaryPokemon(true) : randomPokemon(true));
                    } while (banned.contains(enc.getPokemon()) || area.getBannedPokemon().contains(enc.getPokemon()));
                }
            }
        }

        setEncounters(useTimeOfDay, condenseSlots, currentEncounters);
    }

    @Override
    public void area1to1Encounters(boolean useTimeOfDay, boolean catchEmAll, boolean ceaReasonableOnly, boolean typeThemed,
                                   boolean usePowerLevels, boolean noLegendaries) {
        checkPokemonRestrictions();
        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay, false);
        List<Pokemon> banned = this.bannedForWildEncounters();

        // New: randomize the order encounter sets are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterSet> scrambledEncounters = new ArrayList<>(currentEncounters);
        Collections.shuffle(scrambledEncounters, this.random);

        // Assume EITHER catch em all OR type themed for now
        if (catchEmAll) {
            List<Pokemon> allPokes = noLegendaries ? new ArrayList<>(noLegendaryList) : new ArrayList<>(
                    mainPokemonList);
            allPokes.removeAll(banned);
            if (ceaReasonableOnly) {
                // put all reasonable sets first so they get at least one loop of pokemon
                // (provided there are enough of them)
                scrambledEncounters.sort(new Comparator<EncounterSet>() {
                    @Override
                    public int compare(EncounterSet o1, EncounterSet o2) {
                        if (o1.isReasonable() == o2.isReasonable()) {
                            return 0;
                        } else if (o1.isReasonable()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
            }
            for (EncounterSet area : scrambledEncounters) {
                // Poke-set
                Set<Pokemon> inArea = pokemonInArea(area);
                // Build area map using catch em all
                Map<Pokemon, Pokemon> areaMap = new TreeMap<>();
                List<Pokemon> pickablePokemon = allPokes;
                if (!area.getBannedPokemon().isEmpty()) {
                    pickablePokemon = new ArrayList<>(allPokes);
                    pickablePokemon.removeAll(area.getBannedPokemon());
                }
                for (Pokemon areaPk : inArea) {
                    if (pickablePokemon.isEmpty()) {
                        // No more pickable pokes left, take a random one
                        List<Pokemon> tempPickable = noLegendaries ? new ArrayList<>(noLegendaryList)
                                : new ArrayList<>(mainPokemonList);
                        tempPickable.removeAll(banned);
                        tempPickable.removeAll(area.getBannedPokemon());
                        if (tempPickable.isEmpty()) {
                            throw new RandomizationException("ERROR: Couldn't replace a wild Pokemon!");
                        }
                        int picked = this.random.nextInt(tempPickable.size());
                        Pokemon pickedMN = tempPickable.get(picked);
                        areaMap.put(areaPk, pickedMN);
                    } else {
                        int picked = this.random.nextInt(allPokes.size());
                        Pokemon pickedMN = allPokes.get(picked);
                        areaMap.put(areaPk, pickedMN);
                        pickablePokemon.remove(pickedMN);
                        if (allPokes != pickablePokemon) {
                            allPokes.remove(pickedMN);
                        }
                        if (allPokes.isEmpty()) {
                            // Start again
                            allPokes.addAll(noLegendaries ? noLegendaryList : mainPokemonList);
                            allPokes.removeAll(banned);
                            if (pickablePokemon != allPokes) {
                                pickablePokemon.addAll(allPokes);
                                pickablePokemon.removeAll(area.getBannedPokemon());
                            }
                        }
                    }
                }
                for (Encounter enc : area.getEncounters()) {
                    // Apply the map
                    enc.setPokemon(areaMap.get(enc.getPokemon()));
                }
            }
        } else if (typeThemed) {
            Map<Type, List<Pokemon>> cachedPokeLists = new TreeMap<>();
            for (EncounterSet area : scrambledEncounters) {
                // Poke-set
                Set<Pokemon> inArea = pokemonInArea(area);
                List<Pokemon> possiblePokemon = null;
                int iterLoops = 0;
                while (possiblePokemon == null && iterLoops < 10000) {
                    Type areaTheme = randomType();
                    if (!cachedPokeLists.containsKey(areaTheme)) {
                        List<Pokemon> pType = pokemonOfType(areaTheme, noLegendaries);
                        pType.removeAll(banned);
                        cachedPokeLists.put(areaTheme, pType);
                    }
                    possiblePokemon = new ArrayList<Pokemon>(cachedPokeLists.get(areaTheme));
                    if (!area.getBannedPokemon().isEmpty()) {
                        possiblePokemon.removeAll(area.getBannedPokemon());
                    }
                    if (possiblePokemon.size() < inArea.size()) {
                        // Can't use this type for this area
                        possiblePokemon = null;
                    }
                    iterLoops++;
                }
                if (possiblePokemon == null) {
                    throw new RandomizationException("Could not randomize an area in a reasonable amount of attempts.");
                }

                // Build area map using type theme.
                Map<Pokemon, Pokemon> areaMap = new TreeMap<Pokemon, Pokemon>();
                for (Pokemon areaPk : inArea) {
                    int picked = this.random.nextInt(possiblePokemon.size());
                    Pokemon pickedMN = possiblePokemon.get(picked);
                    areaMap.put(areaPk, pickedMN);
                    possiblePokemon.remove(picked);
                }
                for (Encounter enc : area.getEncounters()) {
                    // Apply the map
                    enc.setPokemon(areaMap.get(enc.getPokemon()));
                }
            }
        } else if (usePowerLevels) {
            List<Pokemon> allowedPokes = noLegendaries ? new ArrayList<Pokemon>(noLegendaryList)
                    : new ArrayList<>(mainPokemonList);
            allowedPokes.removeAll(banned);
            for (EncounterSet area : scrambledEncounters) {
                // Poke-set
                Set<Pokemon> inArea = pokemonInArea(area);
                // Build area map using randoms
                Map<Pokemon, Pokemon> areaMap = new TreeMap<Pokemon, Pokemon>();
                List<Pokemon> usedPks = new ArrayList<Pokemon>();
                List<Pokemon> localAllowed = allowedPokes;
                if (!area.getBannedPokemon().isEmpty()) {
                    localAllowed = new ArrayList<Pokemon>(allowedPokes);
                    localAllowed.removeAll(area.getBannedPokemon());
                }
                for (Pokemon areaPk : inArea) {
                    Pokemon picked = pickWildPowerLvlReplacement(localAllowed, areaPk, false, usedPks);
                    areaMap.put(areaPk, picked);
                    usedPks.add(picked);
                }
                for (Encounter enc : area.getEncounters()) {
                    // Apply the map
                    enc.setPokemon(areaMap.get(enc.getPokemon()));
                }
            }
        } else {
            // Entirely random
            for (EncounterSet area : scrambledEncounters) {
                // Poke-set
                Set<Pokemon> inArea = pokemonInArea(area);
                // Build area map using randoms
                Map<Pokemon, Pokemon> areaMap = new TreeMap<Pokemon, Pokemon>();
                for (Pokemon areaPk : inArea) {
                    Pokemon picked = noLegendaries ? randomNonLegendaryPokemon(true) : randomPokemon(true);
                    while (areaMap.containsValue(picked) || banned.contains(picked)
                            || area.getBannedPokemon().contains(picked)) {
                        picked = noLegendaries ? randomNonLegendaryPokemon(true) : randomPokemon(true);
                    }
                    areaMap.put(areaPk, picked);
                }
                for (Encounter enc : area.getEncounters()) {
                    // Apply the map
                    enc.setPokemon(areaMap.get(enc.getPokemon()));
                }
            }
        }

        setEncounters(useTimeOfDay, false, currentEncounters);

    }

    @Override
    public void game1to1Encounters(boolean useTimeOfDay, boolean usePowerLevels, boolean noLegendaries) {
        checkPokemonRestrictions();
        // Build the full 1-to-1 map
        Map<Pokemon, Pokemon> translateMap = new TreeMap<>();
        List<Pokemon> remainingLeft = allPokemonWithoutNull();
        List<Pokemon> remainingRight = noLegendaries ? new ArrayList<>(noLegendaryList)
                : new ArrayList<>(mainPokemonList);
        List<Pokemon> banned = this.bannedForWildEncounters();
        // Banned pokemon should be mapped to themselves
        for (Pokemon bannedPK : banned) {
            translateMap.put(bannedPK, bannedPK);
            remainingLeft.remove(bannedPK);
            remainingRight.remove(bannedPK);
        }
        while (!remainingLeft.isEmpty()) {
            int pickedLeft = this.random.nextInt(remainingLeft.size());
            if (usePowerLevels) {
                Pokemon pickedLeftP = remainingLeft.remove(pickedLeft);
                Pokemon pickedRightP = null;
                if (remainingRight.size() == 1) {
                    // pick this (it may or may not be the same poke)
                    pickedRightP = remainingRight.get(0);
                } else {
                    // pick on power level with the current one blocked
                    pickedRightP = pickWildPowerLvlReplacement(remainingRight, pickedLeftP, true, null);
                }
                remainingRight.remove(pickedRightP);
                translateMap.put(pickedLeftP, pickedRightP);
            } else {
                int pickedRight = this.random.nextInt(remainingRight.size());
                Pokemon pickedLeftP = remainingLeft.remove(pickedLeft);
                Pokemon pickedRightP = remainingRight.get(pickedRight);
                while (pickedLeftP.getNumber() == pickedRightP.getNumber() && remainingRight.size() != 1) {
                    // Reroll for a different pokemon if at all possible
                    pickedRight = this.random.nextInt(remainingRight.size());
                    pickedRightP = remainingRight.get(pickedRight);
                }
                remainingRight.remove(pickedRight);
                translateMap.put(pickedLeftP, pickedRightP);
            }
            if (remainingRight.isEmpty()) {
                // restart
                remainingRight.addAll(noLegendaries ? noLegendaryList : mainPokemonList);
                remainingRight.removeAll(banned);
            }
        }

        // Map remaining to themselves just in case
        List<Pokemon> allPokes = allPokemonWithoutNull();
        for (Pokemon poke : allPokes) {
            if (!translateMap.containsKey(poke)) {
                translateMap.put(poke, poke);
            }
        }

        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay, false);

        for (EncounterSet area : currentEncounters) {
            for (Encounter enc : area.getEncounters()) {
                // Apply the map
                enc.setPokemon(translateMap.get(enc.getPokemon()));
                if (area.getBannedPokemon().contains(enc.getPokemon())) {
                    // Ignore the map and put a random non-banned poke
                    List<Pokemon> tempPickable = noLegendaries ? new ArrayList<Pokemon>(noLegendaryList)
                            : new ArrayList<>(mainPokemonList);
                    tempPickable.removeAll(banned);
                    tempPickable.removeAll(area.getBannedPokemon());
                    if (tempPickable.isEmpty()) {
                        throw new RandomizationException("ERROR: Couldn't replace a wild Pokemon!");
                    }
                    if (usePowerLevels) {
                        enc.setPokemon(pickWildPowerLvlReplacement(tempPickable, enc.getPokemon(), false, null));
                    } else {
                        int picked = this.random.nextInt(tempPickable.size());
                        enc.setPokemon(tempPickable.get(picked));
                    }
                }
            }
        }

        setEncounters(useTimeOfDay, false, currentEncounters);

    }

    @Override
    public void randomizeTrainerPokes(boolean usePowerLevels, boolean noLegendaries, boolean noEarlyWonderGuard,
                                      int levelModifier) {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();

        // New: randomize the order trainers are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<Trainer> scrambledTrainers = new ArrayList<>(currentTrainers);
        Collections.shuffle(scrambledTrainers, this.random);

        cachedReplacementLists = new TreeMap<>();
        cachedAllList = noLegendaries ? new ArrayList<>(noLegendaryList) : new ArrayList<>(mainPokemonList);

        // Fully random is easy enough - randomize then worry about rival
        // carrying starter at the end
        for (Trainer t : scrambledTrainers) {
            if (t.getTag() != null && t.getTag().equals("IRIVAL")) {
                continue; // skip
            }
            for (TrainerPokemon tp : t.getPokemon()) {
                boolean wgAllowed = (!noEarlyWonderGuard) || tp.getLevel() >= 20;
                tp.setPokemon(pickReplacement(tp.getPokemon(), usePowerLevels, null, noLegendaries, wgAllowed));
                tp.setResetMoves(true);
                if (levelModifier != 0) {
                    tp.setLevel(Math.min(100, (int) Math.round(tp.getLevel() * (1 + levelModifier / 100.0))));
                }
            }
        }

        // Save it all up
        this.setTrainers(currentTrainers);
    }

    @Override
    public void typeThemeTrainerPokes(boolean usePowerLevels, boolean weightByFrequency, boolean noLegendaries,
                                      boolean noEarlyWonderGuard, int levelModifier) {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        cachedReplacementLists = new TreeMap<>();
        cachedAllList = noLegendaries ? new ArrayList<>(noLegendaryList) : new ArrayList<>(
                mainPokemonList);
        typeWeightings = new TreeMap<>();
        totalTypeWeighting = 0;

        // Construct groupings for types
        // Anything starting with GYM or ELITE or CHAMPION is a group
        Set<Trainer> assignedTrainers = new TreeSet<>();
        Map<String, List<Trainer>> groups = new TreeMap<>();
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().equals("IRIVAL")) {
                continue; // skip
            }
            String group = t.getTag() == null ? "" : t.getTag();
            if (group.contains("-")) {
                group = group.substring(0, group.indexOf('-'));
            }
            if (group.startsWith("GYM") || group.startsWith("ELITE") || group.startsWith("CHAMPION")
                    || group.startsWith("THEMED")) {
                // Yep this is a group
                if (!groups.containsKey(group)) {
                    groups.put(group, new ArrayList<>());
                }
                groups.get(group).add(t);
                assignedTrainers.add(t);
            } else if (group.startsWith("GIO")) {
                // Giovanni has same grouping as his gym, gym 8
                if (!groups.containsKey("GYM8")) {
                    groups.put("GYM8", new ArrayList<>());
                }
                groups.get("GYM8").add(t);
                assignedTrainers.add(t);
            }
        }

        // Give a type to each group
        // Gym & elite types have to be unique
        // So do uber types, including the type we pick for champion
        Set<Type> usedGymTypes = new TreeSet<>();
        Set<Type> usedEliteTypes = new TreeSet<>();
        Set<Type> usedUberTypes = new TreeSet<>();
        for (String group : groups.keySet()) {
            List<Trainer> trainersInGroup = groups.get(group);
            // Shuffle ordering within group to promote randomness
            Collections.shuffle(trainersInGroup, random);
            Type typeForGroup = pickType(weightByFrequency, noLegendaries);
            if (group.startsWith("GYM")) {
                while (usedGymTypes.contains(typeForGroup)) {
                    typeForGroup = pickType(weightByFrequency, noLegendaries);
                }
                usedGymTypes.add(typeForGroup);
            }
            if (group.startsWith("ELITE")) {
                while (usedEliteTypes.contains(typeForGroup)) {
                    typeForGroup = pickType(weightByFrequency, noLegendaries);
                }
                usedEliteTypes.add(typeForGroup);
            }
            if (group.equals("CHAMPION")) {
                usedUberTypes.add(typeForGroup);
            }
            // Themed groups just have a theme, no special criteria
            for (Trainer t : trainersInGroup) {
                for (TrainerPokemon tp : t.getPokemon()) {
                    boolean wgAllowed = (!noEarlyWonderGuard) || tp.getLevel() >= 20;
                    tp.setPokemon(pickReplacement(tp.getPokemon(), usePowerLevels, typeForGroup, noLegendaries, wgAllowed));
                    tp.setResetMoves(true);
                    if (levelModifier != 0) {
                        tp.setLevel(Math.min(100, (int) Math.round(tp.getLevel() * (1 + levelModifier / 100.0))));
                    }
                }
            }
        }

        // New: randomize the order trainers are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<Trainer> scrambledTrainers = new ArrayList<>(currentTrainers);
        Collections.shuffle(scrambledTrainers, this.random);

        // Give a type to each unassigned trainer
        for (Trainer t : scrambledTrainers) {
            if (t.getTag() != null && t.getTag().equals("IRIVAL")) {
                continue; // skip
            }

            if (!assignedTrainers.contains(t)) {
                Type typeForTrainer = pickType(weightByFrequency, noLegendaries);
                // Ubers: can't have the same type as each other
                if (t.getTag() != null && t.getTag().equals("UBER")) {
                    while (usedUberTypes.contains(typeForTrainer)) {
                        typeForTrainer = pickType(weightByFrequency, noLegendaries);
                    }
                    usedUberTypes.add(typeForTrainer);
                }
                for (TrainerPokemon tp : t.getPokemon()) {
                    boolean shedAllowed = (!noEarlyWonderGuard) || tp.getLevel() >= 20;
                    tp.setPokemon(pickReplacement(tp.getPokemon(), usePowerLevels, typeForTrainer, noLegendaries, shedAllowed));
                    tp.setResetMoves(true);
                    if (levelModifier != 0) {
                        tp.setLevel(Math.min(100, (int) Math.round(tp.getLevel() * (1 + levelModifier / 100.0))));
                    }
                }
            }
        }

        // Save it all up
        this.setTrainers(currentTrainers);
    }

    @Override
    public void rivalCarriesStarter() {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        rivalCarriesStarterUpdate(currentTrainers, "RIVAL", 1);
        rivalCarriesStarterUpdate(currentTrainers, "FRIEND", 2);
        this.setTrainers(currentTrainers);
    }

    @Override
    public void forceFullyEvolvedTrainerPokes(int minLevel) {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t : currentTrainers) {
            for (TrainerPokemon tp : t.getPokemon()) {
                if (tp.getLevel() >= minLevel) {
                    Pokemon newPokemon = fullyEvolve(tp.getPokemon());
                    if (newPokemon != tp.getPokemon()) {
                        tp.setPokemon(newPokemon);
                        tp.setResetMoves(true);
                    }
                }
            }
        }
        this.setTrainers(currentTrainers);
    }

    // MOVE DATA
    // All randomizers don't touch move ID 165 (Struggle)
    // They also have other exclusions where necessary to stop things glitching.

    @Override
    public void randomizeMovePowers() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.getInternalId() != 165 && mv.getPower() >= 10) {
                // "Generic" damaging move to randomize power
                if (random.nextInt(3) != 2) {
                    // "Regular" move
                    mv.setPower(random.nextInt(11) * 5 + 50); // 50 ... 100
                } else {
                    // "Extreme" move
                    mv.setPower(random.nextInt(27) * 5 + 20); // 20 ... 150
                }
                // Tiny chance for massive power jumps
                for (int i = 0; i < 2; i++) {
                    if (random.nextInt(100) == 0) {
                        mv.setPower(mv.getPower() + 50);
                    }
                }

                if (mv.getHitCount() > 1) {
                    // Divide randomized power by average hit count, round to
                    // nearest 5
                    mv.setPower((int) (Math.round(mv.getPower() / mv.getHitCount() / 5) * 5));
                    if (mv.getPower() <= 0) {
                        mv.setPower(5);
                    }
                }
            }
        }
    }

    @Override
    public void randomizeMovePPs() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.getInternalId() != 165) {
                if (random.nextInt(3) != 2) {
                    // "average" PP: 15-25
                    mv.setPp(random.nextInt(3) * 5 + 15);
                } else {
                    // "extreme" PP: 5-40
                    mv.setPp(random.nextInt(8) * 5 + 5);
                }
            }
        }
    }

    @Override
    public void randomizeMoveAccuracies() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.getInternalId() != 165 && mv.getHitratio() >= 5) {
                // "Sane" accuracy randomization
                // Broken into three tiers based on original accuracy
                // Designed to limit the chances of 100% accurate OHKO moves and
                // keep a decent base of 100% accurate regular moves.

                if (mv.getHitratio() <= 50) {
                    // lowest tier (acc <= 50)
                    // new accuracy = rand(20...50) inclusive
                    // with a 10% chance to increase by 50%
                    mv.setHitratio(random.nextInt(7) * 5 + 20);
                    if (random.nextInt(10) == 0) {
                        mv.setHitratio((mv.getHitratio() * 3 / 2) / 5 * 5);
                    }
                } else if (mv.getHitratio() < 90) {
                    // middle tier (50 < acc < 90)
                    // count down from 100% to 20% in 5% increments with 20%
                    // chance to "stop" and use the current accuracy at each
                    // increment
                    // gives decent-but-not-100% accuracy most of the time
                    mv.setHitratio(100);
                    while (mv.getHitratio() > 20) {
                        if (random.nextInt(10) < 2) {
                            break;
                        }
                        mv.setHitratio(mv.getHitratio() - 5);
                    }
                } else {
                    // highest tier (90 <= acc <= 100)
                    // count down from 100% to 20% in 5% increments with 40%
                    // chance to "stop" and use the current accuracy at each
                    // increment
                    // gives high accuracy most of the time
                    mv.setHitratio(100);
                    while (mv.getHitratio() > 20) {
                        if (random.nextInt(10) < 4) {
                            break;
                        }
                        mv.setHitratio(mv.getHitratio() - 5);
                    }
                }
            }
        }
    }

    @Override
    public void randomizeMoveTypes() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.getInternalId() != 165 && mv.getType() != null) {
                mv.setType(randomType());
            }
        }
    }

    @Override
    public void randomizeMoveCategory() {
        if (!this.hasPhysicalSpecialSplit()) {
            return;
        }
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.getInternalId() != 165 && mv.getCategory() != MoveCategory.STATUS) {
                if (random.nextInt(2) == 0) {
                    mv.setCategory((mv.getCategory() == MoveCategory.PHYSICAL) ? MoveCategory.SPECIAL : MoveCategory.PHYSICAL);
                }
            }
        }

    }

    @Override
    public void randomizeMovesLearnt(boolean typeThemed, boolean noBroken, boolean forceFourStartingMoves,
                                     double goodDamagingProbability) {

        // Get current sets
        List<Integer> hms = this.getHMMoves();
        List<Move> allMoves = this.getMoves();

        @SuppressWarnings("unchecked")
        Set<Integer> allBanned = new HashSet<Integer>(noBroken ? this.getGameBreakingMoves() : Collections.EMPTY_SET);
        allBanned.addAll(hms);
        allBanned.addAll(this.getMovesBannedFromLevelup());

        // Build sets of moves
        List<Move> validMoves = new ArrayList<>();
        List<Move> validDamagingMoves = new ArrayList<>();
        Map<Type, List<Move>> validTypeMoves = new HashMap<>();
        Map<Type, List<Move>> validTypeDamagingMoves = new HashMap<>();

        for (Move mv : allMoves) {
            if (mv != null && !getBannedRandomMoves().contains(mv.getNumber()) && !allBanned.contains(mv.getNumber()) && mv.isValid()) {
                validMoves.add(mv);
                Type moveType = mv.getType();
                if (moveType != null) {
                    if (!validTypeMoves.containsKey(moveType)) {
                        validTypeMoves.put(moveType, new ArrayList<>());
                    }
                    validTypeMoves.get(moveType).add(mv);
                }

                if (!GlobalConstants.bannedForDamagingMove.contains(mv.getNumber())) {
                    if (mv.getPower() >= 2 * GlobalConstants.MIN_DAMAGING_MOVE_POWER
                            || (mv.getPower() >= GlobalConstants.MIN_DAMAGING_MOVE_POWER && mv.getHitratio() >= 90)) {
                        validDamagingMoves.add(mv);
                        if (moveType != null) {
                            if (!validTypeDamagingMoves.containsKey(moveType)) {
                                validTypeDamagingMoves.put(moveType, new ArrayList<>());
                            }
                            validTypeDamagingMoves.get(moveType).add(mv);
                        }
                    }
                }
            }
        }

        for (Pokemon pkmn : getPokemon()) {
            Set<Integer> learnt = new TreeSet<>();
            List<MoveLearnt> moves = getMoveLearnts(forceFourStartingMoves, pkmn);

            // Find last lv1 move
            // lv1index ends up as the index of the first non-lv1 move
            int lv1index = 0;
            while (lv1index < moves.size() && moves.get(lv1index).getLevel() == 1) {
                lv1index++;
            }

            // last lv1 move is 1 before lv1index
            if (lv1index != 0) {
                lv1index--;
            }

            // Replace moves as needed
            for (int i = 0; i < moves.size(); i++) {

                // should this move be forced damaging?
                boolean attemptDamaging = i == lv1index || random.nextDouble() < goodDamagingProbability;

                // type themed?
                Type typeOfMove = null;
                if (typeThemed) {
                    double picked = random.nextDouble();
                    if (pkmn.getPrimaryType() == Type.NORMAL || pkmn.getSecondaryType() == Type.NORMAL) {
                        if (pkmn.getSecondaryType() == null) {
                            // Pure NORMAL: 75% normal, 25% random
                            if (picked < 0.75) {
                                typeOfMove = Type.NORMAL;
                            }
                            // else random
                        } else {
                            // Find the other type
                            // Normal/OTHER: 30% normal, 55% other, 15% random
                            Type otherType = pkmn.getPrimaryType();
                            if (otherType == Type.NORMAL) {
                                otherType = pkmn.getSecondaryType();
                            }
                            if (picked < 0.3) {
                                typeOfMove = Type.NORMAL;
                            } else if (picked < 0.85) {
                                typeOfMove = otherType;
                            }
                            // else random
                        }
                    } else if (pkmn.getSecondaryType() != null) {
                        // Primary/Secondary: 50% primary, 30% secondary, 5%
                        // normal, 15% random
                        if (picked < 0.5) {
                            typeOfMove = pkmn.getPrimaryType();
                        } else if (picked < 0.8) {
                            typeOfMove = pkmn.getSecondaryType();
                        } else if (picked < 0.85) {
                            typeOfMove = Type.NORMAL;
                        }
                        // else random
                    } else {
                        // Primary/None: 60% primary, 20% normal, 20% random
                        if (picked < 0.6) {
                            typeOfMove = pkmn.getPrimaryType();
                        } else if (picked < 0.8) {
                            typeOfMove = Type.NORMAL;
                        }
                        // else random
                    }
                }

                // select a list to pick a move from that has at least one free
                List<Move> pickList = validMoves;
                if (attemptDamaging) {
                    if (typeOfMove != null) {
                        if (validTypeDamagingMoves.containsKey(typeOfMove)
                                && checkForUnusedMove(validTypeDamagingMoves.get(typeOfMove), learnt)) {
                            pickList = validTypeDamagingMoves.get(typeOfMove);
                        } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                            pickList = validDamagingMoves;
                        }
                    } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                        pickList = validDamagingMoves;
                    }
                } else if (typeOfMove != null) {
                    if (validTypeMoves.containsKey(typeOfMove)
                            && checkForUnusedMove(validTypeMoves.get(typeOfMove), learnt)) {
                        pickList = validTypeMoves.get(typeOfMove);
                    }
                }

                // now pick a move until we get a valid one
                Move mv = pickList.get(random.nextInt(pickList.size()));
                while (learnt.contains(mv.getNumber())) {
                    mv = pickList.get(random.nextInt(pickList.size()));
                }

                // write it
                moves.get(i).setMove(mv.getNumber());
                if (i == lv1index) {
                    // just in case, set this to lv1
                    moves.get(i).setLevel(1);
                }
                learnt.add(mv.getNumber());

            }
        }
        // Done, save
        this.writeMovesLearnt();

    }

    private static List<MoveLearnt> getMoveLearnts(boolean forceFourStartingMoves, Pokemon pkmn) {
        List<MoveLearnt> moves = pkmn.getLearnset();

        // 4 starting moves?
        if (forceFourStartingMoves) {

            // To make this work we just padded every learnest in the rom with blank moves
            if (moves.size() > 3) {
                moves.get(0).setLevel(1);
                moves.get(1).setLevel(1);
                moves.get(2).setLevel(1);
                moves.get(3).setLevel(1);
            } else {
                moves.forEach(m -> m.setLevel(1));
            }

        }
        return moves;
    }

    @Override
    public void orderDamagingMovesByDamage() {

        List<Move> allMoves = this.getMoves();
        for (Pokemon pkmn : getPokemon()) {
            List<MoveLearnt> learnset = pkmn.getLearnset();

            // Build up a list of damaging moves and their positions
            List<Integer> damagingMoveIndices = new ArrayList<>();
            List<Move> damagingMoves = new ArrayList<>();
            for (int i = 0; i < learnset.size(); i++) {
                MoveLearnt moveLearnt = learnset.get(i);
                Move mv = allMoves.get(moveLearnt.getMove() - 1);
                if (mv.getPower() > 1) {
                    // considered a damaging move for this purpose
                    damagingMoveIndices.add(i);
                    damagingMoves.add(mv);
                    assert moveLearnt.getMove() == mv.getInternalId();
                }
            }

            // Ties should be sorted randomly, so shuffle the list first.
            Collections.shuffle(damagingMoves, random);

            // Sort the damaging moves by power
            damagingMoves.sort(Comparator.comparingDouble(m -> m.getPower() * Math.max(1, m.getHitCount())));

            // Reassign damaging moves in the ordered positions
            for (int i = 0; i < damagingMoves.size(); i++) {
                Move move = damagingMoves.get(i);
                Integer damagingMoveIndex = damagingMoveIndices.get(i);
                learnset.get(damagingMoveIndex).setMove(move.getInternalId());
            }
        }

        // Done, save
        this.writeMovesLearnt();
    }

    @Override
    public void metronomeOnlyMode() {

        for (Pokemon pkmn : getPokemon()) {
            List<MoveLearnt> learnset = pkmn.getLearnset();
            learnset.forEach(l -> {
                l.setMove(GlobalConstants.METRONOME_MOVE);
                l.setLevel(1);
            });

            if (learnset.size() > 1) {
                learnset.get(1).setMove(GlobalConstants.LEVEL_UP_MOVE_END);
                learnset.get(1).setLevel(0);
            }
        }

        // trainers
        // run this to remove all custom non-Metronome moves
        List<Trainer> trainers = this.getTrainers();

        for (Trainer t : trainers) {
            for (TrainerPokemon tpk : t.getPokemon()) {
                tpk.setResetMoves(true);
            }
        }

        this.setTrainers(trainers);

        // tms
        List<Integer> tmMoves = this.getTMMoves();

        Collections.fill(tmMoves, GlobalConstants.METRONOME_MOVE);

        this.setTMMoves(tmMoves);

        // movetutors
        if (this.hasMoveTutors()) {
            List<Integer> mtMoves = this.getMoveTutorMoves();

            Collections.fill(mtMoves, GlobalConstants.METRONOME_MOVE);

            this.setMoveTutorMoves(mtMoves);
        }

        // move tweaks
        List<Move> moveData = this.getMoves();

        Move metronome = moveData.get(GlobalConstants.METRONOME_MOVE);

        metronome.setPp(40);

        List<Integer> hms = this.getHMMoves();

        for (int hm : hms) {
            Move thisHM = moveData.get(hm);
            thisHM.setPp(0);
        }

        this.writeMovesLearnt();
    }

    @Override
    public void randomizeStaticPokemon(boolean legendForLegend) {
        // Load
        checkPokemonRestrictions();
        List<Pokemon> currentStaticPokemon = this.getStaticPokemon();
        List<Pokemon> replacements = new ArrayList<>();
        List<Pokemon> banned = this.bannedForStaticPokemon();

        if (legendForLegend) {
            List<Pokemon> legendariesLeft = new ArrayList<>(onlyLegendaryList);
            List<Pokemon> nonlegsLeft = new ArrayList<>(noLegendaryList);
            legendariesLeft.removeAll(banned);
            nonlegsLeft.removeAll(banned);
            for (Pokemon old : currentStaticPokemon) {
                Pokemon newPK;
                if (old.isLegendary()) {
                    newPK = legendariesLeft.remove(this.random.nextInt(legendariesLeft.size()));
                    if (legendariesLeft.isEmpty()) {
                        legendariesLeft.addAll(onlyLegendaryList);
                        legendariesLeft.removeAll(banned);
                    }
                } else {
                    newPK = nonlegsLeft.remove(this.random.nextInt(nonlegsLeft.size()));
                    if (nonlegsLeft.isEmpty()) {
                        nonlegsLeft.addAll(noLegendaryList);
                        nonlegsLeft.removeAll(banned);
                    }
                }
                replacements.add(newPK);
            }
        } else {
            List<Pokemon> pokemonLeft = new ArrayList<>(mainPokemonList).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            pokemonLeft.removeAll(banned);
            for (int i = 0; i < currentStaticPokemon.size(); i++) {
                Pokemon newPK = pokemonLeft.remove(this.random.nextInt(pokemonLeft.size()));
                if (pokemonLeft.isEmpty()) {
                    pokemonLeft.addAll(mainPokemonList);
                    pokemonLeft.removeAll(banned);
                }
                replacements.add(newPK);
            }
        }

        // Save
        this.setStaticPokemon(replacements);
    }

    @Override
    public void randomizeTMMoves(boolean noBroken, boolean preserveField, double goodDamagingProbability) {
        // Pick some random TM moves.
        int tmCount = this.getTMCount();
        List<Move> allMoves = this.getMoves();
        List<Integer> hms = this.getHMMoves();
        List<Integer> oldTMs = this.getTMMoves();
        @SuppressWarnings("unchecked")
        List<Integer> banned = new ArrayList<Integer>(noBroken ? this.getGameBreakingMoves() : Collections.EMPTY_LIST);
        // field moves?
        List<Integer> fieldMoves = this.getFieldMoves();
        int preservedFieldMoveCount = 0;

        if (preserveField) {
            List<Integer> banExistingField = new ArrayList<>(oldTMs);
            banExistingField.retainAll(fieldMoves);
            preservedFieldMoveCount = banExistingField.size();
            banned.addAll(banExistingField);
        }

        // Determine which moves are pickable
        List<Move> usableMoves = new ArrayList<>(allMoves);
        usableMoves.remove(0); // remove null entry
        Set<Move> unusableMoves = new HashSet<>();
        Set<Move> unusableDamagingMoves = new HashSet<>();

        for (Move mv : usableMoves) {
            int moveNumber = mv.getNumber();
            if (getBannedRandomMoves().contains(moveNumber) || hms.contains(moveNumber) || banned.contains(moveNumber)) {
                unusableMoves.add(mv);
            } else if (GlobalConstants.bannedForDamagingMove.contains(moveNumber)
                    || mv.getPower() < GlobalConstants.MIN_DAMAGING_MOVE_POWER) {
                unusableDamagingMoves.add(mv);
            }
        }

        usableMoves.removeAll(unusableMoves);
        List<Move> usableDamagingMoves = new ArrayList<>(usableMoves);
        usableDamagingMoves.removeAll(unusableDamagingMoves);

        // pick (tmCount - preservedFieldMoveCount) moves
        List<Integer> pickedMoves = new ArrayList<>();

        for (int i = 0; i < tmCount - preservedFieldMoveCount; i++) {
            Move chosenMove;
            if (random.nextDouble() < goodDamagingProbability && !usableDamagingMoves.isEmpty()) {
                chosenMove = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
            } else {
                chosenMove = usableMoves.get(random.nextInt(usableMoves.size()));
            }
            pickedMoves.add(chosenMove.getNumber());
            usableMoves.remove(chosenMove);
            usableDamagingMoves.remove(chosenMove);
        }

        // shuffle the picked moves because high goodDamagingProbability
        // could bias them towards early numbers otherwise

        Collections.shuffle(pickedMoves, random);

        // finally, distribute them as tms
        int pickedMoveIndex = 0;
        List<Integer> newTMs = new ArrayList<>();

        for (int i = 0; i < tmCount; i++) {
            if (preserveField && fieldMoves.contains(oldTMs.get(i))) {
                newTMs.add(oldTMs.get(i));
            } else {
                newTMs.add(pickedMoves.get(pickedMoveIndex++));
            }
        }

        this.setTMMoves(newTMs);
    }

    @Override
    public void randomizeTMHMCompatibility(Settings.TMsHMsCompatibilityMod mode) {

        int compatibilitySeed = 0;

        switch(mode)
        {
            case UNCHANGED:
                return;
            case FULL:
                compatibilitySeed = 1;
                break;
            case SAME_TYPE:
                compatibilitySeed = 2;
                break;
            case FIFTY_PERCENT:
                // Need to make sure the seed is above 2, and when added to species won't exceed max short
                compatibilitySeed = random.nextInt((Short.MAX_VALUE - 5000) + 1) + 100;
        }

        setTMHMCompatibility(compatibilitySeed);
    }

    @Override
    public void randomizeMoveTutorMoves(boolean noBroken, boolean preserveField, double goodDamagingProbability) {
        if (!this.hasMoveTutors()) {
            return;
        }

        // Pick some random Move Tutor moves, excluding TMs.
        List<Move> allMoves = this.getMoves();
        List<Integer> tms = this.getTMMoves();
        List<Integer> oldMTs = this.getMoveTutorMoves();
        int mtCount = oldMTs.size();
        List<Integer> hms = this.getHMMoves();
        @SuppressWarnings("unchecked")
        List<Integer> banned = new ArrayList<Integer>(noBroken ? this.getGameBreakingMoves() : Collections.EMPTY_LIST);

        // field moves?
        List<Integer> fieldMoves = this.getFieldMoves();
        int preservedFieldMoveCount = 0;
        if (preserveField) {
            List<Integer> banExistingField = new ArrayList<>(oldMTs);
            banExistingField.retainAll(fieldMoves);
            preservedFieldMoveCount = banExistingField.size();
            banned.addAll(banExistingField);
        }

        // Determine which moves are pickable
        List<Move> usableMoves = new ArrayList<>(allMoves);
        usableMoves.remove(0); // remove null entry
        Set<Move> unusableMoves = new HashSet<>();
        Set<Move> unusableDamagingMoves = new HashSet<>();

        for (Move mv : usableMoves) {
            int moveNumber = mv.getNumber();
            if (getBannedRandomMoves().contains(moveNumber) || tms.contains(moveNumber) || hms.contains(moveNumber)
                    || banned.contains(moveNumber)) {
                unusableMoves.add(mv);
            } else if (GlobalConstants.bannedForDamagingMove.contains(moveNumber)
                    || mv.getPower() < GlobalConstants.MIN_DAMAGING_MOVE_POWER) {
                unusableDamagingMoves.add(mv);
            }
        }

        usableMoves.removeAll(unusableMoves);
        List<Move> usableDamagingMoves = new ArrayList<>(usableMoves);
        usableDamagingMoves.removeAll(unusableDamagingMoves);

        // pick (tmCount - preservedFieldMoveCount) moves
        List<Integer> pickedMoves = new ArrayList<>();

        for (int i = 0; i < mtCount - preservedFieldMoveCount; i++) {
            Move chosenMove;
            if (random.nextDouble() < goodDamagingProbability && !usableDamagingMoves.isEmpty()) {
                chosenMove = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
            } else {
                chosenMove = usableMoves.get(random.nextInt(usableMoves.size()));
            }
            pickedMoves.add(chosenMove.getNumber());
            usableMoves.remove(chosenMove);
            usableDamagingMoves.remove(chosenMove);
        }

        // shuffle the picked moves because high goodDamagingProbability
        // could bias them towards early numbers otherwise

        Collections.shuffle(pickedMoves, random);

        // finally, distribute them as tutors
        int pickedMoveIndex = 0;
        List<Integer> newMTs = new ArrayList<>();

        for (int i = 0; i < mtCount; i++) {
            if (preserveField && fieldMoves.contains(oldMTs.get(i))) {
                newMTs.add(oldMTs.get(i));
            } else {
                newMTs.add(pickedMoves.get(pickedMoveIndex++));
            }
        }

        this.setMoveTutorMoves(newMTs);
    }

    @Override
    public void randomizeMoveTutorCompatibility(Settings.MoveTutorsCompatibilityMod mode) {

        int compatibilitySeed = 0;

        switch(mode)
        {
            case UNCHANGED:
                return;
            case FULL:
                compatibilitySeed = 1;
                break;
            case SAME_TYPE:
                compatibilitySeed = 2;
                break;
            case FIFTY_PERCENT:
                // Need to make sure the seed is above 2, and when added to species won't exceed max short
                compatibilitySeed = random.nextInt((Short.MAX_VALUE - 5000) + 1) + 100;
        }

        setMoveTutorCompatibility(compatibilitySeed);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void randomizeTrainerNames(CustomNamesSet customNames) {
        if (!this.canChangeTrainerText()) {
            return;
        }

        // index 0 = singles, 1 = doubles
        List<String>[] allTrainerNames = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        Map<Integer, List<String>>[] trainerNamesByLength = new Map[]{new TreeMap<Integer, List<String>>(),
                new TreeMap<Integer, List<String>>()};

        List<Character> bans = this.getBannedTrainerNameCharacters();

        List<String> repeatedTrainerNames = Arrays.asList("GRUNT", "EXECUTIVE", "SHADOW", "ADMIN", "GOON");

        // Read name lists
        for (String trainername : customNames.getTrainerNames()) {
            boolean okay = true;
            for (char banned : bans) {
                if (trainername.indexOf(banned) != -1) {
                    okay = false;
                    break;
                }
            }
            int len = this.internalStringLength(trainername);
            if (len <= 10 && okay) {
                allTrainerNames[0].add(trainername);
                if (trainerNamesByLength[0].containsKey(len)) {
                    trainerNamesByLength[0].get(len).add(trainername);
                } else {
                    List<String> namesOfThisLength = new ArrayList<>();
                    namesOfThisLength.add(trainername);
                    trainerNamesByLength[0].put(len, namesOfThisLength);
                }
            }
        }

        for (String trainername : customNames.getDoublesTrainerNames()) {
            boolean okay = true;
            for (char banned : bans) {
                if (trainername.indexOf(banned) != -1) {
                    okay = false;
                    break;
                }
            }
            int len = this.internalStringLength(trainername);
            if (len <= 10 && okay) {
                allTrainerNames[1].add(trainername);
                if (trainerNamesByLength[1].containsKey(len)) {
                    trainerNamesByLength[1].get(len).add(trainername);
                } else {
                    List<String> namesOfThisLength = new ArrayList<>();
                    namesOfThisLength.add(trainername);
                    trainerNamesByLength[1].put(len, namesOfThisLength);
                }
            }
        }

        // Get the current trainer names data
        List<String> currentTrainerNames = this.getTrainerNames();
        if (currentTrainerNames.isEmpty()) {
            // RBY have no trainer names
            return;
        }
        TrainerNameMode mode = this.trainerNameMode();
        int maxLength = this.maxTrainerNameLength();
        int totalMaxLength = this.maxSumOfTrainerNameLengths();

        boolean success = false;
        int tries = 0;

        // Init the translation map and new list
        Map<String, String> translation = new HashMap<>();
        List<String> newTrainerNames = new ArrayList<>();
        List<Integer> tcNameLengths = this.getTCNameLengthsByTrainer();

        // loop until we successfully pick names that fit
        // should always succeed first attempt except for gen2.
        while (!success && tries < 10000) {
            success = true;
            translation.clear();
            newTrainerNames.clear();
            int totalLength = 0;

            // Start choosing
            int tnIndex = -1;
            for (String trainerName : currentTrainerNames) {
                tnIndex++;
                if (translation.containsKey(trainerName) && !repeatedTrainerNames.contains(trainerName.toUpperCase())) {
                    // use an already picked translation
                    newTrainerNames.add(translation.get(trainerName));
                    totalLength += this.internalStringLength(translation.get(trainerName));
                } else {
                    int idx = trainerName.contains("&") ? 1 : 0;
                    List<String> pickFrom = allTrainerNames[idx];
                    int intStrLen = this.internalStringLength(trainerName);
                    if (mode == TrainerNameMode.SAME_LENGTH) {
                        pickFrom = trainerNamesByLength[idx].get(intStrLen);
                    }
                    String changeTo = trainerName;
                    int ctl = intStrLen;
                    if (pickFrom != null && !pickFrom.isEmpty() && intStrLen > 1) {
                        int innerTries = 0;
                        changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
                        ctl = this.internalStringLength(changeTo);
                        while ((mode == TrainerNameMode.MAX_LENGTH && ctl > maxLength)
                                || (mode == TrainerNameMode.MAX_LENGTH_WITH_CLASS && ctl + tcNameLengths.get(tnIndex) > maxLength)) {
                            innerTries++;
                            if (innerTries == 100) {
                                changeTo = trainerName;
                                ctl = intStrLen;
                                break;
                            }
                            changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
                            ctl = this.internalStringLength(changeTo);
                        }
                    }
                    translation.put(trainerName, changeTo);
                    newTrainerNames.add(changeTo);
                    totalLength += ctl;
                }

                if (totalLength > totalMaxLength) {
                    success = false;
                    tries++;
                    break;
                }
            }
        }

        if (!success) {
            throw new RandomizationException("Could not randomize trainer names in a reasonable amount of attempts."
                    + "\nPlease add some shorter names to your custom trainer names.");
        }

        // Done choosing, save
        this.setTrainerNames(newTrainerNames);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void randomizeTrainerClassNames(CustomNamesSet customNames) {
        if (!this.canChangeTrainerText()) {
            return;
        }

        // index 0 = singles, index 1 = doubles
        List<String>[] allTrainerClasses = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        Map<Integer, List<String>>[] trainerClassesByLength = new Map[]{new HashMap<Integer, List<String>>(),
                new HashMap<Integer, List<String>>()};

        // Read names data
        for (String trainerClassName : customNames.getTrainerClasses()) {
            allTrainerClasses[0].add(trainerClassName);
            int len = this.internalStringLength(trainerClassName);
            if (trainerClassesByLength[0].containsKey(len)) {
                trainerClassesByLength[0].get(len).add(trainerClassName);
            } else {
                List<String> namesOfThisLength = new ArrayList<>();
                namesOfThisLength.add(trainerClassName);
                trainerClassesByLength[0].put(len, namesOfThisLength);
            }
        }

        for (String trainerClassName : customNames.getDoublesTrainerClasses()) {
            allTrainerClasses[1].add(trainerClassName);
            int len = this.internalStringLength(trainerClassName);
            if (trainerClassesByLength[1].containsKey(len)) {
                trainerClassesByLength[1].get(len).add(trainerClassName);
            } else {
                List<String> namesOfThisLength = new ArrayList<>();
                namesOfThisLength.add(trainerClassName);
                trainerClassesByLength[1].put(len, namesOfThisLength);
            }
        }

        // Get the current trainer names data
        List<String> currentClassNames = this.getTrainerClassNames();
        boolean mustBeSameLength = this.fixedTrainerClassNamesLength();
        int maxLength = this.maxTrainerClassNameLength();

        // Init the translation map and new list
        Map<String, String> translation = new HashMap<>();
        List<String> newClassNames = new ArrayList<>();

        int numTrainerClasses = currentClassNames.size();
        List<Integer> doublesClasses = this.getDoublesTrainerClasses();

        // Start choosing
        for (int i = 0; i < numTrainerClasses; i++) {
            String trainerClassName = currentClassNames.get(i);
            if (translation.containsKey(trainerClassName)) {
                // use an already picked translation
                newClassNames.add(translation.get(trainerClassName));
            } else {
                int idx = doublesClasses.contains(i) ? 1 : 0;
                List<String> pickFrom = allTrainerClasses[idx];
                int intStrLen = this.internalStringLength(trainerClassName);
                if (mustBeSameLength) {
                    pickFrom = trainerClassesByLength[idx].get(intStrLen);
                }
                String changeTo = trainerClassName;
                if (pickFrom != null && !pickFrom.isEmpty()) {
                    do {
                        changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
                    } while (changeTo.length() > maxLength);
                }
                translation.put(trainerClassName, changeTo);
                newClassNames.add(changeTo);
            }
        }

        // Done choosing, save
        this.setTrainerClassNames(newClassNames);
    }

    @Override
    public void randomizeWildHeldItems(boolean banBadItems) {
        List<Pokemon> pokemon = allPokemonWithoutNull();
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        for (Pokemon pk : pokemon) {
            if (pk.getGuaranteedHeldItem() == -1 && pk.getCommonHeldItem() == -1 && pk.getRareHeldItem() == -1
                    && pk.getDarkGrassHeldItem() == -1) {
                // No held items at all, abort
                return;
            }
            boolean canHaveDarkGrass = pk.getDarkGrassHeldItem() != -1;
            double decision = this.random.nextDouble();
            if (pk.getGuaranteedHeldItem() != -1) {
                // Guaranteed held items are supported.
                if (pk.getGuaranteedHeldItem() > 0) {
                    // Currently have a guaranteed item
                    if (decision < 0.9) {
                        // Stay as guaranteed
                        canHaveDarkGrass = false;
                        pk.setGuaranteedHeldItem(possibleItems.randomItem(this.random));
                    } else {
                        // Change to 25% or 55% chance
                        pk.setGuaranteedHeldItem(0);
                        pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                        do {
                            pk.setRareHeldItem(possibleItems.randomItem(this.random));
                        } while (pk.getRareHeldItem() == pk.getCommonHeldItem());
                    }
                } else {
                    // No guaranteed item atm
                    if (decision < 0.5) {
                        // No held item at all
                        pk.setCommonHeldItem(0);
                        pk.setRareHeldItem(0);
                    } else if (decision < 0.65) {
                        // Just a rare item
                        pk.setCommonHeldItem(0);
                        pk.setRareHeldItem(possibleItems.randomItem(this.random));
                    } else if (decision < 0.8) {
                        // Just a common item
                        pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                        pk.setRareHeldItem(0);
                    } else if (decision < 0.95) {
                        // Both a common and rare item
                        pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                        do {
                            pk.setRareHeldItem(possibleItems.randomItem(this.random));
                        } while (pk.getRareHeldItem() == pk.getCommonHeldItem());
                    } else {
                        // Guaranteed item
                        canHaveDarkGrass = false;
                        pk.setGuaranteedHeldItem(possibleItems.randomItem(this.random));
                        pk.setCommonHeldItem(0);
                        pk.setRareHeldItem(0);
                    }
                }
            } else {
                // Code for no guaranteed items
                if (decision < 0.5) {
                    // No held item at all
                    pk.setCommonHeldItem(0);
                    pk.setRareHeldItem(0);
                } else if (decision < 0.65) {
                    // Just a rare item
                    pk.setCommonHeldItem(0);
                    pk.setRareHeldItem(possibleItems.randomItem(this.random));
                } else if (decision < 0.8) {
                    // Just a common item
                    pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                    pk.setRareHeldItem(0);
                } else {
                    // Both a common and rare item
                    pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                    do {
                        pk.setRareHeldItem(possibleItems.randomItem(this.random));
                    } while (pk.getRareHeldItem() == pk.getCommonHeldItem());
                }
            }

            if (canHaveDarkGrass) {
                double dgDecision = this.random.nextDouble();
                if (dgDecision < 0.5) {
                    // Yes, dark grass item
                    pk.setDarkGrassHeldItem(possibleItems.randomItem(this.random));
                } else {
                    pk.setDarkGrassHeldItem(0);
                }
            } else if (pk.getDarkGrassHeldItem() != -1) {
                pk.setDarkGrassHeldItem(0);
            }
        }

    }

    @Override
    public void randomizeStarterHeldItems(boolean banBadItems) {
        List<Integer> oldHeldItems = this.getStarterHeldItems();
        List<Integer> newHeldItems = new ArrayList<Integer>();
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        for (int i = 0; i < oldHeldItems.size(); i++) {
            newHeldItems.add(possibleItems.randomItem(this.random));
        }
        this.setStarterHeldItems(newHeldItems);
    }

    @Override
    public void shuffleFieldItems() {
        List<ItemLocation> currentItems = this.getRegularFieldItems();
        List<FieldTM> currentTMs = this.getCurrentFieldTMs();

        Collections.shuffle(currentItems, this.random);
        Collections.shuffle(currentTMs, this.random);

        List<Integer> newItems = new ArrayList<>();
        List<Integer> newTMs = new ArrayList<>();

        for (ItemLocation il : currentItems) {
            newItems.add(il.getItem());
        }

        for (FieldTM tm : currentTMs) {
            newTMs.add(tm.getTm());
        }

        this.setRegularFieldItems(newItems);
        this.setFieldTMs(newTMs);
    }

    @Override
    public void randomizeFieldItems(boolean banBadItems) {
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        List<ItemLocation> currentItems = this.getRegularFieldItems();
        List<FieldTM> currentTMs = this.getCurrentFieldTMs();
        List<Integer> requiredTMs = this.getRequiredFieldTMs();

        int fieldItemCount = currentItems.size();
        int fieldTMCount = currentTMs.size();
        int reqTMCount = requiredTMs.size();
        int totalTMCount = this.getTMCount();

        List<Integer> newItems = new ArrayList<>();

        for (int i = 0; i < fieldItemCount; i++) {
            newItems.add(possibleItems.randomNonTM(this.random));
        }

        List<Integer> newTMs = new ArrayList<>(requiredTMs);

        for (int i = reqTMCount; i < fieldTMCount; i++) {
            while (true) {
                int tm = this.random.nextInt(totalTMCount) + 1;
                if (!newTMs.contains(tm)) {
                    newTMs.add(tm);
                    break;
                }
            }
        }

        Collections.shuffle(newItems, this.random);
        Collections.shuffle(newTMs, this.random);

        this.setRegularFieldItems(newItems);
        this.setFieldTMs(newTMs);
    }

    @Override
    public void randomizeIngameTrades(boolean randomizeRequest, boolean randomNickname, boolean randomOT,
                                      boolean randomStats, boolean randomItem, CustomNamesSet customNames) {
        checkPokemonRestrictions();
        // Process trainer names
        List<String> trainerNames = new ArrayList<>();
        // Check for the file
        if (randomOT) {
            int maxOT = this.maxTradeOTNameLength();
            for (String trainername : customNames.getTrainerNames()) {
                int len = this.internalStringLength(trainername);
                if (len <= maxOT && !trainerNames.contains(trainername)) {
                    trainerNames.add(trainername);
                }
            }
        }

        // Process nicknames
        List<String> nicknames = new ArrayList<>();
        // Check for the file
        if (randomNickname) {
            int maxNN = this.maxTradeNicknameLength();
            for (String nickname : customNames.getPokemonNicknames()) {
                int len = this.internalStringLength(nickname);
                if (len <= maxNN && !nicknames.contains(nickname)) {
                    nicknames.add(nickname);
                }
            }
        }

        // get old trades
        List<IngameTrade> trades = this.getIngameTrades();
        List<Pokemon> usedRequests = new ArrayList<>();
        List<Pokemon> usedGivens = new ArrayList<>();
        List<String> usedOTs = new ArrayList<>();
        List<String> usedNicknames = new ArrayList<>();
        ItemList possibleItems = this.getAllowedItems();

        int nickCount = nicknames.size();
        int trnameCount = trainerNames.size();

        for (IngameTrade trade : trades) {
            // pick new given pokemon
            Pokemon oldgiven = trade.getGivenPokemon();
            Pokemon given = this.randomPokemon(true);
            int currentTry = 0;
            while (usedGivens.contains(given) && !(currentTry >= 100)) {
                given = this.randomPokemon(true);
                currentTry++;
            }
            usedGivens.add(given);
            trade.setGivenPokemon(given);

            // requested pokemon?
            if (oldgiven == trade.getRequestedPokemon()) {
                // preserve trades for the same pokemon
                trade.setRequestedPokemon(given);
            } else if (randomizeRequest) {
                Pokemon request = this.randomPokemon(true);
                currentTry = 0;
                while ((usedRequests.contains(request) || request == given) && !(currentTry >= 100)) {
                    request = this.randomPokemon(true);
                    currentTry++;
                }
                usedRequests.add(request);
                trade.setRequestedPokemon(request);
            }

            // nickname?
            if (randomNickname && nickCount > usedNicknames.size()) {
                String nickname = nicknames.get(this.random.nextInt(nickCount));
                while (usedNicknames.contains(nickname)) {
                    nickname = nicknames.get(this.random.nextInt(nickCount));
                }
                usedNicknames.add(nickname);
                trade.setNickname(nickname);
            } else if (trade.getNickname().equalsIgnoreCase(oldgiven.getName())) {
                // change the name for sanity
                trade.setNickname(trade.getGivenPokemon().getName());
            }

            if (randomOT && trnameCount > usedOTs.size()) {
                String ot = trainerNames.get(this.random.nextInt(trnameCount));
                while (usedOTs.contains(ot)) {
                    ot = trainerNames.get(this.random.nextInt(trnameCount));
                }
                usedOTs.add(ot);
                trade.setOtName(ot);
                trade.setOtId(this.random.nextInt(65536));
            }

            if (randomStats) {
                int maxIV = this.hasDVs() ? 16 : 32;
                for (int i = 0; i < trade.getIvs().length; i++) {
                    trade.setIv(i, this.random.nextInt(maxIV));
                }
            }

            if (randomItem) {
                trade.setItem(possibleItems.randomItem(this.random));
            }
        }

        // things that the game doesn't support should just be ignored
        this.setIngameTrades(trades);
    }

    @Override
    public void condenseLevelEvolutions(int maxLevel, int maxIntermediateLevel) {
        List<Pokemon> allPokemon = this.getPokemon();
        Set<Evolution> changedEvos = new TreeSet<>();
        // search for level evolutions
        for (Pokemon pk : allPokemon) {
            if (pk != null) {
                for (Evolution checkEvo : pk.getEvolutionsFrom()) {
                    if (checkEvo.getType().usesLevel()) {
                        // bring down the level of this evo if it exceeds max
                        // level
                        if (checkEvo.getExtraInfo() > maxLevel) {
                            checkEvo.setExtraInfo(maxLevel);
                            changedEvos.add(checkEvo);
                        }
                        // Now, seperately, if an intermediate level evo is too
                        // high, bring it down
                        for (Evolution otherEvo : pk.getEvolutionsTo()) {
                            if (otherEvo.getType().usesLevel() && otherEvo.getExtraInfo() > maxIntermediateLevel) {
                                otherEvo.setExtraInfo(maxIntermediateLevel);
                                changedEvos.add(otherEvo);
                            }
                        }
                    }
                }
            }
        }
        // Log changes now that we're done (to avoid repeats)
        log("--Condensed Level Evolutions--");
        for (Evolution evol : changedEvos) {
            log(String.format("%s now evolves into %s at minimum level %d", evol.getFrom().getName(), evol.getTo().getName(),
                    evol.getExtraInfo()));
        }
        logBlankLine();

    }

    @Override
    public void randomizeEvolutions(boolean similarStrength, boolean sameType, boolean limitToThreeStages,
                                    boolean forceChange) {
        checkPokemonRestrictions();
        List<Pokemon> pokemonPool = new ArrayList<>(mainPokemonList);
        int stageLimit = limitToThreeStages ? 3 : 10;

        // Cache old evolutions for data later
        Map<Pokemon, List<Evolution>> originalEvos = new HashMap<>();
        for (Pokemon pk : pokemonPool) {
            originalEvos.put(pk, new ArrayList<>(pk.getEvolutionsFrom()));
        }

        Set<EvolutionPair> newEvoPairs = new HashSet<>();
        Set<EvolutionPair> oldEvoPairs = new HashSet<>();

        if (forceChange) {
            for (Pokemon pk : pokemonPool) {
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    oldEvoPairs.add(new EvolutionPair(ev.getFrom(), ev.getTo()));
                }
            }
        }

        List<Pokemon> replacements = new ArrayList<>();

        int loops = 0;
        while (loops < 1) {
            // Setup for this loop.
            boolean hadError = false;
            for (Pokemon pk : pokemonPool) {
                pk.getEvolutionsFrom().clear();
                pk.getEvolutionsTo().clear();
            }
            newEvoPairs.clear();

            // Shuffle pokemon list so the results aren't overly predictable.
            Collections.shuffle(pokemonPool, this.random);

            for (Pokemon fromPK : pokemonPool) {
                List<Evolution> oldEvos = originalEvos.get(fromPK);
                for (Evolution ev : oldEvos) {
                    // Pick a Pokemon as replacement
                    replacements.clear();

                    // Step 1: base filters
                    for (Pokemon pk : mainPokemonList) {
                        // Prevent evolving into oneself (mandatory)
                        if (pk == fromPK) {
                            continue;
                        }

                        // Force same EXP curve (mandatory)
                        if (pk.getGrowthCurve() != fromPK.getGrowthCurve()) {
                            continue;
                        }

                        EvolutionPair ep = new EvolutionPair(fromPK, pk);
                        // Prevent split evos choosing the same Pokemon
                        // (mandatory)
                        if (newEvoPairs.contains(ep)) {
                            continue;
                        }

                        // Prevent evolving into old thing if flagged
                        if (forceChange && oldEvoPairs.contains(ep)) {
                            continue;
                        }

                        // Prevent evolution that causes cycle (mandatory)
                        if (evoCycleCheck(fromPK, pk)) {
                            continue;
                        }

                        // Prevent evolution that exceeds stage limit
                        Evolution tempEvo = new Evolution(fromPK, pk, false, EvolutionType.EVO_NONE, 0);
                        fromPK.getEvolutionsFrom().add(tempEvo);
                        pk.getEvolutionsTo().add(tempEvo);
                        boolean exceededLimit = false;

                        Set<Pokemon> related = relatedPokemon(fromPK);

                        for (Pokemon pk2 : related) {
                            int numPreEvos = numPreEvolutions(pk2, stageLimit);
                            if (numPreEvos >= stageLimit) {
                                exceededLimit = true;
                                break;
                            } else if (numPreEvos == stageLimit - 1 && pk2.getEvolutionsFrom().isEmpty()
                                    && !originalEvos.get(pk2).isEmpty()) {
                                exceededLimit = true;
                                break;
                            }
                        }

                        fromPK.getEvolutionsFrom().remove(tempEvo);
                        pk.getEvolutionsTo().remove(tempEvo);

                        if (exceededLimit) {
                            continue;
                        }

                        // Passes everything, add as a candidate.
                        replacements.add(pk);
                    }

                    // If we don't have any candidates after Step 1, severe
                    // failure
                    // exit out of this loop and try again from scratch
                    if (replacements.isEmpty()) {
                        hadError = true;
                        break;
                    }

                    // Step 2: filter by type, if needed
                    if (replacements.size() > 1 && sameType) {
                        Set<Pokemon> includeType = new HashSet<>();
                        for (Pokemon pk : replacements) {
                            if (pk.getPrimaryType() == fromPK.getPrimaryType()
                                    || (fromPK.getSecondaryType() != null && pk.getPrimaryType() == fromPK.getSecondaryType())
                                    || (pk.getSecondaryType() != null && pk.getSecondaryType() == fromPK.getPrimaryType())
                                    || (fromPK.getSecondaryType() != null && pk.getSecondaryType() != null && pk.getSecondaryType() == fromPK.getSecondaryType())) {
                                includeType.add(pk);
                            }
                        }

                        if (!includeType.isEmpty()) {
                            replacements.retainAll(includeType);
                        }
                    }

                    // Step 3: pick - by similar strength or otherwise
                    Pokemon picked = null;

                    if (replacements.size() == 1) {
                        // Foregone conclusion.
                        picked = replacements.get(0);
                    } else if (similarStrength) {
                        picked = pickEvoPowerLvlReplacement(replacements, ev.getTo());
                    } else {
                        picked = replacements.get(this.random.nextInt(replacements.size()));
                    }

                    // Step 4: add it to the new evos pool
                    Evolution newEvo = new Evolution(fromPK, picked, ev.isCarryStats(), ev.getType(), ev.getExtraInfo());
                    fromPK.getEvolutionsFrom().add(newEvo);
                    picked.getEvolutionsTo().add(newEvo);
                    newEvoPairs.add(new EvolutionPair(fromPK, picked));
                }

                if (hadError) {
                    // No need to check the other Pokemon if we already errored
                    break;
                }
            }

            // If no error, done and return
            if (!hadError) {
                return;
            } else {
                loops++;
            }
        }

        // If we made it out of the loop, we weren't able to randomize evos.
        throw new RandomizationException("Not able to randomize evolutions in a sane amount of retries.");
    }

    @Override
    public void minimumCatchRate(int rateNonLegendary, int rateLegendary) {
        List<Pokemon> pokes = getPokemon();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            int minCatchRate = pkmn.isLegendary() ? rateLegendary : rateNonLegendary;
            pkmn.setCatchRate(Math.max(pkmn.getCatchRate(), minCatchRate));
        }

    }

    @Override
    public void standardizeEXPCurves() {
        List<Pokemon> pokes = getPokemon();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            pkmn.setGrowthCurve(pkmn.isLegendary() ? ExpCurve.SLOW : ExpCurve.MEDIUM_FAST);
        }
    }

    private Pokemon pickEvoPowerLvlReplacement(List<Pokemon> pokemonPool, Pokemon current) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        List<Pokemon> canPick = new ArrayList<Pokemon>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget && !canPick.contains(pk)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.get(this.random.nextInt(canPick.size()));
    }

    private static double getProbability(boolean preferSameType, Move mv, Pokemon pkmn) {
        double probability = 0.5;
        if (preferSameType) {
            Type moveType = mv.getType();
            if (pkmn.getPrimaryType().equals(moveType)
                    || (pkmn.getSecondaryType() != null && pkmn.getSecondaryType().equals(moveType))) {
                probability = 0.9;
            } else if (!(moveType != null && moveType.equals(Type.NORMAL))) {
                probability = 0.25;
            }
        }
        return probability;
    }

    private static class EvolutionPair {

        private final Pokemon from;
        private final Pokemon to;

        public EvolutionPair(Pokemon from, Pokemon to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EvolutionPair other = (EvolutionPair) obj;
            if (from == null) {
                if (other.from != null)
                    return false;
            } else if (!from.equals(other.from))
                return false;
            if (to == null) {
                return other.to == null;
            } else {
                return to.equals(other.to);
            }
        }
    }

    /**
     * Check whether adding an evolution from one Pokemon to another will cause
     * an evolution cycle.
     */
    private boolean evoCycleCheck(Pokemon from, Pokemon to) {
        Evolution tempEvo = new Evolution(from, to, false, EvolutionType.EVO_NONE, 0);
        from.getEvolutionsFrom().add(tempEvo);
        Set<Pokemon> visited = new HashSet<Pokemon>();
        Set<Pokemon> recStack = new HashSet<Pokemon>();
        boolean recur = isCyclic(from, visited, recStack);
        from.getEvolutionsFrom().remove(tempEvo);
        return recur;
    }

    private boolean isCyclic(Pokemon pk, Set<Pokemon> visited, Set<Pokemon> recStack) {
        if (!visited.contains(pk)) {
            visited.add(pk);
            recStack.add(pk);
            for (Evolution ev : pk.getEvolutionsFrom()) {
                if (!visited.contains(ev.getTo()) && isCyclic(ev.getTo(), visited, recStack)) {
                    return true;
                } else if (recStack.contains(ev.getTo())) {
                    return true;
                }
            }
        }
        recStack.remove(pk);
        return false;
    }

    private interface BasePokemonAction {
        void applyTo(Pokemon pk);
    }

    private interface EvolvedPokemonAction {
        void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo);
    }

    /**
     * Universal implementation for things that have "copy X up evolutions"
     * support.
     *
     * @param bpAction Method to run on all base or no-copy Pokemon
     * @param epAction Method to run on all evolved Pokemon with a linear chain of
     *                 single evolutions.
     */
    private void copyUpEvolutionsHelper(BasePokemonAction bpAction, EvolvedPokemonAction epAction) {
        List<Pokemon> allPokes = this.getPokemon();
        for (Pokemon pk : allPokes) {
            if (pk != null) {
                pk.setTemporaryFlag(false);
            }
        }

        // Get evolution data.
        Set<Pokemon> dontCopyPokes = RomFunctions.getBasicOrNoCopyPokemon(this);
        Set<Pokemon> middleEvos = RomFunctions.getMiddleEvolutions(this);

        for (Pokemon pk : dontCopyPokes) {
            bpAction.applyTo(pk);
            pk.setTemporaryFlag(true);
        }

        // go "up" evolutions looking for pre-evos to do first
        for (Pokemon pk : allPokes) {
            if (pk != null && !pk.isTemporaryFlag()) {
                // Non-randomized pokes at this point must have
                // a linear chain of single evolutions down to
                // a randomized poke.
                Stack<Evolution> currentStack = new Stack<>();
                Evolution ev = pk.getEvolutionsTo().get(0);
                while (!ev.getFrom().isTemporaryFlag()) {
                    currentStack.push(ev);
                    ev = ev.getFrom().getEvolutionsTo().get(0);
                }

                // Now "ev" is set to an evolution from a Pokemon that has had
                // the base action done on it to one that hasn't.
                // Do the evolution action for everything left on the stack.
                epAction.applyTo(ev.getFrom(), ev.getTo(), !middleEvos.contains(ev.getTo()));
                ev.getTo().setTemporaryFlag(true);
                while (!currentStack.isEmpty()) {
                    ev = currentStack.pop();
                    epAction.applyTo(ev.getFrom(), ev.getTo(), !middleEvos.contains(ev.getTo()));
                    ev.getTo().setTemporaryFlag(true);
                }
            }
        }
    }

    private boolean checkForUnusedMove(List<Move> potentialList, Set<Integer> alreadyUsed) {
        for (Move mv : potentialList) {
            if (!alreadyUsed.contains(mv.getNumber())) {
                return true;
            }
        }
        return false;
    }

    private List<Pokemon> pokemonOfType(Type type, boolean noLegendaries) {
        List<Pokemon> typedPokes = new ArrayList<>();
        for (Pokemon pk : mainPokemonList) {
            if (pk != null && (!noLegendaries || !pk.isLegendary())) {
                if (pk.getPrimaryType() == type || pk.getSecondaryType() == type) {
                    typedPokes.add(pk);
                }
            }
        }
        return typedPokes;
    }

    private List<Pokemon> allPokemonWithoutNull() {
        List<Pokemon> allPokes = new ArrayList<>(this.getPokemon());
        //allPokes.remove(0);
        return allPokes;
    }

    private Set<Pokemon> pokemonInArea(EncounterSet area) {
        Set<Pokemon> inArea = new TreeSet<>();
        for (Encounter enc : area.getEncounters()) {
            inArea.add(enc.getPokemon());
        }
        return inArea;
    }

    private Map<Type, Integer> typeWeightings;
    private int totalTypeWeighting;

    private Type pickType(boolean weightByFrequency, boolean noLegendaries) {
        if (totalTypeWeighting == 0) {
            // Determine weightings
            for (Type t : Type.values()) {
                int pkWithTyping = pokemonOfType(t, noLegendaries).size();
                typeWeightings.put(t, pkWithTyping);
                totalTypeWeighting += pkWithTyping;
            }
        }

        if (weightByFrequency) {
            int typePick = this.random.nextInt(totalTypeWeighting);
            int typePos = 0;
            for (Type t : typeWeightings.keySet()) {
                int weight = typeWeightings.get(t);
                if (typePos + weight > typePick) {
                    return t;
                }
                typePos += weight;
            }
            return null;
        } else {
            return randomType();
        }
    }

    private void rivalCarriesStarterUpdate(List<Trainer> currentTrainers, String prefix, int pokemonOffset) {
        // Find the highest rival battle #
        int highestRivalNum = 0;
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().startsWith(prefix)) {
                highestRivalNum = Math.max(highestRivalNum,
                        Integer.parseInt(t.getTag().substring(prefix.length(), t.getTag().indexOf('-'))));
            }
        }

        if (highestRivalNum == 0) {
            // This rival type not used in this game
            return;
        }

        // Get the starters
        // us 0 1 2 => them 0+n 1+n 2+n
        List<Pokemon> starters = this.getStarters();

        // Replace each starter as appropriate
        // Use level to determine when to evolve, not number anymore
        for (int i = 0; i < 3; i++) {
            // Rival's starters are pokemonOffset over from each of ours
            int starterToUse = (i + pokemonOffset) % 3;
            Pokemon thisStarter = starters.get(starterToUse);
            int timesEvolves = numEvolutions(thisStarter);
            // If a fully evolved pokemon, use throughout
            // Otherwise split by evolutions as appropriate
            if (timesEvolves == 0) {
                for (int j = 1; j <= highestRivalNum; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                }
            } else if (timesEvolves == 1) {
                int j = 1;
                for (; j <= highestRivalNum / 2; j++) {
                    if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 30) {
                        break;
                    }
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                }
                thisStarter = pickRandomEvolutionOf(thisStarter, false);
                for (; j <= highestRivalNum; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                }
            } else if (timesEvolves == 2) {
                int j = 1;
                for (; j <= highestRivalNum; j++) {
                    if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 16) {
                        break;
                    }
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                }
                thisStarter = pickRandomEvolutionOf(thisStarter, true);
                for (; j <= highestRivalNum; j++) {
                    if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 36) {
                        break;
                    }
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                }
                thisStarter = pickRandomEvolutionOf(thisStarter, false);
                for (; j <= highestRivalNum; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                }
            }
        }

    }

    private Pokemon pickRandomEvolutionOf(Pokemon base, boolean mustEvolveItself) {
        // Used for "rival carries starter"
        // Pick a random evolution of base Pokemon, subject to
        // "must evolve itself" if appropriate.
        List<Pokemon> candidates = new ArrayList<>();
        for (Evolution ev : base.getEvolutionsFrom()) {
            if (!mustEvolveItself || !ev.getTo().getEvolutionsFrom().isEmpty()) {
                candidates.add(ev.getTo());
            }
        }

        if (candidates.isEmpty()) {
            throw new RandomizationException("Random evolution called on a Pokemon without any usable evolutions.");
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private int getLevelOfStarter(List<Trainer> currentTrainers, String tag) {
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().equals(tag)) {
                // Bingo, get highest level
                // last pokemon is given priority +2 but equal priority
                // = first pokemon wins, so its effectively +1
                // If it's tagged the same we can assume it's the same team
                // just the opposite gender or something like that...
                // So no need to check other trainers with same tag.
                int highestLevel = t.getPokemon().get(0).getLevel();
                int trainerPkmnCount = t.getPokemon().size();
                for (int i = 1; i < trainerPkmnCount; i++) {
                    int levelBonus = (i == trainerPkmnCount - 1) ? 2 : 0;
                    if (t.getPokemon().get(i).getLevel() + levelBonus > highestLevel) {
                        highestLevel = t.getPokemon().get(i).getLevel();
                    }
                }
                return highestLevel;
            }
        }
        return 0;
    }

    private void changeStarterWithTag(List<Trainer> currentTrainers, String tag, Pokemon starter) {
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().equals(tag)) {
                // Bingo
                // Change the highest level pokemon, not the last.
                // BUT: last gets +2 lvl priority (effectively +1)
                // same as above, equal priority = earlier wins
                TrainerPokemon bestPoke = t.getPokemon().get(0);
                int trainerPkmnCount = t.getPokemon().size();
                for (int i = 1; i < trainerPkmnCount; i++) {
                    int levelBonus = (i == trainerPkmnCount - 1) ? 2 : 0;
                    if (t.getPokemon().get(i).getLevel() + levelBonus > bestPoke.getLevel()) {
                        bestPoke = t.getPokemon().get(i);
                    }
                }
                bestPoke.setPokemon(starter);
                bestPoke.setResetMoves(true);
            }
        }

    }

    // Return the max depth of pre-evolutions a Pokemon has
    private int numPreEvolutions(Pokemon pk, int maxInterested) {
        return numPreEvolutions(pk, 0, maxInterested);
    }

    private int numPreEvolutions(Pokemon pk, int depth, int maxInterested) {
        if (pk.getEvolutionsTo().isEmpty()) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxPreEvos = 0;
                for (Evolution ev : pk.getEvolutionsTo()) {
                    maxPreEvos = Math.max(maxPreEvos, numPreEvolutions(ev.getFrom(), depth + 1, maxInterested) + 1);
                }
                return maxPreEvos;
            }
        }
    }

    private int numEvolutions(Pokemon pk) {
        return numEvolutions(pk, 0, 2);
    }

    private int numEvolutions(Pokemon pk, int depth, int maxInterested) {
        if (pk.getEvolutionsFrom().isEmpty()) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxEvos = 0;
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    maxEvos = Math.max(maxEvos, numEvolutions(ev.getTo(), depth + 1, maxInterested) + 1);
                }
                return maxEvos;
            }
        }
    }

    private Pokemon fullyEvolve(Pokemon pokemon) {
        Set<Pokemon> seenMons = new HashSet<>();
        seenMons.add(pokemon);

        while (true) {
            if (pokemon.getEvolutionsFrom().isEmpty()) {
                // fully evolved
                break;
            }

            // check for cyclic evolutions from what we've already seen
            boolean cyclic = false;
            for (Evolution ev : pokemon.getEvolutionsFrom()) {
                if (seenMons.contains(ev.getTo())) {
                    // cyclic evolution detected - bail now
                    cyclic = true;
                    break;
                }
            }

            if (cyclic) {
                break;
            }

            // pick a random evolution to continue from
            pokemon = pokemon.getEvolutionsFrom().get(random.nextInt(pokemon.getEvolutionsFrom().size())).getTo();
            seenMons.add(pokemon);
        }

        return pokemon;
    }

    private Set<Pokemon> relatedPokemon(Pokemon original) {
        Set<Pokemon> results = new HashSet<Pokemon>();
        results.add(original);
        Queue<Pokemon> toCheck = new LinkedList<Pokemon>();
        toCheck.add(original);
        while (!toCheck.isEmpty()) {
            Pokemon check = toCheck.poll();
            for (Evolution ev : check.getEvolutionsFrom()) {
                if (!results.contains(ev.getTo())) {
                    results.add(ev.getTo());
                    toCheck.add(ev.getTo());
                }
            }
            for (Evolution ev : check.getEvolutionsTo()) {
                if (!results.contains(ev.getFrom())) {
                    results.add(ev.getFrom());
                    toCheck.add(ev.getFrom());
                }
            }
        }
        return results;
    }

    private Map<Type, List<Pokemon>> cachedReplacementLists;
    private List<Pokemon> cachedAllList;

    protected Pokemon pickReplacement(Pokemon current, boolean usePowerLevels, Type type, boolean noLegendaries,
                                    boolean wonderGuardAllowed) {
        List<Pokemon> pickFrom = cachedAllList;
        if (type != null && type != Type.NONE && type != Type.MYSTERY && type != Type.STELLAR) {
            if (!cachedReplacementLists.containsKey(type)) {
                cachedReplacementLists.put(type, pokemonOfType(type, noLegendaries));
            }
            pickFrom = cachedReplacementLists.get(type);
        }

        pickFrom = pickFrom.stream().filter(Objects::nonNull).collect(Collectors.toList());

        if (usePowerLevels) {
            // start with within 10% and add 5% either direction till we find
            // something
            int currentBST = current.bstForPowerLevels();
            int minTarget = currentBST - currentBST / 10;
            int maxTarget = currentBST + currentBST / 10;
            List<Pokemon> canPick = new ArrayList<>();
            int expandRounds = 0;
            while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 2)) {
                for (Pokemon pk : pickFrom) {
                    if (pk.bstForPowerLevels() >= minTarget
                            && pk.bstForPowerLevels() <= maxTarget
                            && (wonderGuardAllowed || (pk.getAbility1() != GlobalConstants.WONDER_GUARD_INDEX
                            && pk.getAbility2() != GlobalConstants.WONDER_GUARD_INDEX && pk.getAbility3() != GlobalConstants.WONDER_GUARD_INDEX))) {
                        canPick.add(pk);
                    }
                }
                minTarget -= currentBST / 20;
                maxTarget += currentBST / 20;
                expandRounds++;
            }
            return canPick.get(this.random.nextInt(canPick.size()));
        } else {
            if (wonderGuardAllowed) {
                return pickFrom.get(this.random.nextInt(pickFrom.size()));
            } else {
                Pokemon pk = pickFrom.get(this.random.nextInt(pickFrom.size()));
                while (pk.getAbility1() == GlobalConstants.WONDER_GUARD_INDEX
                        || pk.getAbility2() == GlobalConstants.WONDER_GUARD_INDEX
                        || pk.getAbility3() == GlobalConstants.WONDER_GUARD_INDEX) {
                    pk = pickFrom.get(this.random.nextInt(pickFrom.size()));
                }
                return pk;
            }
        }
    }

    private Pokemon pickWildPowerLvlReplacement(List<Pokemon> pokemonPool, Pokemon current, boolean banSamePokemon,
                                                List<Pokemon> usedUp) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        List<Pokemon> canPick = new ArrayList<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current) && (usedUp == null || !usedUp.contains(pk))
                        && !canPick.contains(pk)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.get(this.random.nextInt(canPick.size()));
    }

    /* Helper methods used by subclasses and/or this class */

    protected void checkPokemonRestrictions() {
        if (!restrictionsSet) {
            setPokemonPool(null);
        }
    }

    protected void log(String log) {
        if (logStream != null) {
            logStream.println(log);
        }
    }

    protected void logBlankLine() {
        if (logStream != null) {
            logStream.println();
        }
    }

    protected void logEvoChangeLevel(String pkFrom, String pkTo, int evoLevel) {
        if (logStream != null) {
            logStream.printf("Made %s evolve into %s at level %d", pkFrom, pkTo, evoLevel);
            logStream.println();
        }
    }

    protected void logEvoChangeLevelWithItem(String pkFrom, String pkTo, String itemName) {
        if (logStream != null) {
            logStream.printf("Made %s evolve into %s by leveling up holding %s", pkFrom, pkTo, itemName);
            logStream.println();
        }
    }

    protected void logEvoChangeStone(String pkFrom, String pkTo, String itemName) {
        if (logStream != null) {
            logStream.printf("Made %s evolve into %s using a %s", pkFrom, pkTo, itemName);
            logStream.println();
        }
    }

    /* Default Implementations */
    /* Used when a subclass doesn't override */
    /*
     * The implication here is that these WILL be overridden by at least one
     * subclass.
     */

    @Override
    public boolean canChangeStarters() {
        return true;
    }

    @Override
    public String abilityName(int number) {
        return "";
    }

    @Override
    public boolean hasTimeBasedEncounters() {
        // DEFAULT: no
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Pokemon> bannedForWildEncounters() {
        return (List<Pokemon>) Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getMovesBannedFromLevelup() {
        return (List<Integer>) Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Pokemon> bannedForStaticPokemon() {
        return (List<Pokemon>) Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Character> getBannedTrainerNameCharacters() {
        return (List<Character>) Collections.EMPTY_LIST;
    }

    @Override
    public int maxTrainerNameLength() {
        // default: no real limit
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxSumOfTrainerNameLengths() {
        // default: no real limit
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxTrainerClassNameLength() {
        // default: no real limit
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxTradeNicknameLength() {
        return 10;
    }

    @Override
    public int maxTradeOTNameLength() {
        return 7;
    }

    @Override
    public List<Integer> getGameBreakingMoves() {
        // Sonicboom & drage
        return customConfig.getGameBreakingMoves() != null ? customConfig.getGameBreakingMoves() : Arrays.asList(49, 82);
    }

    @Override
    public void writeCheckValueToROM(int value) {
        // do nothing
    }

    @Override
    public int miscTweaksAvailable() {
        // default: none
        return 0;
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        // default: do nothing
    }

    @Override
    public boolean canCondenseEncounterSlots() {
        // default: no
        return false;
    }

    protected List<Integer> getBannedRandomMoves() {
        return customConfig.getBannedRandomMoves() != null ? customConfig.getBannedRandomMoves() : GlobalConstants.bannedRandomMoves;
    }

    protected List<Integer> getBannedForDamagingMove() {
        return customConfig.getBannedForDamagingMoves() != null ? customConfig.getBannedForDamagingMoves() : GlobalConstants.bannedForDamagingMove;
    }


    protected List<Integer> battleTrappingAbilities() {
        return customConfig.getBattleTrappingAbilities() != null ? customConfig.getBattleTrappingAbilities() : GlobalConstants.battleTrappingAbilities;
    }


    protected List<Integer> negativeAbilities() {
        return customConfig.getNegativeAbilities() != null ? customConfig.getNegativeAbilities() : GlobalConstants.negativeAbilities;
    }
}
