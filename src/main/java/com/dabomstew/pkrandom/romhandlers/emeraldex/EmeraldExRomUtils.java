package com.dabomstew.pkrandom.romhandlers.emeraldex;

import com.dabomstew.pkrandom.Utils;
import com.dabomstew.pkrandom.constants.EmeraldEXConstants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class EmeraldExRomUtils {

    private static final String CLEAN_EMERALD_US_MD5 = "779FE3EF5965917C0BA13E4F0F96C78F";

    public static boolean detectRomInner(byte[] rom, int romSize, List<RomEntry> roms) {

        if (romSize == 16 * 1024 * 1024) {
            // If they try and load a vanilla us copy of emerald we patch it
            return isVanillaUSEmerald(rom);
        }

        if (romSize != 32 * 1024 * 1024) {
            return false; // size check
        }

        for (RomEntry re : roms) {
            if (romCode(rom, re.getRomCode()) && (rom[EmeraldEXConstants.romVersionOffset] & 0xFF) == re.getVersion()) {
                if (re.getHash() != null && rom.length == romSize) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] digest = md.digest(rom);
                        String hash = Utils.toHexString(digest);
                        return hash.equalsIgnoreCase(re.getHash());
                    } catch (NoSuchAlgorithmException e) {
                        return false;
                    }
                }
                return true; // match
            }
        }
        return false; // GBA rom we don't support yet
    }

    public static boolean isVanillaUSEmerald(byte[] rom) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rom);
            String hash = Utils.toHexString(digest);
            return hash.equals(CLEAN_EMERALD_US_MD5);
        } catch (NoSuchAlgorithmException e) {
            /* Do Nothing */
        }

        return false;
    }

    public static boolean romCode(byte[] rom, String codeToCheck) {
        int sigOffset = EmeraldEXConstants.romCodeOffset;
        byte[] sigBytes = codeToCheck.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < sigBytes.length; i++) {
            if (rom[sigOffset + i] != sigBytes[i]) {
                return false;
            }
        }
        return true;

    }
}
