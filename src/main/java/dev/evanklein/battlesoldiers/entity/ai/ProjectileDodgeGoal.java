package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class ProjectileDodgeGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private Vec3 destination;
	private int ticks;

	public ProjectileDodgeGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.soldier.getCombatRole() == CombatRole.VANGUARD || this.soldier.isUsingItem()) {
			return false;
		}
		this.destination = this.soldier.findProjectileDodgePosition(10.0);
		return this.destination != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.destination != null && this.ticks < 8;
	}

	@Override
	public void start() {
		this.ticks = 0;
	}

	@Override
	public void tick() {
		this.ticks++;
		this.soldier.getNavigation().moveTo(
				this.destination.x,
				this.destination.y,
				this.destination.z,
				1.25
		);
	}

	@Override
	public void stop() {
		this.destination = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
