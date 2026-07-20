package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public final class SoldierWanderGoal extends WaterAvoidingRandomStrollGoal {
	private final BattleSoldierEntity soldier;

	public SoldierWanderGoal(BattleSoldierEntity soldier, double speed) {
		super(soldier, speed);
		this.soldier = soldier;
	}

	@Override
	public boolean canUse() {
		return !this.soldier.shouldHoldRangerPerch() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return !this.soldier.shouldHoldRangerPerch() && super.canContinueToUse();
	}
}
