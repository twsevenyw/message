package dev.evanklein.battlesoldiers.battle;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SquadCoordinator {
	private static final Map<MinecraftServer, ServerBoard> SERVERS =
			Collections.synchronizedMap(new WeakHashMap<>());
	private static final long STALE_TICKS = 60;

	private SquadCoordinator() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(SquadCoordinator::cleanupWorld);
		ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
	}

	public static CombatRole chooseRole(BattleSoldierEntity soldier, GearLevel gear) {
		SquadBoard board = squadBoard(soldier);
		cleanupBoard(board, soldier.level().getGameTime());
		int total = board.soldiers.size();
		if (total < 3) {
			float duelRoll = soldier.getRandom().nextFloat();
			if (duelRoll < 0.52F) {
				return CombatRole.VANGUARD;
			}
			if (duelRoll < 0.88F) {
				return CombatRole.BRUTE;
			}
			return CombatRole.DUELIST;
		}
		int rareCount = (int) board.soldiers.values().stream().filter(Presence::specialist).count();
		float rareChance = switch (gear) {
			case ONE -> 0.01F;
			case TWO -> 0.04F;
			case THREE -> 0.08F;
			case FOUR -> 0.12F;
			case FIVE -> 0.16F;
			case SIX -> 0.20F;
		};
		int rareCap = Math.max(1, (int) Math.floor((total + 1) * 0.20));
		if (rareCount < rareCap && soldier.getRandom().nextFloat() < rareChance) {
			List<CombatRole> eligible = new ArrayList<>();
			eligible.add(CombatRole.DUELIST);
			if (gear.id() >= 2) {
				eligible.add(CombatRole.LANCER);
			}
			if (gear.id() >= 3) {
				eligible.add(CombatRole.MEDIC);
				eligible.add(CombatRole.ENGINEER);
			}
			if (gear.id() >= 4) {
				eligible.add(CombatRole.ALCHEMIST);
				eligible.add(CombatRole.ENDER_SKIRMISHER);
				eligible.add(CombatRole.DEMOLITIONIST);
			}
			eligible.sort((left, right) -> Integer.compare(roleCount(board, left), roleCount(board, right)));
			int choiceBand = Math.min(3, eligible.size());
			return eligible.get(soldier.getRandom().nextInt(choiceBand));
		}

		float roll = soldier.getRandom().nextFloat();
		if (roll < 0.40F) {
			return CombatRole.VANGUARD;
		}
		if (roll < 0.68F) {
			return CombatRole.BRUTE;
		}
		if (roll < 0.86F) {
			return CombatRole.RANGER;
		}
		return gear.id() >= 4 ? CombatRole.TRAPPER : CombatRole.VANGUARD;
	}

	public static void heartbeat(BattleSoldierEntity soldier) {
		SquadBoard board = squadBoard(soldier);
		long tick = soldier.level().getGameTime();
		LivingEntity target = soldier.getTarget();
		board.soldiers.put(
				soldier.getUUID(),
				new Presence(
						soldier.getUUID(),
						soldier.getCombatRole(),
						soldier.position(),
						target == null ? null : target.getUUID(),
						tick
				)
		);
		if (target != null) {
			sampleHabits(board, soldier, target, tick);
			double score = threatScore(board, soldier, target);
			if (board.sharedTarget == null
					|| board.sharedTarget.equals(target.getUUID())
					|| tick - board.sharedTargetTick > 40
					|| score >= board.sharedTargetScore + 20.0) {
				board.sharedTarget = target.getUUID();
				board.sharedTargetTick = tick;
				board.sharedTargetScore = score;
			}
		}
	}

	public static void unregister(BattleSoldierEntity soldier) {
		SquadBoard board = squadBoard(soldier);
		UUID soldierId = soldier.getUUID();
		board.soldiers.remove(soldierId);
		board.reservationBySoldier.remove(soldierId);
		board.meleeReservations.values().forEach(reservations -> reservations.remove(soldierId));
	}

	@Nullable
	public static LivingEntity sharedTarget(BattleSoldierEntity soldier) {
		SquadBoard board = squadBoard(soldier);
		if (board.sharedTarget == null
				|| soldier.level().getGameTime() - board.sharedTargetTick > skill(soldier.getGearLevel()).intelTtlTicks()) {
			return null;
		}
		Entity entity = ((ServerLevel) soldier.level()).getEntity(board.sharedTarget);
		return entity instanceof LivingEntity living && living.isAlive() && soldier.canAttack(living)
				? living
				: null;
	}

	public static MeleeDirective meleeDirective(BattleSoldierEntity soldier, LivingEntity target) {
		SquadBoard board = squadBoard(soldier);
		long tick = soldier.level().getGameTime();
		boolean replacementAvailable = board.soldiers.values().stream().anyMatch(presence ->
				!presence.id().equals(soldier.getUUID())
						&& presence.role().isFrontline()
						&& target.getUUID().equals(presence.targetId())
						&& tick - presence.tick() <= 20
		);
		if (soldier.getHealth() < soldier.getMaxHealth() * 0.45F && replacementAvailable) {
			releaseMelee(soldier);
			Vec3 fallback = squadCentroid(board);
			if (fallback == Vec3.ZERO) {
				fallback = soldier.position().subtract(target.position()).normalize().scale(6.0).add(soldier.position());
			}
			return new MeleeDirective(false, fallback, -1);
		}
		LinkedHashSet<UUID> reservations =
				board.meleeReservations.computeIfAbsent(target.getUUID(), ignored -> new LinkedHashSet<>());
		reservations.removeIf(id -> {
			Presence presence = board.soldiers.get(id);
			return presence == null || tick - presence.tick() > 20;
		});
		UUID soldierId = soldier.getUUID();
		int maxAttackers = skill(soldier.getGearLevel()).maxMeleeAttackers();
		if (reservations.contains(soldierId)) {
			board.reservationBySoldier.put(soldierId, target.getUUID());
			return new MeleeDirective(true, target.position(), 0);
		}
		if (reservations.size() < maxAttackers) {
			reservations.add(soldierId);
			board.reservationBySoldier.put(soldierId, target.getUUID());
			return new MeleeDirective(true, target.position(), reservations.size() - 1);
		}

		int slot = Math.floorMod(soldierId.hashCode(), 12);
		double side = slot % 2 == 0 ? 1.0 : -1.0;
		double angle = Math.toRadians(65.0 + (slot / 2) * 18.0) * side;
		double radius = 4.5 + (slot / 6) * 1.5;
		Vec3 approach = squadCentroid(board).subtract(target.position());
		if (approach.horizontalDistanceSqr() < 0.01) {
			approach = new Vec3(1.0, 0.0, 0.0);
		}
		approach = new Vec3(approach.x, 0.0, approach.z).normalize();
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		Vec3 rotated = new Vec3(
				approach.x * cos - approach.z * sin,
				0.0,
				approach.x * sin + approach.z * cos
		);
		return new MeleeDirective(false, target.position().add(rotated.scale(radius)), slot);
	}

	public static void releaseMelee(BattleSoldierEntity soldier) {
		SquadBoard board = squadBoard(soldier);
		UUID targetId = board.reservationBySoldier.remove(soldier.getUUID());
		if (targetId != null) {
			Set<UUID> reservations = board.meleeReservations.get(targetId);
			if (reservations != null) {
				reservations.remove(soldier.getUUID());
			}
		}
	}

	public static HabitSnapshot habits(BattleSoldierEntity soldier, LivingEntity target) {
		HabitState state = squadBoard(soldier).habits.get(target.getUUID());
		return state == null
				? HabitSnapshot.EMPTY
				: new HabitSnapshot(
						state.ranged,
						state.shielding,
						state.mace,
						state.crystals,
						state.elevated,
						state.strafing
				);
	}

	public static void reportComboEvent(
			BattleSoldierEntity soldier,
			LivingEntity target,
			ComboEvent event
	) {
		SquadBoard board = squadBoard(soldier);
		ComboState state = board.combos.computeIfAbsent(target.getUUID(), ignored -> new ComboState());
		state.mask |= event.mask;
		state.expiresAt = soldier.level().getGameTime() + event.durationTicks;
		board.sharedTarget = target.getUUID();
		board.sharedTargetTick = soldier.level().getGameTime();
		board.sharedTargetScore = Math.max(board.sharedTargetScore, 180.0);
	}

	public static ComboSnapshot combo(BattleSoldierEntity soldier, LivingEntity target) {
		ComboState state = squadBoard(soldier).combos.get(target.getUUID());
		if (state == null || state.expiresAt < soldier.level().getGameTime()) {
			return ComboSnapshot.EMPTY;
		}
		return new ComboSnapshot(
				(state.mask & ComboEvent.WEBBED.mask) != 0,
				(state.mask & ComboEvent.DEBUFFED.mask) != 0,
				(state.mask & ComboEvent.EXPLOSIVE.mask) != 0
		);
	}

	public static boolean isTargetSurrounded(BattleSoldierEntity soldier, LivingEntity target) {
		SquadBoard board = squadBoard(soldier);
		boolean[] quadrants = new boolean[4];
		int nearby = 0;
		for (Presence presence : board.soldiers.values()) {
			double dx = presence.position().x - target.getX();
			double dz = presence.position().z - target.getZ();
			if (dx * dx + dz * dz > 49.0) {
				continue;
			}
			nearby++;
			double angle = Math.atan2(dz, dx);
			int quadrant = Math.floorMod((int) Math.floor((angle + Math.PI) / (Math.PI / 2.0)), 4);
			quadrants[quadrant] = true;
		}
		int covered = 0;
		for (boolean quadrant : quadrants) {
			if (quadrant) {
				covered++;
			}
		}
		return nearby >= 4 && covered >= 3;
	}

	@Nullable
	public static BlockPos predictedEscapeBlock(BattleSoldierEntity soldier, LivingEntity target) {
		if (!target.onGround() || !isTargetSurrounded(soldier, target)) {
			return null;
		}
		Vec3 velocity = target.getDeltaMovement();
		Vec3 horizontal = new Vec3(velocity.x, 0.0, velocity.z);
		if (horizontal.lengthSqr() < 0.0064) {
			return null;
		}
		Vec3 predicted = target.position().add(horizontal.normalize().scale(1.5));
		return BlockPos.containing(predicted);
	}

	public static boolean hasFrontline(BattleSoldierEntity soldier, LivingEntity target) {
		SquadBoard board = squadBoard(soldier);
		for (Presence presence : board.soldiers.values()) {
			if (!presence.role().isFrontline() || presence.targetId() == null) {
				continue;
			}
			double dx = presence.position().x - target.getX();
			double dz = presence.position().z - target.getZ();
			if (dx * dx + dz * dz <= 64.0 && Math.abs(presence.position().y - target.getY()) <= 4.0) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSoloEngagement(BattleSoldierEntity soldier, LivingEntity target) {
		SquadBoard board = squadBoard(soldier);
		long tick = soldier.level().getGameTime();
		int engagedAllies = 0;
		for (Presence presence : board.soldiers.values()) {
			if (tick - presence.tick() <= 20 && target.getUUID().equals(presence.targetId())) {
				engagedAllies++;
				if (engagedAllies > 1) {
					return false;
				}
			}
		}
		return true;
	}

	public static SkillProfile skill(GearLevel gear) {
		return switch (gear) {
			case ONE -> new SkillProfile(10, 8, 30, 0.35, 2);
			case TWO -> new SkillProfile(8, 7, 40, 0.45, 2);
			case THREE -> new SkillProfile(6, 5, 60, 0.60, 3);
			case FOUR -> new SkillProfile(5, 4, 80, 0.75, 3);
			case FIVE -> new SkillProfile(4, 2, 100, 0.90, 4);
			case SIX -> new SkillProfile(2, 1, 120, 1.00, 4);
		};
	}

	private static void sampleHabits(
			SquadBoard board,
			BattleSoldierEntity observer,
			LivingEntity target,
			long tick
	) {
		if (!(target instanceof Player player)) {
			return;
		}
		HabitState habits = board.habits.computeIfAbsent(player.getUUID(), ignored -> new HabitState());
		if (tick - habits.lastSampleTick < 10) {
			return;
		}
		habits.lastSampleTick = tick;
		if (player.isUsingItem()
				&& (player.getUseItem().is(Items.BOW)
					|| player.getUseItem().is(Items.CROSSBOW)
					|| player.getUseItem().is(Items.TRIDENT))) {
			habits.ranged = saturatingIncrement(habits.ranged);
		}
		if (player.isBlocking()) {
			habits.shielding = saturatingIncrement(habits.shielding);
		}
		if (player.getMainHandItem().is(Items.MACE) || player.fallDistance > 5.0F) {
			habits.mace = saturatingIncrement(habits.mace);
		}
		if (player.getY() - observer.getY() >= 2.5) {
			habits.elevated = saturatingIncrement(habits.elevated);
		}
		Vec3 velocity = player.getDeltaMovement();
		Vec3 radial = observer.position().subtract(player.position());
		if (radial.horizontalDistanceSqr() > 0.01) {
			radial = new Vec3(radial.x, 0.0, radial.z).normalize();
			Vec3 horizontalVelocity = new Vec3(velocity.x, 0.0, velocity.z);
			double radialSpeed = Math.abs(horizontalVelocity.dot(radial));
			double lateralSpeed = horizontalVelocity.subtract(radial.scale(horizontalVelocity.dot(radial))).length();
			if (lateralSpeed >= 0.08 && lateralSpeed > radialSpeed * 1.2) {
				habits.strafing = saturatingIncrement(habits.strafing);
			}
		}
		if (observer.findNearestCrystal(12.0) != null) {
			habits.crystals = saturatingIncrement(habits.crystals);
		}
	}

	private static int saturatingIncrement(int value) {
		return Math.min(100, value + 1);
	}

	private static double threatScore(
			SquadBoard board,
			BattleSoldierEntity observer,
			LivingEntity target
	) {
		HabitState habits = board.habits.get(target.getUUID());
		double score = 100.0 - Math.min(40.0, Math.sqrt(observer.distanceToSqr(target)));
		if (target instanceof Player) {
			score += 25.0;
		}
		if (target.getHealth() <= target.getMaxHealth() * 0.35F) {
			score += 22.0;
		}
		if (habits != null) {
			score += Math.min(35.0, habits.mace * 3.0 + habits.crystals * 4.0);
			score += Math.min(20.0, habits.ranged * 1.5 + habits.elevated * 1.5);
			score += Math.min(12.0, habits.shielding + habits.strafing);
		}
		ComboState combo = board.combos.get(target.getUUID());
		if (combo != null && combo.expiresAt >= observer.level().getGameTime()) {
			score += 18.0 * Integer.bitCount(combo.mask);
		}
		return score;
	}

	private static int roleCount(SquadBoard board, CombatRole role) {
		return (int) board.soldiers.values().stream().filter(presence -> presence.role() == role).count();
	}

	private static Vec3 squadCentroid(SquadBoard board) {
		if (board.soldiers.isEmpty()) {
			return Vec3.ZERO;
		}
		Vec3 sum = Vec3.ZERO;
		for (Presence presence : board.soldiers.values()) {
			sum = sum.add(presence.position());
		}
		return sum.scale(1.0 / board.soldiers.size());
	}

	private static SquadBoard squadBoard(BattleSoldierEntity soldier) {
		ServerLevel level = (ServerLevel) soldier.level();
		ServerBoard server = SERVERS.computeIfAbsent(level.getServer(), ignored -> new ServerBoard());
		WorldBoard world = server.worlds.computeIfAbsent(level.dimension(), ignored -> new WorldBoard());
		return world.squads.computeIfAbsent(soldier.getSquad(), ignored -> new SquadBoard());
	}

	private static void cleanupWorld(ServerLevel level) {
		ServerBoard server = SERVERS.get(level.getServer());
		if (server == null) {
			return;
		}
		WorldBoard world = server.worlds.get(level.dimension());
		if (world == null) {
			return;
		}
		long tick = level.getGameTime();
		world.squads.values().forEach(board -> cleanupBoard(board, tick));
	}

	private static void cleanupBoard(SquadBoard board, long tick) {
		board.soldiers.entrySet().removeIf(entry -> tick - entry.getValue().tick() > STALE_TICKS);
		board.meleeReservations.values().forEach(set -> set.removeIf(id -> !board.soldiers.containsKey(id)));
		board.reservationBySoldier.keySet().removeIf(id -> !board.soldiers.containsKey(id));
		if (board.sharedTarget != null && tick - board.sharedTargetTick > 200) {
			board.sharedTarget = null;
			board.sharedTargetScore = 0.0;
		}
		if (tick % 200 == 0) {
			board.habits.values().forEach(HabitState::decay);
		}
		board.combos.entrySet().removeIf(entry -> entry.getValue().expiresAt < tick);
	}

	public record MeleeDirective(boolean mayWindup, Vec3 position, int slot) {
	}

	public record HabitSnapshot(
			int ranged,
			int shielding,
			int mace,
			int crystals,
			int elevated,
			int strafing
	) {
		public static final HabitSnapshot EMPTY = new HabitSnapshot(0, 0, 0, 0, 0, 0);
	}

	public record ComboSnapshot(boolean webbed, boolean debuffed, boolean explosive) {
		public static final ComboSnapshot EMPTY = new ComboSnapshot(false, false, false);

		public int chainStage() {
			return (this.webbed ? 1 : 0) + (this.debuffed ? 1 : 0) + (this.explosive ? 1 : 0);
		}
	}

	public enum ComboEvent {
		WEBBED(1, 120),
		DEBUFFED(2, 160),
		EXPLOSIVE(4, 100);

		private final int mask;
		private final int durationTicks;

		ComboEvent(int mask, int durationTicks) {
			this.mask = mask;
			this.durationTicks = durationTicks;
		}
	}

	public record SkillProfile(
			int decisionPeriodTicks,
			int reactionDelayTicks,
			int intelTtlTicks,
			double leadMultiplier,
			int maxMeleeAttackers
	) {
	}

	private record Presence(
			UUID id,
			CombatRole role,
			Vec3 position,
			@Nullable UUID targetId,
			long tick
	) {
		boolean specialist() {
			return this.role.isSpecialist();
		}
	}

	private static final class HabitState {
		int ranged;
		int shielding;
		int mace;
		int crystals;
		int elevated;
		int strafing;
		long lastSampleTick;

		void decay() {
			this.ranged = Math.max(0, this.ranged - 1);
			this.shielding = Math.max(0, this.shielding - 1);
			this.mace = Math.max(0, this.mace - 1);
			this.crystals = Math.max(0, this.crystals - 1);
			this.elevated = Math.max(0, this.elevated - 1);
			this.strafing = Math.max(0, this.strafing - 1);
		}
	}

	private static final class ComboState {
		int mask;
		long expiresAt;
	}

	private static final class SquadBoard {
		final Map<UUID, Presence> soldiers = new HashMap<>();
		final Map<UUID, LinkedHashSet<UUID>> meleeReservations = new HashMap<>();
		final Map<UUID, UUID> reservationBySoldier = new HashMap<>();
		final Map<UUID, HabitState> habits = new HashMap<>();
		final Map<UUID, ComboState> combos = new HashMap<>();
		@Nullable UUID sharedTarget;
		long sharedTargetTick;
		double sharedTargetScore;
	}

	private static final class WorldBoard {
		final EnumMap<SoldierSquad, SquadBoard> squads = new EnumMap<>(SoldierSquad.class);
	}

	private static final class ServerBoard {
		final Map<ResourceKey<Level>, WorldBoard> worlds = new HashMap<>();
	}
}
