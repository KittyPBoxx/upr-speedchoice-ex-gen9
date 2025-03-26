package com.dabomstew.pkrandom.romhandlers.emeraldex;

/*----------------------------------------------------------------------------*/
/*--  Gen3RomHandler.java - randomizer handler for R/S/E/FR/LG.             --*/
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
import com.dabomstew.pkrandom.constants.EmeraldEXConstants;
import com.dabomstew.pkrandom.constants.GlobalConstants;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.AbstractGBRomHandler;
import com.dabomstew.pkrandom.DSDecmp;
import com.dabomstew.pkrandom.warps.WarpRemapping;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.dabomstew.pkrandom.constants.EmeraldEXConstants.ItemConstants.*;


public class EmeraldEXRomHandler extends AbstractGBRomHandler {

    private final List<RomEntry> roms;

    public EmeraldEXRomHandler(Random random, PrintStream logStream, List<RomEntry> roms) {
        super(random, logStream);
        this.roms = roms;
    }

    private void loadTextTable(String filename) {
        try {
            Scanner sc = new Scanner(FileFunctions.openConfig(filename + ".tbl"), StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String q = sc.nextLine();
                if (!q.trim().isEmpty()) {
                    String[] r = q.split("=", 2);
                    if (r[1].endsWith("\r\n")) {
                        r[1] = r[1].substring(0, r[1].length() - 2);
                    }
                    tb[Integer.parseInt(r[0], 16)] = r[1];
                    d.put(r[1], (byte) Integer.parseInt(r[0], 16));
                }
            }
            sc.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    // This ROM's data
    private Pokemon[] pokes;
    private Pokemon[] pokesInternal;
    private List<Pokemon> pokemonList;
    private int numRealPokemon;
    private Move[] moves;
    private RomEntry romEntry;
    private boolean havePatchedObedience;
    private String[] tb;
    private Map<String, Byte> d;
    private String[] abilityNames;
    private String[] itemNames;
    private boolean mapLoadingDone;
    private List<ItemLocationInner> itemOffs;
    private String[][] mapNames;
    private boolean isRomHack;
    private int[] internalToPokedex;
    private int[] pokedexToInternal;
    private int pokedexCount;
    private String[] pokeNames;
    private ItemList allowedItems;
    private ItemList nonBadItems;
    private List<Integer> freeItems = new ArrayList<>();
    private Map<Type, TypeInteractions> typeEffectivenessTable = new HashMap<>();

    @Override
    public boolean detectRom(byte[] rom) {
        return EmeraldExRomUtils.detectRomInner(rom, rom.length, roms);
    }

    @Override
    public void loadedRom() {
        for (RomEntry re : roms) {
            if (EmeraldExRomUtils.romCode(rom, re.getRomCode()) && (rom[0xBC] & 0xFF) == re.getVersion()) {
                romEntry = new RomEntry(re); // clone so we can modify
                break;
            }
        }

        tb = new String[256];
        d = new HashMap<>();
        isRomHack = false;

        romEntry.getEntries().put("MoveNames", readPointer(EmeraldEXConstants.efrlgMoveNamesPointer));
        romEntry.getEntries().put("AbilityNames", readPointer(EmeraldEXConstants.efrlgAbilityNamesPointer));
        romEntry.getEntries().put("ItemData", readPointer(EmeraldEXConstants.efrlgItemDataPointer));
        romEntry.getEntries().put("PokemonStats", readPointer(EmeraldEXConstants.efrlgPokemonStatsPointer));
        romEntry.getEntries().put("FrontSprites", readPointer(EmeraldEXConstants.efrlgFrontSpritesPointer));
        romEntry.getEntries().put("PokemonPalettes", readPointer(EmeraldEXConstants.efrlgPokemonPalettesPointer));
        romEntry.getEntries().put("MoveTutorCompatibility",
                romEntry.getValue("MoveTutorData") + romEntry.getValue("MoveTutorMoves") * 2);

        loadTextTable(romEntry.getTableFile());

        // We need to parse the species info
        loadPokemonNames();
        loadPokedex();
        loadPokemonStatsAndMoves();
        constructPokemonList();
        populateEvolutions();
        loadMoves();

        List<Integer> bannedMonNumbers = customConfig.getBannedPlayerMonNumbers() != null ?
                customConfig.getBannedPlayerMonNumbers() :
                Arrays.stream(EmeraldEXConstants.BannedEncounterMons.values())
                        .map(EmeraldEXConstants.BannedEncounterMons::getInternalSpeciesValue)
                        .collect(Collectors.toList());

        bannedForPlayer = pokemonList.stream()
                                     .filter(Objects::nonNull)
                                     .filter(p -> bannedMonNumbers.contains(p.getSpeciesNumber()))
                                     .collect(Collectors.toList());

        // map banks
        int mapBanksPtr = romEntry.getValue("MapBanksPtr");

        romEntry.getEntries().put("MapHeaders", mapBanksPtr);
        this.determineMapBankSizes();

        // map labels
        int mapLabelsPtr = romEntry.getValue("MapLabelsPtr");
        romEntry.getEntries().put("MapLabels", mapLabelsPtr);

        mapLoadingDone = false;
        loadAbilityNames();
        loadItemNames();
        loadTypeEffectivenessTable();

        allowedItems = customConfig.getAllowedItems() != null ? customConfig.getAllowedItems() : EmeraldEXConstants.allowedItems.copy();
        nonBadItems = customConfig.getNonBadItems() != null ? customConfig.getNonBadItems() : EmeraldEXConstants.nonBadItems.copy();
    }

    @Override
    public byte[] patchRomIfNeeded(byte[] rom) throws IOException {
        if (rom.length == 16 * 1024 * 1024) {
            return FileFunctions.applyPatch(rom, "SPDX-0.5.3a.xdelta");
        }

        return rom;
    }

    @Override
    public void savingRom() {
        savePokemonStats();
        saveMoves();
    }

    private void loadPokedex() {
        int numInternalPokes = romEntry.getValue("PokemonCount");
        int maxPokedex = numInternalPokes;
        internalToPokedex = new int[numInternalPokes + 1];
        pokedexToInternal = new int[numInternalPokes + 1];

        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");
        int dexOffsetInSpeciesInfo = 58;

        for (int i = 1; i <= numInternalPokes; i++) {
            int dexEntry = readWord(rom, speciesInfoOffset + dexOffsetInSpeciesInfo + (i * speciesInfoEntrySize));
            if (dexEntry != 0) {
                internalToPokedex[i] = dexEntry;
                // take the first pokemon only for each dex entry
                if (pokedexToInternal[dexEntry] == 0) {
                    pokedexToInternal[dexEntry] = i;
                }
                maxPokedex = Math.max(maxPokedex, dexEntry);
            }
        }
        this.pokedexCount = maxPokedex;
    }

    private void constructPokemonList() {
        pokemonList = Arrays.asList(pokes);
        numRealPokemon = pokemonList.size() - 1;
    }

    private void loadPokemonStatsAndMoves() {
        pokes = new Pokemon[this.pokedexCount + 1];
        int numInternalPokes = romEntry.getValue("PokemonCount");
        pokesInternal = new Pokemon[numInternalPokes + 1];
        for (int i = 1; i <= numInternalPokes; i++) {
            Pokemon pk = new Pokemon(customConfig.getLegendaries());
            pk.setName(pokeNames[i]);
            pk.setNumber(internalToPokedex[i]);
            pk.setSpeciesNumber(i);
            if (pk.getNumber() != 0 && pokedexToInternal[pk.getNumber()] == i) {
                pokes[pk.getNumber()] = pk;
            }

            if (!EmeraldEXConstants.unimplementedPokemon.contains(i)) {
                pokesInternal[i] = pk;
                loadBasicPokeStatsAndMoves(pk, i);
            }
        }
    }

    private void savePokemonStats() {
        // Write pokemon names & stats
        int numInternalPokes = romEntry.getValue("PokemonCount");
        for (int i = 1; i <= numInternalPokes; i++) {
            Pokemon pk = pokesInternal[i];
            if (!EmeraldEXConstants.unimplementedPokemon.contains(i)) {
                saveBasicPokeStats(pk, i);
            }
        }

        writeEvolutions();
    }

    private void loadMoves() {
        int moveCount = romEntry.getValue("MoveCount");
        moves = new Move[moveCount + 1];

        int moveInfoOffset = romEntry.getValue("MoveInfoOffset");
        int moveInfoSize = romEntry.getValue("MoveInfoSize");

        int effectOffsetInStruct = 8;
        int shortTypeCategoryPowerOffset = 10;
        int shortAccuracyTargetOffset = 12;
        int ppOffset = 14;
        int longHitCountAndFlagsOffset = 20;

        for (int i = 1; i <= moveCount; i++) {

            int moveNamePtr = readPointer(moveInfoOffset + (moveInfoSize * i));

            moves[i] = new Move();
            moves[i].setName(readVariableLengthString(moveNamePtr));

            moves[i].setNumber(i);
            moves[i].setInternalId(i);

            moves[i].setEffectIndex(readWord(moveInfoOffset + (moveInfoSize * i) + effectOffsetInStruct));

            /*
             *  u16 type:5;
             *  u16 category:2;
             *  u16 power:9; // up to 511
             *  u16 accuracy:7;
             *  u16 target:9;
             */
            byte[] typeCategoryPowerAccuracyTarget = new byte[2];
            typeCategoryPowerAccuracyTarget[1] = rom[moveInfoOffset + (moveInfoSize * i) + shortTypeCategoryPowerOffset + 1];
            typeCategoryPowerAccuracyTarget[0] = rom[moveInfoOffset + (moveInfoSize * i) + shortTypeCategoryPowerOffset];

            int power = BitManipulationUtils.readValue(typeCategoryPowerAccuracyTarget, 5 + 2, 9);
            int type = BitManipulationUtils.readValue(typeCategoryPowerAccuracyTarget, 0, 5);
            moves[i].setPower(power);
            moves[i].setType(EmeraldEXConstants.byteToType(type));

            byte[] accuracyTarget = new byte[2];
            accuracyTarget[1] = rom[moveInfoOffset + (moveInfoSize * i) + shortAccuracyTargetOffset + 1];
            accuracyTarget[0] = rom[moveInfoOffset + (moveInfoSize * i) + shortAccuracyTargetOffset];
            int hitRatio = BitManipulationUtils.readValue(accuracyTarget, 0, 7);
            moves[i].setHitratio(hitRatio);


            moves[i].setPp(rom[moveInfoOffset + (moveInfoSize * i) + ppOffset]);

            /*
            *  s32 priority:4;
            *  u32 recoil:7;
            *  u32 strikeCount:4;
            *  u32 criticalHitStage:2;
            *  u32 alwaysCriticalHit:1;
            *  u32 numAdditionalEffects:2;
            *
            * ...
            *  LOADS OF VARIOUS FLAGS
            * ...
            */
            byte[] hitCountAndFlags = new byte[4];
            hitCountAndFlags[3] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 3];
            hitCountAndFlags[2] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 2];
            hitCountAndFlags[1] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 1];
            hitCountAndFlags[0] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset];
            int hitCount = BitManipulationUtils.readValue(hitCountAndFlags, 4 + 7, 4);

            moves[i].setHitCount(hitCount);

            // TODO: allow restricting a custom move set
            moves[i].setValid(true);
        }

    }

    private void saveMoves() {
        int moveCount = romEntry.getValue("MoveCount");

        int moveInfoOffset = romEntry.getValue("MoveInfoOffset");
        int moveInfoSize = romEntry.getValue("MoveInfoSize");

        int effectOffsetInStruct = 8;
        int shortTypeCategoryPowerOffset = 10;
        int shortAccuracyTargetOffset = 12;
        int ppOffset = 14;
        int longHitCountAndFlagsOffset = 20;

        for (int i = 1; i <= moveCount; i++) {

            Move move = moves[i];

            if (move == null) {
                continue;
            }

            writeWord(moveInfoOffset + (moveInfoSize * i) + effectOffsetInStruct, move.getEffectIndex());

            byte[] typeCategoryPowerAccuracyTarget = new byte[2];
            typeCategoryPowerAccuracyTarget[1] = rom[moveInfoOffset + (moveInfoSize * i) + shortTypeCategoryPowerOffset + 1];
            typeCategoryPowerAccuracyTarget[0] = rom[moveInfoOffset + (moveInfoSize * i) + shortTypeCategoryPowerOffset];
            BitManipulationUtils.writeValue(typeCategoryPowerAccuracyTarget, 5 + 2, 9, move.getPower());
            BitManipulationUtils.writeValue(typeCategoryPowerAccuracyTarget, 0, 5, EmeraldEXConstants.typeToByte(move.getType()));
            rom[moveInfoOffset + (moveInfoSize * i) + shortTypeCategoryPowerOffset + 1] = typeCategoryPowerAccuracyTarget[1];
            rom[moveInfoOffset + (moveInfoSize * i) + shortTypeCategoryPowerOffset] = typeCategoryPowerAccuracyTarget[0];

            byte[] accuracyTarget = new byte[2];
            accuracyTarget[1] = rom[moveInfoOffset + (moveInfoSize * i) + shortAccuracyTargetOffset + 1];
            accuracyTarget[0] = rom[moveInfoOffset + (moveInfoSize * i) + shortAccuracyTargetOffset];
            BitManipulationUtils.writeValue(accuracyTarget, 0, 7, (int) move.getHitratio());
            rom[moveInfoOffset + (moveInfoSize * i) + shortAccuracyTargetOffset + 1] = accuracyTarget[1];
            rom[moveInfoOffset + (moveInfoSize * i) + shortAccuracyTargetOffset] = accuracyTarget[0];

            rom[moveInfoOffset + (moveInfoSize * i) + ppOffset] = (byte) move.getPp();

            byte[] hitCountAndFlags = new byte[4];
            hitCountAndFlags[3] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 3];
            hitCountAndFlags[2] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 2];
            hitCountAndFlags[1] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 1];
            hitCountAndFlags[0] = rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset];
            BitManipulationUtils.writeValue(hitCountAndFlags, 4 + 7, 4, (int) move.getHitCount());
            rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 3] = hitCountAndFlags[3];
            rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 2] = hitCountAndFlags[2];
            rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset + 1] = hitCountAndFlags[1];
            rom[moveInfoOffset + (moveInfoSize * i) + longHitCountAndFlagsOffset] = hitCountAndFlags[0];

        }
    }

    public List<Move> getMoves() {
        return Arrays.stream(moves).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void loadBasicPokeStatsAndMoves(Pokemon pkmn, int speciesNum) {

        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");
        int offset = speciesInfoOffset + (speciesNum * speciesInfoEntrySize);

        pkmn.setHp(rom[offset + EmeraldEXConstants.bsHPOffset] & 0xFF);
        pkmn.setAttack(rom[offset + EmeraldEXConstants.bsAttackOffset] & 0xFF);
        pkmn.setDefense(rom[offset + EmeraldEXConstants.bsDefenseOffset] & 0xFF);
        pkmn.setSpeed(rom[offset + EmeraldEXConstants.bsSpeedOffset] & 0xFF);
        pkmn.setSpatk(rom[offset + EmeraldEXConstants.bsSpAtkOffset] & 0xFF);
        pkmn.setSpdef(rom[offset + EmeraldEXConstants.bsSpDefOffset] & 0xFF);
        // Type
        pkmn.setPrimaryType(EmeraldEXConstants.typeTable[rom[offset + EmeraldEXConstants.bsPrimaryTypeOffset] & 0xFF]);
        pkmn.setSecondaryType(EmeraldEXConstants.typeTable[rom[offset + EmeraldEXConstants.bsSecondaryTypeOffset] & 0xFF]);
        // Only one type?
        if (pkmn.getSecondaryType() == pkmn.getPrimaryType()) {
            pkmn.setSecondaryType(null);
        }
        pkmn.setCatchRate(rom[offset + EmeraldEXConstants.bsCatchRateOffset] & 0xFF);
        pkmn.setGrowthCurve(ExpCurve.fromByte(rom[offset + EmeraldEXConstants.bsGrowthCurveOffset]));
        // Abilities
        pkmn.setAbility1(readWord(offset + EmeraldEXConstants.bsAbility1Offset));
        pkmn.setAbility2(readWord(offset + EmeraldEXConstants.bsAbility2Offset));
        pkmn.setAbility3(readWord(offset + EmeraldEXConstants.bsAbility3Offset));

        // Held Items?
        int item1 = readWord(offset + EmeraldEXConstants.bsCommonHeldItemOffset);
        int item2 = readWord(offset + EmeraldEXConstants.bsRareHeldItemOffset);

        if (item1 == item2) {
            // guaranteed
            pkmn.setGuaranteedHeldItem(item1);
            pkmn.setCommonHeldItem(0);
            pkmn.setRareHeldItem(0);
        } else {
            pkmn.setGuaranteedHeldItem(0);
            pkmn.setCommonHeldItem(item1);
            pkmn.setRareHeldItem(item2);
        }
        pkmn.setDarkGrassHeldItem(-1);


        int learnsetPtrOffset = offset + EmeraldEXConstants.learnsetPtrOffset;
        int learnsetOffset = readPointer(learnsetPtrOffset);
        int levelupMoveSize = 4;

        List<MoveLearnt> movesLearnt = new ArrayList<>();

        for (int i = 0; i < EmeraldEXConstants.MAX_LEVEL_UP_LEARNSET; i++) {

            int move = readWord(learnsetOffset + (i * levelupMoveSize));
            int level = readWord(learnsetOffset + (i * levelupMoveSize) + 2);

            if (move == 0xFFFF) {
                break;
            }

            movesLearnt.add(new MoveLearnt(move, level, learnsetOffset + (i * levelupMoveSize)));

        }

        pkmn.setLearnset(movesLearnt);

        pkmn.setGenderRatio(rom[offset + EmeraldEXConstants.bsGenderRatioOffset] & 0xFF);
    }

    private void saveBasicPokeStats(Pokemon pkmn, int index) {
        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");
        int offset = speciesInfoOffset + (index * speciesInfoEntrySize);

        rom[offset + EmeraldEXConstants.bsHPOffset] = (byte) pkmn.getHp();
        rom[offset + EmeraldEXConstants.bsAttackOffset] = (byte) pkmn.getAttack();
        rom[offset + EmeraldEXConstants.bsDefenseOffset] = (byte) pkmn.getDefense();
        rom[offset + EmeraldEXConstants.bsSpeedOffset] = (byte) pkmn.getSpeed();
        rom[offset + EmeraldEXConstants.bsSpAtkOffset] = (byte) pkmn.getSpatk();
        rom[offset + EmeraldEXConstants.bsSpDefOffset] = (byte) pkmn.getSpdef();
        rom[offset + EmeraldEXConstants.bsPrimaryTypeOffset] = EmeraldEXConstants.typeToByte(pkmn.getPrimaryType());
        if (pkmn.getSecondaryType() == null) {
            rom[offset + EmeraldEXConstants.bsSecondaryTypeOffset] = rom[offset + EmeraldEXConstants.bsPrimaryTypeOffset];
        } else {
            rom[offset + EmeraldEXConstants.bsSecondaryTypeOffset] = EmeraldEXConstants.typeToByte(pkmn.getSecondaryType());
        }
        rom[offset + EmeraldEXConstants.bsCatchRateOffset] = (byte) pkmn.getCatchRate();
        rom[offset + EmeraldEXConstants.bsGrowthCurveOffset] = pkmn.getGrowthCurve().toByte();

        writeWord(offset + EmeraldEXConstants.bsAbility1Offset, pkmn.getAbility1());
        if (pkmn.getAbility2() == 0) {
            // required to not break evos with random ability
            writeWord(offset + EmeraldEXConstants.bsAbility2Offset, pkmn.getAbility1());
        } else {
            writeWord(offset + EmeraldEXConstants.bsAbility2Offset, pkmn.getAbility2());
        }
        if (pkmn.getAbility3() == 0) {
            // required to not break evos with random ability
            writeWord(offset + EmeraldEXConstants.bsAbility3Offset, pkmn.getAbility1());
        } else {
            writeWord(offset + EmeraldEXConstants.bsAbility3Offset, pkmn.getAbility3());
        }

        // Held items
        if (pkmn.getGuaranteedHeldItem() > 0) {
            writeWord(offset + EmeraldEXConstants.bsCommonHeldItemOffset, pkmn.getGuaranteedHeldItem());
            writeWord(offset + EmeraldEXConstants.bsRareHeldItemOffset, pkmn.getGuaranteedHeldItem());
        } else {
            writeWord(offset + EmeraldEXConstants.bsCommonHeldItemOffset, pkmn.getCommonHeldItem());
            writeWord(offset + EmeraldEXConstants.bsRareHeldItemOffset, pkmn.getRareHeldItem());
        }

        rom[offset + EmeraldEXConstants.bsGenderRatioOffset] = (byte) pkmn.getGenderRatio();
    }

    private void loadPokemonNames() {
        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");

        int offsetInSpeciesInfo = 0x1F + 13;

        int nameLen = romEntry.getValue("PokemonNameLength");
        int numInternalPokes = romEntry.getValue("PokemonCount");
        pokeNames = new String[numInternalPokes + 1];
        for (int i = 1; i <= numInternalPokes; i++) {
            pokeNames[i] = readFixedLengthString((speciesInfoOffset + offsetInSpeciesInfo) + (i * speciesInfoEntrySize), nameLen);
        }
    }

    private String readString(int offset, int maxLength) {
        StringBuilder string = new StringBuilder();
        for (int c = 0; c < maxLength; c++) {
            int currChar = rom[offset + c] & 0xFF;
            if (tb[currChar] != null) {
                string.append(tb[currChar]);
            } else {
                if (currChar == EmeraldEXConstants.textTerminator) {
                    break;
                } else if (currChar == EmeraldEXConstants.textVariable) {
                    int nextChar = rom[offset + c + 1] & 0xFF;
                    string.append("\\v").append(String.format("%02X", nextChar));
                    c++;
                } else {
                    string.append("\\x").append(String.format("%02X", currChar));
                }
            }
        }
        return string.toString();
    }

    private byte[] translateString(String text) {
        List<Byte> data = new ArrayList<>();
        while (!text.isEmpty()) {
            int i = Math.max(0, 4 - text.length());
            if (text.charAt(0) == '\\' && text.charAt(1) == 'x') {
                data.add((byte) Integer.parseInt(text.substring(2, 4), 16));
                text = text.substring(4);
            } else if (text.charAt(0) == '\\' && text.charAt(1) == 'v') {
                data.add((byte) EmeraldEXConstants.textVariable);
                data.add((byte) Integer.parseInt(text.substring(2, 4), 16));
                text = text.substring(4);
            } else {
                while (!(d.containsKey(text.substring(0, 4 - i)) || (i == 4))) {
                    i++;
                }
                if (i == 4) {
                    text = text.substring(1);
                } else {
                    data.add(d.get(text.substring(0, 4 - i)));
                    text = text.substring(4 - i);
                }
            }
        }
        byte[] ret = new byte[data.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = data.get(i);
        }
        return ret;
    }

    private String readFixedLengthString(int offset, int length) {
        return readString(offset, length);
    }

    public String readVariableLengthString(int offset) {
        return readString(offset, Integer.MAX_VALUE);
    }

    private void writeFixedLengthString(String str, int offset, int length) {
        byte[] translated = translateString(str);
        int len = Math.min(translated.length, length);
        System.arraycopy(translated, 0, rom, offset, len);
        if (len < length) {
            rom[offset + len] = (byte) EmeraldEXConstants.textTerminator;
            len++;
        }
        while (len < length) {
            rom[offset + len] = 0;
            len++;
        }
    }

    private void writeVariableLengthString(String str, int offset) {
        byte[] translated = translateString(str);
        System.arraycopy(translated, 0, rom, offset, translated.length);
        rom[offset + translated.length] = (byte) 0xFF;
    }

    private int readPointer(int offset) {
        return readLong(offset) - 0x8000000;
    }

    private int readLong(int offset) {
        return (rom[offset] & 0xFF) + ((rom[offset + 1] & 0xFF) << 8) + ((rom[offset + 2] & 0xFF) << 16)
                + (((rom[offset + 3] & 0xFF)) << 24);
    }

    private void writePointer(int offset, int pointer) {
        writeLong(offset, pointer + 0x8000000);
    }

    private void writeLong(int offset, int value) {
        rom[offset] = (byte) (value & 0xFF);
        rom[offset + 1] = (byte) ((value >> 8) & 0xFF);
        rom[offset + 2] = (byte) ((value >> 16) & 0xFF);
        rom[offset + 3] = (byte) (((value >> 24) & 0xFF));
    }

    @Override
    public List<Pokemon> getStarters() {
        List<Pokemon> starters = new ArrayList<Pokemon>();
        int baseOffset = romEntry.getValue("StarterPokemon");
        // do something
        Pokemon starter1 = pokesInternal[readWord(baseOffset)];
        Pokemon starter2 = pokesInternal[readWord(baseOffset + EmeraldEXConstants.rseStarter2Offset)];
        Pokemon starter3 = pokesInternal[readWord(baseOffset + EmeraldEXConstants.rseStarter3Offset)];
        starters.add(starter1);
        starters.add(starter2);
        starters.add(starter3);
        return starters;
    }

    @Override
    public boolean setStarters(List<Pokemon> newStarters) {
        if (newStarters.size() != 3) {
            return false;
        }

        int baseOffset = romEntry.getValue("StarterPokemon");

        int starter0 = newStarters.get(0).getSpeciesNumber();
        int starter1 = newStarters.get(1).getSpeciesNumber();
        int starter2 = newStarters.get(2).getSpeciesNumber();

        // US
        // order: 0, 1, 2
        writeWord(baseOffset, starter0);
        writeWord(baseOffset + EmeraldEXConstants.rseStarter2Offset, starter1);
        writeWord(baseOffset + EmeraldEXConstants.rseStarter3Offset, starter2);
        return true;

    }

    @Override
    public List<Integer> getStarterHeldItems() {
        List<Integer> sHeldItems = new ArrayList<>();
        int baseOffset = romEntry.getValue("StaticVars") + (EmeraldEXConstants.STARTER_ITEM_INDEX * 2);
        int i1 = rom[baseOffset] & 0xFF;
        int i2 = rom[baseOffset + 2] & 0xFF;
        if (i2 == 0) {
            sHeldItems.add(i1);
        } else {
            sHeldItems.add(i2 + 0xFF);
        }
        return sHeldItems;
    }

    @Override
    public void setStarterHeldItems(List<Integer> items) {
        if (items.size() != 1) {
            return;
        }
        int item = items.get(0);
        int baseOffset = romEntry.getValue("StaticVars") + (EmeraldEXConstants.STARTER_ITEM_INDEX * 2);
        writeWord(baseOffset, item);
    }

    @Override
    public List<EncounterSet> getEncounters(boolean useTimeOfDay, boolean condenseSlots) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }

        int startOffs = romEntry.getValue("WildPokemon");
        List<EncounterSet> encounterAreas = new ArrayList<>();
        Set<Integer> seenOffsets = new TreeSet<>();
        int offs = startOffs;
        while (true) {
            // Read pointers
            int bank = rom[offs] & 0xFF;
            int map = rom[offs + 1] & 0xFF;
            if (bank == 0xFF && map == 0xFF) {
                break;
            }

            String mapName = mapNames[bank][map];

            for (EncounterSlot type : EncounterSlot.values()) {
                int typePtr = readPointer(offs + 4 + type.getIndex() * 4);
                if (typePtr >= 0 && typePtr < rom.length && rom[typePtr] != 0) {
                    int rate = rom[typePtr] & 0xFF;
                    if (useTimeOfDay) {
                        for (TimesOfDay time : TimesOfDay.values()) {
                            String areaName = String.format("%s %s (%s)", mapName, type.getName(), time.getName());
                            int dataOffset = readPointer(typePtr + 4 + time.getIndex() * 4);
                            if (!seenOffsets.contains(dataOffset)) {
                                if (type.getIndex() == 3) {
                                    encounterAreas
                                            .add(readWildAreaFishing(dataOffset, rate, type.getSlotEncounters(), areaName));
                                } else {
                                    encounterAreas.add(readWildArea(dataOffset, rate, type.getSlotEncounters(), areaName));
                                }
                                seenOffsets.add(dataOffset);
                            }
                        }
                    } else {
                        // Use Day only
                        String areaName = String.format("%s %s", mapName, type.getName());
                        int dataOffset = readPointer(typePtr + 8);
                        if (!seenOffsets.contains(dataOffset)) {
                            if (type.getIndex() == 3) {
                                encounterAreas
                                        .add(readWildAreaFishing(dataOffset, rate, type.getSlotEncounters(), areaName));
                            } else {
                                encounterAreas.add(readWildArea(dataOffset, rate, type.getSlotEncounters(), areaName));
                            }
                            seenOffsets.add(dataOffset);
                        }
                    }
                }
            }

            offs += (4 + (EncounterSlot.values().length * 4));
        }
        if (romEntry.getArrayEntries().containsKey("BattleTrappersBanned")) {
            // Some encounter sets aren't allowed to have Pokemon
            // with Arena Trap, Shadow Tag etc.
            int[] bannedAreas = romEntry.getArrayEntries().get("BattleTrappersBanned");
            Set<Pokemon> battleTrappers = new HashSet<>();
            for (Pokemon pk : getPokemon()) {
                if (hasBattleTrappingAbility(pk)) {
                    battleTrappers.add(pk);
                }
            }
            for (int areaIdx : bannedAreas) {
                encounterAreas.get(areaIdx).getBannedPokemon().addAll(battleTrappers);
            }
        }
        return encounterAreas;
    }

    private boolean hasBattleTrappingAbility(Pokemon pokemon) {
        return pokemon != null && (battleTrappingAbilities().contains(pokemon.getAbility1())
                || battleTrappingAbilities().contains(pokemon.getAbility2()));
    }

    private EncounterSet readWildArea(int dataOffset, int rate, int numOfEntries, String setName) {
        EncounterSet thisSet = new EncounterSet();
        thisSet.setRate(rate);
        thisSet.setDisplayName(setName);
        // Read the entries
        for (int i = 0; i < numOfEntries; i++) {
            // min, max, species, species
            Encounter enc = new Encounter();
            enc.setLevel(rom[dataOffset + i * 4]);
            enc.setMaxLevel(rom[dataOffset + i * 4 + 1]);
            enc.setPokemon(pokesInternal[readWord(dataOffset + i * 4 + 2)]);
            thisSet.getEncounters().add(enc);
        }
        return thisSet;
    }

    private EncounterSet readWildAreaFishing(int dataOffset, int rate, int numOfEntries, String setName) {
        EncounterSet thisSet = new EncounterSet();
        thisSet.setRate(rate);
        thisSet.setDisplayName(setName);
        // Grab the *real* pointer to data

        // Read the entries
        for (int i = 0; i < numOfEntries; i++) {
            // min, max, species, species
            Encounter enc = new Encounter();
            enc.setLevel(rom[dataOffset + i * 4]);
            enc.setMaxLevel(rom[dataOffset + i * 4 + 1]);
            enc.setPokemon(pokesInternal[readWord(dataOffset + i * 4 + 2)]);
            thisSet.getEncounters().add(enc);
        }
        return thisSet;
    }

    @Override
    public void setEncounters(boolean useTimeOfDay, boolean condenseSlots, List<EncounterSet> encounters) {

        int startOffs = romEntry.getValue("WildPokemon");
        Iterator<EncounterSet> encounterAreas = encounters.iterator();
        Set<Integer> seenOffsets = new TreeSet<>();
        int offs = startOffs;
        while (true) {
            // Read pointers
            int bank = rom[offs] & 0xFF;
            int map = rom[offs + 1] & 0xFF;
            if (bank == 0xFF && map == 0xFF) {
                break;
            }

            for (EncounterSlot type : EncounterSlot.values()) {
                int typePtr = readPointer(offs + 4 + type.getIndex() * 4);
                if (typePtr >= 0 && typePtr < rom.length && rom[typePtr] != 0) {
                    if (useTimeOfDay) {
                        for (TimesOfDay time : TimesOfDay.values()) {
                            int dataOffset = readPointer(typePtr + 4 + time.getIndex() * 4);
                            if (!seenOffsets.contains(dataOffset)) {
                                EncounterSet area = encounterAreas.next();
                                if (time.getIndex() == 0) {
                                    // write encounter rate from first data set
                                    // for each area/type combo
                                    rom[typePtr] = (byte) area.getRate();
                                }
                                if (type.getIndex() == 3) {
                                    writeWildAreaFishing(dataOffset, area);
                                } else {
                                    writeWildArea(dataOffset, type.getSlotEncounters(), area, type.getIndex() == 0);
                                }
                                seenOffsets.add(dataOffset);
                            }
                        }
                    } else {
                        // Use Day only and write it over all times
                        int dayDataOffset = readPointer(typePtr + 8);
                        if (!seenOffsets.contains(dayDataOffset)) {
                            EncounterSet area = encounterAreas.next();
                            rom[typePtr] = (byte) area.getRate();
                            for (TimesOfDay time : TimesOfDay.values()) {
                                int dataOffset = readPointer(typePtr + 4 + time.getIndex() * 4);
                                if (type.getIndex() == 3) {
                                    writeWildAreaFishing(dataOffset, area);
                                } else {
                                    writeWildArea(dataOffset, type.getSlotEncounters(), area, type.getIndex() == 0);
                                }
                            }
                            seenOffsets.add(dayDataOffset);
                        }
                    }
                }
            }

            offs += (4 + (EncounterSlot.values().length * 4));
        }
    }

    private void writeWildArea(int dataOffset, int numOfEntries, EncounterSet encounters, boolean duplicateSlots) {
        // Write the entries
        for (int i = 0; i < numOfEntries; i++) {
            Encounter enc = encounters.getEncounters().get(i);
            // min, max, species, species
            int speciesNumber = enc.getPokemon().getSpeciesNumber();
            writeWord(dataOffset + i * 4 + 2, speciesNumber);
            if (duplicateSlots) {
                writeWord(dataOffset + (i + numOfEntries) * 4 + 2, speciesNumber);
            }
        }
    }

    private void writeWildAreaFishing(int dataOffset, EncounterSet encounters) {
        int numOfEntries = 2;
        // Write the entries
        for (int i = 0; i < numOfEntries; i++) {
            Encounter enc = encounters.getEncounters().get(i);
            // min, max, species, species
            int speciesNumber = enc.getPokemon().getSpeciesNumber();
            writeWord(dataOffset + i * 4 + 2, speciesNumber);
            // Speedchoice duplication.. 4 extra times
            writeWord(dataOffset + (i + numOfEntries) * 4 + 2, speciesNumber);
            writeWord(dataOffset + (i + numOfEntries * 2) * 4 + 2, speciesNumber);
            writeWord(dataOffset + (i + numOfEntries * 3) * 4 + 2, speciesNumber);
            writeWord(dataOffset + (i + numOfEntries * 4) * 4 + 2, speciesNumber);
        }
    }

    @Override
    public List<Pokemon> bannedForWildEncounters() {
        return bannedForPlayer;
    }

    @Override
    public List<Trainer> getTrainers() {
        int baseOffset = romEntry.getValue("TrainerData");
        int amount = romEntry.getValue("TrainerCount");
        int entryLen = romEntry.getValue("TrainerEntrySize");
        List<Trainer> theTrainers = new ArrayList<>();
        List<String> tcnames = this.getTrainerClassNames();

        // Trainer Offsets
        int trainerClassOffset = 0x10;
        int trainerMusicAndGenderOffset = 0x11;
        int trainerPartySizeOffset = 0x20;
        int trainerPartyPtrOffset = 0x04;
        int trainerNameOffset = 0x13;
        // Only first bit is used for double battle, the other 7 are used for mugshot and starting status
        int trainerDoubleBattleOffset = 0x1E;

        // Trainer Mon Offsets
        int trainerMonSize = 0x20;
        int trainerMonIvOffset = 0x08;
        int trainerMonLevelOffset = 0x1A;
        int trainerMonSpeciesOffset = 0x14;
        int trainerMonHeldItemOffset = 0x16;
        int trainerMonMove1Offset = 0x0C;

        for (int i = 1; i < amount; i++) {
            int trOffset = baseOffset + i * entryLen;
            Trainer tr = new Trainer();
            tr.setOffset(trOffset);
            int trainerclass = rom[trOffset + trainerClassOffset] & 0xFF;

            // The last bit of music is used for trainer gender
            tr.setTrainerclass((rom[trOffset + trainerMusicAndGenderOffset] & 0x80) > 0 ? 1 : 0);

            int numPokes = rom[trOffset + trainerPartySizeOffset] & 0xFF;
            int pointerToPokes = readPointer(trOffset + trainerPartyPtrOffset);
            tr.setName(this.readVariableLengthString(trOffset + trainerNameOffset));
            tr.setFullDisplayName(tcnames.get(trainerclass) + " " + tr.getName());
            tr.setDoubleBattle((rom[trainerDoubleBattleOffset + 0x1E] & (1L << 1)) != 0);

            // Pokemon data!
            for (int poke = 0; poke < numPokes; poke++) {
                TrainerPokemon thisPoke = new TrainerPokemon();
                thisPoke.setAILevel(readWord(pointerToPokes + poke * trainerMonSize + trainerMonIvOffset));
                thisPoke.setLevel(readWord(pointerToPokes + poke * trainerMonSize + trainerMonLevelOffset));
                thisPoke.setPokemon(pokesInternal[readWord(pointerToPokes + poke * trainerMonSize + trainerMonSpeciesOffset)]);
                thisPoke.setHeldItem(readWord(pointerToPokes + poke * trainerMonSize + trainerMonHeldItemOffset));
                thisPoke.setMove1(readWord(pointerToPokes + poke * trainerMonSize + trainerMonMove1Offset));
                thisPoke.setMove2(readWord(pointerToPokes + poke * trainerMonSize + trainerMonMove1Offset + 2));
                thisPoke.setMove3(readWord(pointerToPokes + poke * trainerMonSize + trainerMonMove1Offset + 4));
                thisPoke.setMove4(readWord(pointerToPokes + poke * trainerMonSize + trainerMonMove1Offset + 6));
                tr.getPokemon().add(thisPoke);
            }
            theTrainers.add(tr);
        }
        EmeraldEXConstants.trainerTagsE(theTrainers);
        return theTrainers;
    }

    @Override
    public void setTrainers(List<Trainer> trainerData) {
        int baseOffset = romEntry.getValue("TrainerData");
        int amount = romEntry.getValue("TrainerCount");
        int entryLen = romEntry.getValue("TrainerEntrySize");
        Iterator<Trainer> theTrainers = trainerData.iterator();

        // TODO: Need to look into this because I want to avoid writing any extra data to the rom
        int fso = romEntry.getValue("FreeSpace");

        // Trainer Offsets
        int trainerPartySizeOffset = 0x20;
        int trainerPartyPtrOffset = 0x04;

        // Trainer Mon Offsets
        int trainerMonSize = 0x20;
        int trainerMonIvOffset = 0x08;
        int trainerMonLevelOffset = 0x1A;
        int trainerMonSpeciesOffset = 0x14;
        int trainerMonHeldItemOffset = 0x16;
        int trainerMonMove1Offset = 0x0C;

        for (int i = 1; i < amount; i++) {
            int trOffset = baseOffset + i * entryLen;
            Trainer tr = theTrainers.next();
            // Do we need to repoint this trainer's data?
            int oldPokeCount = rom[trOffset + trainerPartySizeOffset] & 0xFF;
            int newPokeCount = tr.getPokemon().size();

            int newDataSize = newPokeCount * trainerMonSize;
            int oldDataSize = oldPokeCount * trainerMonSize;

            // write out new data first...
            rom[trOffset + trainerPartySizeOffset] = (byte) newPokeCount;

            // now, do we need to repoint?
            int pointerToPokes;
            if (newDataSize > oldDataSize) {
                int writeSpace = RomFunctions.freeSpaceFinder(rom, EmeraldEXConstants.freeSpaceByte, newDataSize, fso,
                        true);
                if (writeSpace < fso) {
                    throw new RandomizerIOException("ROM is full");
                }
                writePointer(trOffset + trainerPartyPtrOffset, writeSpace);
                pointerToPokes = writeSpace;
            } else {
                pointerToPokes = readPointer(trOffset + trainerPartyPtrOffset);
            }

            Iterator<TrainerPokemon> pokes = tr.getPokemon().iterator();

            // Write out Pokemon data!
            for (int poke = 0; poke < newPokeCount; poke++) {
                TrainerPokemon tp = pokes.next();
                writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonIvOffset, tp.getLevel());
                writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonLevelOffset, tp.getLevel());
                writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonSpeciesOffset, tp.getPokemon().getSpeciesNumber());
                writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonHeldItemOffset, tp.getHeldItem());

                if (tp.isResetMoves()) {
                    int[] pokeMoves = RomFunctions.getMovesAtLevel(tp.getPokemon(), tp.getLevel());
                    for (int m = 0; m < 4; m++) {
                        writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonMove1Offset + m * 2, pokeMoves[m]);
                    }
                } else {
                    writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonMove1Offset, tp.getMove1());
                    writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonMove1Offset + 2, tp.getMove2());
                    writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonMove1Offset + 4, tp.getMove3());
                    writeWord(pointerToPokes + (poke * trainerMonSize) + trainerMonMove1Offset + 6, tp.getMove4());
                }
            }

        }

    }

    @Override
    public void writeTrainerLevelModifier(int trainersLevelModifier) {
        int levelModifierOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.TRAINER_LEVEL_BOOST_PERCENT_INDEX * 2));
        if (levelModifierOffset != 0) {
            writeWord(levelModifierOffset, trainersLevelModifier + 100);
        }
    }

    @Override
    public List<Pokemon> getPokemon() {
        return pokemonList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void writeMovesLearnt() {

        for (Pokemon pkmn : getPokemon()) {

            List<MoveLearnt> learnset = pkmn.getLearnset();
            learnset.forEach(l -> {
                writeWord(l.getOffset(), l.getMove());
                writeWord(l.getOffset() + 2, l.getLevel());
            });

        }

    }

    @Override
    public List<Pokemon> getStaticPokemon() {
        List<Pokemon> statics = new ArrayList<>();
        List<StaticPokemon> staticsHere = romEntry.getStaticPokemon();
        for (StaticPokemon staticPK : staticsHere) {
            statics.add(pokesInternal[readWord(staticPK.getFirstOffset())]);
        }
        return statics;
    }

    @Override
    public boolean setStaticPokemon(List<Pokemon> staticPokemon) {

        List<StaticPokemon> staticsHere = romEntry.getStaticPokemon();
        if (staticPokemon.size() != staticsHere.size()) {
            return false;
        }

        for (int i = 0; i < staticsHere.size(); i++) {
            int value = staticPokemon.get(i).getSpeciesNumber();

            int existingSpecies = 0;
            for (int offset : staticsHere.get(i).getOffsets()) {

                int speciesAtOffset = readWord(offset);
                if (existingSpecies == 0) {
                    existingSpecies = speciesAtOffset;
                }

                if (speciesAtOffset == existingSpecies) {
                    writeWord(offset, value);
                } else {
                    int finalExistingSpecies = existingSpecies;
                    String speciesName = pokemonList.stream().filter(Objects::nonNull).filter(p -> p.getSpeciesNumber() == finalExistingSpecies).map(p -> p.getName()).findFirst().orElse("UNKNOWN MON");
                    System.out.printf("Trying of overwrite wrong value for %s species %s at index %s offset %x%n", speciesName, existingSpecies, i, offset);
                }

            }


        }
        return true;
    }

    @Override
    public void randomizeFrontier(boolean randomMoves) {

        int baseOffset = romEntry.getValue("FrontierPokemon");
        int numPokes = romEntry.getValue("FrontierPokemonCount");

        int trainerMonSize = 0x20;
        int trainerMonIvOffset = 0x08;
        int trainerMonLevelOffset = 0x1A;
        int trainerMonSpeciesOffset = 0x14;
        int trainerMonHeldItemOffset = 0x16;
        int trainerMonMove1Offset = 0x0C;

        List<TrainerPokemon> frontierMons = new ArrayList<>();

        for (int poke = 0; poke < numPokes; poke++) {
            TrainerPokemon thisPoke = new TrainerPokemon();
            thisPoke.setAILevel(readWord(baseOffset + poke * trainerMonSize + trainerMonIvOffset));
            thisPoke.setLevel(readWord(baseOffset + poke * trainerMonSize + trainerMonLevelOffset));
            thisPoke.setPokemon(pokesInternal[readWord(baseOffset + poke * trainerMonSize + trainerMonSpeciesOffset)]);
            thisPoke.setHeldItem(readWord(baseOffset + poke * trainerMonSize + trainerMonHeldItemOffset));
            thisPoke.setMove1(readWord(baseOffset + poke * trainerMonSize + trainerMonMove1Offset));
            thisPoke.setMove2(readWord(baseOffset + poke * trainerMonSize + trainerMonMove1Offset + 2));
            thisPoke.setMove3(readWord(baseOffset + poke * trainerMonSize + trainerMonMove1Offset + 4));
            thisPoke.setMove4(readWord(baseOffset + poke * trainerMonSize + trainerMonMove1Offset + 6));
            frontierMons.add(thisPoke);
        }

        for (TrainerPokemon tp : frontierMons) {
            tp.setPokemon(pickReplacement(tp.getPokemon(), false, null, false, true));


            List<Integer> battleItems = EmeraldEXConstants.getBattleItems();
            tp.setHeldItem(battleItems.get(random.nextInt(battleItems.size())));

            if (randomMoves) {

                // Set moves
                List<Move> usableMoves = new ArrayList<>(this.getMoves());
                usableMoves.remove(0); // remove null entry
                Set<Move> unusableMoves = new HashSet<>();
                Set<Move> unusableDamagingMoves = new HashSet<>();

                for (Move mv : usableMoves) {
                    int moveNumber = mv.getNumber();
                    if (getBannedRandomMoves().contains(moveNumber)) {
                        unusableMoves.add(mv);
                    } else if (getBannedForDamagingMoves().contains(moveNumber)
                            || mv.getPower() < GlobalConstants.MIN_DAMAGING_MOVE_POWER) {
                        unusableDamagingMoves.add(mv);
                    }
                }

                usableMoves.removeAll(unusableMoves);
                List<Move> usableDamagingMoves = new ArrayList<>(usableMoves);
                usableDamagingMoves.removeAll(unusableDamagingMoves);

                Move move1Damaging = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
                usableMoves.remove(move1Damaging);
                usableDamagingMoves.remove(move1Damaging);

                Move move2Damaging = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));;
                usableMoves.remove(move2Damaging);
                usableDamagingMoves.remove(move2Damaging);

                Move move3 = usableMoves.get(random.nextInt(usableMoves.size()));;
                usableMoves.remove(move3);

                Move move4 = usableMoves.get(random.nextInt(usableMoves.size()));;
                usableMoves.remove(move4);

                tp.setMove1(move1Damaging.getNumber());
                tp.setMove2(move2Damaging.getNumber());
                tp.setMove3(move3.getNumber());
                tp.setMove4(move4.getNumber());

                tp.setResetMoves(false);

            } else {

                tp.setResetMoves(true);

            }
        }

        Iterator<TrainerPokemon> pokes = frontierMons.iterator();

        // Write out Pokemon data!
        for (int poke = 0; poke < numPokes; poke++) {
            TrainerPokemon tp = pokes.next();
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonIvOffset, tp.getLevel());
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonLevelOffset, tp.getLevel());
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonSpeciesOffset, tp.getPokemon().getSpeciesNumber());
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonHeldItemOffset, tp.getHeldItem());

            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset, tp.getMove1());
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + 2, tp.getMove2());
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + 4, tp.getMove3());
            writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + 6, tp.getMove4());

            if (tp.isResetMoves()) {
                int[] pokeMoves = RomFunctions.getMovesAtLevel(tp.getPokemon(), 100);
                for (int m = 0; m < 4; m++) {
                    writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + m * 2, pokeMoves[m]);
                }
            } else {
                writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset, tp.getMove1());
                writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + 2, tp.getMove2());
                writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + 4, tp.getMove3());
                writeWord(baseOffset + (poke * trainerMonSize) + trainerMonMove1Offset + 6, tp.getMove4());
            }
        }

    }

    @Override
    public List<Integer> getTMMoves() {
        List<Integer> tms = new ArrayList<>();
        int itemsOffset = romEntry.getValue("ItemData");
        int structlen = romEntry.getValue("ItemEntrySize");
        int moveIdOffset = 0x4;

        for (int i = 0; i < EmeraldEXConstants.tmCount; i++) {
            tms.add(readWord(itemsOffset + ((i + TMS_START) * structlen) + moveIdOffset));
        }
        return tms;
    }

    @Override
    public List<Integer> getHMMoves() {
        return EmeraldEXConstants.hmMoves;
    }

    @Override
    public void setTMMoves(List<Integer> moveIndexes) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }

        int itemsOffset = romEntry.getValue("ItemData");
        int structlen = romEntry.getValue("ItemEntrySize");
        int[] pals = romEntry.getArrayEntries().get("TmPals");
        int moveIdOffset = 0x4;
        int descriptionPtrOffset = 12;

        int moveInfoOffset = romEntry.getValue("MoveInfoOffset");
        int moveInfoSize = romEntry.getValue("MoveInfoSize");
        int moveDescriptionOffsetInStruct = 0x4;

        for (int i = 0; i < EmeraldEXConstants.tmCount; i++) {

            Move newMove = moves[moveIndexes.get(i)];

            if (newMove == null) {
                newMove = getMoves().get(0);
            }

            int baseOffset = itemsOffset + ((i + TMS_START) * structlen);

            writeWord(baseOffset + moveIdOffset, newMove.getInternalId());

            int newPal = pals[EmeraldEXConstants.typeToPalIndex(newMove.getType())];
            int palOffset = baseOffset + structlen - 4; // last u32 in struct
            writePointer(palOffset, newPal);

            int moveDescriptionPtr = readLong(moveInfoOffset + (newMove.getInternalId() * moveInfoSize) + moveDescriptionOffsetInStruct);
            writeLong(baseOffset + descriptionPtrOffset, moveDescriptionPtr);

            // In game tm text no longer needs updating because the inc files buffer names in dynamically
        }

    }

    private final RomFunctions.StringSizeDeterminer ssd = encodedText -> translateString(encodedText).length;

    @Override
    public int getTMCount() {
        return EmeraldEXConstants.tmCount;
    }

    @Override
    public int getHMCount() {
        return EmeraldEXConstants.hmCount;
    }

    @Override
    public Map<Pokemon, boolean[]> getTMHMCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<Pokemon, boolean[]>();
        int offset = romEntry.getValue("PokemonTMHMCompat");
        for (int i = 1; i <= numRealPokemon; i++) {
            Pokemon pkmn = pokemonList.get(i);
            int compatOffset = offset + (pkmn.getSpeciesNumber()) * 8;
            boolean[] flags = new boolean[EmeraldEXConstants.tmCount + EmeraldEXConstants.hmCount + 1];
            for (int j = 0; j < 8; j++) {
                readByteIntoFlags(flags, j * 8 + 1, compatOffset + j);
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setTMHMCompatibility(int seed) {
        int tmhmCompatibilityOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.TMHM_COMPATIBILITY_INDEX * 2));
        if (tmhmCompatibilityOffset != 0) {
            writeWord(tmhmCompatibilityOffset, seed);
        }
    }

    @Override
    public boolean hasMoveTutors() {
        return true;
    }

    @Override
    public List<Integer> getMoveTutorMoves() {
        if (!hasMoveTutors()) {
            return new ArrayList<>();
        }
        List<Integer> mts = new ArrayList<>();
        for (TutorMove tutorMove : romEntry.getTutorMoves()) {
            int offset = tutorMove.getOffsets().get(0);
            mts.add(readWord(offset));
        }

        return mts;
    }

    @Override
    public void setMoveTutorMoves(List<Integer> moves) {
        if (!hasMoveTutors()) {
            return;
        }

        for (int i = 0; i < romEntry.getTutorMoves().size(); i++) {
            TutorMove tutorMove = romEntry.getTutorMoves().get(i);
            for (Integer offset : tutorMove.getOffsets()) {
                writeWord(offset, moves.get(i));
            }
        }

    }

    @Override
    public void setMoveTutorCompatibility(int seed) {
        int tutorCompatibilityOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.TUTOR_COMPATIBILITY_INDEX * 2));
        if (tutorCompatibilityOffset != 0) {
            writeWord(tutorCompatibilityOffset, seed);
        }
    }

    @Override
    public String getROMName() {
        return romEntry.getName() + (this.isRomHack ? " (ROM Hack)" : "");
    }

    @Override
    public String getROMCode() {
        return romEntry.getRomCode();
    }

    @Override
    public String getSupportLevel() {
        return (romEntry.getValue("StaticPokemonSupport") > 0) ? "Complete" : "No Static Pokemon";
    }

    // For dynamic offsets later
    private int find(String hexString) {
        return find(rom, hexString);
    }

    private static int find(byte[] haystack, String hexString) {
        if (hexString.length() % 2 != 0) {
            return -3; // error
        }
        byte[] searchFor = new byte[hexString.length() / 2];
        for (int i = 0; i < searchFor.length; i++) {
            searchFor[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        List<Integer> found = RomFunctions.search(haystack, searchFor);
        if (found.isEmpty()) {
            return -1; // not found
        } else if (found.size() > 1) {
            return -2; // not unique
        } else {
            return found.get(0);
        }
    }

    private void writeHexString(String hexString, int offset) {
        if (hexString.length() % 2 != 0) {
            return; // error
        }
        for (int i = 0; i < hexString.length() / 2; i++) {
            rom[offset + i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
    }

    public String pointerToHexString(int pointer) {
        String hex = String.format("%08X", pointer + 0x08000000);
        return new String(new char[]{hex.charAt(6), hex.charAt(7), hex.charAt(4), hex.charAt(5), hex.charAt(2),
                hex.charAt(3), hex.charAt(0), hex.charAt(1)});
    }

    private void populateEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                pkmn.getEvolutionsFrom().clear();
                pkmn.getEvolutionsTo().clear();
            }
        }

        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");
        int offsetInSpeciesInfo = EmeraldEXConstants.evolutionsPtrOffset;

        int numInternalPokes = romEntry.getValue("PokemonCount");
        for (int i = 1; i <= numRealPokemon; i++) {

            Pokemon pk = pokemonList.get(i);

            if (pk == null) // This happens when processing some other forms
                continue;

            int idx = pk.getSpeciesNumber();
            int evoOffset = readPointer((speciesInfoOffset + offsetInSpeciesInfo) + (idx) * speciesInfoEntrySize);

            for (int j = 0; j < 10; j++) {

                if (evoOffset < 0) // Species has no evolutions
                    break;

                int method = readWord(evoOffset + j * 8);

                if (method == EvolutionType.EVOLUTIONS_END.toIndex() || EvolutionType.fromIndex(method) == null)
                    break;

                int evolvingTo = readWord(evoOffset + j * 8 + 4);
                if (evolvingTo >= 1 && evolvingTo <= numInternalPokes && EvolutionType.fromIndex(method) != null) {
                    int extraInfo = readWord(evoOffset + j * 8 + 2);
                    EvolutionType et = EvolutionType.fromIndex(method);
                    Evolution evo = new Evolution(pk, pokesInternal[evolvingTo], true, et, extraInfo);
                    if (!pk.getEvolutionsFrom().contains(evo)) {
                        pk.getEvolutionsFrom().add(evo);
                        pokesInternal[evolvingTo].getEvolutionsTo().add(evo);
                    }
                }
            }

            // split evos don't carry stats
            if (pk.getEvolutionsFrom()
                   .stream()
                   .filter(e -> e.getType() != EvolutionType.EVO_NONE)
                   .map(p -> p.getTo().getSpeciesNumber())
                   .distinct()
                   .count()  > 1) {
                for (Evolution e : pk.getEvolutionsFrom()) {
                    e.setCarryStats(false);
                }
            }
        }
    }

    private void writeEvolutions() {

        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");
        int offsetInSpeciesInfo = EmeraldEXConstants.evolutionsPtrOffset;

        for (int i = 1; i <= numRealPokemon; i++) {
            Pokemon pk = pokemonList.get(i);

            if (pk == null) // This happens when processing some other forms
                continue;

            int idx = pk.getSpeciesNumber();
            int evoOffset = readPointer((speciesInfoOffset + offsetInSpeciesInfo) + (idx) * speciesInfoEntrySize);

            for (int j = 0; j < 10; j++) {

                if (j >= pk.getEvolutionsFrom().size())
                    break;

                Evolution evo = pk.getEvolutionsFrom().get(j);

                int method = evo.getType().toIndex();
                if (EvolutionType.fromIndex(method) == null)
                    break;

                writeWord(evoOffset + j * 8, method);
                writeWord(evoOffset + j * 8 + 2, evo.getExtraInfo());
                writeWord(evoOffset + j * 8 + 4, evo.getTo().getSpeciesNumber());

            }
        }
    }

    @Override
    public void removeTradeEvolutions(boolean changeMoveEvos) {

        // no move evos, so no need to check for those
        log("--Removing Trade Evolutions--");
        Set<Evolution> extraEvolutions = new HashSet<>();
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                extraEvolutions.clear();
                for (Evolution evo : pkmn.getEvolutionsFrom()) {
                    // Pure Trade
                    if (evo.getType() == EvolutionType.EVO_TRADE) {
                        // Haunter, Machoke, Kadabra, Graveler
                        // Make it into level 37, we're done.
                        evo.setType(EvolutionType.EVO_LEVEL);
                        evo.setExtraInfo(37);
                        logEvoChangeLevel(evo.getFrom().getName(), evo.getTo().getName(), evo.getExtraInfo());
                    }
                    // Trade w/ Held Item
                    if (evo.getType() == EvolutionType.EVO_TRADE_ITEM) {
                        // Get the current item & evolution
                        int item = evo.getExtraInfo();
                        if (evo.getFrom().getNumber() == EmeraldEXConstants.slowpokeIndex) {
                            // Slowpoke is awkward - he already has a level evo
                            // So we can't do Level up w/ Held Item for him
                            // Put Water Stone instead
                            evo.setType(EvolutionType.EVO_ITEM);
                            evo.setExtraInfo(EmeraldEXConstants.waterStoneIndex); // water
                            // stone
                            logEvoChangeStone(evo.getFrom().getName(), evo.getTo().getName(),
                                    itemNames[EmeraldEXConstants.waterStoneIndex]);
                        } else {
                            logEvoChangeLevelWithItem(evo.getFrom().getName(), evo.getTo().getName(), itemNames[item]);
                            // Replace, for this entry, w/
                            // Level up w/ Held Item at Day
                            evo.setType(EvolutionType.EVO_ITEM_HOLD_DAY);
                            // now add an extra evo for
                            // Level up w/ Held Item at Night
                            Evolution extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true, EvolutionType.EVO_ITEM_HOLD_NIGHT,
                                    item);
                            extraEvolutions.add(extraEntry);
                        }
                    }
                }

                pkmn.getEvolutionsFrom().addAll(extraEvolutions);
                for (Evolution ev : extraEvolutions) {
                    ev.getTo().getEvolutionsTo().add(ev);
                }
            }
        }
        logBlankLine();
    }

    @Override
    public boolean canChangeTrainerText() {
        return true;
    }

    @Override
    public List<String> getTrainerNames() {
        int baseOffset = romEntry.getValue("TrainerData");
        int amount = romEntry.getValue("TrainerCount");
        int entryLen = romEntry.getValue("TrainerEntrySize");
        int trainerNameOffsetInStruct = 19;
        List<String> theTrainers = new ArrayList<>();
        for (int i = 1; i < amount; i++) {
            theTrainers.add(readVariableLengthString(baseOffset + i * entryLen + trainerNameOffsetInStruct));
        }
        return theTrainers;
    }

    @Override
    public void setTrainerNames(List<String> trainerNames) {
        int baseOffset = romEntry.getValue("TrainerData");
        int amount = romEntry.getValue("TrainerCount");
        int entryLen = romEntry.getValue("TrainerEntrySize");
        int nameLen = romEntry.getValue("TrainerNameLength");
        int trainerNameOffsetInStruct = 19;
        Iterator<String> theTrainers = trainerNames.iterator();
        for (int i = 1; i < amount; i++) {
            String newName = theTrainers.next();
            writeFixedLengthString(newName, baseOffset + i * entryLen + trainerNameOffsetInStruct, nameLen);
        }
    }

    @Override
    public TrainerNameMode trainerNameMode() {
        return TrainerNameMode.MAX_LENGTH;
    }

    @Override
    public List<Integer> getTCNameLengthsByTrainer() {
        // not needed
        return new ArrayList<>();
    }

    @Override
    public int maxTrainerNameLength() {
        return romEntry.getValue("TrainerNameLength") - 1;
    }

    @Override
    public List<String> getTrainerClassNames() {
        int baseOffset = romEntry.getValue("TrainerClasses");
        int amount = romEntry.getValue("TrainerClassCount");
        int trainerClassStructLength = romEntry.getValue("TrainerClassStructSize");
        List<String> trainerClasses = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            trainerClasses.add(readVariableLengthString(baseOffset + i * trainerClassStructLength));
        }
        return trainerClasses;
    }

    @Override
    public void setTrainerClassNames(List<String> trainerClassNames) {
        int baseOffset = romEntry.getValue("TrainerClasses");
        int amount = romEntry.getValue("TrainerClassCount");
        int length = romEntry.getValue("TrainerClassNameLength");
        int trainerClassStructLength = romEntry.getValue("TrainerClassStructSize");
        Iterator<String> trainerClasses = trainerClassNames.iterator();
        for (int i = 0; i < amount; i++) {
            writeFixedLengthString(trainerClasses.next(), baseOffset + i * trainerClassStructLength, length);
        }
    }

    @Override
    public int maxTrainerClassNameLength() {
        return romEntry.getValue("TrainerClassNameLength") - 1;
    }

    @Override
    public boolean fixedTrainerClassNamesLength() {
        return false;
    }

    @Override
    public List<Integer> getDoublesTrainerClasses() {
        int[] doublesClasses = romEntry.getArrayEntries().get("DoublesTrainerClasses");
        List<Integer> doubles = new ArrayList<Integer>();
        for (int tClass : doublesClasses) {
            doubles.add(tClass);
        }
        return doubles;
    }

    @Override
    public boolean canChangeStaticPokemon() {
        return (romEntry.getValue("StaticPokemonSupport") > 0);
    }

    @Override
    public boolean hasFrontier() {
        return true;
    }

    @Override
    public String getDefaultExtension() {
        return "gba";
    }

    @Override
    public int abilitiesPerPokemon() {
        return 3;
    }

    @Override
    public int highestAbilityIndex() {
        return EmeraldEXConstants.highestAbilityIndex;
    }

    private void loadAbilityNames() {
        int abilitiesOffset = romEntry.getValue("AbilitiesOffset");
        int namelen = romEntry.getValue("AbilityNameLength");
        int abilityStructSize = romEntry.getValue("AbilityStructSize");
        int nameOffsetInStruct = 0;

        abilityNames = new String[EmeraldEXConstants.highestAbilityIndex + 1];

        for (int i = 0; i <= EmeraldEXConstants.highestAbilityIndex; i++) {
            abilityNames[i] = readFixedLengthString(abilitiesOffset + nameOffsetInStruct + (abilityStructSize * i), namelen);
        }
    }

    @Override
    public String abilityName(int number) {
        return abilityNames[number];
    }

    @Override
    public int internalStringLength(String string) {
        return translateString(string).length;
    }

    @Override
    @SuppressWarnings("all")
    public void applySignature() {
        // Emerald, intro sprite: any Pokemon.
        int introPokemon = randomPokemon(false).getSpeciesNumber();
        writeWord(romEntry.getValue("StaticVars") + (EmeraldEXConstants.BIRCH_INTRO_MON_INDEX * 2), introPokemon);
    }

    private Pokemon randomPokemonLimited(int maxValue, boolean blockNonMales) {
        checkPokemonRestrictions();
        List<Pokemon> validPokemon = new ArrayList<>();
        for (Pokemon pk : this.mainPokemonList) {
            if (pk.getSpeciesNumber() <= maxValue && (!blockNonMales || pk.getGenderRatio() <= 0xFD)) {
                validPokemon.add(pk);
            }
        }
        if (validPokemon.isEmpty()) {
            return null;
        } else {
            return validPokemon.get(random.nextInt(validPokemon.size()));
        }
    }

    private void determineMapBankSizes() {
        int mbpsOffset = romEntry.getValue("MapHeaders");
        List<Integer> mapBankOffsets = new ArrayList<>();

        int offset = mbpsOffset;

        // find map banks
        while (true) {
            boolean valid = true;
            for (int mbOffset : mapBankOffsets) {
                if (mbpsOffset < mbOffset && offset >= mbOffset) {
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                break;
            }
            int newMBOffset = readPointer(offset);
            if (newMBOffset < 0 || newMBOffset >= rom.length) {
                break;
            }
            mapBankOffsets.add(newMBOffset);
            offset += 4;
        }
        int bankCount = mapBankOffsets.size();
        int[] bankMapCounts = new int[bankCount];
        for (int bank = 0; bank < bankCount; bank++) {
            int baseBankOffset = mapBankOffsets.get(bank);
            int count = 0;
            offset = baseBankOffset;
            while (true) {
                boolean valid = true;
                for (int mbOffset : mapBankOffsets) {
                    if (baseBankOffset < mbOffset && offset >= mbOffset) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) {
                    break;
                }
                if (baseBankOffset < mbpsOffset && offset >= mbpsOffset) {
                    break;
                }
                int newMapOffset = readPointer(offset);
                if (newMapOffset < 0 || newMapOffset >= rom.length) {
                    break;
                }
                count++;
                offset += 4;
            }
            bankMapCounts[bank] = count;
        }

        romEntry.getEntries().put("MapBankCount", bankCount);
        romEntry.getArrayEntries().put("MapBankSizes", bankMapCounts);
    }

    private class ItemLocationInner {
        private final int mapBank;
        private final int mapNumber;
        private final int x;
        private final int y;
        private final int offset;
        private final boolean hidden;

        public ItemLocationInner(int mapBank, int mapNumber, int x, int y, int offset, boolean hidden) {
            super();
            this.mapBank = mapBank;
            this.mapNumber = mapNumber;
            this.x = x;
            this.y = y;
            this.offset = offset;
            this.hidden = hidden;
        }

        @Override
        public String toString() {
            return String.format("%s (%d.%d) @ %d, %d (%s)", mapNames[mapBank][mapNumber], mapBank, mapNumber, x, y,
                    hidden ? "hidden" : "visible");
        }
    }

    private void preprocessMaps() {
        itemOffs = new ArrayList<>();
        int bankCount = romEntry.getValue("MapBankCount");
        int[] bankMapCounts = romEntry.getArrayEntries().get("MapBankSizes");
        int itemBall = romEntry.getValue("ItemBallPic");
        mapNames = new String[bankCount][];
        int mbpsOffset = romEntry.getValue("MapHeaders");
        int mapLabels = romEntry.getValue("MapLabels");
        Map<Integer, String> mapLabelsM = new HashMap<>();
        for (int bank = 0; bank < bankCount; bank++) {
            int bankOffset = readPointer(mbpsOffset + bank * 4);
            mapNames[bank] = new String[bankMapCounts[bank]];
            for (int map = 0; map < bankMapCounts[bank]; map++) {
                int mhOffset = readPointer(bankOffset + map * 4);

                // map name
                int mapLabel = rom[mhOffset + 0x14] & 0xFF;
                if (mapLabelsM.containsKey(mapLabel)) {
                    mapNames[bank][map] = mapLabelsM.get(mapLabel);
                } else {
                    mapNames[bank][map] = readVariableLengthString(readPointer(mapLabels + mapLabel * 8 + 4));
                    mapLabelsM.put(mapLabel, mapNames[bank][map]);
                }

                // events
                int eventOffset = readPointer(mhOffset + 4);
                if (eventOffset >= 0 && eventOffset < rom.length) {

                    int pCount = rom[eventOffset] & 0xFF;
                    int spCount = rom[eventOffset + 3] & 0xFF;

                    if (pCount > 0) {
                        int peopleOffset = readPointer(eventOffset + 4);
                        for (int p = 0; p < pCount; p++) {
                            int pSprite = readWord(peopleOffset + p * 24 + 1);
                            if (pSprite == itemBall && readWord(peopleOffset + p * 24 + 0xE) >= 0) {
                                // item ball script
                                itemOffs.add(new ItemLocationInner(bank, map, readWord(peopleOffset + p * 24 + 4),
                                        readWord(peopleOffset + p * 24 + 6), peopleOffset + p * 24 + 0xE, false));

                            }
                        }
                    }

                    if (spCount > 0) {
                        int signpostsOffset = readPointer(eventOffset + 16);
                        for (int sp = 0; sp < spCount; sp++) {
                            int spType = rom[signpostsOffset + sp * 12 + 5];
                            if (spType >= 5 && spType <= 7) {
                                // hidden item
                                int itemHere = readWord(signpostsOffset + sp * 12 + 8);
                                if (itemHere != 0) {
                                    // itemid 0 is coins
                                    itemOffs.add(new ItemLocationInner(bank, map, readWord(signpostsOffset + sp * 12),
                                            readWord(signpostsOffset + sp * 12 + 2), signpostsOffset + sp * 12 + 8,
                                            true));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public ItemList getAllowedItems() {
        return allowedItems;
    }

    @Override
    public ItemList getNonBadItems() {
        return nonBadItems;
    }

    private void loadItemNames() {
        int nameoffs = romEntry.getValue("ItemData");
        int structlen = romEntry.getValue("ItemEntrySize");
        int maxcount = romEntry.getValue("ItemCount");
        int nameInStructOffset = 0x14;

        itemNames = new String[maxcount + 1];
        for (int i = 0; i <= maxcount; i++) {
            itemNames[i] = readVariableLengthString(nameoffs + nameInStructOffset + (structlen * i));

            if (readLong(nameoffs + (structlen * i)) == 0) {
                freeItems.add(i);
            }
        }
    }

    @Override
    public String[] getItemNames() {
        return itemNames;
    }

    @Override
    public List<Integer> getRequiredFieldTMs() {
        // emerald has a few TMs from pickup
        return EmeraldEXConstants.eRequiredFieldTMs;
    }

    @Override
    public List<FieldTM> getCurrentFieldTMs() {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        List<FieldTM> fieldTMs = new ArrayList<FieldTM>();

        for (ItemLocationInner il : itemOffs) {
            int itemHere = readWord(il.offset);
            if (getAllowedItems().isTM(itemHere)) {
                int thisTM = itemHere - EmeraldEXConstants.tmItemOffset + 1;
                // hack for repeat TMs
                FieldTM tmObj = new FieldTM(il.toString(), thisTM);
                if (!fieldTMs.contains(tmObj)) {
                    fieldTMs.add(tmObj);
                }
            }
        }
        return fieldTMs;
    }

    @Override
    public void setFieldTMs(List<Integer> fieldTMs) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        Iterator<Integer> iterTMs = fieldTMs.iterator();
        int[] givenTMs = new int[512];

        for (ItemLocationInner il : itemOffs) {
            int itemHere = readWord(il.offset);
            if (getAllowedItems().isTM(itemHere)) {
                // Cache replaced TMs to duplicate repeats
                if (givenTMs[itemHere] != 0) {
                    writeWord(il.offset, givenTMs[itemHere]);
                } else {
                    // Replace this with a TM from the list
                    int tm = iterTMs.next();
                    tm += EmeraldEXConstants.tmItemOffset - 1;
                    givenTMs[itemHere] = tm;
                    writeWord(il.offset, tm);
                }
            }
        }
    }

    @Override
    public List<ItemLocation> getRegularFieldItems() {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        List<ItemLocation> fieldItems = new ArrayList<>();

        for (ItemLocationInner il : itemOffs) {
            int itemHere = readWord(il.offset);
            if (il.mapBank == -1 /* is plotless key item */ || (getAllowedItems().isAllowed(itemHere)
                    && !(getAllowedItems().isTM(itemHere)))) {
                fieldItems.add(new ItemLocation(il.toString(), itemHere));
            }
        }
        return fieldItems;
    }

    @Override
    public void setRegularFieldItems(List<Integer> items) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        Iterator<Integer> iterItems = items.iterator();

        for (ItemLocationInner il : itemOffs) {
            int itemHere = readWord(il.offset);
            if (getAllowedItems().isAllowed(itemHere)
                    && !(getAllowedItems().isTM(itemHere))) {
                // Replace it
                writeWord(il.offset, iterItems.next());
            }
        }

    }

    @Override
    public void randomizeGivenItems(boolean banBadItems) {

        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        List<Integer> newItems = new ArrayList<>();

        List<GivenItem> givenItems = romEntry.getGivenItems();
        int givenItemsCount = givenItems.size();

        for (int i = 0; i < givenItemsCount; i++) {
            newItems.add(possibleItems.randomNonTM(this.random));
        }

        Collections.shuffle(newItems, this.random);

        Iterator<Integer> iterItems = newItems.iterator();

        for (GivenItem givenItem : givenItems) {
            int itemHere = readWord(givenItem.getOffsets().get(0));
            if (getAllowedItems().isAllowed(itemHere)
                    && !(getAllowedItems().isTM(itemHere))) {
                Integer randomItem = iterItems.next();
                givenItem.getOffsets()
                         .forEach(offset -> writeWord(offset, randomItem));
            }
        }
    }

    @Override
    public void randomizeBerryTrees(boolean banBadItems) {

        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        List<Integer> newItems = new ArrayList<>();

        int berryTreeArrayOffset = romEntry.getValue("BerryTrees");
        int berryTreeCount = EmeraldEXConstants.BERRY_TREE_COUNT;

        for (int i = 0; i < berryTreeCount; i++) {
            newItems.add(possibleItems.randomNonTM(this.random));
        }

        Collections.shuffle(newItems, this.random);

        Iterator<Integer> iterItems = newItems.iterator();

        for (int i = 0; i < berryTreeCount; i++) {
            writeWord(berryTreeArrayOffset + (i * 2), iterItems.next());
        }
    }

    @Override
    public void randomizeMarts(boolean banBadItems, boolean allMartsHaveBallAndRepel) {

        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();

        List<Mart> marts = romEntry.getMarts();

        for (Mart mart : marts) {

            int largestMartInSet = mart.largestInventory();
            List<Integer> newItems = new ArrayList<>();
            for (int i = 0; i < largestMartInSet; i++) {
                newItems.add(possibleItems.randomNonTM(this.random));
            }

            Iterator<Integer> iterItems = newItems.iterator();

            Collections.shuffle(newItems, this.random);

            if (allMartsHaveBallAndRepel) {
                newItems.set(0, possibleItems.randomRepel(this.random));
                newItems.set(1,  possibleItems.randomBall(this.random));
            } else {
                if (random.nextBoolean() && random.nextBoolean()) {
                    newItems.set(0, possibleItems.randomRepel(this.random));
                }
                if (random.nextBoolean()) {
                    newItems.set(1, possibleItems.randomBall(this.random));
                }
            }

            if (newItems.size() >= 4) {
                if (random.nextBoolean()) {
                    newItems.set(2, possibleItems.randomMedicine(this.random));
                }

                if (random.nextBoolean() && random.nextBoolean()) {
                    newItems.set(3, possibleItems.randomXItem(this.random));
                }
            }
            Collections.shuffle(newItems, this.random);


            List<Integer> itemsChosenForThisMart = new ArrayList<>();

            for (int i = 0; i < largestMartInSet; i++) {

                Integer randomItem = iterItems.next();

                // Try and make sure the same item isn't listed twice for a mart
                int maxRerolls = 10;
                for (int j = 0; j < maxRerolls && itemsChosenForThisMart.contains(randomItem); j++) {
                    randomItem += 1;
                }
                itemsChosenForThisMart.add(randomItem);

                for (Mart.MartInventory inventory : mart.getMartInventories()) {

                    int itemHere = readWord(inventory.getOffset() + (2 * i));

                    if (itemHere == 0) {
                        inventory.markComplete();
                    }

                    if (!inventory.isComplete() && !(inventory.getSize() < i)) {
                        writeWord(inventory.getOffset() + (i * 2), randomItem);
                    }

                }

            }

        }

        marts.forEach(Mart::resetState);

        int giftPremierBallOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.MART_PROMO_ITEM_INDEX * 2));
        Collections.shuffle(freeItems, this.random);
        if (giftPremierBallOffset != 0 && !freeItems.isEmpty()) {
            writeWord(giftPremierBallOffset, freeItems.get(0));
        }
    }

    @Override
    public void randomizePickupTime(boolean banBadItems) {

        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        List<Integer> newItems = new ArrayList<>();

        int pickupArrayOffset = romEntry.getValue("PickUpTables");
        int pickupItemsCount = EmeraldEXConstants.PICKUP_ITEM_COUNT;
        int pickupItemStructSize = EmeraldEXConstants.PICKUP_ITEM_STRUCT_SIZE;

        for (int i = 0; i < pickupItemsCount; i++) {
            newItems.add(possibleItems.randomNonTM(this.random));
        }

        Collections.shuffle(newItems, this.random);

        Iterator<Integer> iterItems = newItems.iterator();

        for (int i = 0; i < pickupItemsCount; i++) {
            writeWord(pickupArrayOffset + (i * pickupItemStructSize), iterItems.next());
        }
    }

    @Override
    public void randomizeItemPrices(boolean allMartsHaveBallAndRepel) {

        int itemInfoOffset = romEntry.getValue("ItemInfoOffset");
        int itemCount = romEntry.getValue("ItemCount");
        int itemStructSize = romEntry.getValue("ItemEntrySize");
        int priceOffsetInStruct = 0;

        List<Integer> priceList = new ArrayList<>();
        for (int i = 1; i < itemCount; i++) {
            priceList.add(readLong(itemInfoOffset + priceOffsetInStruct + (i * itemStructSize)));
        }

        Collections.shuffle(priceList, this.random);

        Iterator<Integer> iterItems = priceList.iterator();

        freeItems = new ArrayList<>();

        for (int i = 1; i < itemCount; i++) {

            int itemPrice = iterItems.next();

            // If all marts have a ball and repel they probably want easy access so we should cap the price
            if (allMartsHaveBallAndRepel && itemPrice >= 1000) {
                if ((i < (BALLS_END + 1)) || (i >= REPEL_START && i < (MAX_REPEL + 1))) {
                    itemPrice = random.nextInt(100 + 1) * 10;
                }
            }

            // Reduce free candy odds
            if (itemPrice <= 250) {
                if  ((i >= CANDY_START && i < (CANDY_END + 1))) {
                    if (random.nextInt() % 10 != 0) {
                        itemPrice += 1000;
                    }
                }
            }

            if (itemPrice == 0) {
                freeItems.add(i);
            }

            writeLong(itemInfoOffset + priceOffsetInStruct + (i * itemStructSize), itemPrice);
        }

    }

    @Override
    public List<IngameTrade> getIngameTrades() {
        List<IngameTrade> trades = new ArrayList<>();

        // info
        int tableOffset = romEntry.getValue("TradeTableOffset");
        int tableSize = romEntry.getValue("TradeTableSize");
        int entryLength = 60;

        for (int entry = 0; entry < tableSize; entry++) {
            IngameTrade trade = new IngameTrade();
            int entryOffset = tableOffset + entry * entryLength;
            trade.setNickname(readVariableLengthString(entryOffset));
            trade.setGivenPokemon(pokesInternal[readWord(entryOffset + 14)]);
            trade.setIvs(new int[6]);
            for (int i = 0; i < 6; i++) {
                trade.setIv(i, rom[entryOffset + 16 + i] & 0xFF);
            }
            trade.setOtId(readWord(entryOffset + 24));
            trade.setItem(readWord(entryOffset + 40));
            trade.setOtName(readVariableLengthString(entryOffset + 43));
            trade.setRequestedPokemon(pokesInternal[readWord(entryOffset + 56)]);
            trades.add(trade);
        }

        return trades;

    }

    @Override
    public void setIngameTrades(List<IngameTrade> trades) {
        // info
        int tableOffset = romEntry.getValue("TradeTableOffset");
        int tableSize = romEntry.getValue("TradeTableSize");
        int entryLength = 60;
        int tradeOffset = 0;

        for (int entry = 0; entry < tableSize; entry++) {
            IngameTrade trade = trades.get(tradeOffset++);
            int entryOffset = tableOffset + entry * entryLength;
            writeFixedLengthString(trade.getNickname(), entryOffset, 12);
            writeWord(entryOffset + 14, trade.getGivenPokemon().getSpeciesNumber());
            for (int i = 0; i < 6; i++) {
                rom[entryOffset + 16 + i] = (byte) trade.getIvs()[i];
            }
            writeWord(entryOffset + 24, trade.getOtId());
            writeWord(entryOffset + 40, trade.getItem());
            writeFixedLengthString(trade.getOtName(), entryOffset + 43, 11);
            writeWord(entryOffset + 56, trade.getRequestedPokemon().getSpeciesNumber());
        }
    }

    @Override
    public boolean hasDVs() {
        return false;
    }

    @Override
    public int generationOfPokemon() {
        return 9;
    }

    @Override
    public void removeEvosForPokemonPool() {
        List<Pokemon> pokemonIncluded = this.mainPokemonList;
        Set<Evolution> keepEvos = new HashSet<Evolution>();
        for (Pokemon pk : pokes) {
            if (pk != null) {
                keepEvos.clear();
                for (Evolution evol : pk.getEvolutionsFrom()) {
                    if (pokemonIncluded.contains(evol.getFrom()) && pokemonIncluded.contains(evol.getTo())) {
                        keepEvos.add(evol);
                    } else {
                        evol.getTo().getEvolutionsTo().remove(evol);
                    }
                }
                pk.getEvolutionsFrom().retainAll(keepEvos);
            }
        }
    }

    @Override
    public boolean supportsFourStartingMoves() {
        return true;
    }

    @Override
    public List<Integer> getFieldMoves() {
        // cut, fly, surf, strength, flash,
        // dig, teleport, waterfall,
        // rock smash, sweet scent
        // not softboiled or milk drink
        // dive and secret power in RSE only
        return EmeraldEXConstants.rseFieldMoves;
    }

    @Override
    public List<Integer> getEarlyRequiredHMMoves() {
        // RSE: rock smash
        // FRLG: cut
        return EmeraldEXConstants.rseEarlyRequiredHMMoves;
    }

    @Override
    public int miscTweaksAvailable() {
        int available = MiscTweak.NATIONAL_DEX_AT_START.getValue();
        if ((romEntry.getValue("StaticVars") + (EmeraldEXConstants.WALLY_CATCH_TUTORIAL_OPPONENT_INDEX * 2)) > 0
                || (romEntry.getValue("StaticVars") + (EmeraldEXConstants.WALLY_CATCH_TUTORIAL_MON_INDEX * 2)) > 0) {
            available |= MiscTweak.RANDOMIZE_CATCHING_TUTORIAL.getValue();
        }
        if ((romEntry.getValue("StaticVars") + (EmeraldEXConstants.BATTLE_TUTORIAL_OPPONENT_INDEX * 2)) > 0
                || (romEntry.getValue("StaticVars") + (EmeraldEXConstants.BATTLE_TUTORIAL_OPPONENT_INDEX * 2)) > 0) {
            available |= MiscTweak.RANDOMIZE_BATTLE_TUTORIAL.getValue();
        }
        if ((romEntry.getValue("StaticVars") + (EmeraldEXConstants.PC_START_ITEM_INDEX * 2)) != 0) {
            available |= MiscTweak.RANDOMIZE_PC_POTION.getValue();
        }
        available |= MiscTweak.BAN_LUCKY_EGG.getValue();
        return available;
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        if (tweak == MiscTweak.NATIONAL_DEX_AT_START) {
            // This is on by default now, if we want to make this an option it shouldn't involve patching extra data into the rom
        } else if (tweak == MiscTweak.RANDOMIZE_CATCHING_TUTORIAL) {
            randomizeCatchingTutorial();
        } else if (tweak == MiscTweak.BAN_LUCKY_EGG) {
            allowedItems.banSingles(EmeraldEXConstants.luckyEggIndex);
            nonBadItems.banSingles(EmeraldEXConstants.luckyEggIndex);
        } else if (tweak == MiscTweak.RANDOMIZE_PC_POTION) {
            randomizePCPotion();
        } else if (tweak == MiscTweak.RANDOMIZE_BATTLE_TUTORIAL) {
            randomizeBattleTutorial();
        }
    }

    private void randomizeBattleTutorial() {
        int battleIntroMonOffset = romEntry.getValue("StaticVars") + (EmeraldEXConstants.BATTLE_TUTORIAL_OPPONENT_INDEX * 2);
        if (battleIntroMonOffset != 0) {
            int battleIntroSpecies = randomPokemon(false).getSpeciesNumber();
            writeWord(battleIntroMonOffset, battleIntroSpecies);
        }
    }

    private void randomizeCatchingTutorial() {
        if ((romEntry.getValue("StaticVars") + (EmeraldEXConstants.WALLY_CATCH_TUTORIAL_OPPONENT_INDEX * 2)) > 0) {
            int oppMon = randomPokemon(false).getSpeciesNumber();
            int oppOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.WALLY_CATCH_TUTORIAL_OPPONENT_INDEX * 2));
            writeWord(oppOffset, oppMon);
        }

        if ((romEntry.getValue("StaticVars") + (EmeraldEXConstants.WALLY_CATCH_TUTORIAL_MON_INDEX * 2)) > 0) {
            int playerMon = randomPokemon(false).getSpeciesNumber();
            int playerOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.WALLY_CATCH_TUTORIAL_MON_INDEX * 2));
            writeWord(playerOffset, playerMon);
        }

    }

    private void randomizePCPotion() {
        int pcPotionOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.PC_START_ITEM_INDEX * 2));
        if (pcPotionOffset != 0) {
            writeWord(pcPotionOffset, this.getNonBadItems().randomNonTM(this.random));
        }
    }

    @Override
    public BufferedImage getMascotImage() {
        Pokemon mascotPk = randomPokemon(false);
        int mascotPokemon = mascotPk.getSpeciesNumber();

        int speciesInfoOffset = romEntry.getValue("SpeciesInfo");
        int speciesInfoEntrySize = romEntry.getValue("SpeciesInfoEntrySize");
        int offset = speciesInfoOffset + (mascotPokemon * speciesInfoEntrySize);

        int frontSpriteOffsetInStruct = 0x48 + 16;
        int palSpriteOffsetInStruct = frontSpriteOffsetInStruct + (4 * 4);

        int fsOffset = readPointer(offset + frontSpriteOffsetInStruct);
        int palOffset = readPointer(offset + palSpriteOffsetInStruct);

        byte[] trueFrontSprite = DSDecmp.Decompress(rom, fsOffset);
        byte[] truePalette = DSDecmp.Decompress(rom, palOffset);

        // Convert palette into RGB
        int[] convPalette = new int[16];
        // Leave palette[0] as 00000000 for transparency
        for (int i = 0; i < 15; i++) {
            int palValue = readWord(truePalette, i * 2 + 2);
            convPalette[i + 1] = GFXFunctions.conv16BitColorToARGB(palValue);
        }

        // Make image, 4bpp
        return GFXFunctions.drawTiledImage(trueFrontSprite, convPalette, 64, 64, 4);
    }

    @Override
    public void writeCheckValueToROM(int value) {
        if (romEntry.getValue("CheckValueOffset") > 0) {
            int cvOffset = romEntry.getValue("CheckValueOffset");
            for (int i = 0; i < 4; i++) {
                rom[cvOffset + i] = (byte) ((value >> (3 - i) * 8) & 0xFF);
            }
        }
    }

    @Override
    public void randomizeWarps(int warpRandoLevel, boolean extraDeadendRemoval, boolean inGymOrder) {
        List<WarpRemapping> warpRemappings = EmeraldExWarpRandomizer.randomizeWarps(getSeedUsed(), warpRandoLevel, extraDeadendRemoval,
                inGymOrder);

        int warpRandoOffset = romEntry.getValue("RandomWarpTable");
        int entrySize = 8;

        int maxRemappings = 632;

        // Clean any existing data from the rom
        for (int i = 0; i < maxRemappings; i++) {

            rom[warpRandoOffset + (entrySize * i) + 0] = (byte) (0xFF);
            rom[warpRandoOffset + (entrySize * i) + 1] = (byte) (0xFF);
            rom[warpRandoOffset + (entrySize * i) + 2] = (byte) (0xFF);
            rom[warpRandoOffset + (entrySize * i) + 3] = (byte) (0);
            rom[warpRandoOffset + (entrySize * i) + 4] = (byte) (0);
            rom[warpRandoOffset + (entrySize * i) + 5] = (byte) (0);
        }

        if (warpRemappings.size() >= maxRemappings)
            throw new RuntimeException("Too many warps remapped");

        int offsetIndexInWarpTable = maxRemappings - warpRemappings.size();
        for (int i = 0; i < warpRemappings.size(); i++) {
            WarpRemapping warpRemapping = warpRemappings.get(i);
            int remapIndex = (i + offsetIndexInWarpTable);

            rom[warpRandoOffset + (entrySize * remapIndex) + 0] = (byte) (warpRemapping.getTriggerMapGroup() & 0xFF);
            rom[warpRandoOffset + (entrySize * remapIndex) + 1] = (byte) (warpRemapping.getTriggerMapNo() & 0xFF);
            rom[warpRandoOffset + (entrySize * remapIndex) + 2] = (byte) (warpRemapping.getTriggerWarpNo() & 0xFF);

            rom[warpRandoOffset + (entrySize * remapIndex) + 3] = (byte) (warpRemapping.getTargetMapGroup() & 0xFF);
            rom[warpRandoOffset + (entrySize * remapIndex) + 4] = (byte) (warpRemapping.getTargetMapNo() & 0xFF);
            rom[warpRandoOffset + (entrySize * remapIndex) + 5] = (byte) (warpRemapping.getTargetWarpNo() & 0xFF);
        }
    }

    private void loadTypeEffectivenessTable() {
        int typeTableOffset = romEntry.getValue("TypeEffectivenessTable");
        for (int i = 0; i < Type.values().length; i++) {
            TypeInteractions typeInteractions = new TypeInteractions(EmeraldEXConstants.byteToType(i));
            for (int j = 0; j < Type.values().length; j++) {
                Type defenderType = EmeraldEXConstants.byteToType(j);
                int bytesIn_uq4_12 = 4;
                int rowLength = bytesIn_uq4_12 * (Type.values().length);
                int offsetInRow = j * bytesIn_uq4_12;
                int multiplier = readLong(typeTableOffset + (i * rowLength) + offsetInRow);

                typeInteractions.setDefenderMultiplier(defenderType, multiplier / 4096f);
            }
            typeEffectivenessTable.put(typeInteractions.getAttackerType(), typeInteractions);
        }
    }

    @Override
    public void randomizeTypeCharts(Settings.TypeChartMod mode) {

        if (mode == Settings.TypeChartMod.UNCHANGED) {
            return;
        }
        else if (mode == Settings.TypeChartMod.SHUFFLE_ROW) {

            List<TypeInteractions> interactions = new ArrayList<>();
            for (TypeInteractions interaction : typeEffectivenessTable.values()) {

                if (interaction.getAttackerType() != Type.NONE &&
                    interaction.getAttackerType() != Type.STELLAR &&
                    interaction.getAttackerType() != Type.MYSTERY) {

                    TypeInteractions newInteraction = new TypeInteractions(interaction.getAttackerType());
                    newInteraction.setDefenderMultiplier(new HashMap<>(interaction.getDefenderMultiplier()));

                    interactions.add(newInteraction);
                }

            }

            Collections.shuffle(interactions, random);

            for (TypeInteractions interaction : typeEffectivenessTable.values()) {

                if (interaction.getAttackerType() != Type.NONE &&
                    interaction.getAttackerType() != Type.STELLAR &&
                    interaction.getAttackerType() != Type.MYSTERY) {

                    interaction.setDefenderMultiplier(interactions.get(0).getDefenderMultiplier());
                    interaction.setOriginalType(interactions.get(0).getAttackerType());
                    interactions.remove(0);

                }

            }

        }
        else if (mode == Settings.TypeChartMod.SHUFFLE) {

            List<Map<Type, Float>> interactions = new ArrayList<>();
            for (TypeInteractions interaction : typeEffectivenessTable.values()) {

                if (interaction.getAttackerType() != Type.NONE &&
                        interaction.getAttackerType() != Type.STELLAR &&
                        interaction.getAttackerType() != Type.MYSTERY) {
                    interactions.add(interaction.getDefenderMultiplier());
                }

            }

            List<Float> allValues = new ArrayList<>();
            for (Map<Type, Float> map : interactions) {
                allValues.addAll(map.values());
            }

            Collections.shuffle(allValues, random);

            Iterator<Float> valueIterator = allValues.iterator();
            for (Map<Type, Float> map : interactions) {
                for (Type key : map.keySet()) {
                    if (valueIterator.hasNext()) {
                        map.put(key, valueIterator.next());
                    }
                }
            }

            for (TypeInteractions interaction : typeEffectivenessTable.values()) {

                if (interaction.getAttackerType() != Type.NONE &&
                        interaction.getAttackerType() != Type.STELLAR &&
                        interaction.getAttackerType() != Type.MYSTERY) {

                    interaction.setDefenderMultiplier(interactions.get(0));
                    interactions.remove(0);

                }

            }

        } else {
            for (TypeInteractions interaction : typeEffectivenessTable.values()) {
                for (int i = 0; i < Type.values().length; i++) {
                    Type defenderType = EmeraldEXConstants.byteToType(i);

                    int multiplierRoll = random.nextInt(100);
                    float multiplier = 1;

                    if (multiplierRoll > 90) {
                        multiplier = 0f;
                    } else if (multiplierRoll > 75) {
                        multiplier = 0.5f;
                    } else if (multiplierRoll > 50) {
                        multiplier = 2.0f;
                    }
                    interaction.setDefenderMultiplier(defenderType, multiplier);

                }
            }
        }

        int typeTableOffset = romEntry.getValue("TypeEffectivenessTable");
        for (int i = 0; i < Type.values().length; i++) {
            TypeInteractions typeInteractions = typeEffectivenessTable.get(EmeraldEXConstants.byteToType(i));
            for (int j = 0; j < Type.values().length; j++) {
                Type defenderType = EmeraldEXConstants.byteToType(j);
                int bytesIn_uq4_12 = 4;
                int rowLength = bytesIn_uq4_12 * (Type.values().length);
                int offsetInRow = j * bytesIn_uq4_12;
                Float multiplier = typeInteractions.getDefenderMultiplier(defenderType);
                writeLong(typeTableOffset + (i * rowLength) + offsetInRow, Math.round(multiplier * 4096));
            }
        }
    }

    @Override
    public void saveGenRestrictionsToRom(GenRestrictions currentRestrictions) {
        int genRestrictionsOffset = (romEntry.getValue("StaticVars") + (EmeraldEXConstants.GEN_RESTRICTIONS_INDEX * 2));
        if (genRestrictionsOffset != 0) {
            writeWord(genRestrictionsOffset, currentRestrictions.toInt());
        }
    }

    @Override
    public String getTypeInteractionsLog(Settings.TypeChartMod typeChartMod) {
        String result = "|        " + typeEffectivenessTable.keySet()
                                                            .stream()
                                                            .map(t -> "|" + String.format("%-" + 8 + "s", t.displayName()))
                                                            .collect(Collectors.joining(""));
        result += "\n";
        result += typeEffectivenessTable.values().stream().map(Objects::toString).collect(Collectors.joining("\n"));

        if (typeChartMod == Settings.TypeChartMod.SHUFFLE_ROW) {
            result += "\n\n";
            result += "TYPE     | NOW HAS THE VANILLA EFFECTIVENESS OF\n";
            result += typeEffectivenessTable.values()
                                            .stream()
                                            .filter(t -> t.getAttackerType() != Type.STELLAR)
                                            .filter(t -> t.getAttackerType() != Type.NONE)
                                            .filter(t -> t.getAttackerType() != Type.MYSTERY)
                                            .map(t -> String.format("%-" + 8 + "s", t.getAttackerType().displayName()) +
                                                    " | " +
                                                    String.format("%-" + 8 + "s", t.getOriginalType().displayName()))
                                            .collect(Collectors.joining("\n"));

        }

        result += "\n";
        return result;
    }
}
