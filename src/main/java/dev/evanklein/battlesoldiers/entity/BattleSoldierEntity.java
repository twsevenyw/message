package dev.evanklein.battlesoldiers.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public class BattleSoldierEntity extends Zombie {
	public BattleSoldierEntity(EntityType<? extends BattleSoldierEntity> entityType, Level level) {
		super(entityType, level);
	}

	public static AttributeSupplier.Builder createSoldierAttributes() {
		return Zombie.createAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}
}
