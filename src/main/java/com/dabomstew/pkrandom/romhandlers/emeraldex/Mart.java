package com.dabomstew.pkrandom.romhandlers.emeraldex;

import java.util.ArrayList;
import java.util.List;

public class Mart {

    // The reason a single mart can have multiple offsets is some expand their items based on game flags (and use different tables)
    private final List<MartInventory> martInventory;

    public Mart(List<Integer[]> offsetSizeList) {
        martInventory = new ArrayList<>();
        for (Integer[] offsetSize : offsetSizeList)
        {
            martInventory.add(new MartInventory(offsetSize[0], offsetSize[1]));
        }
    }

    public List<MartInventory> getMartInventories() {
        return martInventory;
    }

    public int largestInventory() {
        return martInventory.stream().mapToInt(MartInventory::getSize).max().orElse(0);
    }

    public void resetState() {
        martInventory.stream().forEach(i -> i.resetState());
    }

    public static class MartInventory {

        private final int offset;

        private final int size;

        private boolean complete;

        public MartInventory(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }

        public boolean isComplete() {
            return complete;
        }

        public void markComplete() {
            this.complete = true;
        }

        public void resetState() {
            this.complete = false;
        }
    }
}
