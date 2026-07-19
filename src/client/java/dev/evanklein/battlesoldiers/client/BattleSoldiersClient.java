package dev.evanklein.battlesoldiers.client;

import dev.evanklein.battlesoldiers.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public final class BattleSoldiersClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.SOLDIER, ZombieRenderer::new);
	}
}
