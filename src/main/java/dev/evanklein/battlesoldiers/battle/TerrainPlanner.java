package dev.evanklein.battlesoldiers.battle;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

public final class TerrainPlanner {
	private TerrainPlanner() {
	}

	@Nullable
	public static TerrainPlan bestPlacement(BattleSoldierEntity soldier, LivingEntity target) {
		Direction forward = directionToward(soldier, target);
		BlockPos ahead = soldier.blockPosition().relative(forward);
		Path path = soldier.getNavigation().createPath(target, 0);
		boolean pathBlocked = path == null || !path.canReach() || soldier.getNavigation().isStuck();
		TerrainPlan best = null;

		BlockPos bridge = ahead.below();
		if (pathBlocked
				&& soldier.level().getBlockState(bridge).isAir()
				&& soldier.level().getBlockState(ahead).isAir()
				&& soldier.level().getBlockState(ahead.above()).isAir()
				&& soldier.canPlaceTacticalBlock(bridge)) {
			best = new TerrainPlan(TerrainObjective.BRIDGE, bridge, 100.0);
		}

		boolean exposed = soldier.isRangedThreat(target)
				&& soldier.getSensing().hasLineOfSight(target)
				&& horizontalDistanceSquared(soldier, target) >= 64.0;
		if (exposed && soldier.canPlaceTacticalBlock(ahead)) {
			double utility = soldier.getHealth() < soldier.getMaxHealth() * 0.7F ? 95.0 : 72.0;
			if (best == null || utility > best.utility()) {
				best = new TerrainPlan(TerrainObjective.COVER, ahead, utility);
			}
		}

		if (pathBlocked
				&& target.getY() - soldier.getY() >= 1.75
				&& horizontalDistanceSquared(soldier, target) <= 144.0
				&& soldier.canPlaceTacticalBlock(ahead)) {
			double utility = 88.0 + Math.min(12.0, target.getY() - soldier.getY());
			if (best == null || utility > best.utility()) {
				best = new TerrainPlan(TerrainObjective.STAIR, ahead, utility);
			}
		}
		return best;
	}

	public static double horizontalDistanceSquared(BattleSoldierEntity soldier, LivingEntity target) {
		double dx = target.getX() - soldier.getX();
		double dz = target.getZ() - soldier.getZ();
		return dx * dx + dz * dz;
	}

	private static Direction directionToward(BattleSoldierEntity soldier, LivingEntity target) {
		double dx = target.getX() - soldier.getX();
		double dz = target.getZ() - soldier.getZ();
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx >= 0.0 ? Direction.EAST : Direction.WEST;
		}
		return dz >= 0.0 ? Direction.SOUTH : Direction.NORTH;
	}

	public enum TerrainObjective {
		BRIDGE,
		COVER,
		STAIR
	}

	public record TerrainPlan(TerrainObjective objective, BlockPos position, double utility) {
	}
}
