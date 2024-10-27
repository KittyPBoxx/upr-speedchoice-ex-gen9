package com.dabomstew.pkrandom.romhandlers.emeraldex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RomEntry {

    private String name;
    private String romCode;
    private String tableFile;
    private String hash;
    private int version;
    private boolean copyStaticPokemon;
    private final Map<String, Integer> entries = new HashMap<>();
    private final Map<String, int[]> arrayEntries = new HashMap<>();
    private final List<StaticPokemon> staticPokemon = new ArrayList<>();
    private final List<TutorMove> tutorMoves = new ArrayList<>();
    private final Map<String, String> codeTweaks = new HashMap<>();
    private final List<GivenItem> givenItems = new ArrayList<>();
    private final List<Mart> marts = new ArrayList<>();

    public RomEntry() {
        this.hash = null;
    }

    public RomEntry(RomEntry toCopy) {
        this.name = toCopy.name;
        this.romCode = toCopy.romCode;
        this.tableFile = toCopy.tableFile;
        this.version = toCopy.version;
        this.copyStaticPokemon = toCopy.copyStaticPokemon;
        this.entries.putAll(toCopy.entries);
        this.arrayEntries.putAll(toCopy.arrayEntries);
        this.staticPokemon.addAll(toCopy.staticPokemon);
        this.tutorMoves.addAll(toCopy.tutorMoves);
        this.codeTweaks.putAll(toCopy.codeTweaks);
        this.givenItems.addAll(toCopy.givenItems);
        this.marts.addAll(toCopy.marts);
        this.hash = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRomCode() {
        return romCode;
    }

    public void setRomCode(String romCode) {
        this.romCode = romCode;
    }

    public String getTableFile() {
        return tableFile;
    }

    public void setTableFile(String tableFile) {
        this.tableFile = tableFile;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isCopyStaticPokemon() {
        return copyStaticPokemon;
    }

    public void setCopyStaticPokemon(boolean copyStaticPokemon) {
        this.copyStaticPokemon = copyStaticPokemon;
    }

    public Map<String, Integer> getEntries() {
        return entries;
    }

    public Map<String, int[]> getArrayEntries() {
        return arrayEntries;
    }

    public List<StaticPokemon> getStaticPokemon() {
        return staticPokemon;
    }

    public Map<String, String> getCodeTweaks() {
        return codeTweaks;
    }

    public List<GivenItem> getGivenItems() {
        return givenItems;
    }

    public List<TutorMove> getTutorMoves() {
        return tutorMoves;
    }

    public List<Mart> getMarts() {
        return marts;
    }

    public int getValue(String key) {
        if (!entries.containsKey(key)) {
            entries.put(key, 0);
        }
        return entries.get(key);
    }
}
