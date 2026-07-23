package dev.evanklein.battlesoldiers.entity.ai;

import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.SquadCoordinator;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class AntiMaceCounterGoal extends Goal {
	private final BattleSoldierEntity soldier;
	private LivingEntity attacker;
	private BlockPos counterPosition;
	private int cooldown;

	public AntiMaceCounterGoal(BattleSoldierEntity soldier) {
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
		this.attacker = this.soldier.getTarget();
		if (this.attacker == null
				|| !this.attacker.isAlive()
				|| !this.attacker.getMainHandItem().is(Items.MACE)
				|| this.attacker.getY() - this.soldier.getY() < 2.5
				|| this.soldier.distanceToSqr(this.attacker) > 100.0) {
			return false;
		}

		Vec3 velocity = this.attacker.getDeltaMovement();
		double ticks = Mth.clamp((this.attacker.getY() - this.soldier.getY()) / 0.7, 2.0, 10.0);
		Vec3 projected = this.attacker.position().add(velocity.scale(ticks));
		ServerLevel level = getServerLevel(this.soldier);
		if (role == CombatRole.TRAPPER) {
			this.counterPosition = level.getHeightmapPos(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					BlockPos.containing(projected.x, 0.0, projected.z)
			);
			return this.soldier.canPlaceCobweb(this.counterPosition, this.attacker);
		}

		int canopyY = Mth.floor(this.soldier.getY()) + 3;
		this.counterPosition = BlockPos.containing(projected.x, canopyY, projected.z);
		return this.attacker.getY() >= canopyY + 1.5
				&& this.soldier.canPlaceTacticalBlock(this.counterPosition);
	}

	@Override
	public void start() {
		this.soldier.getNavigation().stop();
		this.soldier.getLookControl().setLookAt(this.attacker, 50.0F, 50.0F);
		boolean placed = this.soldier.getCombatRole() == CombatRole.TRAPPER
				? this.soldier.placeCobwebTrap(this.counterPosition, this.attacker)
				: this.soldier.placeTacticalBlock(this.counterPosition);
		if (placed) {
			SquadCoordinator.reportComboEvent(
					this.soldier,
					this.attacker,
					SquadCoordinator.ComboEvent.WEBBED
			);
		}
		this.cooldown = placed ? 35 : 15;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}
}
