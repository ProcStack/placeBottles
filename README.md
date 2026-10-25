# Place Bottles (Fabric, Minecraft 26.3)

Shift + right-click the **top** of a block while holding a bottle to place it. Up to 4 bottles fit on one block top (2x2, like candles). Left-click a placed bottle to pick it back up (breaking the block under it drops it too).

Placeable items (edit `src/main/resources/data/placebottles/tags/item/placeable.json`): glass bottle, potion, splash potion, lingering potion, ominous bottle, honey bottle, experience bottle, dragon's breath.

## Why a mod and not a resource pack
Resource packs can't place anything, and a plain datapack can't detect right-clicks with items such as glass bottles. So this is a small Fabric mod (server logic + bundled assets). The jar contains the models/textures too, so there's no separate resource pack to enable. It uses only vanilla entities (`item_display` + `interaction`), no custom items/blocks, so worlds stay vanilla-compatible if the mod is removed (placed bottles remain as display entities).

## Build / install
1. Install JDK 25. 
2. In this folder: `gradlew build`
3. Put `build/libs/placebottles-1.0.0.jar` (not the `-sources` one) in `.minecraft/mods` with Fabric Loader 0.20.1+ and Fabric API 0.165.0+26.3.

On a multiplayer server, clients also need the jar (for the models).

## Potion colors
Each placed bottle stores the full item, so contents are kept and the game's own potion tint is used (every potion, custom colors, etc.). Nothing to maintain per potion.
Non-potion colors (ominous, honey, XP, dragon's breath) and fallbacks live in `tools/colors.json`; edit and run `python tools/build_assets.py` to regenerate `assets/placebottles/items/*.json`.

## Models
Starter models are in `src/main/resources/assets/placebottles/models/item/` (`bottle_empty`, `bottle_full`, `flask_full` for splash/lingering/XP). Replace them with Blockbench exports. Keep the liquid faces on `"tintindex": 0` so the color applies, and keep the model inside the 16x16x16 box with the bottom at y=0. Item -> model/tint mapping is in `assets/placebottles/items/`. Don't run `build_assets.py --models` after hand-editing models; it overwrites them.

## Known limitations
- Only top faces; bottles are removed (dropped) when a player breaks the block under them, but not by pistons/explosions.
- `reference_pack/` is the third-party Forge "Decorative Bottles" mod, kept for reference only and not used.
