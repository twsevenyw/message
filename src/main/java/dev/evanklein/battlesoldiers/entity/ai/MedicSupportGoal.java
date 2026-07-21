package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;

import java.util.EnumSet;

public final class MedicSupportGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private LivingEntity patient;
	private int cooldown;
	private int timeout;
	private boolean complete;

	public MedicSupportGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		if (this.soldier.getCombatRole() != CombatRole.MEDIC) {
			return false;
		}
		this.patient = this.soldier.findWoundedAlly(18.0, 0.72);
		return this.patient != null;
	}

	@Override
	public boolean canContinueToUse() {
		return !this.complete
				&& this.timeout < 100
				&& this.patient != null
				&& this.patient.isAlive()
				&& this.patient.getHealth() < this.patient.getMaxHealth() * 0.90F;
	}

	@Override
	public void start() {
		this.complete = false;
		this.timeout = 0;
	}

	@Override
	public void tick() {
		this.timeout++;
		this.soldier.getLookControl().setLookAt(this.patient, 40.0F, 40.0F);
		if (this.soldier.distanceToSqr(this.patient) > 9.0) {
			this.soldier.getNavigation().moveTo(this.patient, 1.05);
			return;
		}
		this.soldier.getNavigation().stop();
		this.complete = this.soldier.applyInventoryPotion(
				Potions.STRONG_HEALING,
				Items.POTION,
				this.patient
		) || this.soldier.applyInventoryPotion(Potions.HEALING, Items.POTION, this.patient)
				|| this.soldier.applyInventoryPotion(Potions.REGENERATION, Items.POTION, this.patient);
	}

	@Override
	public void stop() {
		this.cooldown = this.complete ? 180 : 60;
		this.patient = null;
		this.complete = false;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
