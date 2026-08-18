package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

public final class ResourceShareGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BattleSoldierEntity recipient;
	private Item item;
	private int amount;
	private int cooldown;
	private int timeout;

	public ResourceShareGoal(BattleSoldierEntity soldier) {
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
		if (target != null && this.soldier.distanceToSqr(target) < 100.0) {
			return false;
		}
		if (this.planTransfer(CombatRole.RANGER, Items.ARROW, 16, 8, 8)) {
			return true;
		}
		if (this.planTransfer(CombatRole.ENGINEER, Items.COBBLESTONE, 12, 8, 4)) {
			return true;
		}
		if (this.planTransfer(CombatRole.MEDIC, Items.GOLDEN_APPLE, 3, 1, 1)) {
			return true;
		}
		return this.planTransfer(CombatRole.ENDER_SKIRMISHER, Items.ENDER_PEARL, 2, 1, 1);
	}

	@Override
	public boolean canContinueToUse() {
		return this.recipient != null && this.recipient.isAlive() && this.timeout < 80;
	}

	@Override
	public void start() {
		this.timeout = 0;
	}

	@Override
	public void tick() {
		this.timeout++;
		this.soldier.getLookControl().setLookAt(this.recipient, 30.0F, 30.0F);
		if (this.soldier.distanceToSqr(this.recipient) > 9.0) {
			this.soldier.getNavigation().moveTo(this.recipient, 1.0);
			return;
		}
		this.soldier.getNavigation().stop();
		this.soldier.transferInventoryItem(this.item, this.recipient, this.amount);
		this.timeout = 80;
	}

	@Override
	public void stop() {
		this.cooldown = 120;
		this.recipient = null;
		this.item = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean planTransfer(
			CombatRole role,
			Item item,
			int donorMinimum,
			int recipientMaximum,
			int amount
	) {
		if (this.soldier.countInventoryItem(item) <= donorMinimum) {
			return false;
		}
		BattleSoldierEntity ally = this.soldier.findNearbyAlly(role, 12.0);
		if (ally == null || ally.countInventoryItem(item) >= recipientMaximum) {
			return false;
		}
		this.recipient = ally;
		this.item = item;
		this.amount = amount;
		return true;
	}
}
