package com.dabomstew.pkrandom.romhandlers.emeraldex;

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

public class EmeraldExRomHandlerFactory extends RomHandler.Factory {

    private static final String CONFIG_FILE_NAME = "emerald_ex_offsets.ini";

    private static final Integer MAX_BYTES = 0x100000;

    private List<RomEntry> roms;

    public EmeraldExRomHandlerFactory() {
        init();
    }

    @Override
    public EmeraldEXRomHandler create(Random random, PrintStream logStream) {
        return new EmeraldEXRomHandler(random, logStream, roms);
    }

    public boolean isLoadable(String filename) {
        long fileLength = new File(filename).length();
        if (fileLength > 32 * 1024 * 1024) {
            return false;
        }
        byte[] loaded = loadFilePartial(filename);
        if (loaded.length == 0) {
            // nope
            return false;
        }
        return EmeraldExRomUtils.detectRomInner(loaded, (int) fileLength, roms);
    }

    private void init()
    {
        roms = ConfigLoader.loadConfigFile(CONFIG_FILE_NAME);
    }

    private static byte[] loadFilePartial(String filename) {
        try {
            File fh = new File(filename);
            if (!fh.exists() || !fh.isFile() || !fh.canRead()) {
                return new byte[0];
            }
            long fileSize = fh.length();
            if (fileSize > Integer.MAX_VALUE) {
                return new byte[0];
            }
            FileInputStream fis = new FileInputStream(filename);
            byte[] file = FileFunctions.readFullyIntoBuffer(fis, Math.min((int) fileSize, MAX_BYTES));
            fis.close();
            return file;
        } catch (IOException ex) {
            return new byte[0];
        }
    }
}
