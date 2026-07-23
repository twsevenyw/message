package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.SquadCoordinator;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class TrapperWebGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private BlockPos trapPosition;
	private LivingEntity victim;

	public TrapperWebGoal(BattleSoldierEntity soldier) {
		this.soldier = soldier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.soldier.getTarget();
		boolean airborneMace = target != null
				&& target.getMainHandItem().is(Items.MACE)
				&& target.getY() - this.soldier.getY() >= 2.5;
		if (this.soldier.getCombatRole() != CombatRole.TRAPPER
				|| this.soldier.getGearLevel().id() < 4
				|| target == null
				|| !target.isAlive()
				|| !target.onGround() && !airborneMace
				|| this.soldier.getWebTrapCooldown() > 0
				|| !this.soldier.hasCobwebs()
				|| this.soldier.distanceToSqr(target) > 49.0) {
			return false;
		}

		this.victim = target;
		this.trapPosition = this.predictTrapPosition(target);
		if (!this.soldier.canPlaceCobweb(this.trapPosition, target)) {
			this.trapPosition = target.blockPosition().immutable();
		}
		return this.soldier.canPlaceCobweb(this.trapPosition, target);
	}

	@Override
	public void start() {
		this.soldier.getNavigation().stop();
		this.soldier.getLookControl().setLookAt(this.victim, 50.0F, 50.0F);
		if (this.soldier.placeCobwebTrap(this.trapPosition, this.victim)) {
			SquadCoordinator.reportComboEvent(
					this.soldier,
					this.victim,
					SquadCoordinator.ComboEvent.WEBBED
			);
			int cooldown = switch (this.soldier.getGearLevel()) {
				case SIX -> 45;
				case FIVE -> 60;
				default -> 80;
			};
			this.soldier.setWebTrapCooldown(cooldown);
		}
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	private BlockPos predictTrapPosition(LivingEntity target) {
		Vec3 movement = target.getDeltaMovement();
		if (!target.onGround() && target.getMainHandItem().is(Items.MACE)) {
			double ticks = Math.max(2.0, Math.min(10.0, (target.getY() - this.soldier.getY()) / 0.7));
			Vec3 lead = target.position().add(movement.scale(ticks));
			ServerLevel level = getServerLevel(this.soldier);
			return level.getHeightmapPos(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					BlockPos.containing(lead.x, 0.0, lead.z)
			);
		}
		if (movement.horizontalDistanceSqr() > 0.015) {
			Vec3 lead = target.position().add(movement.normalize().scale(1.4));
			return BlockPos.containing(lead);
		}
		return target.blockPosition().immutable();
	}
}
