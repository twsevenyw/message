package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.TerrainPlanner;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

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
		if (this.soldier.getCombatRole() == CombatRole.RANGER
				&& (this.soldier.shouldHoldRangerPerch()
						|| this.soldier.hasSpentRangerTowerThisEngagement())) {
			return false;
		}
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

		TerrainPlanner.TerrainPlan plan = TerrainPlanner.bestPlacement(this.soldier, target);
		if (plan == null) {
			return false;
		}
		this.placement = plan.position().immutable();
		this.cooldownTicks = switch (plan.objective()) {
			case BRIDGE -> 18;
			case STAIR -> 45;
			case COVER -> 100;
		};
		return true;
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
