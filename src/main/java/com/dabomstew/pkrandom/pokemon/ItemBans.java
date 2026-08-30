package com.dabomstew.pkrandom.pokemon;

public class ItemBans {

    private boolean banBadItems;
    private boolean banSlateportItems;
    private boolean banEvItems;
    private boolean banBattleItems;

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

    public boolean isBanEvItems() {
        return banEvItems;
    }

    public ItemBans setBanEvItems(boolean banEvItems) {
        this.banEvItems = banEvItems;
        return this;
    }

    public boolean isBanBattleItems() {
        return banBattleItems;
    }

    public ItemBans setBanBattleItems(boolean banBattleItems) {
        this.banBattleItems = banBattleItems;
        return this;
    }
}
