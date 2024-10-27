package com.dabomstew.pkrandom.warps;

import java.util.List;

public class EscapePaths {

    private List<List<String>> paths;

    public List<List<String>> getPaths() {
        return paths;
    }

    public void setPaths(List<List<String>> paths) {
        this.paths = paths;
    }

    @Override
    public String toString() {
        return "EscapePaths{" +
                "paths=" + paths +
                '}';
    }

}
