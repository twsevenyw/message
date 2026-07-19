package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public final class TacticalBuildGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BlockPos placement;
	private int nextAttemptTick;

	public TacticalBuildGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
	}

	@Override
	public boolean canUse() {
		if (this.soldier.tickCount < this.nextAttemptTick || !this.soldier.canBuild()) {
			return false;
		}
		this.nextAttemptTick = this.soldier.tickCount + 12;

		LivingEntity target = this.soldier.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}

		Direction forward = this.soldier.getDirection();
		BlockPos ahead = this.soldier.blockPosition().relative(forward);
		BlockPos bridge = ahead.below();

		if (this.soldier.level().getBlockState(bridge).isAir()
				&& this.soldier.level().getBlockState(ahead).isAir()
				&& this.soldier.canPlaceTacticalBlock(bridge)) {
			this.placement = bridge.immutable();
			return true;
		}

		if (this.soldier.distanceToSqr(target) >= 36.0 && this.soldier.getRandom().nextInt(4) == 0) {
			Direction side = this.soldier.getRandom().nextBoolean()
					? forward.getClockWise()
					: forward.getCounterClockWise();
			BlockPos cover = this.soldier.blockPosition().relative(forward).relative(side);
			if (this.soldier.canPlaceTacticalBlock(cover)) {
				this.placement = cover.immutable();
				return true;
			}
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
}
