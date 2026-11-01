# Place Bottles (Fabric, Minecraft 26.3)

Shift + right-click the **top** of a block while holding a bottle to place it. Up to 4 bottles fit on one block top (2x2, like candles). Left-click a placed bottle to pick it back up (breaking the block under it drops it too).

Placeable items (edit `src/main/resources/data/placebottles/tags/item/placeable.json`): glass bottle, potion, splash potion, lingering potion, ominous bottle, honey bottle, experience bottle, dragon's breath.

Each placed bottle reads the tint of the potion, so this, so contents are kept and the game's own potion tint is used (every potion, custom colors, etc.)

## Build / install
1. Install JDK 25. 
2. In this folder: `gradlew build`
3. Put `build/libs/placebottles-#.#.#.jar` (not the `-sources` one) in `.minecraft/mods` with Fabric Loader 0.20.1+ and Fabric API 0.165.0+26.3.

On a multiplayer server, clients also need the jar (for the models).


## Known limitations
- Only top faces; bottles are removed (dropped) when a player breaks the block under them, but not by pistons/explosions.
