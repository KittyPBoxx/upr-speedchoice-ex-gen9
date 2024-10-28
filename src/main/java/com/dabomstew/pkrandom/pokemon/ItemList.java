package com.dabomstew.pkrandom.pokemon;

import com.dabomstew.pkrandom.constants.EmeraldEXConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.dabomstew.pkrandom.constants.EmeraldEXConstants.ItemConstants.*;

public class ItemList {

    private final boolean[] items;
    private final boolean[] tms;

    private List<int[]> groups;

    public ItemList(int highestIndex) {
        items = new boolean[highestIndex + 1];
        tms = new boolean[highestIndex + 1];
        for (int i = 1; i <= highestIndex; i++) {
            items[i] = true;
        }
    }

    public boolean isTM(int index) {
        if (index < 0 || index >= tms.length) {
            return false;
        }
        return tms[index];
    }

    public boolean isAllowed(int index) {
        if (index < 0 || index >= tms.length) {
            return false;
        }
        return items[index];
    }

    public void banSingles(int... indexes) {
        for (int index : indexes) {
            items[index] = false;
        }
    }

    public void banRange(int startIndex, int length) {
        for (int i = 0; i < length; i++) {
            items[i + startIndex] = false;
        }
    }

    public void tmRange(int startIndex, int length) {
        for (int i = 0; i < length; i++) {
            tms[i + startIndex] = true;
        }
    }

    public int randomItem(Random random) {
        int group = random.nextInt(groups.size());
        int[] groupRange = groups.get(group);
        int chosen = 0;
        while (!items[chosen]) {
            chosen = groupRange[0] + random.nextInt(groupRange[1] - groupRange[0]);
        }
        return chosen;
    }

    public int randomNonTM(Random random) {
        int group = random.nextInt(groups.size());
        int[] groupRange = groups.get(group);
        int chosen = 0;
        while (!items[chosen] || tms[chosen]) {
            chosen = groupRange[0] + random.nextInt(groupRange[1] - groupRange[0]);
        }
        return chosen;
    }

    public int randomTM(Random random) {
        int chosen = 0;
        while (!tms[chosen]) {
            chosen = random.nextInt(items.length);
        }
        return chosen;
    }

    public int randomBall(Random random) {
        return random.nextInt((BALLS_END + 1) - BALLS_START) + BALLS_START;
    }

    public int randomRepel(Random random) {
        return random.nextInt((MAX_REPEL + 1) - REPEL_START) + REPEL_START;
    }

    public int randomMedicine(Random random) {
        return random.nextInt((MEDICINE_END + 1) - MEDICINE_START) + MEDICINE_START;
    }

    public int randomXItem(Random random) {
        return random.nextInt((X_ITEMS_END + 1) - X_ITEMS_START) + X_ITEMS_START;
    }

    public ItemList copy() {
        ItemList other = new ItemList(items.length - 1);
        System.arraycopy(items, 0, other.items, 0, items.length);
        System.arraycopy(tms, 0, other.tms, 0, tms.length);
        if (groups != null) {
            other.setGroups(new ArrayList<>(groups));
        }
        return other;
    }

    public void setGroups(List<int[]> groups) {
        this.groups = groups;
    }

    public void configureGroups(List<int[]> groups)
    {
        this.groups = groups.stream().filter(g -> {
            for (int itemIndex : IntStream.range(g[0], g[1]).toArray()) {
                if (items[itemIndex]) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

}
