package com.dabomstew.pkrandom.pokemon;

public class IngameTrade {

    private int id;
    private Pokemon requestedPokemon;
    private Pokemon givenPokemon;
    private String nickname;
    private String otName;
    private int otId;
    private int[] ivs = new int[0];
    private int item = 0;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Pokemon getRequestedPokemon() {
        return requestedPokemon;
    }

    public void setRequestedPokemon(Pokemon requestedPokemon) {
        this.requestedPokemon = requestedPokemon;
    }

    public Pokemon getGivenPokemon() {
        return givenPokemon;
    }

    public void setGivenPokemon(Pokemon givenPokemon) {
        this.givenPokemon = givenPokemon;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getOtName() {
        return otName;
    }

    public void setOtName(String otName) {
        this.otName = otName;
    }

    public int getOtId() {
        return otId;
    }

    public void setOtId(int otId) {
        this.otId = otId;
    }

    public int[] getIvs() {
        return ivs;
    }

    public void setIvs(int[] ivs) {
        this.ivs = ivs;
    }

    public void setIv(int index, int value) {
        this.getIvs()[index] = value;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }
}
