package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.SquadCoordinator;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class SoldierCombatGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private int attackCooldown;
	private int attackWindup;
	private int pathCooldown;
	private int shieldTicks;
	private int shieldCooldown;
	private int bowCooldown;
	private int rangerOutOfRangeTicks;
	private int strafeTicks;
	private boolean strafeClockwise;
	private boolean critJump;
	private boolean critAirborne;
	private int critTimeout;
	private int critCooldown;
	private int comboCount;
	private int comboWindow;
	private int feintCooldown;
	private int feintTicks;
	private int sprintBurstTicks;
	private int hopCooldown;
	private int strafeFlipAt = 18;
	private int disengageTicks;
	private int patienceTicks;
	private int patienceCooldown;
	private int sidestepCooldown;
	private int webBreakTicks;
	private boolean holdingFlank;
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
		SquadCoordinator.releaseMelee(this.soldier);
		this.attackWindup = 0;
		this.critJump = false;
		this.disengageTicks = 0;
		this.patienceTicks = 0;
		this.holdingFlank = false;
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
		if (this.comboWindow > 0) {
			this.comboWindow--;
		} else {
			this.comboCount = 0;
		}
		if (this.feintCooldown > 0) {
			this.feintCooldown--;
		}
		if (this.sprintBurstTicks > 0) {
			this.sprintBurstTicks--;
		}
		if (this.hopCooldown > 0) {
			this.hopCooldown--;
		}
		if (this.patienceCooldown > 0) {
			this.patienceCooldown--;
		}
		if (this.sidestepCooldown > 0) {
			this.sidestepCooldown--;
		}
		this.tickMovementQuality(target);

		double effectiveHealth = this.soldier.getHealth() + this.soldier.getAbsorptionAmount();
		double predictedDamage = this.soldier.estimatedIncomingDamage(target);
		boolean hasTotem = this.soldier.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
				|| this.soldier.hasInventoryItem(Items.TOTEM_OF_UNDYING);
		if (predictedDamage >= effectiveHealth * 0.90
				&& !hasTotem
				&& this.soldier.getCombatRole() != CombatRole.VANGUARD) {
			this.moveAwayFromPoint(target.position(), 6.0, 1.18);
			return;
		}

		if (this.tickOverheadWeaponEvasion(target)) {
			return;
		}
		if (this.tickCrystalResponse()) {
			return;
		}
		if (this.tickWebEscape(target)) {
			return;
		}
		if (this.tickWaterCombat(target)) {
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
			case LANCER -> this.tickLancer(target);
			case DUELIST -> {
				this.soldier.equipSword();
				this.tickMelee(target, CombatRole.DUELIST);
			}
			case MEDIC, ENGINEER, ALCHEMIST, ENDER_SKIRMISHER, DEMOLITIONIST -> {
				this.soldier.equipSword();
				this.tickMelee(target, this.soldier.getCombatRole());
			}
		}
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	/**
	 * Webbed soldiers behave like webbed players: keep swinging at anything in
	 * reach while ripping the web out in under a second, instead of standing
	 * paralyzed while their footwork AI strafes uselessly in place.
	 */
	private boolean tickWebEscape(LivingEntity target) {
		BlockPos webPos = null;
		BlockPos feet = this.soldier.blockPosition();
		if (this.soldier.level().getBlockState(feet).is(Blocks.COBWEB)) {
			webPos = feet;
		} else {
			BlockPos head = BlockPos.containing(this.soldier.getEyePosition());
			if (this.soldier.level().getBlockState(head).is(Blocks.COBWEB)) {
				webPos = head;
			}
		}
		ServerLevel level = getServerLevel(this.soldier);
		if (webPos == null) {
			if (this.webBreakTicks > 0) {
				level.destroyBlockProgress(this.soldier.getId(), this.soldier.blockPosition(), -1);
				this.webBreakTicks = 0;
			}
			return false;
		}

		this.critJump = false;
		this.disengageTicks = 0;
		this.patienceTicks = 0;
		this.soldier.getNavigation().stop();
		// Fight through the web: anything in reach still gets hit on cadence.
		if (this.attackCooldown <= 0 && this.soldier.isWithinMeleeAttackRange(target)) {
			this.performMeleeAttack(target, this.soldier.getCombatRole());
			this.disengageTicks = 0;
		}
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return true;
		}
		this.webBreakTicks++;
		int required = Math.max(4, 11 - this.soldier.getGearLevel().id());
		if (this.webBreakTicks % 3 == 0) {
			this.soldier.swing(InteractionHand.MAIN_HAND);
		}
		level.destroyBlockProgress(
				this.soldier.getId(),
				webPos,
				Math.min(9, this.webBreakTicks * 10 / required)
		);
		if (this.webBreakTicks >= required) {
			level.destroyBlock(webPos, false, this.soldier);
			level.destroyBlockProgress(this.soldier.getId(), webPos, -1);
			this.webBreakTicks = 0;
		}
		return true;
	}

	/**
	 * Water combat: footwork (disengage arcs, poke-waiting, sidesteps) is
	 * worthless while swimming, so drop it entirely — hop-swim straight at the
	 * target, keep swinging in reach, and let step-assist pop out of shallow
	 * player-placed pools.
	 */
	private boolean tickWaterCombat(LivingEntity target) {
		if (!this.soldier.isInWater()) {
			return false;
		}
		this.critJump = false;
		this.disengageTicks = 0;
		this.patienceTicks = 0;
		if (this.attackCooldown <= 0 && this.soldier.isWithinMeleeAttackRange(target)) {
			this.performMeleeAttack(target, this.soldier.getCombatRole());
			this.disengageTicks = 0;
		}
		Vec3 predicted = this.predictTargetPosition(target, 1.25, this.soldier.getCombatRole());
		this.soldier.getMoveControl().setWantedPosition(predicted.x, predicted.y, predicted.z, 1.25);
		// Walk/wade straight through shallow water (step assist handles exits);
		// jump only to climb banks or reach a higher target — constant hopping
		// just bounces in place.
		if (this.hopCooldown <= 0
				&& (this.soldier.horizontalCollision || target.getY() - this.soldier.getY() > 1.0)) {
			this.soldier.getJumpControl().jump();
			this.hopCooldown = 8;
		}
		return true;
	}

	/**
	 * Player-like movement layer: real sprinting whenever closing distance,
	 * sprint-jump hops on open ground, and an immediate momentum surge back
	 * toward the fight after taking knockback instead of flailing to a stop.
	 */
	private void tickMovementQuality(LivingEntity target) {
		boolean wantSprint = this.shouldSprint(target) || this.sprintBurstTicks > 0;
		if (wantSprint != this.soldier.isSprinting()) {
			this.soldier.setSprinting(wantSprint);
		}

		// Knockback recovery: hurtTime counts down from 10, so this fires once per hit.
		if (this.soldier.hurtTime == 8 && !this.critJump) {
			Vec3 toTarget = target.position().subtract(this.soldier.position());
			Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
			if (horizontal.lengthSqr() > 4.0) {
				Vec3 impulse = horizontal.normalize().scale(0.22);
				Vec3 current = this.soldier.getDeltaMovement();
				this.soldier.setDeltaMovement(current.x + impulse.x, current.y, current.z + impulse.z);
			}
			this.strafeClockwise = this.soldier.getRandom().nextBoolean();
			this.strafeTicks = 0;
			this.sprintBurstTicks = Math.max(this.sprintBurstTicks, 10);
		}

		// Sprint-jump bursts while chasing across open ground.
		double distance = this.soldier.distanceToSqr(target);
		if (distance > 25.0
				&& this.hopCooldown <= 0
				&& this.soldier.isSprinting()
				&& this.soldier.onGround()
				&& !this.soldier.isInWater()
				&& this.soldier.getSensing().hasLineOfSight(target)
				&& this.soldier.getDeltaMovement().horizontalDistanceSqr() > 0.02) {
			this.soldier.getJumpControl().jump();
			this.hopCooldown = 12 + this.soldier.getRandom().nextInt(10);
		}
	}

	private boolean shouldSprint(LivingEntity target) {
		if (this.soldier.isUsingItem()
				|| this.critJump
				|| this.attackWindup > 0
				|| this.feintTicks > 0
				|| this.shieldTicks > 0
				|| this.soldier.isInWater()
				|| this.soldier.isHoldingRangerPerch()) {
			return false;
		}
		// Flankers waiting for an attack slot pace the perimeter calmly instead
		// of sprint-orbiting like a mob ring.
		if (this.holdingFlank && this.soldier.distanceToSqr(target) < 64.0) {
			return false;
		}
		return this.soldier.distanceToSqr(target) > 9.0;
	}

	/** Strafe rhythm with randomized flip intervals so circling is not metronome-predictable. */
	private void advanceStrafe(int baseInterval) {
		this.advanceStrafe(baseInterval, null);
	}

	/**
	 * With a target, flips bias 70% toward the side that circles behind the
	 * target's view direction — soldiers work toward your back like players.
	 */
	private void advanceStrafe(int baseInterval, @Nullable LivingEntity target) {
		if (++this.strafeTicks >= this.strafeFlipAt) {
			this.strafeTicks = 0;
			this.strafeFlipAt = Math.max(6, baseInterval / 2)
					+ this.soldier.getRandom().nextInt(Math.max(4, baseInterval));
			if (target != null && this.soldier.getRandom().nextFloat() < 0.7F) {
				Vec3 view = target.getLookAngle();
				Vec3 toSoldier = this.soldier.position().subtract(target.position());
				double cross = view.x * toSoldier.z - view.z * toSoldier.x;
				this.strafeClockwise = cross < 0.0;
			} else {
				this.strafeClockwise = !this.strafeClockwise;
			}
		}
	}

	/** True while the soldier should keep respecting a ready, aimed opponent. */
	private boolean shouldWaitForOpening(LivingEntity target, boolean soloEngagement) {
		if (!(target instanceof Player player) || this.patienceCooldown > 0) {
			return false;
		}
		boolean aimedAndReady = this.isFacingSoldier(target, 0.55)
				&& player.getAttackStrengthScale(0.0F) >= 0.65F
				&& !player.isUsingItem();
		if (!aimedAndReady) {
			this.patienceTicks = 0;
			return false;
		}
		if (this.patienceTicks <= 0) {
			int base = soloEngagement ? 4 : 11;
			this.patienceTicks = Math.max(
					3,
					base + this.soldier.getRandom().nextInt(soloEngagement ? 8 : 12)
							- this.soldier.getGearLevel().id()
			);
		}
		if (--this.patienceTicks <= 0) {
			// Patience spent: commit anyway and stay committed for a while.
			this.patienceCooldown = 30 + this.soldier.getRandom().nextInt(20);
			return false;
		}
		return true;
	}

	/** Poised at the edge of trade range: circling, adjusting, facing the target. */
	private void holdPokeRange(LivingEntity target) {
		this.soldier.getNavigation().stop();
		double distance = this.soldier.distanceToSqr(target);
		float forward = distance < 5.0 ? -0.30F : distance > 10.5 ? 0.30F : 0.05F;
		this.soldier.combatStrafe(forward, this.strafeClockwise ? 0.55F : -0.55F, 1.05);
		this.advanceStrafe(10, target);
	}

	/** Recovery movement: arc out to reset spacing while staying locked on. */
	private void tickDisengage(LivingEntity target) {
		this.soldier.getNavigation().stop();
		double distance = this.soldier.distanceToSqr(target);
		float back = distance < 20.25 ? -0.85F : 0.05F;
		this.soldier.combatStrafe(back, this.strafeClockwise ? 0.42F : -0.42F, 1.2);
		this.advanceStrafe(12, target);
		if (this.soldier.horizontalCollision) {
			this.strafeClockwise = !this.strafeClockwise;
		}
	}

	private boolean tickOverheadWeaponEvasion(LivingEntity target) {
		ItemStack weapon = target.isUsingItem() ? target.getUseItem() : target.getMainHandItem();
		boolean mace = weapon.is(Items.MACE) || weapon.getItem() instanceof MaceItem;
		boolean kinetic = weapon.has(DataComponents.KINETIC_WEAPON);
		boolean genericWeapon = weapon.has(DataComponents.WEAPON) || kinetic;
		boolean severeFall = target.fallDistance > 5.0F;
		double verticalDistance = target.getY() - this.soldier.getY();
		double deltaX = target.getX() - this.soldier.getX();
		double deltaZ = target.getZ() - this.soldier.getZ();
		double horizontalDistance = deltaX * deltaX + deltaZ * deltaZ;
		boolean descending = target.getDeltaMovement().y < -0.05 || target.fallDistance > 0.5F;
		boolean dangerous = verticalDistance >= 2.5
				&& horizontalDistance <= 20.25
				&& (severeFall || genericWeapon && (descending || mace || kinetic));
		if (!dangerous) {
			return false;
		}

		if (this.soldier.isUsingItem()) {
			this.soldier.stopUsingItem();
		}
		double predictionTicks = Mth.clamp(verticalDistance / 0.7, 2.0, 10.0);
		Vec3 impact = target.position().add(target.getDeltaMovement().scale(predictionTicks));
		this.moveAwayFromPoint(impact, 5.0, 1.18);
		this.sprintBurstTicks = Math.max(this.sprintBurstTicks, 6);
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
			if (target.isBlocking()) {
				this.soldier.equipAxe();
			} else {
				this.soldier.equipSword();
			}
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
		this.soldier.equipSword();
		this.tickMelee(target, CombatRole.BRUTE);
	}

	private void tickTrapper(LivingEntity target) {
		this.soldier.equipSword();
		double distance = this.soldier.distanceToSqr(target);
		if (distance >= 7.0 && distance <= 25.0) {
			this.soldier.getNavigation().stop();
			this.soldier.combatStrafe(0.15F, this.strafeClockwise ? 0.45F : -0.45F, 1.0);
			this.advanceStrafe(24, target);
		}
		this.tickMelee(target, CombatRole.TRAPPER);
	}

	private void tickLancer(LivingEntity target) {
		this.soldier.equipSpear();
		ItemStack spear = this.soldier.getMainHandItem();
		KineticWeapon kinetic = spear.get(DataComponents.KINETIC_WEAPON);
		if (kinetic == null) {
			this.tickMelee(target, CombatRole.LANCER);
			return;
		}
		double distance = this.soldier.distanceToSqr(target);
		if (distance > 100.0) {
			this.moveToPredicted(target, 1.08, CombatRole.LANCER);
			return;
		}
		if (!this.soldier.isUsingItem()) {
			this.soldier.startUsingItem(InteractionHand.MAIN_HAND);
		}
		this.soldier.getLookControl().setLookAt(target, 40.0F, 40.0F);
		this.moveToPredicted(target, 1.12, CombatRole.LANCER);
		if (this.soldier.getTicksUsingItem() >= Math.max(12, kinetic.computeDamageUseDuration())) {
			this.soldier.stopUsingItem();
			this.attackCooldown = 22;
		}
	}

	private void tickRanger(LivingEntity target) {
		double deltaX = target.getX() - this.soldier.getX();
		double deltaZ = target.getZ() - this.soldier.getZ();
		double distance = deltaX * deltaX + deltaZ * deltaZ;
		double bowRange = Math.min(30.0, 14.0 + this.soldier.getGearLevel().id() * 2.0);
		double preferredMax = bowRange * bowRange;

		if (distance > preferredMax) {
			this.rangerOutOfRangeTicks++;
			if (this.rangerOutOfRangeTicks >= 40
					&& this.soldier.getRangerBacklineTeleportCooldown() <= 0) {
				this.soldier.teleportRangerToBackline(target);
				this.rangerOutOfRangeTicks = 0;
				return;
			}
		} else {
			this.rangerOutOfRangeTicks = 0;
		}

		if (!this.soldier.hasArrows()) {
			this.soldier.equipBackupMelee();
			this.tickMelee(target, CombatRole.RANGER);
			return;
		}

		boolean canSee = this.soldier.getSensing().hasLineOfSight(target);
		boolean antiAirMace = (target.getMainHandItem().is(Items.MACE) || target.fallDistance > 5.0F)
				&& target.getY() - this.soldier.getY() >= 2.0;
		if (antiAirMace) {
			this.soldier.equipBow();
			this.soldier.getNavigation().stop();
			this.soldier.getMoveControl().setWait();
			if (canSee) {
				this.tickBowDrawAndFire(target, true);
			} else {
				this.moveToPredicted(target, 1.05, CombatRole.RANGER);
			}
			return;
		}
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
			this.soldier.combatStrafe(
					distance < preferredMin * 1.35 ? -0.25F : 0.12F,
					this.strafeClockwise ? 0.38F : -0.38F,
					1.0
			);
			this.advanceStrafe(30, target);
		}
		this.tickBowDrawAndFire(target);
	}

	private void tickBowDrawAndFire(Entity target) {
		this.tickBowDrawAndFire(target, false);
	}

	private void tickBowDrawAndFire(Entity target, boolean antiAir) {
		if (this.soldier.isUsingItem()) {
			int drawTicks = this.soldier.getTicksUsingItem();
			int requiredDraw = antiAir ? 10 : BowItem.MAX_DRAW_DURATION;
			if (drawTicks >= requiredDraw) {
				this.soldier.stopUsingItem();
				this.soldier.shootArrowAt(target, BowItem.getPowerForTime(drawTicks));
				int interval = this.soldier.getGearLevel().bowAttackInterval();
				if (target instanceof LivingEntity living) {
					interval -= SquadCoordinator.combo(this.soldier, living).chainStage() * 4;
				}
				this.bowCooldown = antiAir ? 10 : Math.max(16, interval);
			}
		} else if (this.bowCooldown <= 0) {
			this.soldier.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.soldier, Items.BOW));
		}
	}

	private void tickMelee(LivingEntity target, CombatRole role) {
		double targetDistance = this.soldier.distanceToSqr(target);
		boolean soloEngagement = SquadCoordinator.isSoloEngagement(this.soldier, target);

		// Hit-and-run rhythm: after a swing lands (or a combo ends), spend the
		// recovery arcing OUT of trade range instead of hugging the target.
		if (this.disengageTicks > 0) {
			boolean chasedDown = this.attackCooldown <= 0 && targetDistance <= 6.25;
			// If the target retreats instead, abort the arc and hunt — never
			// hand a fleeing player free distance.
			boolean targetFleeing = targetDistance > 30.25;
			if (chasedDown || targetFleeing) {
				this.disengageTicks = 0;
			} else {
				this.disengageTicks--;
				this.tickDisengage(target);
				return;
			}
		}

		if (!soloEngagement && targetDistance <= 36.0) {
			SquadCoordinator.MeleeDirective directive =
					SquadCoordinator.meleeDirective(this.soldier, target);
			if (!directive.mayWindup()) {
				this.holdingFlank = true;
				this.soldier.setAttackTelegraphed(false);
				this.soldier.getNavigation().moveTo(
						directive.position().x,
						directive.position().y,
						directive.position().z,
						1.05
				);
				return;
			}
			this.holdingFlank = false;
		} else {
			this.holdingFlank = false;
		}

		// Read-and-dodge: sidestep an incoming swing when no shield is available.
		if (this.sidestepCooldown <= 0
				&& this.attackWindup <= 0
				&& !this.critJump
				&& this.soldier.onGround()
				&& !this.soldier.getOffhandItem().is(Items.SHIELD)
				&& targetDistance <= 12.25
				&& this.isMeleeAttackImminent(target)) {
			Vec3 toTarget = target.position().subtract(this.soldier.position());
			Vec3 side = new Vec3(-toTarget.z, 0.0, toTarget.x);
			if (side.lengthSqr() > 0.01) {
				side = side.normalize().scale(this.strafeClockwise ? 0.30 : -0.30);
				this.soldier.addDeltaMovement(new Vec3(side.x, 0.0, side.z));
			}
			this.sidestepCooldown = Math.max(16, 46 - this.soldier.getGearLevel().id() * 4);
		}

		if (this.feintTicks > 0) {
			this.soldier.getNavigation().stop();
			this.soldier.combatStrafe(-0.12F, this.strafeClockwise ? 0.55F : -0.55F, 1.1);
			if (--this.feintTicks == 0) {
				this.attackWindup = Math.max(3, this.attackWindupTicks(role) / 2);
				this.soldier.setAttackTelegraphed(true);
			}
			return;
		}
		if (this.attackWindup > 0) {
			boolean canFeint = !soloEngagement
					&& (role == CombatRole.VANGUARD || role == CombatRole.DUELIST);
			SquadCoordinator.HabitSnapshot habits = SquadCoordinator.habits(this.soldier, target);
			if (canFeint
					&& this.feintCooldown <= 0
					&& this.attackWindup > 3
					&& (target.isBlocking() || habits.shielding() >= 4)
					&& this.soldier.getRandom().nextInt(Math.max(4, 12 - this.soldier.getGearLevel().id())) == 0) {
				this.attackWindup = 0;
				this.feintTicks = 4;
				this.feintCooldown = 60;
				this.soldier.setAttackTelegraphed(false);
				return;
			}
			if (this.soldier.isWithinMeleeAttackRange(target)) {
				this.soldier.getNavigation().stop();
				this.soldier.combatStrafe(0.08F, this.strafeClockwise ? 0.28F : -0.28F, 0.9);
			} else {
				this.steerTowardPrediction(target, 0.82);
			}
			if (--this.attackWindup == 0) {
				this.performMeleeAttack(target, role);
			}
			return;
		}

		if (this.shouldAttemptCritical(target, role)) {
			this.startCriticalJump(target, role);
			return;
		}

		// Commit to the attack slightly outside strict reach: the windup steers
		// inward, so swings start on approach the way players click into range.
		boolean inCommitRange = this.soldier.isWithinMeleeAttackRange(target) || targetDistance <= 6.25;
		if (inCommitRange) {
			this.soldier.getNavigation().stop();
			if (this.attackCooldown <= 0) {
				// Respect a ready, aimed opponent: hold poke range and wait for
				// their swing, their back, or an item use before committing.
				if (this.shouldWaitForOpening(target, soloEngagement)) {
					this.holdPokeRange(target);
					return;
				}
				this.patienceTicks = 0;
				if (target.isBlocking() && role == CombatRole.VANGUARD) {
					this.soldier.equipAxe();
				}
				this.attackWindup = soloEngagement
						? Math.min(6, this.attackWindupTicks(role))
						: this.attackWindupTicks(role);
				this.soldier.setAttackTelegraphed(true);
			} else if (this.soldier.isWithinMeleeAttackRange(target)) {
				// Player-like spacing during recovery — but never step off an
				// immobilized (webbed) target; that is free damage.
				boolean targetStuck = target.getInBlockState().is(Blocks.COBWEB);
				float forward = !targetStuck && this.attackCooldown > 8 ? -0.12F : 0.30F;
				this.soldier.combatStrafe(forward, this.strafeClockwise ? 0.48F : -0.48F, 1.05);
				this.advanceStrafe(14, target);
			} else {
				// At the commit-range edge on cooldown: keep closing pressure.
				this.steerTowardPrediction(target, 0.92);
			}
			return;
		}

		if (this.soldier.horizontalCollision && targetDistance <= 16.0) {
			this.soldier.getJumpControl().jump();
			this.soldier.combatStrafe(0.32F, this.strafeClockwise ? 0.62F : -0.62F, 1.1);
			this.strafeClockwise = !this.strafeClockwise;
		}

		double speed = switch (role) {
			case VANGUARD -> 1.08;
			case BRUTE -> 1.02;
			case RANGER -> 1.05;
			case TRAPPER -> 1.10;
			case LANCER -> 1.08;
			case DUELIST -> 1.16;
			case ENDER_SKIRMISHER -> 1.14;
			case MEDIC, ALCHEMIST -> 1.0;
			case ENGINEER, DEMOLITIONIST -> 0.95;
		};
		if (soloEngagement) {
			speed = Math.min(1.24, speed + 0.12);
		}
		// Close range with clear sight on similar ground: steer directly every
		// tick (no A* node stutter) and weave laterally so the approach is a
		// strafing arc rather than a straight mob line.
		boolean directSteer = targetDistance <= 110.0
				&& Math.abs(target.getY() - this.soldier.getY()) <= 1.5
				&& this.soldier.getSensing().hasLineOfSight(target);
		if (directSteer) {
			this.soldier.getNavigation().stop();
			double weave = targetDistance > 12.0 ? 1.1 : 0.0;
			this.steerWeaving(target, speed, weave, role);
			this.advanceStrafe(16);
			this.pathCooldown = 0;
			return;
		}
		if (this.pathCooldown-- <= 0 || this.soldier.getNavigation().isDone()) {
			this.moveToPredicted(target, speed, role);
			int decisionPeriod = SquadCoordinator.skill(this.soldier.getGearLevel()).decisionPeriodTicks();
			this.pathCooldown = Math.max(2, decisionPeriod / 2)
					+ this.soldier.getRandom().nextInt(2);
		}
	}

	/** Direct per-tick steering toward the predicted position with a lateral weave offset. */
	private void steerWeaving(LivingEntity target, double speed, double weave, CombatRole role) {
		Vec3 predicted = this.predictTargetPosition(target, speed, role);
		if (weave > 0.0) {
			Vec3 toPredicted = predicted.subtract(this.soldier.position());
			Vec3 side = new Vec3(-toPredicted.z, 0.0, toPredicted.x);
			if (side.lengthSqr() > 0.01) {
				predicted = predicted.add(
						side.normalize().scale(this.strafeClockwise ? weave : -weave)
				);
			}
		}
		this.soldier.getMoveControl().setWantedPosition(predicted.x, predicted.y, predicted.z, speed);
	}

	private void performMeleeAttack(LivingEntity target, CombatRole role) {
		boolean criticalReach = this.critJump
				&& this.soldier.distanceToSqr(target) <= 4.0;
		if (!this.soldier.isWithinMeleeAttackRange(target) && !criticalReach) {
			this.soldier.setAttackTelegraphed(false);
			SquadCoordinator.releaseMelee(this.soldier);
			this.attackCooldown = 8;
			return;
		}

		boolean wasBlocking = target.isBlocking();
		ItemStack blockingItem = target.getItemBlockingWith();
		this.soldier.setAttackTelegraphed(false);
		SquadCoordinator.ComboSnapshot squadCombo = SquadCoordinator.combo(this.soldier, target);
		if (squadCombo.chainStage() >= 2) {
			this.soldier.markCriticalAttack(1.15);
		}
		if (this.comboCount >= 2 && !criticalReach) {
			this.soldier.markCriticalAttack(1.20);
		}
		this.soldier.swing(InteractionHand.MAIN_HAND);
		ServerLevel level = getServerLevel(this.soldier);
		boolean hit = this.soldier.doHurtTarget(level, target);
		if (hit) {
			this.comboCount = Math.min(4, this.comboCount + 1);
			this.comboWindow = 40;
			this.sprintBurstTicks = 6;
		} else {
			this.comboCount = 0;
			this.comboWindow = 0;
		}
		if (hit && wasBlocking && blockingItem != null && this.soldier.isHoldingAxe()) {
			BlocksAttacks blocksAttacks = blockingItem.get(DataComponents.BLOCKS_ATTACKS);
			if (blocksAttacks != null) {
				blocksAttacks.disable(level, target, 3.0F, blockingItem);
			}
		}
		int recovery = this.attackRecoveryTicks(role) - this.comboCount * 2;
		boolean solo = SquadCoordinator.isSoloEngagement(this.soldier, target);
		if (solo) {
			recovery = (int) Math.ceil(recovery * 0.65);
		}
		// Human cadence jitter instead of a metronome.
		this.attackCooldown = Math.max(7, recovery) + this.soldier.getRandom().nextInt(3) - 1;
		// Hit-and-run: stay in only to continue a fresh combo; otherwise arc out
		// for the recovery instead of standing in trade range.
		boolean targetNearlyDead =
				target.getHealth() + target.getAbsorptionAmount() <= 7.0F;
		boolean stayForCombo = hit
				&& (targetNearlyDead
						|| this.comboCount < 2
								&& this.soldier.getRandom().nextFloat() < (solo ? 0.70F : 0.40F));
		boolean targetStuck = target.getInBlockState().is(Blocks.COBWEB);
		if (!stayForCombo && !targetStuck) {
			// Slightly longer than the cooldown: a visible reset pass, not a wobble.
			this.disengageTicks = this.attackCooldown + 4 + this.soldier.getRandom().nextInt(6);
		}
		SquadCoordinator.releaseMelee(this.soldier);
		if (role == CombatRole.VANGUARD) {
			this.soldier.equipSword();
			this.shieldCooldown = Math.max(18, 50 - this.soldier.getGearLevel().id() * 4);
		} else if (role == CombatRole.BRUTE) {
			this.soldier.equipSword();
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
			if (this.criticalRole == CombatRole.BRUTE) {
				this.soldier.equipSword();
			}
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
			case DUELIST -> 3;
			case ENDER_SKIRMISHER -> 5;
			case TRAPPER -> 7;
			case LANCER -> 8;
			case RANGER -> 10;
			case VANGUARD -> 16;
			case MEDIC, ENGINEER, ALCHEMIST, DEMOLITIONIST -> 18;
		};
		return this.soldier.getRandom().nextInt(attemptRate) == 0;
	}

	private void startCriticalJump(LivingEntity target, CombatRole role) {
		this.critJump = true;
		this.critAirborne = false;
		this.critTimeout = 14;
		this.criticalRole = role;
		if (role == CombatRole.BRUTE) {
			this.soldier.equipAxe();
		}
		this.attackWindup = 0;
		this.soldier.setAttackTelegraphed(true);
		this.soldier.getNavigation().stop();
		this.soldier.setSprinting(false);

		Vec3 predictedTarget = target.position().add(target.getDeltaMovement().scale(3.0));
		Vec3 difference = predictedTarget.subtract(this.soldier.position());
		Vec3 horizontal = new Vec3(difference.x, 0.0, difference.z);
		if (horizontal.lengthSqr() > 0.01) {
			double lunge = switch (role) {
				case BRUTE -> 0.50;
				case DUELIST, ENDER_SKIRMISHER -> 0.45;
				case LANCER -> 0.40;
				default -> 0.35;
			};
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
		SquadCoordinator.SkillProfile skill = SquadCoordinator.skill(this.soldier.getGearLevel());
		SquadCoordinator.HabitSnapshot habits = SquadCoordinator.habits(this.soldier, target);
		double horizontalDistance = this.soldier.position().subtract(targetPosition).horizontalDistance();
		double movementSpeed = Math.max(
				0.08,
				this.soldier.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
						* speed
		);
		double leadTicks = Mth.clamp(
				horizontalDistance / movementSpeed * skill.leadMultiplier(),
				1.0,
				8.0
		);
		if (habits.strafing() >= 4) {
			leadTicks *= 1.25;
		}
		double overshootCap = switch (role) {
			case VANGUARD -> 1.45;
			case BRUTE -> 1.90;
			case RANGER -> 1.60;
			case TRAPPER -> 1.70;
			case DUELIST, ENDER_SKIRMISHER -> 2.0;
			case LANCER -> 2.1;
			case MEDIC, ALCHEMIST -> 1.5;
			case ENGINEER, DEMOLITIONIST -> 1.6;
		};
		if (habits.mace() >= 3 || habits.elevated() >= 4) {
			overshootCap += 0.35;
		}
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
			case DUELIST -> 10 - Math.min(4, tier / 2);
			case LANCER -> 13 - Math.min(4, tier / 2);
			case ENDER_SKIRMISHER -> 11 - Math.min(3, tier / 2);
			case MEDIC, ENGINEER, ALCHEMIST -> 16 - Math.min(4, tier / 2);
			case DEMOLITIONIST -> 18 - Math.min(4, tier / 2);
		};
	}

	private int attackRecoveryTicks(CombatRole role) {
		int tier = this.soldier.getGearLevel().id();
		return switch (role) {
			case VANGUARD -> 21 - Math.min(4, tier);
			case BRUTE -> 33 - tier;
			case RANGER -> 25 - tier;
			case TRAPPER -> 27 - tier;
			case DUELIST -> 17 - Math.min(4, tier / 2);
			case LANCER -> 24 - Math.min(4, tier / 2);
			case ENDER_SKIRMISHER -> 20 - Math.min(4, tier / 2);
			case MEDIC, ENGINEER, ALCHEMIST -> 27 - Math.min(3, tier / 2);
			case DEMOLITIONIST -> 30 - Math.min(3, tier / 2);
		};
	}
}
