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
| `src/client/java/dev/evanklein/battlesoldiers/client/BattleSoldiersClient.java` | Vanilla zombie renderer registration for the custom soldier type |
| `releases/battle-soldiers-1.0.0.jar` | Previous prebuilt GitHub-hosted release |
| `releases/battle-soldiers-1.1.0.jar` | Previous player-like-inventory release |
| `releases/battle-soldiers-1.2.0.jar` | Previous custom-combat release |
| `releases/battle-soldiers-1.3.0.jar` | Previous reactive-combat release |
| `releases/battle-soldiers-1.4.0.jar` | Previous enchanted-tier-six release |
| `releases/battle-soldiers-1.5.0.jar` | Current adaptive-threat GitHub-hosted release |

## Current State
- Complete implementation is on `cursor/battle-soldiers-mod-1918`; draft PR #1 targets `main`.
- `/soldiers <count> <gear 1-6>` and advanced battle/team/join/clear/status subcommands are implemented.
- Version 1.5.0 uses a custom `Monster` entity and unified combat state machine; no vanilla zombie, melee, or bow combat goals remain.
- Four classes are implemented: shield-countering Vanguard, slow jump-crit Brute, tower/cover Ranger, and tier-4/5 cobweb Trapper.
- Effective class movement is about 0.22–0.28, class health remains 18–22, and pursuit predicts moving targets without returning to extreme speeds.
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
- `./gradlew clean build --warning-mode all` passes without warnings; output is `build/libs/battle-soldiers-1.5.0.jar`.
- A downloadable copy is staged at `/opt/cursor/artifacts/battle-soldiers-1.0.0.jar` (SHA-256 `344011d03587c796d13c037b1112eae672c0d950bcb42c1ff0d7107b635e43e1`).
- Version 1.1.0 is committed at `releases/battle-soldiers-1.1.0.jar` with SHA-256 `627ebf2259d9be25a4a844b36646a9a43b1997065cb2b4b4ed1b154a1c80f6ab`.
- Version 1.2.0 is committed at `releases/battle-soldiers-1.2.0.jar` with SHA-256 `5e7d3ba81067e7af9f2db521dc79f3d4513e6928c81da8b85ebfd17eb37c5363`.
- Version 1.3.0 is committed at `releases/battle-soldiers-1.3.0.jar` with SHA-256 `f360abdbde95f14494550d20b34397f5be5e15c4be8d325496caf6949f010aa1`.
- Version 1.4.0 is committed at `releases/battle-soldiers-1.4.0.jar` with SHA-256 `420a767977a5758a234aa447f453ccfa786dc840964d83d9b6ea304fdc630be0`.
- Version 1.5.0 is committed at `releases/battle-soldiers-1.5.0.jar` with SHA-256 `13bcdbd95886056f9e680822e02ed4888b14b9911109b40aa9b00e084ac60809`.
- Dedicated-server checks passed for elevated-Mace adaptation, Ranger standoff from crystals, exact perch retention after a target moved 60+ blocks, tier-6 systems, zero XP, reactive shields, crits, cleanup, persistence, and drops.
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
