package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public final class TacticalBuildGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BlockPos placement;
	private int nextAttemptTick;
	private int cooldownTicks;

	public TacticalBuildGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (this.soldier.tickCount < this.nextAttemptTick
				|| !this.soldier.canBuild()
				|| this.soldier.isUsingItem()) {
			return false;
		}
		this.nextAttemptTick = this.soldier.tickCount + 20;

		LivingEntity target = this.soldier.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}

		Direction forward = directionToward(this.soldier, target);
		BlockPos ahead = this.soldier.blockPosition().relative(forward);
		BlockPos bridge = ahead.below();
		Path path = this.soldier.getNavigation().createPath(target, 0);
		boolean pathBlocked = path == null || !path.canReach() || this.soldier.getNavigation().isStuck();

		if (pathBlocked
				&& this.soldier.level().getBlockState(bridge).isAir()
				&& this.soldier.level().getBlockState(ahead).isAir()
				&& this.soldier.level().getBlockState(ahead.above()).isAir()
				&& this.soldier.canPlaceTacticalBlock(bridge)) {
			this.placement = bridge.immutable();
			this.cooldownTicks = 18;
			return true;
		}

		boolean needsRangedCover = this.soldier.isRangedThreat(target)
				&& this.soldier.getSensing().hasLineOfSight(target)
				&& this.soldier.distanceToSqr(target) >= 64.0
				&& (this.soldier.isArcher() || this.soldier.getHealth() < this.soldier.getMaxHealth() * 0.7F);
		if (needsRangedCover) {
			BlockPos cover = ahead;
			if (this.soldier.canPlaceTacticalBlock(cover)) {
				this.placement = cover.immutable();
				this.cooldownTicks = 100;
				return true;
			}
		}

		boolean needsStep = pathBlocked
				&& target.getY() - this.soldier.getY() >= 1.75
				&& this.soldier.distanceToSqr(target) <= 144.0;
		if (needsStep && this.soldier.canPlaceTacticalBlock(ahead)) {
			this.placement = ahead.immutable();
			this.cooldownTicks = 45;
			return true;
		}

		return false;
	}

	@Override
	public void start() {
		if (this.placement != null) {
			this.soldier.placeTacticalBlock(this.placement);
			this.placement = null;
		}
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	private static Direction directionToward(BattleSoldierEntity soldier, LivingEntity target) {
		double deltaX = target.getX() - soldier.getX();
		double deltaZ = target.getZ() - soldier.getZ();
		if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
			return deltaX >= 0.0 ? Direction.EAST : Direction.WEST;
		}
		return deltaZ >= 0.0 ? Direction.SOUTH : Direction.NORTH;
	}
}
