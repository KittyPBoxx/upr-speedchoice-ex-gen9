package com.dabomstew.pkrandom.pokemon;

public class ItemBans {

    private boolean banBadItems;
    private boolean banSlateportItems;

    public boolean isBanBadItems() {
        return banBadItems;
    }

    public ItemBans setBanBadItems(boolean banBadItems) {
        this.banBadItems = banBadItems;
        return this;
    }

    public boolean isBanSlateportItems() {
        return banSlateportItems;
    }

    public ItemBans setBanSlateportItems(boolean banSlateportItems) {
        this.banSlateportItems = banSlateportItems;
        return this;
    }
}
