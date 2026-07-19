package dev.evanklein.battlesoldiers.entity;

import dev.evanklein.battlesoldiers.BattleSoldiersMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.Registry;

public final class ModEntities {
	public static final EntityType<BattleSoldierEntity> SOLDIER = register(
			"soldier",
			EntityType.Builder.<BattleSoldierEntity>of(BattleSoldierEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.eyeHeight(1.74F)
					.clientTrackingRange(10)
	);

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
		Identifier id = Identifier.fromNamespaceAndPath(BattleSoldiersMod.MOD_ID, path);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(SOLDIER, BattleSoldierEntity.createSoldierAttributes());
	}
}
