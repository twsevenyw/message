# Battle Soldiers

An advanced Fabric combat-practice mod for **Minecraft Java Edition 1.21.11**.

Battle Soldiers adds persistent, player-like AI fighters with randomized loadouts, 36-slot inventories, adaptive combat, consumables, tactical building, and obstacle breaching.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.19.3+**
- Fabric API **0.141.5+1.21.11**
- Java **21**

Install the mod and Fabric API in the `mods` folder. Multiplayer servers and connecting players both need the mod because it registers a custom rendered entity.

## Download

[Download Battle Soldiers 1.1.0](releases/battle-soldiers-1.1.0.jar?raw=1)

## Quick start

Enable commands/cheats, enter a world, then run:

```mcfunction
/soldiers 10 3
```

This deploys 10 training soldiers with gear level 3. Training soldiers attack nearby survival-mode players and work together without friendly fire.

Start an army battle:

```mcfunction
/soldiers battle 12 3 5
```

This deploys 12 gear-level-3 red soldiers against 12 gear-level-5 blue soldiers.

## Commands

| Command | Action |
| --- | --- |
| `/soldiers <count> <gear>` | Spawn 1–64 training soldiers; gear must be 1–5 |
| `/soldiers battle <count-per-team> <red-gear> <blue-gear>` | Spawn two opposing armies |
| `/soldiers team <training\|red\|blue> <count> <gear>` | Spawn a specific squad |
| `/soldiers join <training\|red\|blue>` | Join a squad so its soldiers treat you as an ally |
| `/soldiers join none` | Leave soldier squads |
| `/soldiers status` | Show squad totals, active engagements, and shield users |
| `/soldiers clear` | Remove all loaded soldiers and their tactical blocks |
| `/soldiers clear <training\|red\|blue>` | Remove one loaded squad |

Commands require game-master permission (cheats in single-player or operator access on a server). The battlefield is capped at 128 loaded soldiers.

## Gear levels

| Level | Equipment | Health | Supplies and capability |
| ---: | --- | ---: | --- |
| 1 | Wood/leather pool | 20 | Basic randomized supplies and breaching |
| 2 | Stone/chainmail pool | 24 | Shields, occasional archers and rare potions |
| 3 | Iron pool | 28 | Better armor coverage, supplies, potions, and arrows |
| 4 | Diamond/iron pool | 34 | Rare totems, strong buff odds, and heavy breaching |
| 5 | Netherite/diamond pool | 42 | Highest totem/buff odds, mobility, and hard-block breaching |

Each soldier independently rolls a swordsman, axe-fighter, or archer role. Armor pieces can be missing, downgraded, or worn, and carried supplies vary even within the same gear level.

## AI behavior

- Every soldier owns a persistent 36-slot inventory containing its actual blocks, arrows, backup weapons, food, golden apples, shields, totems, and potions.
- Swordsmen and axe fighters sprint into combat and use timed shield windows, facing attackers while blocking before lowering the shield to strike.
- Archers consume finite arrows, fight at range, and swap to a backup melee weapon and shield when opponents close in.
- Some higher-level soldiers carry and intelligently drink strength, swiftness, regeneration, fire-resistance, or healing potions. Potions are deliberately not guaranteed.
- Wounded soldiers retreat before consuming golden apples or healing supplies.
- Totem-equipped soldiers use vanilla Totem of Undying mechanics and automatically move a spare shield or totem into the offhand afterward.
- Soldiers detect opponents behind nearby obstacles, approach the reachable face, show block-breaking cracks, and breach blocks according to gear capability.
- Soldiers only place blocks for a detected gap, ranged cover, or an elevation step—not randomly. Placed cobblestone/planks are tracked and cleaned up automatically.
- Equipment, inventory slots, role, squad, cooldowns, and placed-block records persist across saves.
- Soldiers drop their worn equipment and remaining inventory on death.
- The red and blue squads fight each other. Neutral players can spectate; players who join a squad become valid targets only for the opposing squad.
- Creative and spectator players are never selected as practice targets.

World modification respects the Minecraft 1.21.11 `mob_griefing` game rule:

```mcfunction
/gamerule mob_griefing true
```

Soldier-placed cobblestone and planks are cleaned up automatically. Blocks deliberately breached during combat are normal block breaks and are not restored.

## Build from source

```bash
./gradlew build
```

The distributable mod is written to:

```text
build/libs/battle-soldiers-1.1.0.jar
```

For local development:

```bash
./gradlew runClient
./gradlew runServer
```

## License

MIT
