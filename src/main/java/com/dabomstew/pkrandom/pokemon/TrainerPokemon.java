package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  TrainerPokemon.java - represents a Pokemon owned by a trainer.        --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

public class TrainerPokemon {

    private Pokemon pokemon;
    private int level;

    private int move1;
    private int move2;
    private int move3;
    private int move4;

    private int AILevel;
    private int heldItem;
    
    private boolean resetMoves = false;

    public Pokemon getPokemon() {
        return pokemon;
    }

    public void setPokemon(Pokemon pokemon) {
        this.pokemon = pokemon;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMove1() {
        return move1;
    }

    public void setMove1(int move1) {
        this.move1 = move1;
    }

    public int getMove2() {
        return move2;
    }

    public void setMove2(int move2) {
        this.move2 = move2;
    }

    public int getMove3() {
        return move3;
    }

    public void setMove3(int move3) {
        this.move3 = move3;
    }

    public int getMove4() {
        return move4;
    }

    public void setMove4(int move4) {
        this.move4 = move4;
    }

    public int getAILevel() {
        return AILevel;
    }

    public void setAILevel(int AILevel) {
        this.AILevel = AILevel;
    }

    public int getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(int heldItem) {
        this.heldItem = heldItem;
    }

    public boolean isResetMoves() {
        return resetMoves;
    }

    public void setResetMoves(boolean resetMoves) {
        this.resetMoves = resetMoves;
    }

    public String toString() {
        return pokemon == null ? "unset" : pokemon.getName() + " Lv" + level;
    }

}
