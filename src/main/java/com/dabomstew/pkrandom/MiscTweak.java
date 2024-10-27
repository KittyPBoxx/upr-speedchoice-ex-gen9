package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  MiscTweak.java - represents a miscellaneous tweak that can be applied --*/
/*--                   to some or all games that the randomizer supports.   --*/
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
import java.util.Locale;
import java.util.ResourceBundle;

public class MiscTweak implements Comparable<MiscTweak> {

    private static final ResourceBundle bundle = java.util.ResourceBundle.getBundle("Bundle", Locale.ROOT);

    public static List<MiscTweak> allTweaks = new ArrayList<>();

    /* @formatter:off */
    // Higher priority value (third argument) = run first
    public static final MiscTweak RANDOMIZE_BATTLE_TUTORIAL = new MiscTweak(4, "battleTutorial", 0);
    public static final MiscTweak RANDOMIZE_PC_POTION = new MiscTweak(32, "pcPotion", 0);
    public static final MiscTweak NATIONAL_DEX_AT_START = new MiscTweak(128, "nationalDex", 0);
    public static final MiscTweak RANDOMIZE_CATCHING_TUTORIAL = new MiscTweak(2048, "catchingTutorial", 0);
    public static final MiscTweak BAN_LUCKY_EGG = new MiscTweak(4096, "luckyEgg", 1);
    /* @formatter:on */

    private final int value;
    private final String tweakName;
    private final String tooltipText;
    private final int priority;

    private MiscTweak(int value, String tweakID, int priority) {
        this.value = value;
        this.tweakName = bundle.getString("CodeTweaks." + tweakID + ".name");
        this.tooltipText = bundle.getString("CodeTweaks." + tweakID + ".toolTipText");
        this.priority = priority;

        // I don't want to remove the national dex tweak right now but we give it by default in the rom
        if (!tweakID.equals("nationalDex")) {
            allTweaks.add(this);
        }
    }

    public int getValue() {
        return value;
    }

    public String getTweakName() {
        return tweakName;
    }

    public String getTooltipText() {
        return tooltipText;
    }

    @Override
    public int compareTo(MiscTweak o) {
        // Order according to reverse priority, so higher priority = earlier in
        // ordering
        return o.priority - priority;
    }

}
