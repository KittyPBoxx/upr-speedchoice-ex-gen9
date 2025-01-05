# Emerald Ex Speedchoice UPR + Map Randomizer

This is a version of UPR that works with Emerald and will add in Gen 9 pokemon.
Specifically it was made to work with a version of Emerald Ex Speedchoice that uses Gen 9 pokemon. It also supports map randomization. 

You can find a tracker to use with the map randomizer here: https://sekii.gitlab.io/pokemon-tracker/

# How to use

- Download and extract the zip from the Releases. https://github.com/KittyPBoxx/upr-speedchoice-ex-gen9/releases/
- Run the Jar with java 11 or higher
- Open a vanilla copy of Emerald (USA) and it will be automatically patched and ready for randomization. (or pre-patch the rom yourself with the patch file supplied).

# ChangeList
<sub>Changes from the gen 8 ex speedchoice map rando</sub>

### UPR changes

*Technical*

- Java 11 or higher now required to run

*Quality Of Life*

- No browser required for map rando, UPR will now do all the map randomization offline inside upr (which is much faster) 
- Style updates, more modern components, scroll/tab view have been merged, dark mode
- You can now drag files from your desktop onto the 'Open ROM' and 'Load Settings' buttons  
- Settings load automatically, when your load a rom it will check the roms folder and the upr folder for a 'spdx.rqns' file and try and load it
- Automatic patching, if a vanilla copy of emerald is loaded, and the patch is in the UPR folder, UPR will now automatically patch your rom
- Bulk create now supported, create 1, 10, 25, 50 or 100 ROMs at once
- Preset modal has been replaces with a box to add a custom seed
- Settings (rqns) format has been changed so you can now modify it with a text editor

*Feature Changes*

- Limit pokemon changed, it no longer removes evo/baby forms from other gens but lets you select any combination of gens 1-9 to play with. Gen limits now also apply to random evos when playing with 'evo every level'
- You can now randomize the type effectiveness chart
- You can now randomize very first battle
- You can now randomzie items given by NPCs
- You can now randomize berry trees
- You can now randomize Pickup Tables
- You can now randomize item prices
- You can now randomize Marts. This includes Premier Ball but not any speedchoice marts (e.g Slateport special marts) or vending machines. For the slateport energy guru, his vanilla items (e.g carbos) will be randomized but his speedchoice ones (e.g mints) won't be.
- There is an option to mak sure every mart sells at least one type of ball and one repel
- National Dex checkbox is removed as that is now default.
- Impossible Evo/Easier Evo are now removed as all evo items are available and every pokemon will evolve by level up after level 60 (where a pokemon has multiple evos the one with the lowest nat dex num will be used as the level 60 evo)
- Catch tutorial wally's item now random
- Cable car pokemon walking up mountain now random 
- Trainer level boost now works with in-game level scaling option (i.e so you boost by 50% and turn on scaling the opponents will always be 50% higher level than you)

### Ex Speedchoice changes

*Main change*
- Update to Gen 9 Pokemon, moves, items and abilities (with lots of bugs fixed from the previous versions) and an overhaul of the lighting system

*New Features*
- Dex Nav is now available like ORAS. Pokedex > Start > Dexnav. Register a pokemon you've seen with R. Hold L + Tap R in the field to do a search. You must continue to hold L to sneak. When you get close you will be able to see info about the pokemon.
- Ability to register multiple items at the same time and select from them in an item wheel like ORAS
- Headbutt encounters on large trees (with a headbutt tutor in the cut section of Petalburg woods)
- You can now enable follower mons from the party menu (including a spheal that rolls when you run)

*Settings, Modes and debug menu Updates*
- When enabled debug menu is accessed with R+START (or SELECT when in battle or a summary screen)
- Ingame speedup changes. This speeds up the game without altering audio. The option has been moved to the speedchoice menu, (OFF|R|ON). R means that speedup will only happen if you are holding R. On means speedup will stop happening if you are holding R.    
- Battle speed options. There are now 4 different battle speeds to choose from in the speed choice menu
- Changing presents now automatically update the selections (as not pressing 'A' on each preset caused issues for a number of people)
- Autosave option added, this saves whenever healing at a pokecenter (this replaces the animation so it takes the same amount of time)
- When speed choice 100% catch rate is on all catches are not critical
- Debug menus can be enabled/disabled from speedchoice menu, by talking to birches computer, or from the options menu (Hold START + Press SELECT)
- Debug flags 'Always Obey'. This means pokemon always obey you and is default in map rando (because obedience is now gen 9 and otherwise high level catches won't obey you)
- Current debug flags (All Fly Flags, Pokedex, National Dex, All Badges, Pokenav, Match Call, Frontier Pass, All Pokedex Mons)
- Random Warps (Can be used to disable/enable random warps at any time)
- Inverse Battles (Inverse type effectiveness in battle)
- Level Scaling (Opponents pokemon will be scaled to your highest level pokemon, this is before applying upr level boost)
- Level Cap (Stops you leveling up higher than the next boss)
- Bag Use (Prevents you using bag in battle)
- Catching (Prevents you catching pokemon)
- Ai vs Ai (Ai plays all the battles vs itself)
- Encounter (Whether you get wild encounters, you will still get fishing, headbutt or sweet scent encounters)
- Trainer See (whether trainers can see you)
- Collision (Walk through walls)
- Catch Exp (Choose whether catch exp is enabled)
- Scripts Speedchoice menu, you can now bring up the speedchoice menu in the middle of a game
- Slow mo. Old slow mo has been moved to scripts. This lets you go in slow motion on the mach bike (Tap R to cancel)
- Force Gym Battle (When in a Gym or town with gym this will instantly start the battle with the leader, whatever state the game is in)
- Unlock Doors (When on a map with locked doors or blocking map tiles e.g Lilycove wailmer blockade) this will remove them
- Unexist NPCs (All NPCs on your screen disapear, they will come back if you move away and come back)
- PC/Bag (Access a portable PC)
- Give X (Give yourself any item, pokemon, max money, daycare egg, coins e.t.c). Rare candy is item ID 102. Repel is item ID 114
- Party (Remind any pokemon of any move, Nickname a mon, Teach a mon headbutt, Hatch an egg, Heal Party, Inflict a status, Check IV/EV)
- Utilities, Fly to map, Warp to a map, Warp to a goal (Warp to any map rando goal to check where it is)
- Utilities Change player name/gender, Change clock, berry tree functions e.t.c
- Sound BGM OFF (allows you to play with sound effects but no music, i.e if you wish to play your own background music)
- TY Safe Music (Normal battle music will play instead of champion themes, even with music shuffled, as these can get content striked on youtube)
- More than 50 additional music tracks. The last version only had D/P/Pt + HGSS music. This mostly add BW music from CyanSMP64. Some additional tracks from other games have beem added from LibertyTwins music pack.

*Item Changes*
- Tera shards are now a usable item that can be used to change a pokemons tera type. They can be bought from slateport market
- Slateport market now sells all evo and form change items. Including now SV ones. These are always available, you don't need to pick up hidden power first
- Bag has been split into more item pockets like later games
- Mach bike starts registered
- Sleeping bag is now a registerable item so can be used outside your bag
- New Item Repel case. You start with this registerable key item that can be used in the field. It uses one of your repels, largest first
- New Item Scent pouch. You start with this registerable key item that can be used in the field. It has the same effect as sweet scent
- New Item Teleporter. You start with this registerable key item that can be used in the field. It has the same effect as teleport
- You now start with the Tera Orb, Mega Ring and Z-Power Ring. Tera orb needs charging at a Pokecenter, like pre DLC SV
- New Item Travel Pass. This is a registerable key item with the same effect as fly. It is given to you when you get the Fly TM. It works indoors and skips the fly animation.

*Battle Changes*
- Battles backgrounds updated to Gen 4
- You can now see if a move is Physical/Special, and how effective it will be before using it. 
- You can now see the full move details and description in battle. Press R in battle while hovering over the move.  

*General Game Changes*
- Day/Night system no has 4 times of day (Dawn,Day,Dusk,Night) with different encounters / lighting
- Tate and Lisa can now be fought as a 1 vs 2 battle (this applies to all double battle but you have to speak to the trainers)
- Cable car odds have been increased to 1/16. The bug has been fixed allowing a Zigzagoon to appear (1/64) as well (but that can now be randomized to another mon) 
- Mum will no longer stop you to give running shoes as you already start with them
- Pokecenter no longer tries to check if somone is in the union room every time you enter
- Evolutions will always happen at level 60. If a pokemon has multple EVOs the lowest dex number one will be used.
- You can now fly to routes as well as towns. Some large routes have multiple fly points you go to.
- NPCs now all move faster, some of the scott encounters have been removed
- Mossdeep/Evergrande Flypoints now unlocks when you visit the map (rather than by specific tiles)
- Ds style, 2 column party menu
- Faster soft resets (a + b + start + select) resets then skips the intro
- Faster saving added, this is slightly faster on real hardware and much faster on mgba
- Quick running, Holding L+B allows you to attempt to run before even sending out a pokemon

*Field Changes*
- If you were on a bike, then surf, then land, you will go back to being on a bike
- HMs no longer need to be taught. If you have the HM, the right badge, and a pokemon that could learn the HM, you can use the HM
- For Rock Smash rocks, Strength boulders, Cut trees and Surfable water walking into the tile will trigger the HM effect automatically
- Dark caves are not pitch black but will be much darker if you don't have flash
- Acro bike up ledges. To help with map rando locks the acro bike can now just up ledges (and over small children when going south). To make sure this dosn't effect progression a hiker will now block the bottom of meteor falls until either Noman or Watsons badge has been obtained.
- Faster surfing, holding B now makes you go faster when surfing (like running)
- Bike switching controls have been updates so L+B will switch your bike even if you are mid movement
- Mach Assist mode, if on, holding B on a mach bike will mean it dosn't bonk, instead it will turn to the direction that has the most free tiles

*Item Changes*
- Fishing rod level scaling. Like regular speedchoice every fishing rod can still access every encounter slot. But the level of the encounter will now be capped depending on the rod. 
- Pokeflute now works like the blue flute
- Like the old version of speedchoice ex, the odd keystone can be used on the mossdeep white rock for a spiritomb encounter. This isn't a change, just no one seemed to know this was a thing. 

*Map Rando Changes*
- Escape warp behaviour changed. When indoors it will still take you to the last time you were outdoors. When out outdoors it will alternate between Oldale and your last heal point. This is to make escaping a softlock less of a punishment and give you more options. 
- Boats are now entirely gone as the logic (Boats in logic, tickets never in logic) was confusing people. All boat tickets will be refused when in map rando mode. 
- Abandoned ship locked doors room is gone as this confused people
- In map rando the scorched slab kecleon has been removed as too many people un-surfed there and got softlocked.
- Entering backwards through a locked door will now, permenantly unlock that door.(e.g regi caves)
- In map rando mode the guy blocking west of Petalburg has been removed. People didn't realise the catch tutorial was how to remove them.
- In map rando mode the magma space center event will never start 
- Updates to how map rando interacts with speedchoice flags. The key points are getting the devon scope will still let you skip the aqua mt pyre battles to get the magma emblem more easily, like in the old version. Doing the catch tutorial will still let you skip the aqua grunt in Petalburg woods but will no longer remove any of the magma hideout grunts. It can still be skipped, like in the older version. 

*Bug Fixes*
- Various battle mechanics and move interactions have been fixed from the last version
- There should be no moves or abilities that have no effect because they have not been implemented. UPR should avoid picking items with broken effects. 
- Escalators should no longer glitch door closed
- Fixed glitch weather in caves
- Fixed 'Ghost Rayquaza' 
- Fixed bug where you sometimes walked on water going to aqua hideout enterance or scorched slab
- Algorithm tweaks to mitigate the 'flag dump' issue. This was mostly an issue an issue when requiring gyms in order. An extreme example is if the starting area gets walled by surf gyms 1-5 would all have appear early on to make the seed possible. There was some mitigation for this before, but two gyms in a town still happened a statistically improbable amount.

### Speedchoice and Setting Explainations

You can press select over any of the speedchoice options to get more of an explaination. If new to emerald speedchoice it's also importaint to know that terra shards, evo/form change items are available in slateport outdoor mart. Mega ans Z move items are available from the league mart.  

 Preset - Will set options to random presets.

    Vanilla: keeps all the options like they are in the normal game.

    MapRando: without this setting random warps will not be enabled. This will change the EXP to BW, remove plot, changes the menu to something more convenient, turns on Fast Catch, Gen 7 X Items, and Auto Save, allows Debug Menus, and changes the Battle Speed to Instant.

    Bingo: will change the EXP to BW, remove plot, gives early Fly and Surf, changes the menu to something more convenient, False Swipe is tutorable, turns on Gen 7 X Items and Autosave, and changes the Battle Speed to Fast.

    CEA: will change the EXP to BW, remove plot, remove spinners, gives early Fly and Surf, changes the menu to something more convenient, False Swipe is tutorable, turns on Fast Catch, Gen 7 X Items, and Autosave, and changes Battle Speed to Fast.

    Race: will change the EXP to BW, remove plot, remove spinners, gives early Fly and Surf, gives good early-game Pokémon, changes the menu to something more convenient, False Swipe is tutorable, turns on Gen 7 X Items, and changes Battle Speed to Instant.

    Meme: will change the EXP to none, remove plot, make spinners spin wildly, makes all trainers have maximum vision and walk through walls to battle you, gives a chance of fully evolved Pokémon, evolves a Pokémon every level they gain, turns on Autosave, randomizes music every music change, allows Debug Menus, and changes the Battle Speed to Fast.

EXP - allows normal Pokémon Emerald experience gain (KEEP), Pokémon Black/White experience gain (BW), or to gain no experience (NONE). Keep uses the newest gens formula (without modern exp share). Black and White used a formula where level difference has a large effect on exp gain, so it's quicker to train up new mons to the current level but harder to overlevel. 

Plotless - allows you to decide on some of the plot (SEMI), all the plot (KEEP), or not have any plot (FULL). Semi stops the plot after mt. chimney. Full removes all the magma aqa plot lines.

Spinners - effect how spinning trainers function. You can make it so they can't spin (PURGE), spin normally (KEEP), spin fast (HELL), or spin wildly each frame (WHY). 
 
Max Vision - impacts if trainers can see the maximum distance. It can be normal vision (OFF), maximum distance (SANE), or maximum distance with the ability to walk through anything to reach you (HELL). 

Early Fly - allows you to get Fly earlier (YES) or get Fly normally (NO). In early mode Fly is given by the rival fight under the cycle pass, then they item finder will be give in the battle before Fortree 

Good Early Wilds - allows you to find fully evolve Pokémon in earlier routes. You can have normal encounters (OFF), fully-evolved encounters (SAME), or have it up to luck whether get fully evolved encounters or not (RAND)

Early Surf - allows you to get Surf earlier (YES) or get Surf normally (NO). This switches where you get the tms for Strength and Surf.

Nice Menu Order - organizes the menu better (YES) or keeps the menu the same (NO). This is supposed to swap the order of summary/field moves + check tag/use for berries. I say supposed to because, unless I'm missing something in the code, this options been ignored for several versions...

Easy False Swipe - makes it easier to get False Swipe. It can either be obtained normally (OFF), taught through a tutor (TUTOR), or replace HM05 Flash (HM05). When the setting is TUTOR it will replace the Slateport swagger tutor

Fast Catch - makes the catch normal (OFF) or fast (ON). Fast catch makes the catch rate 100% and removes shaking.

Gen 7 X Items - allows either +1 boosts (OFF) or +2 boosts (ON) from their respective X Item. 

Evo Every Level - either lets Pokémon evolve normally (OFF) or evolve every single level into something random (ON). 

AutoSave - either allows saves to be done normally (OFF) or save after healing at a Pokémon Center (ON). Autosaving at a center skips the animation so no time is lost.

Shuffle Music - allows the music and jingles to stay unshuffled (OFF), be shuffled with other music tracks or jingles (ON), or allows the music to be shuffled all over the place (EXP). Exp adds music from HGSS, DPPt and some other games as well.

Debug Menus - allow you to either not have them (OFF) or allow you to access them through R+ Start (ON). Debug menus are incredibly useful as they allow access to other options through Settings, like the Map Randomizer or Trainer Level Scaling. It can also be turned on and off through Professor Birth's computer in his lab in Littleroot Town. 

Battle Speed - allows you to adjust how fast battle text and animations. It can be slow (SLOW), normal (MID), fast (FAST), or instantaneous (INST). 

Speedup - allows you to double the speed of the game inside the game if you don't have an emulator. It can be inaccessible (OFF), set to R (R), or always on (ON). 

Mach Assist - can either let the Mach Bike be normal (OFF) or automatically takes the best path without bumping into anything while holding B (B). 

## Debug menu options

FLAGS

    Always obey (pokemon always obey you, otherwise gen 9 obedience is used)

    Fly Flags (enables all the fly points on the map

    Pokedex, National Dex, PokeNav, Match Call and Frontier Pass (all toggle if the flag for those is set in game)

    All badges (toggles if you have all/no badges

    Pokedex Flags All (marks all pokemon in the pokedex as caught)

SETTINGS

    Inverse Battles (Type effectiveness is inverted in battles)

    Level Scaling (All trainers will be scaled so their highest level pokemon matches yours, their other pokemon will be scaled up by the same number of levels, upr level boost is applied after level scaling)

    Level Cap (This stops your pokemon being leveled above the next gyms highest level pokemon)

    Map Rando (toggles whether random warps are enabled)

    Bag Use OFF (toggle if you can use ag in battle)

    Catching OFF (toggle if you can catch pokemon)

    AI vs AI (AI will control you're pokemon as well as the opponents)

    Encounter off (turns of wild encounters, except with sweetscent, fishing, rocksmash or headbutt trees)

    Trainer see (trainers will not fight you unless you talk to them)

    Collision off (walk through walls)

    Catch EXP Off (you will not get exp from catching pokemon)

SCRIPTS

    Speedchoice Menu (edit the speedchoice options in the middle of a game)

    Slow Mo (Runs the game at half speed until 'R' is pressed)

    Force Gym Battle (Immediately start the gym battle for whatever Town/Gym you are in)

    Unlock Doors (Unlocks any doors/Blocking tiles you can see on the current map)

    Unexist NPCS (Removes any NPCs that are currently in your vision)

    Find broken warps (Checks in engine that every expected random warp is mapped and has exactly 1 remapping)


PC/BAG

    Access PC (access the pc from anywhere)

    Fill (fills all your boxes with pokemon)

    Clear bag (deletes all your items)

    Clear Storeage Boxes (deletes all your boxed pokemon

    GIVE X

    Give Item (give yourself any item)

    Pokemon basic (Give yourself any pokemon at any level)

    Pokemon Complex (give yourself any pokemon at any level with any EV's and IV's)

    Max Money (Gives you max money)

    Max Coins (Gives you max coins in your coin case)

    Max Battle Points (Gives you max battle points for the battle frontier)

    Daycare Egg (Causes the pokemon in the daycare to produce an egg)

PARTY

    Move Reminder (use the move reminder at any point)

    Mon Nickname (change a pokemons name at any point)

    Headbutt Tutor (teaches a pokemon headbutt. The tutor is normally found in petalburg woods in the cut tree section)

    Hatch egg (hatches an egg that is in your party)

    Heal Party (fully heal your party from anywhere)

    Inflict status (Choose a status for one of your pokemon to have)

    Check EVs/IVs (Shows those values a pokemon in your party)

    Clear party (deletes all the pokemon in your party)

UTILITIES

    Fly to map (opens the fly menu, can be used even if you don't have fly)

    Warp to map (Lets you warp to any warp point in game, you must specify it's 3 byte code)

    Warp to goal (Lets you warp to key locations, such as gyms, legendaries e.t.c)

    Player Name (lets you change the player name)

    Toggle Gender (Lets you change the players gender)

    Set Weather (Sets the weather effect on the current map)

    Check Clock/Set Clock (use/set your bedroom clock from anywhere)

    New Trainer (randomly assigns you a new trainer id)

    Cheat start (Gives you all badges, fly locations, some start pokemon e.t.c)

    Berry functions (control berry tree growth without waiting)

    SOUND

    BGM OFF (Keeps all the game sound effects but stops all the songs from playing, for if you want to play your own music but still here the game effects)

    TY Safe OFF (Prevents any Champion Themes playing, as these often get copywright strikes on youtube)

    SFX/MUSIC (Play any sound effects or music tracks)



