package dev.evanklein.battlesoldiers.client;

import dev.evanklein.battlesoldiers.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class BattleSoldiersClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.SOLDIER, BattleSoldierRenderer::new);
	}
}
