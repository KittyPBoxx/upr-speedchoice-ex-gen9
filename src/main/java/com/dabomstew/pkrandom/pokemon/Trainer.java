package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Trainer.java - represents a Trainer's pokemon set/other details.      --*/
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Trainer implements Comparable<Trainer> {
    private int offset;
    private List<TrainerPokemon> pokemon = new ArrayList<>();
    private String tag;
    private String name;
    private int trainerclass;
    private String fullDisplayName;
    private boolean doubleBattle;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<TrainerPokemon> getPokemon() {
        return pokemon;
    }

    public void setPokemon(List<TrainerPokemon> pokemon) {
        this.pokemon = pokemon;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTrainerclass() {
        return trainerclass;
    }

    public void setTrainerclass(int trainerclass) {
        this.trainerclass = trainerclass;
    }

    public String getFullDisplayName() {
        return fullDisplayName;
    }

    public void setFullDisplayName(String fullDisplayName) {
        this.fullDisplayName = fullDisplayName;
    }

    public boolean isDoubleBattle() {
        return doubleBattle;
    }

    public void setDoubleBattle(boolean doubleBattle) {
        this.doubleBattle = doubleBattle;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if (fullDisplayName != null) {
            sb.append(fullDisplayName).append(" ");
        } else if (name != null) {
            sb.append(name).append(" ");
        }
        if (trainerclass != 0) {
            sb.append("(").append(trainerclass).append(") - ");
        }
        sb.append(String.format("%x", offset));
        sb.append(" => ");
        boolean first = true;
        for (TrainerPokemon p : pokemon) {
            if (p.getPokemon() == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            sb.append(p.getPokemon().getName()).append(" Lv").append(p.getLevel());
            first = false;
        }
        sb.append(']');
        if (tag != null) {
            sb.append(" (").append(tag).append(")");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + offset;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Trainer other = (Trainer) obj;
        return offset == other.offset;
    }

    @Override
    public int compareTo(Trainer o) {
        return offset - o.offset;
    }

    public void removeEmptyPokemon() {
        pokemon = pokemon.stream().filter(p -> p.getPokemon() != null).collect(Collectors.toList());
    }

    public void fillPokemon() {

        Optional<TrainerPokemon> firstMon = pokemon.stream().filter(p -> p.getPokemon() != null).findFirst();
        firstMon.ifPresent(trainerPokemon -> pokemon.forEach(p -> {
            if (p.getPokemon() == null) {
                p.setPokemon(trainerPokemon.getPokemon());
            }
        }));
    }
}
