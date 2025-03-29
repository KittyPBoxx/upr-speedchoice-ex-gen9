package com.dabomstew.pkrandom;

import com.dabomstew.pkrandom.pokemon.ItemList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dabomstew.pkrandom.constants.EmeraldEXConstants.getItemTypeRanges;

public class CustomConfig {

    private Config config;

    private Map<String, Integer> mons;

    private Map<String, Integer> abilities;

    private Map<String, Integer> items;

    private Map<String, Integer> moves;

    private ItemList allowedItems = null;
    private ItemList nonBadItems = null;
    private  List<Integer> bannedMonNumbers = null;
    private List<Integer> bannedPlayerMonNumbers = null;
    private List<Integer> gameBreakingMoves = null;
    private List<Integer> bannedRandomMoves = null;
    private List<Integer> bannedForDamagingMoves = null;
    private List<Integer> battleTrappingAbilities = null;
    private List<Integer> negativeAbilities = null;
    private List<Integer> legendaries = null;

    public void init() {
        List<int[]> itemTypeRanges = getItemTypeRanges();

        ItemList allowedItems =  new ItemList(833);
        config.getBannedItems().stream().map(i -> items.get(i)).forEach(i -> allowedItems.banSingles(i));
        allowedItems.configureGroups(itemTypeRanges);
        this.allowedItems = allowedItems;

        ItemList nonBadItems = this.allowedItems.copy();
        config.getBannedBadItems().stream().map(i -> items.get(i)).forEach(i -> nonBadItems.banSingles(i));
        nonBadItems.configureGroups(itemTypeRanges);
        this.nonBadItems = nonBadItems;

        this.bannedMonNumbers = config.getMonsBannedForEveryone().stream().map(i -> mons.get(i)).collect(Collectors.toList());

        this.bannedPlayerMonNumbers =  config.getMonsBannedForPlayer().stream().map(i -> mons.get(i)).collect(Collectors.toList());
        bannedPlayerMonNumbers.addAll(this.bannedMonNumbers);

        this.legendaries = config.getLegendaries().stream().map(i -> mons.get(i)).collect(Collectors.toList());

        this.gameBreakingMoves = config.getBannedGameBreakingMoves().stream().map(i -> moves.get(i)).collect(Collectors.toList());
        this.bannedRandomMoves = config.getBannedMoves().stream().map(i -> moves.get(i)).collect(Collectors.toList());
        this.bannedForDamagingMoves = config.getBannedDamagingMoves().stream().map(i -> moves.get(i)).collect(Collectors.toList());

        this.battleTrappingAbilities = config.getBannedTrappingAbilities().stream().map(i -> abilities.get(i)).collect(Collectors.toList());
        this.negativeAbilities = config.getBannedNegativeAbilities().stream().map(i -> abilities.get(i)).collect(Collectors.toList());
    }

    public Config getConfig() {
        return config;
    }

    public Map<String, Integer> getMons() {
        return mons;
    }

    public void setMons(Map<String, Integer> mons) {
        this.mons = mons;
    }

    public Map<String, Integer> getAbilities() {
        return abilities;
    }

    public void setAbilities(Map<String, Integer> abilities) {
        this.abilities = abilities;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public void setItems(Map<String, Integer> items) {
        this.items = items;
    }

    public Map<String, Integer> getMoves() {
        return moves;
    }

    public void setMoves(Map<String, Integer> moves) {
        this.moves = moves;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public ItemList getAllowedItems() {
        return allowedItems;
    }

    public ItemList getNonBadItems() {
        return nonBadItems;
    }

    public List<Integer> getBannedMonNumbers() {
        return bannedMonNumbers;
    }

    public List<Integer> getBannedPlayerMonNumbers() {
        return bannedPlayerMonNumbers;
    }

    public List<Integer> getGameBreakingMoves() {
        return gameBreakingMoves;
    }

    public List<Integer> getBannedRandomMoves() {
        return this.bannedRandomMoves;
    }

    public List<Integer> getBannedForDamagingMoves() {
        return this.bannedForDamagingMoves;
    }

    public List<Integer> getBattleTrappingAbilities() {
        return battleTrappingAbilities;
    }

    public List<Integer> getNegativeAbilities() {
        return negativeAbilities;
    }

    public List<Integer> getLegendaries() {
        return legendaries;
    }

    static class Config {
        private List<String> bannedItems;
        private List<String> bannedBadItems;
        private List<String> monsBannedForPlayer;
        private List<String> monsBannedForEveryone;
        private List<String> bannedNegativeAbilities;
        private List<String> bannedTrappingAbilities;
        private List<String> bannedMoves;
        private List<String> bannedDamagingMoves;
        private List<String> bannedGameBreakingMoves;
        private List<String> legendaries;

        public List<String> getBannedItems() {
            return bannedItems;
        }

        public void setBannedItems(List<String> bannedItems) {
            this.bannedItems = bannedItems;
        }

        public List<String> getBannedBadItems() {
            return bannedBadItems;
        }

        public void setBannedBadItems(List<String> bannedBadItems) {
            this.bannedBadItems = bannedBadItems;
        }

        public List<String> getMonsBannedForPlayer() {
            return monsBannedForPlayer;
        }

        public void setMonsBannedForPlayer(List<String> monsBannedForPlayer) {
            this.monsBannedForPlayer = monsBannedForPlayer;
        }

        public List<String> getMonsBannedForEveryone() {
            return monsBannedForEveryone;
        }

        public void setMonsBannedForEveryone(List<String> monsBannedForEveryone) {
            this.monsBannedForEveryone = monsBannedForEveryone;
        }

        public List<String> getBannedNegativeAbilities() {
            return bannedNegativeAbilities;
        }

        public void setBannedNegativeAbilities(List<String> bannedNegativeAbilities) {
            this.bannedNegativeAbilities = bannedNegativeAbilities;
        }

        public List<String> getBannedTrappingAbilities() {
            return bannedTrappingAbilities;
        }

        public void setBannedTrappingAbilities(List<String> bannedTrappingAbilities) {
            this.bannedTrappingAbilities = bannedTrappingAbilities;
        }

        public List<String> getBannedMoves() {
            return bannedMoves;
        }

        public void setBannedMoves(List<String> bannedMoves) {
            this.bannedMoves = bannedMoves;
        }

        public List<String> getBannedGameBreakingMoves() {
            return bannedGameBreakingMoves;
        }

        public void setBannedGameBreakingMoves(List<String> bannedGameBreakingMoves) {
            this.bannedGameBreakingMoves = bannedGameBreakingMoves;
        }

        public List<String> getBannedDamagingMoves() {
            return bannedDamagingMoves;
        }

        public void setBannedDamagingMoves(List<String> bannedDamagingMoves) {
            this.bannedDamagingMoves = bannedDamagingMoves;
        }

        public List<String> getLegendaries() {
            return legendaries;
        }

        public void setLegendaries(List<String> legendaries) {
            this.legendaries = legendaries;
        }
    }
}
