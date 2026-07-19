package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.item.Items;

public final class SoldierMeleeAttackGoal extends ZombieAttackGoal {
	private final BattleSoldierEntity soldier;
	private int shieldTicks;
	private int shieldCooldown;

	public SoldierMeleeAttackGoal(BattleSoldierEntity soldier) {
		super(soldier, 1.2, true);
		this.soldier = soldier;
	}

	@Override
	public void tick() {
		LivingEntity target = this.soldier.getTarget();
		if (target == null) {
			return;
		}

		if (this.shieldCooldown > 0) {
			this.shieldCooldown--;
		}

		if (this.shieldTicks > 0) {
			this.tickShield(target);
			return;
		}

		if (this.shouldRaiseShield(target)) {
			this.beginShield(target);
			return;
		}

		double distance = this.soldier.distanceToSqr(target);
		this.soldier.setSprinting(distance > 16.0 && this.soldier.getSensing().hasLineOfSight(target));
		super.tick();
	}

	@Override
	protected boolean canPerformAttack(LivingEntity target) {
		return this.shieldTicks <= 0 && super.canPerformAttack(target);
	}

	@Override
	public void stop() {
		this.lowerShield();
		this.soldier.setSprinting(false);
		super.stop();
	}

	private boolean shouldRaiseShield(LivingEntity target) {
		if (this.shieldCooldown > 0
				|| this.soldier.isUsingItem()
				|| !this.soldier.getOffhandItem().is(Items.SHIELD)) {
			return false;
		}

		double distance = this.soldier.distanceToSqr(target);
		if (this.soldier.isRangedThreat(target) && distance <= 225.0) {
			return true;
		}
		if (this.soldier.getHealth() <= this.soldier.getMaxHealth() * 0.55F && distance <= 36.0) {
			return true;
		}
		return distance <= 25.0 && this.soldier.getRandom().nextInt(45) == 0;
	}

	private void beginShield(LivingEntity target) {
		this.shieldTicks = 14 + this.soldier.getRandom().nextInt(11);
		this.soldier.getLookControl().setLookAt(target, 40.0F, 40.0F);
		this.soldier.startUsingItem(InteractionHand.OFF_HAND);
		this.soldier.setSprinting(false);
	}

	private void tickShield(LivingEntity target) {
		this.soldier.getLookControl().setLookAt(target, 50.0F, 50.0F);
		double distance = this.soldier.distanceToSqr(target);
		if (distance > 9.0) {
			this.soldier.getNavigation().moveTo(target, 0.72);
		} else {
			this.soldier.getNavigation().stop();
		}

		if (!this.soldier.getOffhandItem().is(Items.SHIELD) || !this.soldier.isUsingItem()) {
			this.lowerShield();
			return;
		}

		if (--this.shieldTicks <= 0) {
			this.lowerShield();
			this.shieldCooldown = 24 + this.soldier.getRandom().nextInt(22);
		}
	}

	private void lowerShield() {
		if (this.soldier.isUsingItem() && this.soldier.getUseItem().is(Items.SHIELD)) {
			this.soldier.stopUsingItem();
		}
		this.shieldTicks = 0;
	}
}
