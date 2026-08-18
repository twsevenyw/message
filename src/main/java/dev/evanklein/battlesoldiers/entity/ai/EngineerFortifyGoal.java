package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class EngineerFortifyGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BlockPos wallBase;
	private int cooldown;

	public EngineerFortifyGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		LivingEntity target = this.soldier.getTarget();
		if (this.soldier.getCombatRole() != CombatRole.ENGINEER
				|| target == null
				|| !target.isAlive()
				|| !this.soldier.canBuild()
				|| this.soldier.distanceToSqr(target) < 36.0
				|| !this.soldier.getSensing().hasLineOfSight(target)) {
			return false;
		}
		Direction direction = directionToward(this.soldier, target);
		this.wallBase = this.soldier.blockPosition().relative(direction);
		return this.soldier.canPlaceTacticalBlock(this.wallBase)
				|| this.soldier.canPlaceTacticalBlock(this.wallBase.above());
	}

	@Override
	public void start() {
		this.soldier.getNavigation().stop();
		boolean placed = this.soldier.placeTacticalBlock(this.wallBase);
		placed |= this.soldier.placeTacticalBlock(this.wallBase.above());
		this.cooldown = placed ? 120 : 40;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	private static Direction directionToward(BattleSoldierEntity soldier, LivingEntity target) {
		double dx = target.getX() - soldier.getX();
		double dz = target.getZ() - soldier.getZ();
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx >= 0.0 ? Direction.EAST : Direction.WEST;
		}
		return dz >= 0.0 ? Direction.SOUTH : Direction.NORTH;
	}
}
