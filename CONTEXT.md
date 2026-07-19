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
| `releases/battle-soldiers-1.1.0.jar` | Current prebuilt GitHub-hosted release |

## Current State
- Complete implementation is on `cursor/battle-soldiers-mod-1918`; draft PR #1 targets `main`.
- `/soldiers <count> <gear 1-5>` and advanced battle/team/join/clear/status subcommands are implemented.
- Version 1.1.0 soldiers have randomized swordsman/axe-fighter/archer roles, 36-slot persistent inventories, varied armor/durability/supplies, finite arrows, active shields, totems, occasional potions, adaptive weapon switching, purposeful building, and full death drops.
- `./gradlew clean build --warning-mode all` passes without warnings; output is `build/libs/battle-soldiers-1.1.0.jar`.
- A downloadable copy is staged at `/opt/cursor/artifacts/battle-soldiers-1.0.0.jar` (SHA-256 `344011d03587c796d13c037b1112eae672c0d950bcb42c1ff0d7107b635e43e1`).
- Version 1.1.0 is committed at `releases/battle-soldiers-1.1.0.jar` with SHA-256 `627ebf2259d9be25a4a844b36646a9a43b1997065cb2b4b4ed1b154a1c80f6ab`.
- Dedicated-server runtime checks passed for randomized inventories/equipment, restart persistence, mixed-role battles, active shield blocking, totem activation, potion buffs, consumables, and inventory/equipment drops.
- The repository's pre-existing Python encryption/web-app files remain outside the Gradle source sets and are unchanged.

## Decisions Log
| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-07-19 | Target Minecraft Java 1.21.11 with official Mojang mappings. | 1.21.11 is the requested final obfuscated release; official mappings ease future post-obfuscation ports. |
| 2026-07-19 | Implement a custom `Zombie` subtype and reuse the vanilla zombie renderer. | Provides full mob AI/equipment behavior with a reliable built-in humanoid presentation. |
| 2026-07-19 | Use training, red, and blue scoreboard squads. | Supports player practice, spectated army battles, team colors, and vanilla friendly-fire semantics. |
| 2026-07-19 | Gate building/breaching on `mob_griefing`; remove only tracked cobblestone automatically. | Allows real block interaction while making soldier-created terrain reversible and preserving intentional breach consequences. |
| 2026-07-19 | Use obstruction-aware target conditions that ignore acquisition-time line of sight. | Vanilla target retention and acquisition use separate visibility checks; disabling both is required for reliable breaching. |
| 2026-07-19 | Commit the 38 KB release JAR under `releases/`. | The user requested a durable direct GitHub download rather than an ephemeral Cursor artifact link. |
| 2026-07-19 | Replace fixed kits/counters with persistent 36-slot inventories and randomized combat roles. | Fixed armored-zombie behavior was repetitive and did not meet the player-like opponent goal. |
| 2026-07-19 | Restrict placement to bridge, ranged-cover, and elevation needs. | Unconditional/random block placement made fights noisy instead of tactical. |
| 2026-07-19 | Guarantee worn-equipment drops and drop all carried inventory on normal death. | Player-like combat should produce visible, useful loot regardless of whether another soldier or a player lands the kill. |

## Agent Activity Log
| Date | Agent | What Changed |
| --- | --- | --- |
| 2026-07-19 | GPT-5.6 Sol | Created shared context for the initial Fabric mod request. |
| 2026-07-19 | GPT-5.6 Sol | Built, documented, runtime-tested, and packaged the complete Fabric 1.21.11 Battle Soldiers mod; opened draft PR #1. |
| 2026-07-19 | GPT-5.6 Sol | Staged the compiled JAR as a downloadable Cursor artifact and recorded its checksum. |
| 2026-07-19 | GPT-5.6 Sol | Added the verified prebuilt JAR to the GitHub branch and linked it from the README. |
| 2026-07-19 | GPT-5.6 Sol | Shipped the 1.1.0 player-like AI overhaul with randomized inventories, active offhand use, purposeful tactics, death drops, runtime validation, and a rebuilt GitHub artifact. |
