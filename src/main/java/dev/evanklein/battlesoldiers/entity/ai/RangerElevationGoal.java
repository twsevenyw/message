package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class RangerElevationGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BlockPos footPosition;
	private int desiredLayers;
	private int placedLayers;
	private int timeout;
	private boolean jumpRequested;
	private boolean placedThisJump;

	public RangerElevationGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.soldier.getTarget();
		return this.soldier.getCombatRole() == CombatRole.RANGER
				&& target != null
				&& target.isAlive()
				&& this.soldier.getRangerTowerCooldown() <= 0
				&& this.soldier.canBuild()
				&& this.soldier.onGround()
				&& !this.soldier.isInWater()
				&& this.soldier.distanceToSqr(target) <= 100.0
				&& this.soldier.getY() - target.getY() < 2.0
				&& this.hasHeadroom();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = this.soldier.getTarget();
		return target != null
				&& target.isAlive()
				&& this.placedLayers < this.desiredLayers
				&& this.timeout < 80
				&& this.soldier.canBuild();
	}

	@Override
	public void start() {
		this.desiredLayers = this.soldier.getGearLevel().id() >= 5
				? 3
				: this.soldier.getGearLevel().id() >= 3 ? 2 : 1;
		this.placedLayers = 0;
		this.timeout = 0;
		this.jumpRequested = false;
		this.placedThisJump = false;
		this.soldier.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.timeout++;
		LivingEntity target = this.soldier.getTarget();
		if (target != null) {
			this.soldier.getLookControl().setLookAt(target, 40.0F, 40.0F);
		}

		if (!this.jumpRequested && this.soldier.onGround()) {
			this.footPosition = this.soldier.blockPosition().immutable();
			this.placedThisJump = false;
			this.jumpRequested = true;
			this.soldier.getJumpControl().jump();
			return;
		}

		if (this.jumpRequested
				&& !this.placedThisJump
				&& !this.soldier.onGround()
				&& this.soldier.getDeltaMovement().y > 0.05
				&& this.soldier.getY() >= this.footPosition.getY() + 0.45) {
			if (this.soldier.placePillarBlock(this.footPosition)) {
				this.placedLayers++;
				this.placedThisJump = true;
			}
		}

		if (this.jumpRequested && this.placedThisJump && this.soldier.onGround()) {
			this.jumpRequested = false;
		}
	}

	@Override
	public void stop() {
		this.soldier.setRangerTowerCooldown(this.placedLayers > 0 ? 180 : 90);
		this.soldier.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean hasHeadroom() {
		BlockPos origin = this.soldier.blockPosition();
		for (int height = 1; height <= 5; height++) {
			if (!this.soldier.level().getBlockState(origin.above(height)).isAir()) {
				return false;
			}
		}
		return true;
	}
}
