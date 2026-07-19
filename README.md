# Battle Soldiers

An advanced Fabric combat-practice mod for **Minecraft Java Edition 1.21.11**.

Battle Soldiers adds persistent AI fighters that use tiered equipment, fight players or opposing armies, fire bows, eat golden apples, place tactical blocks, and breach obstacles.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.19.3+**
- Fabric API **0.141.5+1.21.11**
- Java **21**

Install the mod and Fabric API in the `mods` folder. Multiplayer servers and connecting players both need the mod because it registers a custom rendered entity.

## Download

[Download Battle Soldiers 1.0.0](releases/battle-soldiers-1.0.0.jar?raw=1)

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
| `/soldiers status` | Show squad totals and active engagements |
| `/soldiers clear` | Remove all loaded soldiers and their tactical blocks |
| `/soldiers clear <training\|red\|blue>` | Remove one loaded squad |

Commands require game-master permission (cheats in single-player or operator access on a server). The battlefield is capped at 128 loaded soldiers.

## Gear levels

| Level | Equipment | Health | Supplies and capability |
| ---: | --- | ---: | --- |
| 1 | Wooden sword, leather armor | 20 | 1 golden apple, basic building and breaching |
| 2 | Stone sword, chainmail, shield | 24 | Archers begin appearing; faster breaching |
| 3 | Iron sword and armor, shield | 28 | More apples and tactical blocks |
| 4 | Diamond sword and armor, shield | 34 | Strong builders and heavy breachers |
| 5 | Netherite sword and armor, shield | 42 | Most supplies, fastest movement, can breach very hard blocks |

Every fourth eligible soldier is an archer; level 5 deploys archers more frequently.

## AI behavior

- Melee soldiers pursue targets and coordinate through scoreboard-backed squads.
- Archers draw and fire real arrows with difficulty-scaled accuracy.
- Wounded soldiers visibly hold and consume golden apples, receiving vanilla regeneration and absorption effects.
- Soldiers detect opponents behind nearby obstacles, approach the reachable face, show block-breaking cracks, and breach blocks according to gear capability.
- Soldiers bridge gaps and place cobblestone cover. Their placed blocks are tracked, saved with the entity, and automatically removed when the soldier dies, is cleared, or changes dimensions.
- Equipment, role, squad, remaining supplies, cooldowns, and placed-block records persist across saves.
- The red and blue squads fight each other. Neutral players can spectate; players who join a squad become valid targets only for the opposing squad.
- Creative and spectator players are never selected as practice targets.

World modification respects the Minecraft 1.21.11 `mob_griefing` game rule:

```mcfunction
/gamerule mob_griefing true
```

Soldier-placed cobblestone is cleaned up automatically. Blocks deliberately breached during combat are normal block breaks and are not restored.

## Build from source

```bash
./gradlew build
```

The distributable mod is written to:

```text
build/libs/battle-soldiers-1.0.0.jar
```

For local development:

```bash
./gradlew runClient
./gradlew runServer
```

## License

MIT
