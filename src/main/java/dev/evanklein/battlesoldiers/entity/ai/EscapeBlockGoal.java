package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.SquadCoordinator;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class EscapeBlockGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private LivingEntity victim;
	private BlockPos escapeCell;
	private int cooldown;

	public EscapeBlockGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		CombatRole role = this.soldier.getCombatRole();
		if (role != CombatRole.TRAPPER && role != CombatRole.ENGINEER) {
			return false;
		}
		this.victim = this.soldier.getTarget();
		if (this.victim == null
				|| !this.victim.isAlive()
				|| !this.victim.onGround()
				|| this.soldier.distanceToSqr(this.victim) > 64.0) {
			return false;
		}
		this.escapeCell = SquadCoordinator.predictedEscapeBlock(this.soldier, this.victim);
		if (this.escapeCell == null) {
			return false;
		}
		return role == CombatRole.TRAPPER
				? this.soldier.canPlaceCobweb(this.escapeCell, this.victim)
				: this.soldier.canPlaceTacticalBlock(this.escapeCell);
	}

	@Override
	public void start() {
		this.soldier.getNavigation().stop();
		this.soldier.getLookControl().setLookAt(this.victim, 45.0F, 45.0F);
		boolean placed = this.soldier.getCombatRole() == CombatRole.TRAPPER
				? this.soldier.placeCobwebTrap(this.escapeCell, this.victim)
				: this.soldier.placeTacticalBlock(this.escapeCell);
		if (placed) {
			SquadCoordinator.reportComboEvent(
					this.soldier,
					this.victim,
					SquadCoordinator.ComboEvent.WEBBED
			);
		}
		this.cooldown = placed ? 100 : 35;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}
}
