package dev.evanklein.battlesoldiers.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Vanilla {@link MoveControl#strafe} hardcodes a 0.25 speed modifier, which is
 * why stock mob strafing is a quarter-speed shuffle. This exposes the same
 * strafe operation at a caller-chosen speed so combat footwork (circling,
 * spacing, disengage arcs) moves at player-like pace.
 */
public final class SoldierMoveControl extends MoveControl {
	public SoldierMoveControl(Mob mob) {
		super(mob);
	}

	public void strafeAt(float forward, float sideways, double speedMultiplier) {
		this.operation = Operation.STRAFE;
		this.strafeForwards = forward;
		this.strafeRight = sideways;
		this.speedModifier = speedMultiplier;
	}
}
