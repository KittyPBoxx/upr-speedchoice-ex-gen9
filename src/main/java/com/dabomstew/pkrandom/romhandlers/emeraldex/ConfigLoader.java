package com.dabomstew.pkrandom.romhandlers.emeraldex;

import com.dabomstew.pkrandom.FileFunctions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConfigLoader {

    public static List<RomEntry> loadConfigFile(String name) {

        List<RomEntry> roms = new ArrayList<>();
        RomEntry current = new RomEntry();

        try {

            Scanner sc = new Scanner(FileFunctions.openConfig(name), "UTF-8");

            while (sc.hasNextLine()) {

                String q = removeComments(sc.nextLine().trim());

                if (!q.isEmpty()) {

                    if (isHeaderOrArray(q)) {

                        // New rom
                        current.setName(q.substring(1, q.length() - 1));
                        roms.add(current);

                    } else {

                        String[] r = q.split("=", 2);

                        if (r.length == 1) {
                            System.err.println("invalid entry " + q);
                            continue;
                        }
                        
                        String key = r[0];
                        String value = getValue(r[1]);

                        switch (key)
                        {
                            case "StaticPokemon[]":
                                parseStaticMon(value, current);
                                break;
                            case "GivenItems[]":
                                parseGivenItems(value, current);
                                break;
                            case "TutorMoves[]":
                                parseTutorMoves(value, current);
                                break;
                            case "Marts[]":
                                parseMarts(value, current);
                                break;
                            case "Game":
                                current.setRomCode(value);
                                break;
                            case "Version":
                                current.setVersion(parseRIInt(value));
                                break;
                            case "MD5Hash":
                                current.setHash(value);
                                break;
                            case "TableFile":
                                current.setTableFile(value);
                                break;
                            case "Tweak":
                                current.getCodeTweaks().put(key, value);
                                break;
                            default:
                                parseGeneralProperty(value, current, key);
                        }

                    }
                }
            }
            sc.close();
        } catch (IOException e) {
            // Do Nothing
        }

        return roms;
    }

    private static void parseGeneralProperty(String value, RomEntry current, String key) {
        if (isHeaderOrArray(value)) {
            String[] offsets = value.substring(1, value.length() - 1).split(",");
            if (offsets.length == 1 && offsets[0].trim().isEmpty()) {
                current.getArrayEntries().put(key, new int[0]);
            } else {
                int[] offs = new int[offsets.length];
                int c = 0;
                for (String off : offsets) {
                    offs[c++] = parseRIInt(off);
                }
                current.getArrayEntries().put(key, offs);
            }
        } else {
            int offs = parseRIInt(value);
            current.getEntries().put(key, offs);
        }
    }

    private static void parseGivenItems(String value, RomEntry current) {
        if (isHeaderOrArray(value)) {
            String[] parts = value.substring(1, value.length() - 1).split(",");
            List<Integer> offsets = new ArrayList<>();
            for (String part : parts) {
                String offset = part.substring(1, part.length() - 1);
                offsets.add(Integer.decode(offset));
            }
            current.getGivenItems().add(new GivenItem(offsets));
        }
    }

    private static void parseTutorMoves(String value, RomEntry current) {
        if (isHeaderOrArray(value)) {
            String[] parts = value.substring(1, value.length() - 1).split(",");
            List<Integer> offsets = new ArrayList<>();
            for (String part : parts) {
                String offset = part.substring(1, part.length() - 1);
                offsets.add(Integer.decode(offset));
            }
            current.getTutorMoves().add(new TutorMove(offsets));
        }
    }


    private static void parseMarts(String value, RomEntry current) {
        if (isHeaderOrArray(value)) {
            String[] parts = value.substring(1, value.length() - 1).split("],\\[");
            List<Integer[]> offsetSizeList = new ArrayList<>();
            for (String part : parts) {
                String[] offsetSize = part.replaceAll("]", "")
                                          .replaceAll("\\[", "")
                                          .split(",");
                Integer[] offsetSizeArray = new Integer[2];
                offsetSizeArray[0] = Integer.decode(offsetSize[0]);
                offsetSizeArray[1] = Integer.decode(offsetSize[1]);
                offsetSizeList.add(offsetSizeArray);
            }
            current.getMarts().add(new Mart(offsetSizeList));
        }
    }

    private static void parseStaticMon(String value, RomEntry current) {
        if (isHeaderOrArray(value)) {
            String[] offsets = value.substring(1, value.length() - 1).split(",");
            int[] offs = new int[offsets.length];
            int c = 0;
            for (String off : offsets) {
                offs[c++] = parseRIInt(off);
            }
            current.getStaticPokemon().add(new StaticPokemon(offs));
        } else {
            int offs = parseRIInt(value);
            current.getStaticPokemon().add(new StaticPokemon(offs));
        }
    }

    private static boolean isHeaderOrArray(String q) {
        return q.startsWith("[") && q.endsWith("]");
    }

    private static String removeComments(String q) {
        if (q.contains("//")) {
            q = q.substring(0, q.indexOf("//")).trim();
        }
        return q;
    }

    private static String getValue(String value) {
        if (value.endsWith("\r\n")) {
            value = value.substring(0, value.length() - 2);
        }
        return value.trim();
    }

    private static int parseRIInt(String off) {
        int radix = 10;
        off = off.trim().toLowerCase();
        if (off.startsWith("0x") || off.startsWith("&h")) {
            radix = 16;
            off = off.substring(2);
        }
        try {
            return Integer.parseInt(off, radix);
        } catch (NumberFormatException ex) {
            System.err.println("invalid base " + radix + "number " + off);
            return 0;
        }
    }
}
