package dev.evanklein.battlesoldiers.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.evanklein.battlesoldiers.battle.BattleTeams;
import dev.evanklein.battlesoldiers.battle.ClassInfo;
import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.battle.SoldierSquad;
import dev.evanklein.battlesoldiers.debug.DebugTools;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import dev.evanklein.battlesoldiers.entity.ModEntities;
import dev.evanklein.battlesoldiers.gui.SoldierMenus;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SoldierCommands {
	private static final int MAX_SINGLE_SPAWN = 64;
	private static final int MAX_BATTLE_SIDE = 32;
	private static final int MAX_ACTIVE_SOLDIERS = 128;

	private SoldierCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				LiteralArgumentBuilder<CommandSourceStack> root =
						Commands.literal("soldiers")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(spawnTree((context, count, gear, role) -> spawnSquad(
										context.getSource(),
										SoldierSquad.TRAINING,
										count,
										gear,
										role,
										context.getSource().getPosition()
								)))
								.then(Commands.literal("battle").then(battleTree()))
								.then(Commands.literal("team")
										.then(teamSpawnCommand(SoldierSquad.TRAINING))
										.then(teamSpawnCommand(SoldierSquad.RED))
										.then(teamSpawnCommand(SoldierSquad.BLUE)))
								.then(Commands.literal("join")
										.then(joinCommand(SoldierSquad.TRAINING))
										.then(joinCommand(SoldierSquad.RED))
										.then(joinCommand(SoldierSquad.BLUE))
										.then(Commands.literal("none").executes(SoldierCommands::leaveTeam)))
								.then(Commands.literal("clear")
										.executes(context -> clearSoldiers(context, null))
										.then(clearCommand("training", SoldierSquad.TRAINING))
										.then(clearCommand("red", SoldierSquad.RED))
										.then(clearCommand("blue", SoldierSquad.BLUE)))
								.then(Commands.literal("status").executes(SoldierCommands::status))
								.then(Commands.literal("menu").executes(SoldierCommands::openMenu))
								.then(Commands.literal("config").executes(SoldierCommands::openMenu))
								.then(infoCommand());
				if (DebugTools.enabled()) {
					root.then(DebugTools.commandTree());
				}
				dispatcher.register(root);
		});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> infoCommand() {
		LiteralArgumentBuilder<CommandSourceStack> info = Commands.literal("info")
				.executes(context -> {
					for (Component line : ClassInfo.overviewLines()) {
						context.getSource().sendSystemMessage(line);
					}
					return 1;
				});
		for (CombatRole role : CombatRole.values()) {
			info.then(Commands.literal(role.id()).executes(context -> {
				for (Component line : ClassInfo.detailLines(role)) {
					context.getSource().sendSystemMessage(line);
				}
				return 1;
			}));
		}
		return info;
	}

	private static int openMenu(CommandContext<CommandSourceStack> context)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		SoldierMenus.openMain(player);
		return 1;
	}

	@FunctionalInterface
	private interface SpawnRunner {
		int run(
				CommandContext<CommandSourceStack> context,
				int count,
				GearLevel gear,
				@Nullable CombatRole role
		);
	}

	/** Builds {@code <count> <gear> [class]} where the class literal is optional. */
	private static RequiredArgumentBuilder<CommandSourceStack, Integer> spawnTree(SpawnRunner runner) {
		RequiredArgumentBuilder<CommandSourceStack, Integer> gearArg =
				Commands.argument("gear", IntegerArgumentType.integer(1, 6))
						.executes(context -> runner.run(
								context,
								IntegerArgumentType.getInteger(context, "count"),
								GearLevel.byId(IntegerArgumentType.getInteger(context, "gear")),
								null
						));
		for (CombatRole role : CombatRole.values()) {
			gearArg.then(Commands.literal(role.id()).executes(context -> runner.run(
					context,
					IntegerArgumentType.getInteger(context, "count"),
					GearLevel.byId(IntegerArgumentType.getInteger(context, "gear")),
					role
			)));
		}
		return Commands.argument("count", IntegerArgumentType.integer(1, MAX_SINGLE_SPAWN))
				.then(gearArg);
	}

	/**
	 * Builds {@code <count-per-team> <red-gear> <blue-gear> [red-class [blue-class]]};
	 * one class applies to both teams unless a second is given.
	 */
	private static RequiredArgumentBuilder<CommandSourceStack, Integer> battleTree() {
		RequiredArgumentBuilder<CommandSourceStack, Integer> blueGear =
				Commands.argument("blue-gear", IntegerArgumentType.integer(1, 6))
						.executes(context -> startBattle(context, null, null));
		for (CombatRole redRole : CombatRole.values()) {
			LiteralArgumentBuilder<CommandSourceStack> redLiteral =
					Commands.literal(redRole.id())
							.executes(context -> startBattle(context, redRole, redRole));
			for (CombatRole blueRole : CombatRole.values()) {
				redLiteral.then(Commands.literal(blueRole.id())
						.executes(context -> startBattle(context, redRole, blueRole)));
			}
			blueGear.then(redLiteral);
		}
		return Commands.argument("count-per-team", IntegerArgumentType.integer(1, MAX_BATTLE_SIDE))
				.then(Commands.argument("red-gear", IntegerArgumentType.integer(1, 6))
						.then(blueGear));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> teamSpawnCommand(SoldierSquad squad) {
		return Commands.literal(squad.id())
				.then(spawnTree((context, count, gear, role) -> spawnSquad(
						context.getSource(),
						squad,
						count,
						gear,
						role,
						context.getSource().getPosition()
				)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> joinCommand(SoldierSquad squad) {
		return Commands.literal(squad.id()).executes(context -> {
			ServerPlayer player = context.getSource().getPlayerOrException();
			BattleTeams.joinPlayer(player, squad);
			context.getSource().sendSuccess(
					() -> Component.literal("Joined the " + squad.displayName() + " squad."),
					false
			);
			return 1;
		});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> clearCommand(String name, SoldierSquad squad) {
		return Commands.literal(name).executes(context -> clearSoldiers(context, squad));
	}

	private static int spawnSquad(
			CommandSourceStack source,
			SoldierSquad squad,
			int requested,
			GearLevel gear,
			@Nullable CombatRole role,
			Vec3 center
	) {
		int room = MAX_ACTIVE_SOLDIERS - countActive(source.getServer());
		if (room <= 0) {
			source.sendFailure(Component.literal(
					"The battlefield limit of " + MAX_ACTIVE_SOLDIERS + " active soldiers has been reached."));
			return 0;
		}

		int count = Math.min(requested, room);
		int spawned = spawnFormation(source.getLevel(), center, count, squad, gear, role);
		String classLabel = role == null ? "" : " " + role.displayName();
		source.sendSuccess(
				() -> Component.literal(
						"Deployed " + spawned + " level-" + gear.id() + classLabel + " soldier(s)."),
				true
		);
		if (count < requested) {
			source.sendFailure(Component.literal(
					"Only " + count + " of " + requested + " requested soldiers fit under the battlefield limit."));
		}
		return spawned;
	}

	private static int startBattle(
			CommandContext<CommandSourceStack> context,
			@Nullable CombatRole redRole,
			@Nullable CombatRole blueRole
	) {
		CommandSourceStack source = context.getSource();
		int count = IntegerArgumentType.getInteger(context, "count-per-team");
		GearLevel redGear = GearLevel.byId(IntegerArgumentType.getInteger(context, "red-gear"));
		GearLevel blueGear = GearLevel.byId(IntegerArgumentType.getInteger(context, "blue-gear"));
		int room = MAX_ACTIVE_SOLDIERS - countActive(source.getServer());
		if (room < count * 2) {
			source.sendFailure(Component.literal(
					"This battle needs " + count * 2 + " free soldier slots, but only " + room + " remain."));
			return 0;
		}

		double separation = Math.max(7.0, Math.ceil(Math.sqrt(count)) * 1.5 + 4.0);
		Vec3 center = source.getPosition();
		int red = spawnFormation(
				source.getLevel(),
				center.add(-separation, 0.0, 0.0),
				count,
				SoldierSquad.RED,
				redGear,
				redRole
		);
		int blue = spawnFormation(
				source.getLevel(),
				center.add(separation, 0.0, 0.0),
				count,
				SoldierSquad.BLUE,
				blueGear,
				blueRole
		);
		String redLabel = redRole == null ? "" : " " + redRole.displayName() + "s";
		String blueLabel = blueRole == null ? "" : " " + blueRole.displayName() + "s";
		source.sendSuccess(
				() -> Component.literal(
						"Battle started: " + red + " red" + redLabel + " vs " + blue + " blue" + blueLabel + "."),
				true
		);
		return red + blue;
	}

	private static int spawnFormation(
			ServerLevel level,
			Vec3 center,
			int count,
			SoldierSquad squad,
			GearLevel gear,
			@Nullable CombatRole role
	) {
		int columns = Math.max(1, (int) Math.ceil(Math.sqrt(count)));
		double spacing = 1.8;
		int spawned = 0;

		for (int index = 0; index < count; index++) {
			int row = index / columns;
			int column = index % columns;
			double x = center.x + (column - (columns - 1) / 2.0) * spacing;
			double z = center.z + (row - (columns - 1) / 2.0) * spacing;
			BlockPos xz = BlockPos.containing(x, center.y, z);
			BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
			BattleSoldierEntity soldier = ModEntities.SOLDIER.spawn(
					level,
					surface,
					EntitySpawnReason.COMMAND
			);
			if (soldier == null) {
				continue;
			}

			soldier.initializeSoldier(squad, gear, role);
			soldier.setYRot(squad == SoldierSquad.RED ? -90.0F : 90.0F);
			spawned++;
		}
		return spawned;
	}

	private static int leaveTeam(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BattleTeams.leavePlayer(player);
		context.getSource().sendSuccess(
				() -> Component.literal("Left all soldier squads."),
				false
		);
		return 1;
	}

	private static int clearSoldiers(CommandContext<CommandSourceStack> context, SoldierSquad squad) {
		List<BattleSoldierEntity> soldiers = getSoldiers(context.getSource().getServer());
		int removed = 0;
		for (BattleSoldierEntity soldier : soldiers) {
			if (squad == null || soldier.getSquad() == squad) {
				soldier.discard();
				removed++;
			}
		}

		int result = removed;
		context.getSource().sendSuccess(
				() -> Component.literal("Removed " + result + " soldier(s) and cleaned up tactical blocks."),
				true
		);
		return removed;
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		int training = 0;
		int red = 0;
		int blue = 0;
		int engaged = 0;
		int blocking = 0;
		int reactiveBlocks = 0;
		int criticalHits = 0;
		int escapeBlocks = 0;
		int homingShots = 0;
		for (BattleSoldierEntity soldier : getSoldiers(context.getSource().getServer())) {
			switch (soldier.getSquad()) {
				case TRAINING -> training++;
				case RED -> red++;
				case BLUE -> blue++;
			}
			if (soldier.getTarget() != null) {
				engaged++;
			}
			if (soldier.isBlocking()) {
				blocking++;
			}
			reactiveBlocks += soldier.getReactiveShieldUses();
			criticalHits += soldier.getCriticalHits();
			escapeBlocks += soldier.getEscapeBlocksPlaced();
			homingShots += soldier.getHomingShotsFired();
		}

		int total = training + red + blue;
		int trainingResult = training;
		int redResult = red;
		int blueResult = blue;
		int engagedResult = engaged;
		int blockingResult = blocking;
		int reactiveBlocksResult = reactiveBlocks;
		int criticalHitsResult = criticalHits;
		int escapeBlocksResult = escapeBlocks;
		int homingShotsResult = homingShots;
		context.getSource().sendSuccess(
				() -> Component.literal(
						total + " active soldier(s): " + trainingResult + " training, "
								+ redResult + " red, " + blueResult + " blue; "
								+ engagedResult + " engaged, " + blockingResult + " actively blocking, "
								+ reactiveBlocksResult + " reactive shield raises, "
								+ criticalHitsResult + " landed criticals, "
								+ escapeBlocksResult + " validated escape blocks, "
								+ homingShotsResult + " homing Ranger shots."
				),
				false
		);
		return total;
	}

	private static int countActive(MinecraftServer server) {
		return getSoldiers(server).size();
	}

	private static List<BattleSoldierEntity> getSoldiers(MinecraftServer server) {
		List<BattleSoldierEntity> soldiers = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			level.getEntities(ModEntities.SOLDIER, Entity::isAlive, soldiers);
		}
		return soldiers;
	}
}
