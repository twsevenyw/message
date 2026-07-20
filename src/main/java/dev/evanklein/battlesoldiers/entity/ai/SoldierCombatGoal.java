package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
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
	private int critCooldown;
	private CombatRole criticalRole = CombatRole.BRUTE;

	public SoldierCombatGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.soldier.getTarget();
		return target != null && target.isAlive() || this.soldier.findNearestCrystal(20.0) != null;
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
		this.soldier.setAttackTelegraphed(false);
		this.attackWindup = 0;
		this.critJump = false;
	}

	@Override
	public void tick() {
		LivingEntity target = this.soldier.getTarget();
		if (target == null) {
			this.tickCrystalResponse();
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
		if (this.critCooldown > 0) {
			this.critCooldown--;
		}

		if (this.tickOverheadWeaponEvasion(target)) {
			return;
		}
		if (this.tickCrystalResponse()) {
			return;
		}
		if (this.critJump) {
			this.tickCriticalJump(target);
			return;
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

	private boolean tickOverheadWeaponEvasion(LivingEntity target) {
		ItemStack weapon = target.isUsingItem() ? target.getUseItem() : target.getMainHandItem();
		boolean mace = weapon.is(Items.MACE) || weapon.getItem() instanceof MaceItem;
		boolean kinetic = weapon.has(DataComponents.KINETIC_WEAPON);
		boolean genericWeapon = weapon.has(DataComponents.WEAPON) || kinetic;
		double verticalDistance = target.getY() - this.soldier.getY();
		double deltaX = target.getX() - this.soldier.getX();
		double deltaZ = target.getZ() - this.soldier.getZ();
		double horizontalDistance = deltaX * deltaX + deltaZ * deltaZ;
		boolean descending = target.getDeltaMovement().y < -0.05 || target.fallDistance > 0.5F;
		boolean dangerous = genericWeapon
				&& verticalDistance >= 2.5
				&& horizontalDistance <= 49.0
				&& (descending || mace || kinetic);
		if (!dangerous) {
			return false;
		}

		if (this.soldier.isUsingItem()) {
			this.soldier.stopUsingItem();
		}
		double predictionTicks = Mth.clamp(verticalDistance / 0.7, 2.0, 10.0);
		Vec3 impact = target.position().add(target.getDeltaMovement().scale(predictionTicks));
		this.moveAwayFromPoint(impact, 5.0, 1.18);
		this.soldier.setSprinting(true);
		return true;
	}

	private boolean tickCrystalResponse() {
		EndCrystal crystal = this.soldier.findNearestCrystal(20.0);
		if (crystal == null) {
			return false;
		}

		double distance = this.soldier.distanceToSqr(crystal);
		boolean safeToPop = this.soldier.canSafelyPopCrystal(crystal);
		if (distance < 144.0 || !safeToPop) {
			if (this.soldier.isUsingItem()) {
				this.soldier.stopUsingItem();
			}
			this.moveAwayFromPoint(crystal.position(), 7.0, 1.12);
			return true;
		}

		if (this.soldier.getCombatRole() == CombatRole.RANGER
				&& this.soldier.hasArrows()
				&& distance <= 900.0) {
			this.soldier.equipBow();
			if (!this.soldier.hasLineOfSight(crystal)) {
				if (this.soldier.isUsingItem()) {
					this.soldier.stopUsingItem();
				}
				this.moveAwayFromPoint(crystal.position(), 4.0, 1.0);
				return true;
			}
			this.soldier.getNavigation().stop();
			this.soldier.getMoveControl().setWait();
			this.soldier.getLookControl().setLookAt(crystal, 45.0F, 45.0F);
			this.tickBowDrawAndFire(crystal);
			return true;
		}
		return false;
	}

	private void tickVanguard(LivingEntity target) {
		if (this.attackWindup <= 0) {
			this.soldier.equipSword();
		}
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
		double deltaX = target.getX() - this.soldier.getX();
		double deltaZ = target.getZ() - this.soldier.getZ();
		double distance = deltaX * deltaX + deltaZ * deltaZ;
		double bowRange = Math.min(30.0, 14.0 + this.soldier.getGearLevel().id() * 2.0);
		double preferredMax = bowRange * bowRange;

		if (!this.soldier.hasArrows()) {
			this.soldier.equipBackupMelee();
			this.tickMelee(target, CombatRole.RANGER);
			return;
		}

		boolean canSee = this.soldier.getSensing().hasLineOfSight(target);
		if (this.soldier.shouldHoldRangerPerch()) {
			this.soldier.equipBow();
			this.soldier.getNavigation().stop();
			this.soldier.getMoveControl().setWait();
			if (!canSee || distance > preferredMax) {
				if (this.soldier.isUsingItem()) {
					this.soldier.stopUsingItem();
				}
				return;
			}
			this.tickBowDrawAndFire(target);
			return;
		}

		boolean hasFrontline = this.soldier.hasFrontlineSupport(target);
		double preferredMin = hasFrontline ? 36.0 : 9.0;
		if (distance < preferredMin) {
			if (this.soldier.isUsingItem()) {
				this.soldier.stopUsingItem();
			}
			this.soldier.equipBackupMelee();
			if (!hasFrontline || distance <= 4.0) {
				this.tickMelee(target, CombatRole.RANGER);
			} else {
				this.retreatFrom(target, 5.0);
			}
			return;
		}

		this.soldier.equipBow();
		if (distance > preferredMax || !canSee) {
			if (this.soldier.isUsingItem()) {
				this.soldier.stopUsingItem();
			}
			this.moveToPredicted(target, 0.98, CombatRole.RANGER);
			return;
		}

		this.soldier.getNavigation().stop();
		if (hasFrontline) {
			this.soldier.getMoveControl().strafe(
					distance < preferredMin * 1.35 ? -0.25F : 0.12F,
					this.strafeClockwise ? 0.38F : -0.38F
			);
			if (++this.strafeTicks >= 30) {
				this.strafeTicks = 0;
				this.strafeClockwise = !this.strafeClockwise;
			}
		}
		this.tickBowDrawAndFire(target);
	}

	private void tickBowDrawAndFire(Entity target) {
		if (this.soldier.isUsingItem()) {
			int drawTicks = this.soldier.getTicksUsingItem();
			if (drawTicks >= BowItem.MAX_DRAW_DURATION) {
				this.soldier.stopUsingItem();
				this.soldier.shootArrowAt(target, BowItem.getPowerForTime(drawTicks));
				this.bowCooldown = this.soldier.getGearLevel().bowAttackInterval();
			}
		} else if (this.bowCooldown <= 0) {
			this.soldier.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.soldier, Items.BOW));
		}
	}

	private void tickMelee(LivingEntity target, CombatRole role) {
		if (this.attackWindup > 0) {
			if (this.soldier.isWithinMeleeAttackRange(target)) {
				this.soldier.getNavigation().stop();
				this.soldier.getMoveControl().strafe(0.08F, this.strafeClockwise ? 0.28F : -0.28F);
			} else {
				this.steerTowardPrediction(target, 0.82);
			}
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
				this.soldier.setAttackTelegraphed(true);
			}
			return;
		}

		if (this.shouldAttemptCritical(target, role)) {
			this.startCriticalJump(target, role);
			return;
		}

		if (this.pathCooldown-- <= 0 || this.soldier.getNavigation().isDone()) {
			double speed = switch (role) {
				case VANGUARD -> 1.08;
				case BRUTE -> 1.02;
				case RANGER -> 1.05;
				case TRAPPER -> 1.10;
			};
			this.moveToPredicted(target, speed, role);
			this.pathCooldown = 3 + this.soldier.getRandom().nextInt(3);
		}
		this.soldier.setSprinting(false);
	}

	private void performMeleeAttack(LivingEntity target, CombatRole role) {
		boolean criticalReach = this.critJump
				&& this.soldier.distanceToSqr(target) <= 4.0;
		if (!this.soldier.isWithinMeleeAttackRange(target) && !criticalReach) {
			this.soldier.setAttackTelegraphed(false);
			this.attackCooldown = 8;
			return;
		}

		boolean wasBlocking = target.isBlocking();
		ItemStack blockingItem = target.getItemBlockingWith();
		this.soldier.setAttackTelegraphed(false);
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
			this.shieldCooldown = Math.max(18, 50 - this.soldier.getGearLevel().id() * 4);
		}
	}

	private boolean shouldRaiseShield(LivingEntity target) {
		boolean incomingProjectile = this.soldier.hasIncomingProjectile(10.0);
		boolean meleeAttackImminent = this.isMeleeAttackImminent(target);
		if (this.soldier.isUsingItem()
				|| !this.soldier.getOffhandItem().is(Items.SHIELD)
				|| this.attackWindup > 0) {
			return false;
		}
		if (this.shieldCooldown > 0
				&& !incomingProjectile
				&& !(meleeAttackImminent && this.shieldCooldown <= 8)) {
			return false;
		}
		return incomingProjectile
				|| meleeAttackImminent
				|| this.isRangedReleaseImminent(target);
	}

	private void beginShield() {
		this.shieldTicks = this.soldier.getGearLevel().shieldWindowTicks() + 5;
		this.soldier.recordReactiveShieldUse();
		this.soldier.startUsingItem(InteractionHand.OFF_HAND);
	}

	private void tickShield(LivingEntity target) {
		this.soldier.getLookControl().setLookAt(target, 50.0F, 50.0F);
		double distance = this.soldier.distanceToSqr(target);
		if (distance > 10.0) {
			this.moveToPredicted(target, 0.72, CombatRole.VANGUARD);
		} else {
			this.soldier.getNavigation().stop();
		}
		if (!this.soldier.isUsingItem() || !this.soldier.getOffhandItem().is(Items.SHIELD)) {
			this.lowerShield();
			return;
		}
		if (--this.shieldTicks <= 0) {
			this.lowerShield();
			this.shieldCooldown = Math.max(18, 50 - this.soldier.getGearLevel().id() * 4);
		}
	}

	private void lowerShield() {
		if (this.soldier.isUsingItem() && this.soldier.getUseItem().is(Items.SHIELD)) {
			this.soldier.stopUsingItem();
		}
		this.shieldTicks = 0;
	}

	private boolean isMeleeAttackImminent(LivingEntity target) {
		if (!this.soldier.getSensing().hasLineOfSight(target)
				|| this.soldier.distanceToSqr(target) > 25.0) {
			return false;
		}
		if (target instanceof BattleSoldierEntity battleSoldier) {
			return battleSoldier.isAttackTelegraphed();
		}
		if (!this.isFacingSoldier(target, 0.30)) {
			return false;
		}
		if (target.isUsingItem()) {
			ItemStack used = target.getUseItem();
			if (used.has(DataComponents.KINETIC_WEAPON)) {
				return target.getTicksUsingItem() >= 3 && this.isFacingSoldier(target, 0.22);
			}
			if (used.is(Items.BOW)
					|| used.is(Items.CROSSBOW)
					|| used.is(Items.TRIDENT)
					|| used.is(Items.SHIELD)) {
				return false;
			}
		}
		if (target instanceof Player player) {
			return player.getAttackStrengthScale(0.0F) >= 0.72F;
		}
		if (target instanceof Mob mob) {
			return mob.isAggressive() && (mob.isWithinMeleeAttackRange(this.soldier) || target.swinging);
		}
		return target.swinging;
	}

	private boolean isRangedReleaseImminent(LivingEntity target) {
		if (!this.soldier.getSensing().hasLineOfSight(target) || !this.isFacingSoldier(target, 0.24)) {
			return false;
		}

		if (target.isUsingItem()) {
			ItemStack used = target.getUseItem();
			int chargeTicks = target.getTicksUsingItem();
			if (used.is(Items.BOW) && chargeTicks >= BowItem.MAX_DRAW_DURATION - 8) {
				return true;
			}
			if (used.is(Items.CROSSBOW)
					&& chargeTicks >= CrossbowItem.getChargeDuration(used, target) - 4) {
				return true;
			}
			if (used.is(Items.TRIDENT) && chargeTicks >= TridentItem.THROW_THRESHOLD_TIME - 3) {
				return true;
			}
		}
		return CrossbowItem.isCharged(target.getMainHandItem())
				|| CrossbowItem.isCharged(target.getOffhandItem());
	}

	private boolean isFacingSoldier(LivingEntity target, double minimumDot) {
		Vec3 toSoldier = this.soldier.getEyePosition().subtract(target.getEyePosition());
		if (toSoldier.lengthSqr() < 0.001) {
			return true;
		}
		return target.getViewVector(1.0F).normalize().dot(toSoldier.normalize()) >= minimumDot;
	}

	private void tickCriticalJump(LivingEntity target) {
		this.soldier.getLookControl().setLookAt(target, 45.0F, 45.0F);
		this.steerTowardPrediction(target, 0.90);
		if (!this.soldier.onGround()) {
			this.critAirborne = true;
		}
		boolean descending = this.critAirborne
				&& (this.soldier.fallDistance > 0.05F || this.soldier.getDeltaMovement().y < 0.0);
		if (descending && this.soldier.distanceToSqr(target) <= 4.0) {
			double multiplier = this.criticalRole == CombatRole.BRUTE ? 1.5 : 1.25;
			this.soldier.markCriticalAttack(multiplier);
			this.performMeleeAttack(target, this.criticalRole);
			this.critJump = false;
			this.soldier.setAttackTelegraphed(false);
			this.critCooldown = this.criticalRole == CombatRole.BRUTE ? 30 : 55;
			return;
		}
		if (--this.critTimeout <= 0 || (this.critAirborne && this.soldier.onGround())) {
			this.critJump = false;
			this.soldier.setAttackTelegraphed(false);
			this.attackCooldown = 6;
			this.critCooldown = 24;
		}
	}

	private void retreatFrom(LivingEntity target, double distance) {
		Vec3 predictedTarget = target.position().add(target.getDeltaMovement().scale(4.0));
		this.moveAwayFromPoint(predictedTarget, distance, 1.08);
	}

	private void moveAwayFromPoint(Vec3 threat, double distance, double speed) {
		Vec3 difference = this.soldier.position().subtract(threat);
		Vec3 away = new Vec3(difference.x, 0.0, difference.z);
		if (away.horizontalDistanceSqr() < 0.01) {
			Vec3 look = this.soldier.getLookAngle();
			away = new Vec3(-look.z, 0.0, look.x);
			if (away.horizontalDistanceSqr() < 0.01) {
				away = new Vec3(1.0, 0.0, 0.0);
			}
		}
		Vec3 destination = this.soldier.position().add(away.normalize().scale(distance));
		this.soldier.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
	}

	private boolean shouldAttemptCritical(LivingEntity target, CombatRole role) {
		if (this.critCooldown > 0
				|| this.attackCooldown > 0
				|| this.attackWindup > 0
				|| !this.soldier.onGround()
				|| this.soldier.isInWater()
				|| this.soldier.isUsingItem()) {
			return false;
		}
		double distance = this.soldier.distanceToSqr(target);
		if (distance <= 1.0 || distance > 11.0) {
			return false;
		}
		int attemptRate = switch (role) {
			case BRUTE -> 1;
			case TRAPPER -> 7;
			case RANGER -> 10;
			case VANGUARD -> 16;
		};
		return this.soldier.getRandom().nextInt(attemptRate) == 0;
	}

	private void startCriticalJump(LivingEntity target, CombatRole role) {
		this.critJump = true;
		this.critAirborne = false;
		this.critTimeout = 14;
		this.criticalRole = role;
		this.attackWindup = 0;
		this.soldier.setAttackTelegraphed(true);
		this.soldier.getNavigation().stop();
		this.soldier.setSprinting(false);

		Vec3 predictedTarget = target.position().add(target.getDeltaMovement().scale(3.0));
		Vec3 difference = predictedTarget.subtract(this.soldier.position());
		Vec3 horizontal = new Vec3(difference.x, 0.0, difference.z);
		if (horizontal.lengthSqr() > 0.01) {
			double lunge = role == CombatRole.BRUTE ? 0.50 : 0.35;
			Vec3 current = this.soldier.getDeltaMovement();
			Vec3 impulse = horizontal.normalize().scale(lunge);
			this.soldier.setDeltaMovement(current.x + impulse.x, current.y, current.z + impulse.z);
		}
		this.soldier.getJumpControl().jump();
	}

	private void moveToPredicted(LivingEntity target, double speed, CombatRole role) {
		Vec3 predicted = this.predictTargetPosition(target, speed, role);
		this.soldier.getNavigation().moveTo(predicted.x, predicted.y, predicted.z, speed);
	}

	private void steerTowardPrediction(LivingEntity target, double speed) {
		Vec3 predicted = this.predictTargetPosition(target, speed, this.soldier.getCombatRole());
		this.soldier.getMoveControl().setWantedPosition(predicted.x, predicted.y, predicted.z, speed);
	}

	private Vec3 predictTargetPosition(LivingEntity target, double speed, CombatRole role) {
		Vec3 targetPosition = target.position();
		Vec3 velocity = target.getDeltaMovement();
		double horizontalDistance = this.soldier.position().subtract(targetPosition).horizontalDistance();
		double movementSpeed = Math.max(
				0.08,
				this.soldier.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
						* speed
		);
		double leadTicks = Mth.clamp(horizontalDistance / movementSpeed, 2.0, 8.0);
		double overshootCap = switch (role) {
			case VANGUARD -> 1.45;
			case BRUTE -> 1.90;
			case RANGER -> 1.60;
			case TRAPPER -> 1.70;
		};
		Vec3 offset = new Vec3(velocity.x, 0.0, velocity.z).scale(leadTicks);
		double offsetLength = offset.horizontalDistance();
		if (offsetLength > overshootCap) {
			offset = offset.scale(overshootCap / offsetLength);
		}
		return targetPosition.add(offset);
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
