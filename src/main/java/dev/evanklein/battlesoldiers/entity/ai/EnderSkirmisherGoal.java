package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

public final class EnderSkirmisherGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private LivingEntity victim;
	private int cooldown;

	public EnderSkirmisherGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		this.victim = this.soldier.getTarget();
		if (this.soldier.getCombatRole() != CombatRole.ENDER_SKIRMISHER
				|| this.victim == null
				|| !this.victim.isAlive()
				|| !this.soldier.hasInventoryItem(Items.ENDER_PEARL)) {
			return false;
		}
		return this.soldier.distanceToSqr(this.victim) >= 64.0
				|| this.victim.getY() - this.soldier.getY() >= 3.0
				|| this.soldier.getNavigation().isStuck();
	}

	@Override
	public void start() {
		this.soldier.getNavigation().stop();
		boolean teleported = this.soldier.blinkBehindTarget(this.victim);
		this.cooldown = teleported ? 120 : 40;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}
}
