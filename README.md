# Battle Soldiers

An advanced Fabric combat-practice mod for **Minecraft Java Edition 1.21.11**.

Battle Soldiers adds persistent, player-like AI fighters with custom class combat logic, randomized loadouts, 36-slot inventories, adaptive weapons, consumables, tactical construction, and traps. Soldiers do not use vanilla zombie or skeleton combat goals.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.19.3+**
- Fabric API **0.141.5+1.21.11**
- Java **21**

Install the mod and Fabric API in the `mods` folder **on the game that hosts the world only** (your singleplayer/LAN client or the dedicated server). Polymer is bundled inside the jar — nothing else to install.

## Friends join with zero mods

Joining players need **nothing installed** — a completely vanilla launcher works:

- Open your world to LAN (or run a dedicated server with the mod) and they connect like any vanilla server.
- Soldiers appear to vanilla clients as zombies wearing the real equipment, with live class names, health, and squad colors.
- Every feature works for them: soldiers target and fight vanilla players, all items/drops are vanilla, and the config GUI is a standard chest screen any client can use.
- Players who *do* install the mod see the proper soldier model instead of the zombie disguise; both kinds of players can share the same world.

## Download

[Download Battle Soldiers 2.7.4](releases/battle-soldiers-2.7.4.jar?raw=1)

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
| `/soldiers <count> <gear> [class]` | Spawn 1–64 training soldiers; gear must be 1–6, class optional |
| `/soldiers battle <count-per-team> <red-gear> <blue-gear> [red-class] [blue-class]` | Spawn two opposing armies; one class applies to both teams unless a second is given |
| `/soldiers team <training\|red\|blue> <count> <gear> [class]` | Spawn a specific squad |
| `/soldiers join <training\|red\|blue>` | Join a squad so its soldiers treat you as an ally |
| `/soldiers join none` | Leave soldier squads |
| `/soldiers status` | Show squads, engagements, active/cumulative reactive blocks, and landed criticals |
| `/soldiers clear` | Remove all loaded soldiers and their tactical blocks |
| `/soldiers clear <training\|red\|blue>` | Remove one loaded squad |
| `/soldiers menu` (alias `/soldiers config`) | Open the in-game config GUI |
| `/soldiers info` | Chat overview of every class with live spawn percentages |
| `/soldiers info <class>` | Full chat breakdown of one class: tactics, stats, counters |

Commands require game-master permission (cheats in single-player or operator access on a server). The battlefield is capped at 128 loaded soldiers.

The optional class is one of `vanguard`, `brute`, `ranger`, `trapper`, `medic`, `engineer`, `lancer`, `duelist`, `alchemist`, `ender_skirmisher`, `demolitionist` (tab-completes in-game). Class-forced spawns are exact: they bypass spawn weights, gear gating, tiny-squad rules, and the specialist cap — `/soldiers 1 6 duelist` gives you exactly one gear-6 Duelist.

## In-game config GUI

`/soldiers menu` opens a chest-style GUI so every knob can be edited in-game without touching files. All changes save instantly to `config/battle-soldiers.json` and survive restarts.

- **Class Guide** — every class as a hoverable icon with stats, availability, and live spawn share; click one for the full chat breakdown.
- **Spawn Chances** — every class (core and specialist) with its spawn weight. Left-click +0.5, right-click −0.5, shift for ±5, plus a reset-to-defaults button. Ranger defaults to **7.5%**.
- **Tier Loadouts** — pick a gear tier (1–6), then:
  - **Gear row** (helmet, chestplate, leggings, boots, sword, axe, bow, shield, spear): click a piece to open the item editor. Overridden items are given to every soldier of that tier exactly as configured; right-click clears an override.
  - **Item editor (PvP-Legacy style)** — the item sits up top and every applicable enchantment is laid out as leveled enchanted books (Protection I–IV, Sharpness I–V, …). Click a book to apply exactly that level; click the highlighted ✔ book again to remove it. Concrete arrows cycle the material (leather → chainmail → copper → gold → iron → diamond → netherite) while keeping enchantments, and there are buttons to clear all enchantments or drop the override.
  - **Supply row** — golden apples, enchanted golden apples, Trapper webs, Ranger arrows, building blocks, totems, Vanguard shield chance, and utility potion chance. Left/right adjust, shift+left for big steps, shift+right resets to default.
- **Soldier Inspector** — live list of every active soldier sorted by distance; click one to open its inventory laid out like a player inventory (armor, hands, and all 36 slots with real durability and enchantments visible on hover). The view auto-refreshes every second.

A configured bow override replaces the automatic Ranger Power V guarantee; tier-6 auto-enchanting also defers to any override.

## Combat classes

Spawn percentages below are the configurable defaults; edit them live via `/soldiers menu` → Spawn Chances.

| Class | Health | Default share | Behavior |
| --- | ---: | ---: | --- |
| Vanguard | 20 | 38% | Raises a shield, advances slowly, lowers it for telegraphed sword counters, and switches to an axe against blockers |
| Brute | 22 | 26% | Slow axe fighter with long recoveries and strong descending jump-critical attacks |
| Ranger | 18 | 7.5% | Keeps distance, strafes while drawing a bow, switches to backup melee up close, and builds cover/towers |
| Trapper | 20 | 14% | Tier-4+ control class with a large replenishing cobweb supply |

### Rare specialists

Specialists are composition-capped to at most 20% of a squad, so frontline classes always remain the majority. Each specialist has a distinct name color for immediate battlefield identification.

| Specialist | Health | Default share | Behavior |
| --- | ---: | ---: | --- |
| Medic | 18 | 2% | Finds wounded allies and consumes finite healing/regeneration potions on them |
| Engineer | 22 | 2% | Carries extra blocks/ladders and builds two-block squad fortifications |
| Lancer | 20 | 2% | Uses tiered kinetic spears with extended charge reach |
| Duelist | 18 | 3.5% | Fast sword pressure, aggressive flanks, and frequent jump-critical attempts |
| Alchemist | 18 | 1.5% | Consumes finite poison, weakness, and slowness supplies without debuffing allies |
| Ender Skirmisher | 18 | 1.5% | Consumes pearls to blink behind distant, elevated, or unreachable targets |
| Demolitionist | 22 | 2% | Primes owned TNT against blocked positions only when allies have cleared the blast area |

Commander, personality variants, and Crystalist are intentionally not implemented.

## Gear levels

| Level | Equipment | Supplies and capability |
| ---: | --- | --- |
| 1 | Wood/leather pool | Basic randomized supplies, all core classes except Trapper |
| 2 | Stone/chainmail pool | Better shield timing and 5% potion chance |
| 3 | Iron pool | More Ranger presence, arrows, blocks, and 10% potion chance |
| 4 | Diamond/iron pool | Trappers unlock with five webs; four-layer towers, guaranteed heal, and rare totems |
| 5 | Netherite/diamond pool | Eight-web Trappers, five-layer towers, guaranteed heal, and improved supplies |
| 6 | Fully enchanted netherite | Max combat enchants, 5–7 gaps, 1–2 enchanted gaps, 2–3 totems, 12-web Trappers, and six-layer Ranger towers |

Gear tiers improve equipment and tactical timing—not health. Every tier stays at its class health; base movement remains roughly 0.22–0.29 with the standard +30% sprint while closing distance. Tiers 1–5 carry at least 2–3 golden apples; tier 6 is the fully enchanted endgame loadout.

## AI behavior

- Every soldier owns a persistent 36-slot inventory containing its actual blocks, arrows, backup weapons, food, golden apples, shields, totems, and potions.
- Every non-Trapper carries 3–5 finite webs and uses the same predictive trapping AI; dedicated Trappers retain larger unlimited reserves.
- Every class continuously replenishes its critical role supplies: Ranger arrows/blocks, Trapper webs, Medic/Alchemist potions, Ender pearls, Engineer materials, Demolitionist TNT, and required role weapons.
- Squads share target intelligence, threat rankings, frontline state, and learned player habits through a server-side blackboard.
- Only a tier-scaled number of melee soldiers may wind up at once; excess soldiers rotate into stable replacement and flank positions instead of dogpiling. Attack slots free instantly when their holder dies, switches targets, or pauses to heal, so the attacker cap never fills with ghosts.
- Melee commitment starts just outside strict reach (the windup steers inward like a player clicking into range), and soldiers never back off an enemy stuck in a cobweb.
- Squads learn repeated shielding, strafing, ranged use, elevation, Maces, and crystals, then adjust prediction and counters.
- Decision cadence, reaction time, shared-intel lifetime, pursuit lead, and attack-slot count scale by skill tier rather than health.
- Consumables are selected by utility scores based on missing health, danger, effects, distance, and learned habits.
- Terrain choices compare bridge, cover, and stair utility rather than executing the first fixed rule.
- Non-shield soldiers calculate projectile closest approach and dodge laterally into collision-checked positions.
- Soldiers sprint like players when closing distance, complete with sprint-jump hops on open ground, and drop sprint inside strike range for controlled strafing.
- Close-range approaches use direct per-tick steering with a lateral weave instead of pathfinding node-hops, so soldiers arc in like strafing players rather than walking straight mob lines.
- Knockback is answered with an immediate momentum surge back into the fight plus an unpredictable strafe-direction reset.
- Strafe rhythm flips at randomized intervals; after each swing soldiers back out of trade range and surge back in as their next attack readies.
- Soldiers step straight up single blocks (player-style step assist) instead of stopping to jump at every ledge.
- Melee fighters choose armor/shield counters, chain timed hits around recovery windows, and use third-hit finishers.
- Solo engagements bypass group rotations/flanks, scan targets every tick, shorten windups/recovery, and maintain movement during cooldowns.
- Tiny squads only roll Vanguard, Brute, or Duelist, preventing passive support/Ranger 1v1 opponents.
- Brutes use swords for normal pressure and pull axes only for jump crits; Vanguards pull axes only against an actively raised shield.
- Wall-pinned soldiers jump-strafe laterally instead of waiting on navigation or attack recovery.
- Vanguards and Duelists feint habitual blockers; damage prediction chooses totems, healing, or emergency disengagement before lethal hits.
- Post-hit sprint resets, predictive windup tracking, pearls, and class-specific crit lunges create sustained individual pressure.
- Squad focus fire is advisory: soldiers already fighting a live target keep it, retaliation against whoever hits them sticks, and idle soldiers rally to the squad target only when no other enemy player is clearly closer — so squads split naturally across multiple players instead of tunneling one until it dies.
- Squads increase pressure after web/debuff/explosive combo stages.
- Low-health attackers rotate out of reserved frontline slots while replacements immediately advance.
- Soldiers share arrows, blocks, gaps, and pearls with the rare role that needs them.
- Engineers and Trappers may block a predicted escape cell only after four allies cover three quadrants, the target is grounded and moving, and the cell passes ally/collision safety checks.
- A single custom state machine controls pathing, spacing, attack windups, recovery windows, shields, bows, weapon swaps, and class tactics.
- Vanguards react to enemy attack telegraphs, melee posture, charged ranged weapons, and converging projectiles instead of raising shields on a timer.
- Weapon analysis uses vanilla weapon/kinetic components, so Maces, spears, and future component-based weapons are treated as real threats rather than ignored.
- Soldiers predict overhead Mace/smash impact positions and leave the blast column instead of standing directly below elevated attackers.
- Only soldiers inside the actual Mace impact radius evade; surrounding units maintain pressure.
- Rangers fire rapid anti-air shots, Trappers web predicted landing cells, and Engineers place canopy blocks into fall paths to cancel smash distance.
- Any attacker with more than five blocks of accumulated fall distance is treated as an imminent Mace threat even if they are still holding another weapon.
- Soldiers scan for End Crystals, retreat outside their 12-block damage reach, and Rangers shoot safely exposed crystals when allies are clear.
- Every melee class predicts moving targets and attempts telegraphed jump criticals; Brutes lunge for 1.5× crits while other classes use lighter 1.25× crits.
- Pursuit leads a target's current velocity and continues tracking during attack windups, making simple circle-strafing less effective without returning to extreme speed.
- Rangers consume finite arrows, build one owned tower per engagement, and hold that perch even when a target moves far away or briefly leaves targeting range.
- Every Ranger bow is maintained at Power V regardless of gear tier.
- Rangers outside bow range for two seconds teleport to a collision-checked backline position behind their allied pack, resetting velocity and fall distance.
- Every third Ranger arrow is a no-gravity homing shot that continuously steers toward the target's predicted center.
- Ground Rangers detect surviving frontline allies; once the melee line is gone they stop endless kiting, advance, shoot from a stand, and use backup melee up close.
- Tier-4/5/6 Trappers receive 5/8/12 webs with faster high-tier trap cooldowns while refusing placements near allies.
- Some soldiers carry and intelligently drink strength, swiftness, fire-resistance, or healing potions. Buff-potion odds are capped at 20%.
- Tier-4/5 soldiers always carry exactly one healing option; wounded soldiers retreat briefly, then consume even if an escape path fails or an enemy keeps pressure on them.
- Totem-equipped soldiers use vanilla Totem of Undying mechanics and automatically move a spare shield or totem into the offhand afterward.
- Soldiers detect opponents behind nearby obstacles, approach the reachable face, show block-breaking cracks, and breach blocks according to gear capability.
- Soldiers only place blocks for a detected gap, ranged cover, or an elevation step—not randomly. Placed cobblestone/planks are tracked and cleaned up automatically.
- The mod is fully server-side (Polymer): vanilla clients see soldiers as equipped zombies while all AI, classes, and combat logic run on the host.
- Class spawn weights and every tier's gear/enchant/supply overrides come from `config/battle-soldiers.json`, fully editable through the in-game GUI.
- Equipment, inventory slots, role, squad, cooldowns, and placed-block records persist across saves.
- Soldier armor loses durability from incoming armor-affected damage exactly like player armor, respects Unbreaking, and can break during combat.
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
build/libs/battle-soldiers-2.7.4.jar
```

For local development:

```bash
./gradlew runClient
./gradlew runServer
```

## License

MIT
