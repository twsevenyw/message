package dev.evanklein.battlesoldiers.battle;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.Optional;

public final class BattleTeams {
	private BattleTeams() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(BattleTeams::ensureTeams);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof BattleSoldierEntity soldier) {
				assignSoldier(soldier);
			}
		});
	}

	public static void ensureTeams(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();
		for (SoldierSquad squad : SoldierSquad.values()) {
			PlayerTeam team = scoreboard.getPlayerTeam(squad.teamName());
			if (team == null) {
				team = scoreboard.addPlayerTeam(squad.teamName());
			}
			team.setDisplayName(Component.literal(squad.displayName() + " Soldiers"));
			team.setColor(squad.color());
			team.setAllowFriendlyFire(false);
			team.setSeeFriendlyInvisibles(true);
		}
	}

	public static void assignSoldier(BattleSoldierEntity soldier) {
		if (soldier.level().isClientSide()) {
			return;
		}

		MinecraftServer server = soldier.getServer();
		if (server == null) {
			return;
		}

		ensureTeams(server);
		Scoreboard scoreboard = server.getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam(soldier.getSquad().teamName());
		if (team != null) {
			scoreboard.addPlayerToTeam(soldier.getScoreboardName(), team);
		}
	}

	public static void removeSoldier(BattleSoldierEntity soldier) {
		MinecraftServer server = soldier.getServer();
		if (server != null) {
			server.getScoreboard().removePlayerFromTeam(soldier.getScoreboardName());
		}
	}

	public static void joinPlayer(ServerPlayer player, SoldierSquad squad) {
		MinecraftServer server = player.level().getServer();
		ensureTeams(server);
		PlayerTeam team = server.getScoreboard().getPlayerTeam(squad.teamName());
		if (team != null) {
			server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
		}
	}

	public static void leavePlayer(ServerPlayer player) {
		Scoreboard scoreboard = player.level().getScoreboard();
		Team team = player.getTeam();
		if (team != null && isBattleTeam(team.getName())) {
			scoreboard.removePlayerFromTeam(player.getScoreboardName());
		}
	}

	public static Optional<SoldierSquad> squadOf(Entity entity) {
		Team team = entity.getTeam();
		if (team == null) {
			return Optional.empty();
		}

		for (SoldierSquad squad : SoldierSquad.values()) {
			if (squad.teamName().equals(team.getName())) {
				return Optional.of(squad);
			}
		}
		return Optional.empty();
	}

	private static boolean isBattleTeam(String name) {
		for (SoldierSquad squad : SoldierSquad.values()) {
			if (squad.teamName().equals(name)) {
				return true;
			}
		}
		return false;
	}
}
