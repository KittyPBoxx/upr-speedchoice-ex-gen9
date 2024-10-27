package com.dabomstew.pkrandom.pokemon;

public class ItemLocation {
    
    private String description;
    private int item;

    public ItemLocation(String description, int item) {
        super();
        this.description = description;
        this.item = item;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }
}
