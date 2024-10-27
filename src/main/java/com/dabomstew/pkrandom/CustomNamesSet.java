package com.dabomstew.pkrandom;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class CustomNamesSet {

    private final List<String> trainerNames;
    private final List<String> trainerClasses;
    private final List<String> doublesTrainerNames;
    private final List<String> doublesTrainerClasses;
    private final List<String> pokemonNicknames;

    private static final int CUSTOM_NAMES_VERSION = 2;

    // Standard constructor: read binary data from an input stream.
    public CustomNamesSet(List<String> names) throws IOException {
        names = names.stream().filter(n -> !n.contains("#") && !n.isEmpty()).collect(Collectors.toList());
        trainerNames = names.subList(0, 60);
        trainerClasses = names.subList(60, 120);
        doublesTrainerNames = names.subList(120, 180);
        doublesTrainerClasses = names.subList(180, 240);
        pokemonNicknames = names.subList(240, 300);
    }

    // Alternate constructor: blank all lists
    // Used for importing old names and on the editor dialog.
    public CustomNamesSet() {
        trainerNames = new ArrayList<>();
        trainerClasses = new ArrayList<>();
        doublesTrainerNames = new ArrayList<>();
        doublesTrainerClasses = new ArrayList<>();
        pokemonNicknames = new ArrayList<>();
    }

    public List<String> getData() throws IOException {
        ArrayList<String> customNames = new ArrayList<>();
        customNames.addAll(trainerNames);
        customNames.addAll(trainerClasses);
        customNames.addAll(doublesTrainerNames);
        customNames.addAll(doublesTrainerClasses);
        customNames.addAll(pokemonNicknames);
        return customNames;
    }

    public List<String> getTrainerNames() {
        return Collections.unmodifiableList(trainerNames);
    }

    public List<String> getTrainerClasses() {
        return Collections.unmodifiableList(trainerClasses);
    }

    public List<String> getDoublesTrainerNames() {
        return Collections.unmodifiableList(doublesTrainerNames);
    }

    public List<String> getDoublesTrainerClasses() {
        return Collections.unmodifiableList(doublesTrainerClasses);
    }

    public List<String> getPokemonNicknames() {
        return Collections.unmodifiableList(pokemonNicknames);
    }
    
    public void setTrainerNames(List<String> names) {
        trainerNames.clear();
        trainerNames.addAll(names);
    }
    
    public void setTrainerClasses(List<String> names) {
        trainerClasses.clear();
        trainerClasses.addAll(names);
    }
    
    public void setDoublesTrainerNames(List<String> names) {
        doublesTrainerNames.clear();
        doublesTrainerNames.addAll(names);
    }
    
    public void setDoublesTrainerClasses(List<String> names) {
        doublesTrainerClasses.clear();
        doublesTrainerClasses.addAll(names);
    }
    
    public void setPokemonNicknames(List<String> names) {
        pokemonNicknames.clear();
        pokemonNicknames.addAll(names);
    }

    public static CustomNamesSet importOldNames() throws IOException {
        CustomNamesSet cns = new CustomNamesSet();

        // Trainer Names
        if (FileFunctions.configExists(SysConstants.tnamesFile)) {
            Scanner sc = new Scanner(FileFunctions.openConfig(SysConstants.tnamesFile), StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String trainername = sc.nextLine().trim();
                if (trainername.isEmpty()) {
                    continue;
                }
                if (trainername.startsWith("\uFEFF")) {
                    trainername = trainername.substring(1);
                }
                if (trainername.contains("&")) {
                    cns.doublesTrainerNames.add(trainername);
                } else {
                    cns.trainerNames.add(trainername);
                }
            }
            sc.close();
        }

        // Trainer Classes
        if (FileFunctions.configExists(SysConstants.tclassesFile)) {
            Scanner sc = new Scanner(FileFunctions.openConfig(SysConstants.tclassesFile), StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String trainerClassName = sc.nextLine().trim();
                if (trainerClassName.isEmpty()) {
                    continue;
                }
                if (trainerClassName.startsWith("\uFEFF")) {
                    trainerClassName = trainerClassName.substring(1);
                }
                String checkName = trainerClassName.toLowerCase();
                int idx = (checkName.endsWith("couple") || checkName.contains(" and ") || checkName.endsWith("kin")
                        || checkName.endsWith("team") || checkName.contains("&") || (checkName.endsWith("s") && !checkName
                        .endsWith("ss"))) ? 1 : 0;
                if (idx == 1) {
                    cns.doublesTrainerClasses.add(trainerClassName);
                } else {
                    cns.trainerClasses.add(trainerClassName);
                }
            }
            sc.close();
        }

        // Nicknames
        if (FileFunctions.configExists(SysConstants.nnamesFile)) {
            Scanner sc = new Scanner(FileFunctions.openConfig(SysConstants.nnamesFile), StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String nickname = sc.nextLine().trim();
                if (nickname.isEmpty()) {
                    continue;
                }
                if (nickname.startsWith("\uFEFF")) {
                    nickname = nickname.substring(1);
                }
                cns.pokemonNicknames.add(nickname);
            }
            sc.close();
        }

        return cns;
    }

}
