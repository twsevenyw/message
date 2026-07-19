package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class SoldierCombatGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private int attackCooldown;
	private int attackWindup;
	private int pathCooldown;
	private int shieldTicks;
	private int shieldCooldown;
	private int bowCooldown;
	private int strafeTicks;
	private boolean strafeClockwise;
	private boolean critJump;
	private boolean critAirborne;
	private int critTimeout;

	public SoldierCombatGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.soldier.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void start() {
		this.soldier.setAggressive(true);
		this.pathCooldown = 0;
	}

	@Override
	public void stop() {
		this.lowerShield();
		if (this.soldier.isUsingItem()) {
			this.soldier.stopUsingItem();
		}
		this.soldier.getNavigation().stop();
		this.soldier.setSprinting(false);
		this.soldier.setAggressive(false);
		this.attackWindup = 0;
		this.critJump = false;
	}

	@Override
	public void tick() {
		LivingEntity target = this.soldier.getTarget();
		if (target == null) {
			return;
		}

		this.soldier.getLookControl().setLookAt(target, 35.0F, 35.0F);
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}
		if (this.shieldCooldown > 0) {
			this.shieldCooldown--;
		}
		if (this.bowCooldown > 0) {
			this.bowCooldown--;
		}

		switch (this.soldier.getCombatRole()) {
			case VANGUARD -> this.tickVanguard(target);
			case BRUTE -> this.tickBrute(target);
			case RANGER -> this.tickRanger(target);
			case TRAPPER -> this.tickTrapper(target);
		}
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void tickVanguard(LivingEntity target) {
		this.soldier.equipSword();
		if (this.shieldTicks > 0) {
			this.tickShield(target);
			return;
		}
		if (this.shouldRaiseShield(target)) {
			this.beginShield();
			this.tickShield(target);
			return;
		}
		this.tickMelee(target, CombatRole.VANGUARD);
	}

	private void tickBrute(LivingEntity target) {
		this.soldier.equipAxe();
		if (this.critJump) {
			this.tickCriticalJump(target);
			return;
		}

		double distance = this.soldier.distanceToSqr(target);
		if (this.attackCooldown <= 0
				&& this.attackWindup <= 0
				&& this.soldier.onGround()
				&& distance > 1.0
				&& distance <= 10.0) {
			this.critJump = true;
			this.critAirborne = false;
			this.critTimeout = 16;
			this.soldier.getNavigation().moveTo(target, 0.95);
			this.soldier.getJumpControl().jump();
			return;
		}
		this.tickMelee(target, CombatRole.BRUTE);
	}

	private void tickTrapper(LivingEntity target) {
		this.soldier.equipSword();
		double distance = this.soldier.distanceToSqr(target);
		if (distance >= 7.0 && distance <= 25.0) {
			this.soldier.getNavigation().stop();
			this.soldier.getMoveControl().strafe(0.15F, this.strafeClockwise ? 0.45F : -0.45F);
			if (++this.strafeTicks >= 24) {
				this.strafeTicks = 0;
				this.strafeClockwise = !this.strafeClockwise;
			}
		}
		this.tickMelee(target, CombatRole.TRAPPER);
	}

	private void tickRanger(LivingEntity target) {
		double distance = this.soldier.distanceToSqr(target);
		double preferredMin = 36.0;
		double preferredMax = Math.pow(10.0 + this.soldier.getGearLevel().id() * 1.6, 2.0);

		if (!this.soldier.hasArrows()) {
			this.soldier.equipBackupMelee();
			this.tickMelee(target, CombatRole.RANGER);
			return;
		}

		if (distance < preferredMin) {
			if (this.soldier.isUsingItem()) {
				this.soldier.stopUsingItem();
			}
			this.soldier.equipBackupMelee();
			this.retreatFrom(target, 5.0);
			if (this.soldier.isWithinMeleeAttackRange(target)) {
				this.tickMelee(target, CombatRole.RANGER);
			}
			return;
		}

		this.soldier.equipBow();
		boolean canSee = this.soldier.getSensing().hasLineOfSight(target);
		if (distance > preferredMax || !canSee) {
			if (this.soldier.isUsingItem()) {
				this.soldier.stopUsingItem();
			}
			this.soldier.getNavigation().moveTo(target, 0.85);
			return;
		}

		this.soldier.getNavigation().stop();
		this.soldier.getMoveControl().strafe(
				distance < preferredMin * 1.35 ? -0.25F : 0.12F,
				this.strafeClockwise ? 0.38F : -0.38F
		);
		if (++this.strafeTicks >= 30) {
			this.strafeTicks = 0;
			this.strafeClockwise = !this.strafeClockwise;
		}

		if (this.soldier.isUsingItem()) {
			int drawTicks = this.soldier.getTicksUsingItem();
			if (drawTicks >= BowItem.MAX_DRAW_DURATION) {
				this.soldier.stopUsingItem();
				this.soldier.performRangedAttack(target, BowItem.getPowerForTime(drawTicks));
				this.bowCooldown = this.soldier.getGearLevel().bowAttackInterval();
			}
		} else if (this.bowCooldown <= 0) {
			this.soldier.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.soldier, Items.BOW));
		}
	}

	private void tickMelee(LivingEntity target, CombatRole role) {
		if (this.attackWindup > 0) {
			this.soldier.getNavigation().stop();
			this.soldier.getMoveControl().strafe(0.08F, this.strafeClockwise ? 0.28F : -0.28F);
			if (--this.attackWindup == 0) {
				this.performMeleeAttack(target, role);
			}
			return;
		}

		if (this.soldier.isWithinMeleeAttackRange(target)) {
			this.soldier.getNavigation().stop();
			if (this.attackCooldown <= 0) {
				if (target.isBlocking() && role == CombatRole.VANGUARD) {
					this.soldier.equipAxe();
				}
				this.attackWindup = this.attackWindupTicks(role);
			}
			return;
		}

		if (this.pathCooldown-- <= 0) {
			this.soldier.getNavigation().moveTo(target, role == CombatRole.BRUTE ? 0.92 : 1.0);
			this.pathCooldown = 6 + this.soldier.getRandom().nextInt(6);
		}
		this.soldier.setSprinting(false);
	}

	private void performMeleeAttack(LivingEntity target, CombatRole role) {
		if (!this.soldier.isWithinMeleeAttackRange(target)) {
			this.attackCooldown = 8;
			return;
		}

		boolean wasBlocking = target.isBlocking();
		ItemStack blockingItem = target.getItemBlockingWith();
		this.soldier.swing(InteractionHand.MAIN_HAND);
		ServerLevel level = getServerLevel(this.soldier);
		boolean hit = this.soldier.doHurtTarget(level, target);
		if (hit && wasBlocking && blockingItem != null && this.soldier.isHoldingAxe()) {
			BlocksAttacks blocksAttacks = blockingItem.get(DataComponents.BLOCKS_ATTACKS);
			if (blocksAttacks != null) {
				blocksAttacks.disable(level, target, 3.0F, blockingItem);
			}
		}
		this.attackCooldown = this.attackRecoveryTicks(role);
		if (role == CombatRole.VANGUARD) {
			this.soldier.equipSword();
			this.shieldCooldown = Math.max(24, 64 - this.soldier.getGearLevel().id() * 4);
		}
	}

	private boolean shouldRaiseShield(LivingEntity target) {
		if (this.shieldCooldown > 0
				|| this.soldier.isUsingItem()
				|| !this.soldier.getOffhandItem().is(Items.SHIELD)
				|| this.attackWindup > 0) {
			return false;
		}
		double distance = this.soldier.distanceToSqr(target);
		return distance <= 64.0 || (this.soldier.isRangedThreat(target) && distance <= 196.0);
	}

	private void beginShield() {
		this.shieldTicks = this.soldier.getGearLevel().shieldWindowTicks() + 5;
		this.soldier.startUsingItem(InteractionHand.OFF_HAND);
	}

	private void tickShield(LivingEntity target) {
		this.soldier.getLookControl().setLookAt(target, 50.0F, 50.0F);
		double distance = this.soldier.distanceToSqr(target);
		if (distance > 10.0) {
			this.soldier.getNavigation().moveTo(target, 0.55);
		} else {
			this.soldier.getNavigation().stop();
		}
		if (!this.soldier.isUsingItem() || !this.soldier.getOffhandItem().is(Items.SHIELD)) {
			this.lowerShield();
			return;
		}
		if (--this.shieldTicks <= 0) {
			this.lowerShield();
			this.shieldCooldown = Math.max(24, 64 - this.soldier.getGearLevel().id() * 4);
		}
	}

	private void lowerShield() {
		if (this.soldier.isUsingItem() && this.soldier.getUseItem().is(Items.SHIELD)) {
			this.soldier.stopUsingItem();
		}
		this.shieldTicks = 0;
	}

	private void tickCriticalJump(LivingEntity target) {
		this.soldier.getLookControl().setLookAt(target, 45.0F, 45.0F);
		this.soldier.getNavigation().moveTo(target, 0.90);
		if (!this.soldier.onGround()) {
			this.critAirborne = true;
		}
		boolean descending = this.critAirborne && this.soldier.getDeltaMovement().y < 0.0;
		if (descending && this.soldier.distanceToSqr(target) <= 4.0) {
			this.soldier.markCriticalAttack();
			this.performMeleeAttack(target, CombatRole.BRUTE);
			this.critJump = false;
			return;
		}
		if (--this.critTimeout <= 0 || (this.critAirborne && this.soldier.onGround())) {
			this.critJump = false;
			this.attackCooldown = 12;
		}
	}

	private void retreatFrom(LivingEntity target, double distance) {
		Vec3 away = this.soldier.position().subtract(target.position());
		if (away.horizontalDistanceSqr() < 0.01) {
			away = new Vec3(1.0, 0.0, 0.0);
		}
		Vec3 destination = this.soldier.position().add(away.normalize().scale(distance));
		this.soldier.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.95);
	}

	private int attackWindupTicks(CombatRole role) {
		int tier = this.soldier.getGearLevel().id();
		return switch (role) {
			case VANGUARD -> 13 - tier;
			case BRUTE -> 19 - tier;
			case RANGER -> 15 - tier;
			case TRAPPER -> 15 - tier;
		};
	}

	private int attackRecoveryTicks(CombatRole role) {
		int tier = this.soldier.getGearLevel().id();
		return switch (role) {
			case VANGUARD -> 21 - Math.min(4, tier);
			case BRUTE -> 33 - tier;
			case RANGER -> 25 - tier;
			case TRAPPER -> 27 - tier;
		};
	}
}
