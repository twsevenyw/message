package dev.evanklein.battlesoldiers.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.evanklein.battlesoldiers.battle.BattleTeams;
import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.battle.SoldierSquad;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import dev.evanklein.battlesoldiers.entity.ModEntities;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SoldierCommands {
	private static final int MAX_SINGLE_SPAWN = 64;
	private static final int MAX_BATTLE_SIDE = 32;
	private static final int MAX_ACTIVE_SOLDIERS = 128;

	private SoldierCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(
						Commands.literal("soldiers")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SINGLE_SPAWN))
										.then(Commands.argument("gear", IntegerArgumentType.integer(1, 5))
												.executes(context -> spawnTraining(
														context,
														IntegerArgumentType.getInteger(context, "count"),
														IntegerArgumentType.getInteger(context, "gear")
												))))
								.then(Commands.literal("battle")
										.then(Commands.argument("count-per-team", IntegerArgumentType.integer(1, MAX_BATTLE_SIDE))
												.then(Commands.argument("red-gear", IntegerArgumentType.integer(1, 5))
														.then(Commands.argument("blue-gear", IntegerArgumentType.integer(1, 5))
																.executes(SoldierCommands::startBattle)))))
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
				)
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> teamSpawnCommand(SoldierSquad squad) {
		return Commands.literal(squad.id())
				.then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SINGLE_SPAWN))
						.then(Commands.argument("gear", IntegerArgumentType.integer(1, 5))
								.executes(context -> spawnSquad(
										context.getSource(),
										squad,
										IntegerArgumentType.getInteger(context, "count"),
										GearLevel.byId(IntegerArgumentType.getInteger(context, "gear")),
										context.getSource().getPosition()
								))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> joinCommand(SoldierSquad squad) {
		return Commands.literal(squad.id()).executes(context -> {
			ServerPlayer player = context.getSource().getPlayerOrException();
			BattleTeams.joinPlayer(player, squad);
			context.getSource().sendSuccess(
					() -> Component.translatable("commands.battle_soldiers.joined", squad.displayName()),
					false
			);
			return 1;
		});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> clearCommand(String name, SoldierSquad squad) {
		return Commands.literal(name).executes(context -> clearSoldiers(context, squad));
	}

	private static int spawnTraining(CommandContext<CommandSourceStack> context, int count, int gear) {
		return spawnSquad(
				context.getSource(),
				SoldierSquad.TRAINING,
				count,
				GearLevel.byId(gear),
				context.getSource().getPosition()
		);
	}

	private static int spawnSquad(
			CommandSourceStack source,
			SoldierSquad squad,
			int requested,
			GearLevel gear,
			Vec3 center
	) {
		int room = MAX_ACTIVE_SOLDIERS - countActive(source.getServer());
		if (room <= 0) {
			source.sendFailure(Component.translatable("commands.battle_soldiers.limit", MAX_ACTIVE_SOLDIERS));
			return 0;
		}

		int count = Math.min(requested, room);
		int spawned = spawnFormation(source.getLevel(), center, count, squad, gear);
		source.sendSuccess(
				() -> Component.translatable(
						"commands.battle_soldiers.spawned",
						spawned,
						gear.id()
				),
				true
		);
		if (count < requested) {
			source.sendFailure(Component.translatable("commands.battle_soldiers.partial", count, requested));
		}
		return spawned;
	}

	private static int startBattle(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		int count = IntegerArgumentType.getInteger(context, "count-per-team");
		GearLevel redGear = GearLevel.byId(IntegerArgumentType.getInteger(context, "red-gear"));
		GearLevel blueGear = GearLevel.byId(IntegerArgumentType.getInteger(context, "blue-gear"));
		int room = MAX_ACTIVE_SOLDIERS - countActive(source.getServer());
		if (room < count * 2) {
			source.sendFailure(Component.translatable(
					"commands.battle_soldiers.not_enough_room",
					count * 2,
					room
			));
			return 0;
		}

		double separation = Math.max(7.0, Math.ceil(Math.sqrt(count)) * 1.5 + 4.0);
		Vec3 center = source.getPosition();
		int red = spawnFormation(
				source.getLevel(),
				center.add(-separation, 0.0, 0.0),
				count,
				SoldierSquad.RED,
				redGear
		);
		int blue = spawnFormation(
				source.getLevel(),
				center.add(separation, 0.0, 0.0),
				count,
				SoldierSquad.BLUE,
				blueGear
		);
		source.sendSuccess(
				() -> Component.translatable("commands.battle_soldiers.battle", red, blue),
				true
		);
		return red + blue;
	}

	private static int spawnFormation(
			ServerLevel level,
			Vec3 center,
			int count,
			SoldierSquad squad,
			GearLevel gear
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

			int archerFrequency = gear == GearLevel.FIVE ? 3 : 4;
			boolean archer = gear.archerEligible() && (index + 1) % archerFrequency == 0;
			soldier.initializeSoldier(squad, gear, archer);
			soldier.setYRot(squad == SoldierSquad.RED ? -90.0F : 90.0F);
			spawned++;
		}
		return spawned;
	}

	private static int leaveTeam(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BattleTeams.leavePlayer(player);
		context.getSource().sendSuccess(
				() -> Component.translatable("commands.battle_soldiers.left"),
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
				() -> Component.translatable("commands.battle_soldiers.cleared", result),
				true
		);
		return removed;
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		int training = 0;
		int red = 0;
		int blue = 0;
		int engaged = 0;
		for (BattleSoldierEntity soldier : getSoldiers(context.getSource().getServer())) {
			switch (soldier.getSquad()) {
				case TRAINING -> training++;
				case RED -> red++;
				case BLUE -> blue++;
			}
			if (soldier.getTarget() != null) {
				engaged++;
			}
		}

		int total = training + red + blue;
		int trainingResult = training;
		int redResult = red;
		int blueResult = blue;
		int engagedResult = engaged;
		context.getSource().sendSuccess(
				() -> Component.translatable(
						"commands.battle_soldiers.status",
						total,
						trainingResult,
						redResult,
						blueResult,
						engagedResult
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
