# Shared Agent Context
> **Purpose**: This file is shared memory across all agents working on this project. Every agent should read this file at the start of each interaction and update it with important context, decisions, or state changes before finishing.

## Project Overview
- **Product**: Advanced Fabric Minecraft combat-practice mod with configurable AI soldiers.
- **Tech stack**: Java 21, Minecraft 1.21.11, Fabric Loader 0.19.3, Fabric API 0.141.5, Loom 1.17.16, Gradle 9.5.1, official Mojang mappings.
- **Repo**: https://github.com/twsevenyw/message
- **Owner**: Evan Klein

## Key Files
| File | Purpose |
| --- | --- |
| `CONTEXT.md` | Shared project state and agent handoff notes |
| `README.md` | Installation, commands, gear tiers, behavior, and build guide |
| `build.gradle` / `gradle.properties` | Fabric 1.21.11 build and pinned dependency versions |
| `src/main/java/dev/evanklein/battlesoldiers/entity/BattleSoldierEntity.java` | Soldier state, equipment, combat, healing, persistence, building, and cleanup |
| `src/main/java/dev/evanklein/battlesoldiers/entity/ai/` | Obstruction-aware targeting, breaching, building, and golden-apple goals |
| `src/main/java/dev/evanklein/battlesoldiers/command/SoldierCommands.java` | `/soldiers` command tree, spawning, battles, teams, limits, status, and cleanup |
| `src/main/java/dev/evanklein/battlesoldiers/battle/` | Gear tiers and scoreboard-backed squad management |
| `src/main/java/dev/evanklein/battlesoldiers/battle/SquadCoordinator.java` | Shared targets, habits, skill profiles, melee reservations, flanks, and specialist composition caps |
| `src/main/java/dev/evanklein/battlesoldiers/battle/TerrainPlanner.java` | Utility-scored bridge, cover, and stair placement plans |
| `src/main/java/dev/evanklein/battlesoldiers/battle/ClassInfo.java` | Per-class documentation shared by `/soldiers info` and the GUI |
| `src/main/java/dev/evanklein/battlesoldiers/config/SoldierConfig.java` | JSON-persisted spawn weights and per-tier gear/supply overrides |
| `src/main/java/dev/evanklein/battlesoldiers/gui/` | Server-side chest GUI suite: main menu, class guide, weight editor, tier loadout editor, enchant-book item editor, soldier inspector |
| `src/main/java/dev/evanklein/battlesoldiers/debug/DebugTools.java` | Fake-player GUI click harness, gated behind `-Dbattlesoldiers.debug=true` |
| `src/client/java/dev/evanklein/battlesoldiers/client/BattleSoldiersClient.java` | Vanilla zombie renderer registration for the custom soldier type |
| `releases/battle-soldiers-1.0.0.jar` | Previous prebuilt GitHub-hosted release |
| `releases/battle-soldiers-1.1.0.jar` | Previous player-like-inventory release |
| `releases/battle-soldiers-1.2.0.jar` | Previous custom-combat release |
| `releases/battle-soldiers-1.3.0.jar` | Previous reactive-combat release |
| `releases/battle-soldiers-1.4.0.jar` | Previous enchanted-tier-six release |
| `releases/battle-soldiers-1.5.0.jar` | Previous adaptive-threat release |
| `releases/battle-soldiers-2.0.0.jar` | Previous coordinated-specialist release |
| `releases/battle-soldiers-2.0.1.jar` | Previous specialist-visibility release |
| `releases/battle-soldiers-2.1.0.jar` | Previous full-combat-system release |
| `releases/battle-soldiers-2.2.0.jar` | Previous solo-duel release |
| `releases/battle-soldiers-2.3.0.jar` | Previous infinite-supplies/anti-Mace release |
| `releases/battle-soldiers-2.4.0.jar` | Previous delayed-Mace/homing-Ranger release |
| `releases/battle-soldiers-2.5.0.jar` | Previous Power-V/backline/shared-web release |
| `releases/battle-soldiers-2.5.1.jar` | Previous armor-durability release |
| `releases/battle-soldiers-2.6.0.jar` | Previous config-GUI release |
| `releases/battle-soldiers-2.7.0.jar` | Previous server-side/vanilla-client release |
| `releases/battle-soldiers-2.7.1.jar` | Previous class-spawn release |
| `releases/battle-soldiers-2.7.2.jar` | Previous multiplayer-targeting release |
| `releases/battle-soldiers-2.7.3.jar` | Current player-movement GitHub-hosted release |

## Current State
- Complete implementation is on `cursor/battle-soldiers-mod-1918`; draft PR #1 targets `main`.
- Version 2.7.0 makes the mod fully server-side via Polymer 0.15.2 (bundled jar-in-jar): vanilla clients join with zero mods installed.
- `BattleSoldierEntity implements PolymerEntity` and is disguised as `minecraft:zombie` on the wire (matching 0.6x1.95 hitbox); `PolymerEntityUtils.registerType` hides the type from vanilla-client registry sync; saves keep the real `battle_soldiers:soldier` id so persistence is unaffected.
- All command feedback is literal text (no translatable keys), so vanilla clients read it correctly; the GUI was already vanilla chest menus and needs nothing client-side.
- Clients that do install the mod still see the real entity and custom renderer; both client kinds share one world.
- `/soldiers <count> <gear 1-6> [class]` and advanced battle/team/join/clear/status subcommands are implemented; the optional class literal (all 11 roles, tab-completed) forces exact spawns, bypassing weights, gear gating, tiny-squad rules, and specialist caps.
- `/soldiers battle <count> <red-gear> <blue-gear> [red-class] [blue-class]` supports class-locked armies; a single class applies to both teams.
- Version 2.6.0 adds `/soldiers menu` (alias `config`), `/soldiers info [class]`, a JSON-persisted `SoldierConfig`, and a full server-side chest-GUI config suite.
- All 11 class spawn chances are config weights edited in the GUI; defaults are Vanguard 38, Brute 26, Ranger 7.5, Trapper 14, Duelist 3.5, Medic/Engineer/Lancer/Demolitionist 2, Alchemist/Ender Skirmisher 1.5 (sums to 100).
- Role selection is a weighted pick over gear-eligible roles: Trapper/Alchemist/Ender Skirmisher/Demolitionist need gear 4+, Medic/Engineer 3+, Lancer 2+; the 20% squad specialist cap and the tiny-squad Vanguard/Brute/Duelist restriction still apply.
- Per-tier gear overrides (helmet/chest/legs/boots/sword/axe/bow/shield/spear) apply the exact configured ItemStack to every soldier of the tier, including inventory copies and replenished weapons; entity weapon identity (equipSword/equipAxe/equipSpear/isHoldingAxe) resolves through the override item.
- The item editor is PvP-Legacy style: leveled enchanted books per applicable enchantment (curses excluded), click-to-apply/click-again-to-remove, material cycling (leather→chainmail→copper→gold→iron→diamond→netherite) that preserves enchants, clear-enchants and remove-override buttons.
- Per-tier supply overrides: golden apples, enchanted golden apples, Trapper webs, Ranger arrows, building blocks, totems, Vanguard shield chance, utility potion chance; overrides also drive replenishment floors.
- A configured bow override supersedes the Ranger Power V guarantee; tier-6 auto-enchanting defers to overrides.
- The Soldier Inspector lists live soldiers sorted by distance and opens a per-soldier live inventory view (armor/hands/36 slots, durability and enchants on hover) refreshing every second.
- Config persists to `config/battle-soldiers.json` (Fabric config dir) on every edit and loads on server start; ItemStacks use the registry-ops codec.
- A debug fake-player harness (`/soldiers debug join/open/click/menuinfo`, only with `-Dbattlesoldiers.debug=true`) drives GUI clicks headlessly for automated testing.
- Version 2.5.1 introduced a custom `Monster` entity and unified combat state machine; no vanilla zombie, melee, or bow combat goals remain.
- Core classes remain Vanguard, Brute, Ranger, and Trapper.
- Rare specialists are Medic, Engineer, Lancer, Duelist, Alchemist, Ender Skirmisher, and Demolitionist; specialists are capped at 20% per squad.
- Rare specialists have distinct role colors.
- Commander, personality variants, and Crystalist are intentionally excluded.
- A server-scoped squad blackboard shares ranked targets, frontline state, habits, reservations, and tier skill profiles.
- Shared-target adoption is idle-only as of 2.7.2: soldiers with a live, attackable target are never overridden (retaliation sticks), and idle soldiers prefer a valid enemy player under 2/3 the shared target's distance; squads therefore split correctly across multiple players.
- Simultaneous melee attackers are capped; excess soldiers receive stable flank/replacement positions.
- Squads learn shielding, ranged use, strafing, Maces, crystals, and elevation; utility and prediction adapt to those habits.
- Consumables use utility scoring, terrain uses scored bridge/cover/stair plans, and non-shield units directionally dodge converging projectiles.
- Individual combat now includes armor-aware weapon choice, hit combos, sprint resets, feints, and incoming-damage prediction.
- Collective combat includes stable focus fire, role combo chains, low-health rotations, and real resource transfers.
- Escape blocking is enabled only after a strict four-allies/three-quadrants/moving-target safety test.
- Solo engagements bypass group rotations/flanks, use immediate target scans, shorter windups/recovery, cooldown strafing, faster pursuit, and wall-jump movement.
- Tiny squads only roll Vanguard, Brute, or Duelist; Brutes reserve axes for jump crits and Vanguards reserve axes for active shield breaks.
- Every role continuously replenishes its critical class supplies (arrows/blocks/webs/pearls/potions/TNT/weapons/shields).
- Mace counterplay is layered: only units in the 4.5-block impact zone evade, Rangers fire rapid anti-air shots, Trappers web landing cells, and Engineers place fall-canceling canopy blocks.
- Any fall over five blocks is treated as a delayed-Mace threat regardless of the currently held item.
- Every third Ranger arrow is a persistent no-gravity homing shot steered toward the target's predicted center.
- Every Ranger bow is Power V at every tier, including replenished and old loaded bows.
- Out-of-range Rangers teleport to a collision-checked squad backline and reset fall distance/velocity.
- Every non-Trapper gets 3–5 finite webs and the shared predictive trap AI; Trappers retain unlimited reserves.
- Soldier armor now uses the player equipment-damage pipeline, respects Unbreaking/bypass rules, and can break during combat.
- Effective class movement is about 0.22–0.28, class health remains 18–22, and pursuit predicts moving targets without returning to extreme speeds.
- 2.7.3 movement layer: soldiers sprint (+30% standard modifier) whenever closing beyond ~3 blocks (never while using items, winding up, feinting, shielding, in water, or holding a Ranger perch), sprint-jump hop on open ground, and step straight up 1-block ledges (STEP_HEIGHT 1.0).
- Close range (≤~10 blocks, LOS, ≤1.5 Y-diff) uses direct per-tick MoveControl steering with a ~1.1-block lateral weave instead of A* paths; navigation remains the fallback for obstructed/vertical approaches.
- Knockback triggers a one-time momentum surge back toward the target (hurtTime==8, +0.22 impulse when beyond 2 blocks) plus a randomized strafe-direction reset; strafe flips use randomized intervals and post-swing spacing backpedals (-0.20 forward) turn into +0.30 re-engages as the attack readies.
- Healing now always transitions from retreat to consumption; tier-4/5 soldiers carry a guaranteed healing option.
- Shields react to explicit attack telegraphs, charged ranged weapons, and converging projectiles instead of distance timers.
- Every melee class attempts predictive jump criticals; Brutes use 1.5× and other classes 1.25× total-attack multipliers.
- Tier 6 has fully enchanted netherite role gear, 5–7 gaps, 1–2 enchanted gaps, and 2–3 totems.
- All lower tiers carry at least 2–3 gaps; Trappers receive 5/8/12 webs at tiers 4/5/6.
- Ranger towers scale to 3/3/4/4/5/6 layers and Rangers hold elevated firing perches.
- Weapon threat analysis uses vanilla `WEAPON`/`KINETIC_WEAPON` components and special Mace fall-state detection.
- Soldiers evade predicted overhead-smash impact columns and retreat from unsafe End Crystal blast zones; safe Rangers can attempt crystal shots.
- Rangers persist one owned perch per engagement and never path, strafe, heal-retreat, build, breach, or wander off it.
- Unsupported ground Rangers detect the loss of frontline allies and advance/fight instead of retreating indefinitely.
- Soldiers never drop XP orbs.
- `./gradlew clean build --warning-mode all` passes without warnings; output is `build/libs/battle-soldiers-2.7.3.jar`.
- A downloadable copy is staged at `/opt/cursor/artifacts/battle-soldiers-1.0.0.jar` (SHA-256 `344011d03587c796d13c037b1112eae672c0d950bcb42c1ff0d7107b635e43e1`).
- Version 2.0.0 is also staged at `/opt/cursor/artifacts/battle-soldiers-2.0.0.jar` for direct chat delivery because the user's FortiGate policy blocks `raw.githubusercontent.com`.
- Version 1.1.0 is committed at `releases/battle-soldiers-1.1.0.jar` with SHA-256 `627ebf2259d9be25a4a844b36646a9a43b1997065cb2b4b4ed1b154a1c80f6ab`.
- Version 1.2.0 is committed at `releases/battle-soldiers-1.2.0.jar` with SHA-256 `5e7d3ba81067e7af9f2db521dc79f3d4513e6928c81da8b85ebfd17eb37c5363`.
- Version 1.3.0 is committed at `releases/battle-soldiers-1.3.0.jar` with SHA-256 `f360abdbde95f14494550d20b34397f5be5e15c4be8d325496caf6949f010aa1`.
- Version 1.4.0 is committed at `releases/battle-soldiers-1.4.0.jar` with SHA-256 `420a767977a5758a234aa447f453ccfa786dc840964d83d9b6ea304fdc630be0`.
- Version 1.5.0 is committed at `releases/battle-soldiers-1.5.0.jar` with SHA-256 `13bcdbd95886056f9e680822e02ed4888b14b9911109b40aa9b00e084ac60809`.
- Version 2.0.0 is committed at `releases/battle-soldiers-2.0.0.jar` with SHA-256 `5aac16f72eeb2d7ebc98d3b84aba19e98e8a5953cb6e7e7dcbd7ecf8d5c942a9`.
- Version 2.0.1 is committed at `releases/battle-soldiers-2.0.1.jar` with SHA-256 `9c1dff69370a24561069df0901326d781ffe67705226c8cad7f3fe1299efb076`.
- Version 2.1.0 is committed at `releases/battle-soldiers-2.1.0.jar` with SHA-256 `0e4436628940c8861af87ac25b548e5a79f899a4882ac7a322afb672e1d9394b`.
- Version 2.2.0 is committed at `releases/battle-soldiers-2.2.0.jar` with SHA-256 `f159ae69991333d8b72c90d984689ea4854d52c0834c23d32d459fa4eeb9801e`.
- Version 2.3.0 is committed at `releases/battle-soldiers-2.3.0.jar` with SHA-256 `a9a49b7523e03a3bc70612579659823a74c277285db7e3d893cfc5772fdab582`.
- Version 2.4.0 is committed at `releases/battle-soldiers-2.4.0.jar` with SHA-256 `2ef5e180426ebc153ccdbf150b915de3bdff83a4867e393e171f4ff992f6765d`.
- Version 2.5.0 is committed at `releases/battle-soldiers-2.5.0.jar` with SHA-256 `67fb7c1183ac2a91a8ee9b9eae51ca6835e04a7c2c865fbf21028f8a7cc6a8a6`.
- Version 2.5.1 is committed at `releases/battle-soldiers-2.5.1.jar` with SHA-256 `d45b5f9c80ab014a003d13579fbe618d966056c82f5dd1f6d2922cc20486dc75`.
- Version 2.6.0 is committed at `releases/battle-soldiers-2.6.0.jar` with SHA-256 `20359f40a1e296b180f608889cfecf54633259b908da473e20e76424b2f6b269` and staged at `/opt/cursor/artifacts/battle-soldiers-2.6.0.jar`.
- Version 2.7.0 is committed at `releases/battle-soldiers-2.7.0.jar` with SHA-256 `591ef873b84b35c2ef09c2d93648125a90733d7192a94b43b7cfd8e482c6a22e` and staged at `/opt/cursor/artifacts/battle-soldiers-2.7.0.jar`.
- Version 2.7.1 is committed at `releases/battle-soldiers-2.7.1.jar` with SHA-256 `688464c4610068572d377f5e1a023e4f6bafccb1db3ad9604f4751ba1820f84b` and staged at `/opt/cursor/artifacts/battle-soldiers-2.7.1.jar`.
- 2.7.1 dedicated-server checks passed: `soldiers 1 6 duelist`, tier-1 forced Trappers, `team red 2 4 ranger`, `battle 3 5 5 brute duelist` (correct per-team roles and labels), and unlabeled random spawns; NBT confirmed forced CombatRole values with zero log errors.
- Version 2.7.2 is committed at `releases/battle-soldiers-2.7.2.jar` with SHA-256 `7ab5a935b84ce350faedf16a4409f4bbd892e0a5a3563b54e97e641e19d63166` and staged at `/opt/cursor/artifacts/battle-soldiers-2.7.2.jar`.
- 2.7.2 two-player reproduction passed: a squad locked on an unkillable fake player split to attack a vanilla-protocol client the moment it swung at them (3/4 switched, victim still alive), fresh spawns beside the second player targeted it over the distant shared target, single-target rally still works, and the log stayed error-free.
- Version 2.7.3 is committed at `releases/battle-soldiers-2.7.3.jar` with SHA-256 `a1eef31c8ac2cab9b6342c30decdcc1027f539ec27e9901941910496dce70516` and staged at `/opt/cursor/artifacts/battle-soldiers-2.7.3.jar`.
- 2.7.3 movement checks passed: position polling showed a chasing Vanguard covering ~5.5-6 blocks/s with sprinting=true and mid-hop airborne samples, sprint dropping to false inside strike range with circling positions, and a clean 5v5 battle with zero log errors.
- 2.7.0 checks passed with a real vanilla-protocol client (node minecraft-protocol, offline auth): joined through Fabric+Polymer configuration (answering the config-phase ping like a real vanilla client), reached PLAY with no registry-sync kick, received soldiers as `minecraft:zombie` spawns, was killed by "Training Vanguard • Gear 3", and stayed connected through a 5v5 battle; fake-player GUI regression and pre-Polymer world persistence also passed with zero log errors.
- 2.6.0 dedicated-server checks passed: GUI click persistence, deterministic 27/30-Ranger weight test, tier-3 diamond-sword Sharpness V + Fire Aspect II override applied in-game and after restart, enchant toggle-off, override removal, golden-apple supply override, live inventory inspector, 6v6 battle regression, and debug-tree absence in release mode.
- Dedicated-server checks passed for 12.5% specialist composition in a 64-soldier sample, all seven specialists, Medic consumption, Engineer fortifications, Alchemist debuffs, Lancer spears, Demolitionist TNT, squad coordination, and prior combat systems.
- All source, documentation, Gradle wrapper files, and release JARs are committed and synchronized to the GitHub feature branch.
- The repository's pre-existing Python encryption/web-app files remain outside the Gradle source sets and are unchanged.

## Decisions Log
| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-07-19 | Target Minecraft Java 1.21.11 with official Mojang mappings. | 1.21.11 is the requested final obfuscated release; official mappings ease future post-obfuscation ports. |
| 2026-07-19 | Replace the original `Zombie` subtype with a custom `Monster` entity and normal humanoid renderer in 1.2.0. | Delegating combat to zombie/bow goals caused aimless pursuit and prevented genuinely class-specific decisions. |
| 2026-07-19 | Use training, red, and blue scoreboard squads. | Supports player practice, spectated army battles, team colors, and vanilla friendly-fire semantics. |
| 2026-07-19 | Gate building/breaching on `mob_griefing`; remove only tracked cobblestone automatically. | Allows real block interaction while making soldier-created terrain reversible and preserving intentional breach consequences. |
| 2026-07-19 | Use obstruction-aware target conditions that ignore acquisition-time line of sight. | Vanilla target retention and acquisition use separate visibility checks; disabling both is required for reliable breaching. |
| 2026-07-19 | Commit the 38 KB release JAR under `releases/`. | The user requested a durable direct GitHub download rather than an ephemeral Cursor artifact link. |
| 2026-07-19 | Replace fixed kits/counters with persistent 36-slot inventories and randomized combat roles. | Fixed armored-zombie behavior was repetitive and did not meet the player-like opponent goal. |
| 2026-07-19 | Restrict placement to bridge, ranged-cover, and elevation needs. | Unconditional/random block placement made fights noisy instead of tactical. |
| 2026-07-19 | Guarantee worn-equipment drops and drop all carried inventory on normal death. | Player-like combat should produce visible, useful loot regardless of whether another soldier or a player lands the kill. |
| 2026-07-19 | Use one custom combat state machine for shields, windups, recoveries, bow drawing, weapon swaps, spacing, and jump crits. | A single movement/attack owner avoids conflicting goals and makes every action readable and punishable. |
| 2026-07-19 | Keep tier progression in equipment/timing rather than health or raw movement. | Difficulty should come from tactics; class health is fixed at 18–22 and movement at roughly 0.19–0.24. |
| 2026-07-19 | Give Rangers tracked towers/cover and tier-4/5 Trappers finite cobwebs. | These create distinct battlefield roles while retaining mob-griefing gates and automatic cleanup. |
| 2026-07-20 | Trigger shields from concrete threat sensors and enemy attack telegraphs. | Distance-based guard cycles looked random and routinely ended before the actual attack window. |
| 2026-07-20 | Predict moving targets during pursuit/windups and raise movement to 0.22–0.28. | Current-position pathing plus 0.19–0.24 movement was easy to circle-strafe and dodge. |
| 2026-07-20 | Guarantee high-tier healing and force retreat to end in consumption. | A continuation off-by-one stopped healing under pressure one tick before item use began. |
| 2026-07-20 | Add tier 6 as an enchanted endgame loadout without adding more health. | The user requested a materially stronger tier; enchantments, supplies, towers, webs, and timing provide it without another stat-sponge HP increase. |
| 2026-07-20 | Disable all soldier XP drops. | XP orbs repaired player Mending gear mid-fight and distorted practice-battle balance. |
| 2026-07-20 | Raise Ranger towers and Trapper web budgets substantially. | Short towers were still melee-reachable and 2–3 webs expired too quickly in serious battles. |
| 2026-07-20 | Classify weapon components and add explicit overhead-smash evasion. | Maces/kinetic weapons and vertical attacks bypassed the old sword/axe/bow assumptions and left soldiers standing below attackers. |
| 2026-07-20 | Treat End Crystals as area-denial hazards and safe Ranger targets. | Soldiers must leave lethal blast zones rather than face-tank crystals; ranged destruction is only safe without allied collateral. |
| 2026-07-20 | Persist one Ranger perch per engagement and change unsupported Ranger behavior. | Rangers wasted blocks rebuilding/jumping off towers and endlessly retreated after melee allies died. |
| 2026-07-21 | Cap rare specialists at 20% and keep core classes as the squad majority. | Support/ranged-heavy random compositions would collapse without enough soldiers able to absorb frontline pressure. |
| 2026-07-21 | Add a shared squad blackboard and melee reservation/flank system. | Independent per-soldier decisions caused target thrashing, dogpiles, and no formation replacement. |
| 2026-07-21 | Implement Medic, Engineer, Lancer, Duelist, Alchemist, Ender Skirmisher, and Demolitionist only. | The user requested these variety roles while explicitly excluding Commander, personality variants, and Crystalist. |
| 2026-07-21 | Give specialists role colors and raise the Trapper core roll by two points. | Specialists needed instant visual identification, while Trappers were just below the desired battlefield frequency. |
| 2026-07-21 | Add individual combos/feints/damage prediction and collective focus/chains/rotations/logistics. | Tier-6 soldiers still lost too easily in isolation and coordinated squads lacked layered follow-through. |
| 2026-07-21 | Enable escape blocking only behind a strict surround-and-motion validator. | Escape denial is powerful but unacceptable if it spams blocks, traps allies, or guesses stationary escape routes. |
| 2026-07-21 | Add a dedicated solo-engagement path and prohibit passive specialist rolls in tiny squads. | Group rotation/spacing logic and support roles caused 1v1 soldiers to look idle or disengage without replacements. |
| 2026-07-21 | Reserve axes for Brute crits and active Vanguard shield breaks. | Normal axe swings wasted the weapon's burst identity and contradicted the intended crit-focused playstyle. |
| 2026-07-23 | Continuously replenish critical class supplies. | Class identity collapsed once finite arrows, webs, pearls, potions, blocks, TNT, or role weapons were exhausted. |
| 2026-07-23 | Replace squad-wide Mace evasion with layered anti-air counters. | Pure retreat inconvenienced a Mace player but did not punish repeated aerial smashes or protect the formation. |
| 2026-07-23 | Treat 5+ block falls as Mace threats before the held-item swap occurs. | PvP players commonly begin the fall with another weapon and switch to the Mace only immediately before impact. |
| 2026-07-23 | Make every third Ranger shot a homing arrow. | Rangers needed a predictable accuracy spike that punishes open-ground dodging without making every arrow unavoidable. |
| 2026-07-23 | Enforce Power V for every Ranger and teleport out-of-range Rangers to the squad backline. | Low-tier Rangers lacked damage and could become irrelevant or take fall damage when the engagement moved beyond their tower range. |
| 2026-07-23 | Give every non-Trapper 3–5 finite webs and shared trap AI. | Squads needed residual control after dedicated unlimited-web Trappers died. |
| 2026-07-24 | Apply player-style durability damage to soldier armor. | Vanilla LivingEntity armor hooks are no-ops for mobs, so equipped soldier armor otherwise never degraded or broke. |
| 2026-08-18 | Replace hardcoded role probabilities with config weights defaulting Ranger to exactly 7.5%. | The user requested a precise Ranger percentage plus GUI-editable spawn chances for every class. |
| 2026-08-18 | Build the config UI as server-side vanilla chest menus with click interception. | Works without any client-side screens, looks like a real inventory, and matches the PvP Legacy enchant-book editing flow the user referenced. |
| 2026-08-18 | Apply gear overrides after procedural generation and resolve weapon identity through the override item. | Guarantees the exact configured item everywhere (equipment, inventory copies, replenishment) without breaking weapon-swap AI when materials change. |
| 2026-08-18 | Let bow overrides supersede the automatic Power V guarantee. | An explicit user-configured bow must be authoritative, including its enchantments. |
| 2026-08-18 | Gate a fake-player click harness behind `-Dbattlesoldiers.debug=true`. | Enables real end-to-end GUI click tests on a headless server while keeping release behavior untouched. |
| 2026-08-18 | Integrate Polymer (bundled jar-in-jar) and disguise soldiers as zombies for non-modded clients. | Fabric registry sync otherwise kicks vanilla clients over the custom entity type; the user wants friends to join without installing anything. |
| 2026-08-18 | Replace all translatable command feedback with literal text. | Vanilla clients lack the mod's lang file and would render raw translation keys. |
| 2026-08-18 | Verify the vanilla-join path with a real protocol client (node minecraft-protocol 1.21.11). | Reaching PLAY state, seeing zombie-disguised soldiers, and being killed by one is the only conclusive proof of vanilla-client compatibility. |
| 2026-08-18 | Let class-forced spawns bypass gear gating, weights, tiny-squad rules, and specialist caps. | An explicit `/soldiers 1 6 duelist` request is a sandbox tool; silently substituting a different class would be wrong. |
| 2026-08-18 | Make shared-target adoption idle-only with a nearest-player preference. | The 4-tick forced sync locked whole squads onto one player and overrode retaliation, so a second player was ignored until the first died. |
| 2026-08-18 | Rebuild movement around sprinting, direct steering, weaves, hops, step assist, and knockback surges. | The user identified movement as the biggest skill gap: A*-node walking with burst-only sprint read as mob-like against real PvP movement. |

## Agent Activity Log
| Date | Agent | What Changed |
| --- | --- | --- |
| 2026-07-19 | GPT-5.6 Sol | Created shared context for the initial Fabric mod request. |
| 2026-07-19 | GPT-5.6 Sol | Built, documented, runtime-tested, and packaged the complete Fabric 1.21.11 Battle Soldiers mod; opened draft PR #1. |
| 2026-07-19 | GPT-5.6 Sol | Staged the compiled JAR as a downloadable Cursor artifact and recorded its checksum. |
| 2026-07-19 | GPT-5.6 Sol | Added the verified prebuilt JAR to the GitHub branch and linked it from the README. |
| 2026-07-19 | GPT-5.6 Sol | Shipped the 1.1.0 player-like AI overhaul with randomized inventories, active offhand use, purposeful tactics, death drops, runtime validation, and a rebuilt GitHub artifact. |
| 2026-07-19 | GPT-5.6 Sol | Replaced vanilla combat inheritance with the 1.2.0 custom four-class state machine, rebalanced stats, runtime-tested class tactics, and packaged the new release. |
| 2026-07-19 | GPT-5.6 Sol | Verified the complete 1.2.0 project and release history are committed and pushed to GitHub. |
| 2026-07-20 | GPT-5.6 Sol | Shipped 1.3.0 with guaranteed pressured healing, reactive shields, predictive interception, class-wide jump crits, telemetry validation, and a rebuilt artifact. |
| 2026-07-20 | GPT-5.6 Sol | Added 1.4.0 tier 6, deterministic enchantments, expanded gaps/totems/webs, taller Ranger perches, zero XP, runtime validation, and a rebuilt artifact. |
| 2026-07-20 | GPT-5.6 Sol | Added 1.5.0 generic weapon/Mace adaptation, crystal standoff logic, persistent Ranger perches, frontline-aware Ranger aggression, runtime checks, and a rebuilt artifact. |
| 2026-07-21 | GPT-5.6 Sol | Added 2.0.0 squad coordination, habit learning, reservations/formations, utility/terrain planning, projectile dodging, seven capped rare specialists, runtime validation, and a rebuilt artifact. |
| 2026-07-21 | GPT-5.6 Sol | Staged the 2.0.0 JAR as a direct Cursor artifact after the user's network blocked GitHub raw-content downloads. |
| 2026-07-21 | GPT-5.6 Sol | Added 2.0.1 specialist name colors, a slight Trapper-frequency increase, a clean build, and a new release artifact. |
| 2026-07-21 | GPT-5.6 Sol | Added 2.1.0 armor counters, combo/feint movement, damage prediction, focus chains, rotations, logistics, validated escape blocking, runtime telemetry, and a rebuilt artifact. |
| 2026-07-21 | GPT-5.6 Sol | Added 2.2.0 solo-duel mode, immediate target scans, wall pressure, reduced duel downtime, crit-only Brute axes, shield-only Vanguard axes, runtime telemetry, and a rebuilt artifact. |
| 2026-07-23 | GPT-5.6 Sol | Added 2.3.0 infinite role supplies, rapid Ranger anti-air fire, predicted Trapper landing webs, Engineer Mace canopies, focused runtime checks, and a rebuilt artifact. |
| 2026-07-23 | GPT-5.6 Sol | Added 2.4.0 delayed-Mace fall detection, every-third Ranger homing arrows, server tracking, persistence/telemetry, runtime checks, and a rebuilt artifact. |
| 2026-07-23 | GPT-5.6 Sol | Added 2.5.0 universal Ranger Power V, safe pack-backline teleports, finite webs for all non-Trappers, shared trap AI, runtime checks, and a rebuilt artifact. |
| 2026-07-24 | GPT-5.6 Sol | Added 2.5.1 player-style armor durability, Unbreaking/bypass compatibility, runtime durability proof, and a rebuilt artifact. |
| 2026-08-18 | Claude Fable 5 | Added 2.6.0 config-driven spawn weights (Ranger 7.5%), `/soldiers info`, the full chest-GUI config suite (weights, tier loadouts, PvP-Legacy enchant books, supply editors, live soldier inspector), JSON persistence, a debug click harness, end-to-end runtime GUI tests, and a rebuilt artifact. |
| 2026-08-18 | Claude Fable 5 | Added 2.7.0 full server-side support: bundled Polymer, zombie wire-disguise, literal command feedback, vanilla-protocol join/combat/GUI verification, docs, and a rebuilt artifact. |
| 2026-08-18 | Claude Fable 5 | Added 2.7.1 optional class arguments for spawn/team/battle commands with runtime verification and a rebuilt artifact. |
| 2026-08-18 | Claude Fable 5 | Fixed 2.7.2 multiplayer tunnel vision: idle-only shared-target adoption, sticky retaliation, nearest-player preference, verified with a two-player (fake + vanilla-protocol) reproduction, and a rebuilt artifact. |
| 2026-08-18 | Claude Fable 5 | Added 2.7.3 player-movement layer: sprint + sprint-jump chase (~5.5-6 b/s measured), direct-steer weaving approach, knockback surges, randomized strafe rhythm, spacing backpedals, step assist, runtime speed verification, and a rebuilt artifact. |
