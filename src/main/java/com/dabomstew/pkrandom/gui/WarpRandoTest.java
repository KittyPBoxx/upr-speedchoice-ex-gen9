package com.dabomstew.pkrandom.gui;

import com.dabomstew.pkrandom.romhandlers.emeraldex.EmeraldExWarpRandomizer;
import com.dabomstew.pkrandom.warps.WarpConfig;
import com.dabomstew.pkrandom.warps.WarpData;
import com.dabomstew.pkrandom.warps.WarpRemapping;

import java.util.List;
import java.util.stream.Collectors;

public class WarpRandoTest {

    public static void main(String[] args) {

        int hoennLevel = 10;
        boolean extraDeadendRemoval = true;
        int seed = 12345;
        boolean inGymOrder = true;

        WarpData warpData = EmeraldExWarpRandomizer.loadRandomWarpData();
        WarpConfig warpConfig = new WarpConfig(hoennLevel, extraDeadendRemoval, seed, inGymOrder);
        List<WarpRemapping> remappings = EmeraldExWarpRandomizer.getRandomWarps(warpConfig, warpData);
        System.out.println(remappings.stream().map(WarpRemapping::toString).collect(Collectors.joining("\n")));
    }



}
