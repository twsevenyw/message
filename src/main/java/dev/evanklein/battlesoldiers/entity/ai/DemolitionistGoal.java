package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DemolitionistGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private LivingEntity victim;
	private int cooldown;
	private boolean planted;

	public DemolitionistGoal(BattleSoldierEntity soldier) {
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
		if (this.soldier.getCombatRole() != CombatRole.DEMOLITIONIST
				|| this.victim == null
				|| !this.victim.isAlive()
				|| !this.soldier.hasInventoryItem(Items.TNT)) {
			return false;
		}
		double distance = this.soldier.distanceToSqr(this.victim);
		if (distance < 16.0 || distance > 100.0) {
			return false;
		}
		ServerLevel level = getServerLevel(this.soldier);
		boolean allyTooClose = !level.getEntitiesOfClass(
				BattleSoldierEntity.class,
				this.soldier.getBoundingBox().inflate(7.0),
				ally -> ally != this.soldier
						&& ally.isAlive()
						&& ally.getSquad() == this.soldier.getSquad()
		).isEmpty();
		return !allyTooClose
				&& (this.soldier.horizontalCollision
						|| this.soldier.getNavigation().isStuck()
						|| !this.soldier.getSensing().hasLineOfSight(this.victim));
	}

	@Override
	public boolean canContinueToUse() {
		return this.planted && this.cooldown > 120;
	}

	@Override
	public void start() {
		this.planted = this.soldier.plantTnt(60);
		this.cooldown = this.planted ? 300 : 80;
	}

	@Override
	public void tick() {
		Vec3 away = this.soldier.position().subtract(this.victim.position());
		if (away.horizontalDistanceSqr() < 0.01) {
			away = new Vec3(1.0, 0.0, 0.0);
		}
		Vec3 destination = this.soldier.position().add(away.normalize().scale(12.0));
		this.soldier.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.15);
	}

	@Override
	public void stop() {
		this.planted = false;
		this.victim = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
