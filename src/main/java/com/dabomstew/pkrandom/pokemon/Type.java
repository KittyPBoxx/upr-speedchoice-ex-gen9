package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Type.java - represents a Pokemon or move type.                        --*/
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

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum Type {

    NONE(true),
    NORMAL(true),
    FIGHTING(true),
    FLYING(true),
    GRASS(true),
    WATER(true),
    FIRE(true),
    ROCK(true),
    GROUND(true),
    PSYCHIC(true),
    BUG(true),
    DRAGON(true),
    ELECTRIC(true),
    GHOST(true),
    POISON(true),
    ICE(true),
    STEEL(true),
    MYSTERY(false),
    DARK(true),
    FAIRY(true),
    STELLAR(false);

    private static final List<Type> VALUES = List.of(values());
    private static final int SIZE = VALUES.size();
    private final boolean monsExistWithType;

    Type(boolean monsExistWithType) {
        this.monsExistWithType = monsExistWithType;
    }

    public static Type randomType(Random random, boolean onlyUsePokemonTypes) {

        if (onlyUsePokemonTypes)
        {
            List<Type> validTypes = VALUES.stream()
                                          .filter(type -> type.monsExistWithType)
                                          .collect(Collectors.toList());
            return validTypes.get(random.nextInt(validTypes.size()));
        }

        return VALUES.get(random.nextInt(SIZE));
    }

    public String displayName() {
        return this == Type.MYSTERY ? "???" : this.name();
    }

}
