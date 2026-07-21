package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.alchemy.Potions;

import java.util.EnumSet;

public final class AlchemistDebuffGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private LivingEntity victim;
	private int cooldown;
	private int timeout;
	private boolean complete;

	public AlchemistDebuffGoal(BattleSoldierEntity soldier) {
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
		return this.soldier.getCombatRole() == CombatRole.ALCHEMIST
				&& this.victim != null
				&& this.victim.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return !this.complete && this.timeout < 80 && this.victim != null && this.victim.isAlive();
	}

	@Override
	public void start() {
		this.timeout = 0;
		this.complete = false;
	}

	@Override
	public void tick() {
		this.timeout++;
		this.soldier.getLookControl().setLookAt(this.victim, 40.0F, 40.0F);
		double distance = this.soldier.distanceToSqr(this.victim);
		if (distance > 100.0) {
			this.soldier.getNavigation().moveTo(this.victim, 1.0);
			return;
		}
		this.soldier.getNavigation().stop();
		if (!this.victim.hasEffect(MobEffects.SLOWNESS) && distance >= 36.0) {
			this.complete = this.soldier.applyDebuffFromInventory(
					Potions.SLOWNESS,
					this.victim,
					new MobEffectInstance(MobEffects.SLOWNESS, 240, 1)
			);
		} else if (!this.victim.hasEffect(MobEffects.POISON)) {
			this.complete = this.soldier.applyDebuffFromInventory(
					Potions.POISON,
					this.victim,
					new MobEffectInstance(MobEffects.POISON, 180, 0)
			);
		} else if (!this.victim.hasEffect(MobEffects.WEAKNESS)) {
			this.complete = this.soldier.applyDebuffFromInventory(
					Potions.WEAKNESS,
					this.victim,
					new MobEffectInstance(MobEffects.WEAKNESS, 240, 0)
			);
		} else {
			this.complete = true;
		}
	}

	@Override
	public void stop() {
		this.cooldown = this.complete ? 100 : 40;
		this.victim = null;
		this.complete = false;
	}
}
