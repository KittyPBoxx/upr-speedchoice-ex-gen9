package com.dabomstew.pkrandom.warps;

import java.util.Map;
import java.util.stream.Collectors;

public class WarpData {

    private EscapePaths escapePaths;

    private Flags flags;

    private KeyLocations keyLocations;

    private Map<String, Warp> warps;

    public void setEscapePaths(EscapePaths escapePaths) {
        this.escapePaths = escapePaths;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public void setKeyLocations(KeyLocations keyLocations) {
        this.keyLocations = keyLocations;
    }

    public void setWarps(Map<String, Warp> warps) {
        this.warps = warps;
    }

    public EscapePaths getEscapePaths() {
        return escapePaths;
    }

    public Flags getFlags() {
        return flags;
    }

    public KeyLocations getKeyLocations() {
        return keyLocations;
    }

    public Map<String, Warp> getWarps() {

        return warps.entrySet()
                    .stream()
                    .peek(entry -> entry.getValue().setId(entry.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
    }
}
