package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  FileFunctions.java - functions relating to file I/O.                  --*/
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

import com.dabomstew.pkrandom.gui.RandomizerGUI;
import com.davidehrmann.vcdiff.VCDiffDecoder;
import com.davidehrmann.vcdiff.VCDiffDecoderBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class FileFunctions {

    public static File fixFilename(File original, String defaultExtension) {
        return fixFilename(original, defaultExtension, null);
    }

    // Behavior:
    // if file has no extension, add defaultExtension
    // if there are banned extensions & file has a banned extension, replace
    // with defaultExtension
    // else, leave as is
    public static File fixFilename(File original, String defaultExtension, List<String> bannedExtensions) {
        String filename = original.getName();
        if (filename.lastIndexOf('.') >= filename.length() - 5 && filename.lastIndexOf('.') != filename.length() - 1
                && filename.length() > 4 && filename.lastIndexOf('.') != -1) {
            // valid extension, read it off
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (bannedExtensions != null && bannedExtensions.contains(ext)) {
                // replace with default
                filename = filename.substring(0, filename.lastIndexOf('.') + 1) + defaultExtension;
            }
            // else no change
        } else {
            // add extension
            filename += "." + defaultExtension;
        }
        return new File(original.getAbsolutePath().replace(original.getName(), "") + filename);
    }

    private static final List<String> overrideFiles = Arrays.asList(SysConstants.customNamesFile,
            SysConstants.tclassesFile, SysConstants.tnamesFile, SysConstants.nnamesFile);

    public static boolean configExists(String filename) {
        if (overrideFiles.contains(filename)) {
            File fh = new File(SysConstants.ROOT_PATH + filename);
            if (fh.exists() && fh.canRead()) {
                return true;
            }
            fh = new File("./" + filename);
            if (fh.exists() && fh.canRead()) {
                return true;
            }
        }

        return FileFunctions.class.getResourceAsStream("/config/" + filename) != null;
    }

    public static InputStream openConfig(String filename) throws IOException {
        if (overrideFiles.contains(filename)) {
            File fh = new File(SysConstants.ROOT_PATH + filename);
            if (fh.exists() && fh.canRead()) {
                return Files.newInputStream(fh.toPath());
            }
            fh = new File("./" + filename);
            if (fh.exists() && fh.canRead()) {
                return Files.newInputStream(fh.toPath());
            }
        }
        return FileFunctions.class.getResourceAsStream("/config/" + filename);
    }

    public static CustomNamesSet getCustomNames() throws IOException {
        InputStream resourceStream = FileFunctions.class.getResourceAsStream("/config/" + SysConstants.customNamesFile);
        List<String> names = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.toList());

        return new CustomNamesSet(names);
    }

    public static int readFullInt(byte[] data, int offset) {
        ByteBuffer buf = ByteBuffer.allocate(4).put(data, offset, 4);
        buf.rewind();
        return buf.getInt();
    }

    public static int read2ByteInt(byte[] data, int index) {
        return (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8);
    }

    public static void writeFullInt(byte[] data, int offset, int value) {
        byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
        System.arraycopy(valueBytes, 0, data, offset, 4);
    }

    public static byte[] readFileFullyIntoBuffer(String filename) throws IOException {
        File fh = new File(filename);
        if (!fh.exists() || !fh.isFile() || !fh.canRead()) {
            throw new FileNotFoundException(filename);
        }
        long fileSize = fh.length();
        if (fileSize > Integer.MAX_VALUE) {
            throw new IOException(filename + " is too long to read in as a byte-array.");
        }
        FileInputStream fis = new FileInputStream(filename);
        byte[] buf = readFullyIntoBuffer(fis, (int) fileSize);
        fis.close();
        return buf;
    }

    public static byte[] readFullyIntoBuffer(InputStream in, int bytes) throws IOException {
        byte[] buf = new byte[bytes];
        readFully(in, buf, 0, bytes);
        return buf;
    }

    public static void readFully(InputStream in, byte[] buf, int offset, int length) throws IOException {
        int offs = 0, read = 0;
        while (offs < length && (read = in.read(buf, offs + offset, length - offs)) != -1) {
            offs += read;
        }
    }

    public static void writeBytesToFile(String filename, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(data);
        fos.close();
    }

    public static int getFileChecksum(String filename) {
        try {
            return getFileChecksum(openConfig(filename));
        } catch (IOException e) {
            return 0;
        }
    }

    public static int getFileChecksum(InputStream stream) {
        Scanner sc = new Scanner(stream, "UTF-8");
        CRC32 checksum = new CRC32();
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (!line.isEmpty()) {
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                checksum.update(bytes, 0, bytes.length);
            }
        }
        sc.close();
        return (int) checksum.getValue();
    }

    public static boolean checkOtherCRC(byte[] data, int byteIndex, int switchIndex, String filename, int offsetInData) {
        // If the switch at data[byteIndex].switchIndex is on, then check that
        // the CRC at data[offsetInData] ... data[offsetInData+3] matches the
        // CRC of filename.
        // If not, return false.
        // If any other case, return true.
        int switches = data[byteIndex] & 0xFF;
        if (((switches >> switchIndex) & 0x01) == 0x01) {
            // have to check the CRC
            int crc = readFullInt(data, offsetInData);

            return getFileChecksum(filename) == crc;
        }
        return true;
    }

    public static File getJarDirectory() throws URISyntaxException {
        File jarDirectory;
        URI probablyJarPath = RandomizerGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        jarDirectory = new File(probablyJarPath).toPath().getParent().toFile();

        if (!jarDirectory.exists()) {
            System.out.println("Couldn't apply patch. Could not access directory for jar " + probablyJarPath.getPath());
        }

        return jarDirectory;
    }

    public static byte[] tryFindPatchFile(String filename) throws IOException, URISyntaxException {

        File jarDirectory = getJarDirectory();

        if (!jarDirectory.exists()) {
            throw new IOException("Could not find patch file.");
        }

        File[] files = jarDirectory.listFiles(f -> f.getName().equals(filename));

        if (files == null || files.length < 1) {
            System.out.println("Couldn't find patch with name " + filename + " in directory " + jarDirectory.getPath());
            throw new IOException("Could not find patch file.");
        }

        ArrayList<File> matchingFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(files)));
        FileInputStream is = new FileInputStream(matchingFiles.get(0));

        byte[] buf = readFullyIntoBuffer(is, is.available());
        is.close();
        return buf;
    }

    public static byte[] downloadFile(String url) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int count;
        while ((count = in.read(buf, 0, 1024)) != -1) {
            out.write(buf, 0, count);
        }
        in.close();
        return out.toByteArray();
    }

    /**
     *  xdelta3 (-S -A -n) flags are required for compatibility. The command bellow should give an optimal patch
     *  -
     *  xdelta3 -S -A -n -s original_us_emerald.gba newly_compiled_emerald.gba patch.xdelta
     */
    public static byte[] applyPatch(byte[] rom, String patchName) throws IOException {

        try {
            byte[] patch = tryFindPatchFile(patchName);
            VCDiffDecoder decoder = VCDiffDecoderBuilder.builder().buildSimple();
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            byte[] expandedRom = new byte[32 * 1024 * 1024];
            System.arraycopy(rom, 0, expandedRom, 0, rom.length);

            decoder.decode(expandedRom, patch, result);
            return result.toByteArray();
        } catch (URISyntaxException e) {
            throw new IOException("Could not apply patch.");
        }

    }
}