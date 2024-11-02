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
        long seed = 147914221764327L;
        boolean inGymOrder = false;

//        for (long i = 0; i < 100; i++)
//        {
//            WarpData warpData = EmeraldExWarpRandomizer.loadRandomWarpData();
//            WarpConfig warpConfig = new WarpConfig(hoennLevel, extraDeadendRemoval, seed + 1, inGymOrder);
//            List<WarpRemapping> remappings = EmeraldExWarpRandomizer.getRandomWarps(warpConfig, warpData);
//            System.out.println("Finished for seed " + i);
//        }

        WarpData warpData = EmeraldExWarpRandomizer.loadRandomWarpData();
        WarpConfig warpConfig = new WarpConfig(hoennLevel, extraDeadendRemoval, seed, inGymOrder);
        List<WarpRemapping> remappings = EmeraldExWarpRandomizer.getRandomWarps(warpConfig, warpData);
        System.out.println(remappings.stream().map(WarpRemapping::toString).collect(Collectors.joining("\n")));
    }



}
