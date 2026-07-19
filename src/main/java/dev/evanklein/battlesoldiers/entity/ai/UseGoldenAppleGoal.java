package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class UseGoldenAppleGoal extends Goal {
	private final BattleSoldierEntity soldier;

	public UseGoldenAppleGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.soldier.canUseGoldenApple();
	}

	@Override
	public boolean canContinueToUse() {
		return this.soldier.isUsingItem();
	}

	@Override
	public void start() {
		this.soldier.getNavigation().stop();
		this.soldier.beginGoldenApple();
	}

	@Override
	public void stop() {
		this.soldier.finishGoldenApple();
	}
}
