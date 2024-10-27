package com.dabomstew.pkrandom.warps;

import java.util.Map;
import java.util.Set;

public class KeyLocations {
    private Map<String, String> locationsTrigger;
    private Map<String, String> keyLocations;
    private Set<String> rootCandidates;
    private Set<String> oddOnOutWarps;
    private Set<String> oddOnOutWithDeadendsRemovedWarps;

    // Getters and Setters
    public Map<String, String> getLocationsTrigger() {
        return locationsTrigger;
    }

    public void setLocationsTrigger(Map<String, String> locationsTrigger) {
        this.locationsTrigger = locationsTrigger;
    }

    public Map<String, String> getKeyLocations() {
        return keyLocations;
    }

    public void setKeyLocations(Map<String, String> keyLocations) {
        this.keyLocations = keyLocations;
    }

    public Set<String> getRootCandidates() {
        return rootCandidates;
    }

    public void setRootCandidates(Set<String> rootCandidates) {
        this.rootCandidates = rootCandidates;
    }

    public Set<String> getOddOnOutWarps() {
        return oddOnOutWarps;
    }

    public void setOddOnOutWarps(Set<String> oddOnOutWarps) {
        this.oddOnOutWarps = oddOnOutWarps;
    }

    public Set<String> getOddOnOutWithDeadendsRemovedWarps() {
        return oddOnOutWithDeadendsRemovedWarps;
    }

    public void setOddOnOutWithDeadendsRemovedWarps(Set<String> oddOnOutWithDeadendsRemovedWarps) {
        this.oddOnOutWithDeadendsRemovedWarps = oddOnOutWithDeadendsRemovedWarps;
    }

    @Override
    public String toString() {
        return "KeyLocations{" +
                "LOCATIONS_TRIGGER=" + locationsTrigger +
                ", KEY_LOCATIONS=" + keyLocations +
                '}';
    }
}
