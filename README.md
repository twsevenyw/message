# Battle Soldiers

An advanced Fabric combat-practice mod for **Minecraft Java Edition 1.21.11**.

Battle Soldiers adds persistent, player-like AI fighters with custom class combat logic, randomized loadouts, 36-slot inventories, adaptive weapons, consumables, tactical construction, and traps. Soldiers do not use vanilla zombie or skeleton combat goals.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.19.3+**
- Fabric API **0.141.5+1.21.11**
- Java **21**

Install the mod and Fabric API in the `mods` folder. Multiplayer servers and connecting players both need the mod because it registers a custom rendered entity.

## Download

[Download Battle Soldiers 1.5.0](releases/battle-soldiers-1.5.0.jar?raw=1)

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
| `/soldiers <count> <gear>` | Spawn 1–64 training soldiers; gear must be 1–6 |
| `/soldiers battle <count-per-team> <red-gear> <blue-gear>` | Spawn two opposing armies |
| `/soldiers team <training\|red\|blue> <count> <gear>` | Spawn a specific squad |
| `/soldiers join <training\|red\|blue>` | Join a squad so its soldiers treat you as an ally |
| `/soldiers join none` | Leave soldier squads |
| `/soldiers status` | Show squads, engagements, active/cumulative reactive blocks, and landed criticals |
| `/soldiers clear` | Remove all loaded soldiers and their tactical blocks |
| `/soldiers clear <training\|red\|blue>` | Remove one loaded squad |

Commands require game-master permission (cheats in single-player or operator access on a server). The battlefield is capped at 128 loaded soldiers.

## Combat classes

| Class | Health | Behavior |
| --- | ---: | --- |
| Vanguard | 20 | Raises a shield, advances slowly, lowers it for telegraphed sword counters, and switches to an axe against blockers |
| Brute | 22 | Slow axe fighter with long recoveries and strong descending jump-critical attacks |
| Ranger | 18 | Keeps distance, strafes while drawing a finite-ammo bow, switches to backup melee up close, and builds cover/towers |
| Trapper | 20 | Tier-4+ control class that predicts movement and places a large but finite supply of cobweb traps |

## Gear levels

| Level | Equipment | Supplies and capability |
| ---: | --- | --- |
| 1 | Wood/leather pool | Basic randomized supplies, all core classes except Trapper |
| 2 | Stone/chainmail pool | Better shield timing and 5% potion chance |
| 3 | Iron pool | More Ranger presence, arrows, blocks, and 10% potion chance |
| 4 | Diamond/iron pool | Trappers unlock with five webs; four-layer towers, guaranteed heal, and rare totems |
| 5 | Netherite/diamond pool | Eight-web Trappers, five-layer towers, guaranteed heal, and improved supplies |
| 6 | Fully enchanted netherite | Max combat enchants, 5–7 gaps, 1–2 enchanted gaps, 2–3 totems, 12-web Trappers, and six-layer Ranger towers |

Gear tiers improve equipment and tactical timing—not health. Every tier stays at its class health, and effective movement remains roughly 0.22–0.29. Tiers 1–5 carry at least 2–3 golden apples; tier 6 is the fully enchanted endgame loadout.

## AI behavior

- Every soldier owns a persistent 36-slot inventory containing its actual blocks, arrows, backup weapons, food, golden apples, shields, totems, and potions.
- A single custom state machine controls pathing, spacing, attack windups, recovery windows, shields, bows, weapon swaps, and class tactics.
- Vanguards react to enemy attack telegraphs, melee posture, charged ranged weapons, and converging projectiles instead of raising shields on a timer.
- Weapon analysis uses vanilla weapon/kinetic components, so Maces, spears, and future component-based weapons are treated as real threats rather than ignored.
- Soldiers predict overhead Mace/smash impact positions and leave the blast column instead of standing directly below elevated attackers.
- Soldiers scan for End Crystals, retreat outside their 12-block damage reach, and Rangers shoot safely exposed crystals when allies are clear.
- Every melee class predicts moving targets and attempts telegraphed jump criticals; Brutes lunge for 1.5× crits while other classes use lighter 1.25× crits.
- Pursuit leads a target's current velocity and continues tracking during attack windups, making simple circle-strafing less effective without returning to extreme speed.
- Rangers consume finite arrows, build one owned tower per engagement, and hold that perch even when a target moves far away or briefly leaves targeting range.
- Ground Rangers detect surviving frontline allies; once the melee line is gone they stop endless kiting, advance, shoot from a stand, and use backup melee up close.
- Tier-4/5/6 Trappers receive 5/8/12 webs with faster high-tier trap cooldowns while refusing placements near allies.
- Some soldiers carry and intelligently drink strength, swiftness, fire-resistance, or healing potions. Buff-potion odds are capped at 20%.
- Tier-4/5 soldiers always carry exactly one healing option; wounded soldiers retreat briefly, then consume even if an escape path fails or an enemy keeps pressure on them.
- Totem-equipped soldiers use vanilla Totem of Undying mechanics and automatically move a spare shield or totem into the offhand afterward.
- Soldiers detect opponents behind nearby obstacles, approach the reachable face, show block-breaking cracks, and breach blocks according to gear capability.
- Soldiers only place blocks for a detected gap, ranged cover, or an elevation step—not randomly. Placed cobblestone/planks are tracked and cleaned up automatically.
- Equipment, inventory slots, role, squad, cooldowns, and placed-block records persist across saves.
- Soldiers drop their worn equipment and remaining inventory on death.
- Soldiers never drop experience orbs, preventing mid-fight Mending repairs.
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
build/libs/battle-soldiers-1.5.0.jar
```

For local development:

```bash
./gradlew runClient
./gradlew runServer
```

## License

MIT
