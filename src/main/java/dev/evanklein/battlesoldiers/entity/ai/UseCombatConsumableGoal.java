package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class UseCombatConsumableGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private int retreatTicks;
	private boolean consuming;

	public UseCombatConsumableGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.soldier.prepareCombatConsumable();
	}

	@Override
	public boolean canContinueToUse() {
		return this.consuming
				? this.soldier.isUsingItem()
				: this.soldier.hasPreparedCombatConsumable();
	}

	@Override
	public void start() {
		this.retreatTicks = 18;
		this.consuming = false;
		if (!this.shouldRetreat()) {
			this.beginConsuming();
		}
	}

	@Override
	public void tick() {
		if (this.consuming) {
			return;
		}

		LivingEntity threat = this.soldier.getTarget();
		if (threat != null && threat.isAlive()) {
			this.soldier.getLookControl().setLookAt(threat, 40.0F, 40.0F);
			if (this.retreatTicks > 0 && this.soldier.distanceToSqr(threat) < 64.0) {
				this.retreatTicks--;
				Vec3 difference = this.soldier.position().subtract(threat.position());
				Vec3 away = new Vec3(difference.x, 0.0, difference.z);
				if (away.lengthSqr() > 0.01) {
					Vec3 destination = this.soldier.position().add(away.normalize().scale(8.0));
					this.soldier.setSprinting(false);
					if (this.soldier.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0)) {
						return;
					}
				}
				this.retreatTicks = 0;
			}
		}

		this.beginConsuming();
	}

	@Override
	public void stop() {
		this.soldier.setSprinting(false);
		this.soldier.finishCombatConsumable();
		this.consuming = false;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean shouldRetreat() {
		if (this.soldier.shouldHoldRangerPerch()) {
			return false;
		}
		LivingEntity threat = this.soldier.getTarget();
		return threat != null && threat.isAlive() && this.soldier.distanceToSqr(threat) < 64.0;
	}

	private void beginConsuming() {
		this.soldier.setSprinting(false);
		this.soldier.getNavigation().stop();
		this.soldier.beginCombatConsumable();
		this.consuming = true;
	}
}
