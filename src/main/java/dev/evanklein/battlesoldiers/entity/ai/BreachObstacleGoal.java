package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class BreachObstacleGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BlockPos obstacle;
	private int breakTicks;
	private int requiredBreakTicks;
	private int nextScanTick;

	public BreachObstacleGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!(this.soldier.level() instanceof ServerLevel level) || this.soldier.tickCount < this.nextScanTick) {
			return false;
		}
		this.nextScanTick = this.soldier.tickCount + 8;

		LivingEntity target = this.soldier.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (!this.soldier.horizontalCollision
				&& !this.soldier.getNavigation().isStuck()
				&& this.soldier.getSensing().hasLineOfSight(target)) {
			return false;
		}

		BlockHitResult hit = level.clip(new ClipContext(
				this.soldier.getEyePosition(),
				new Vec3(target.getX(), this.soldier.getEyeY(), target.getZ()),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				this.soldier
		));
		if (hit.getType() == HitResult.Type.MISS || !this.soldier.canBreakBlock(hit.getBlockPos())) {
			return false;
		}

		this.obstacle = hit.getBlockPos().immutable();
		BlockState state = level.getBlockState(this.obstacle);
		this.requiredBreakTicks = this.soldier.getBreakTicks(state, this.obstacle);
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = this.soldier.getTarget();
		return this.obstacle != null
				&& target != null
				&& target.isAlive()
				&& this.breakTicks < 240
				&& this.soldier.canBreakBlock(this.obstacle);
	}

	@Override
	public void start() {
		this.breakTicks = 0;
	}

	@Override
	public void tick() {
		if (!(this.soldier.level() instanceof ServerLevel level) || this.obstacle == null) {
			return;
		}

		Vec3 center = Vec3.atCenterOf(this.obstacle);
		this.soldier.getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 30.0F);
		Direction approachDirection = this.getApproachDirection();
		Vec3 approach = Vec3.atBottomCenterOf(this.obstacle.relative(approachDirection));
		if (this.soldier.distanceToSqr(center) > 18.0) {
			this.soldier.getNavigation().moveTo(approach.x, approach.y, approach.z, 1.15);
			return;
		}

		this.soldier.getNavigation().stop();
		if (this.breakTicks % 6 == 0) {
			this.soldier.swing(InteractionHand.MAIN_HAND);
		}

		this.breakTicks++;
		int crack = Math.min(9, (int) (10.0F * this.breakTicks / this.requiredBreakTicks));
		level.destroyBlockProgress(this.soldier.getId(), this.obstacle, crack);

		if (this.breakTicks >= this.requiredBreakTicks) {
			BlockState state = level.getBlockState(this.obstacle);
			if (level.destroyBlock(this.obstacle, true, this.soldier, 512)) {
				this.soldier.onBlockBreached(state);
			}
			level.destroyBlockProgress(this.soldier.getId(), this.obstacle, -1);
			this.obstacle = null;
		}
	}

	@Override
	public void stop() {
		if (this.obstacle != null) {
			this.soldier.level().destroyBlockProgress(this.soldier.getId(), this.obstacle, -1);
		}
		this.obstacle = null;
		this.breakTicks = 0;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private Direction getApproachDirection() {
		int deltaX = this.soldier.getBlockX() - this.obstacle.getX();
		int deltaZ = this.soldier.getBlockZ() - this.obstacle.getZ();
		if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
			return deltaX >= 0 ? Direction.EAST : Direction.WEST;
		}
		return deltaZ >= 0 ? Direction.SOUTH : Direction.NORTH;
	}
}
