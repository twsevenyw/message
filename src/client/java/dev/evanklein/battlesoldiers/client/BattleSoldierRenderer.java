package dev.evanklein.battlesoldiers.client;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public final class BattleSoldierRenderer
		extends HumanoidMobRenderer<BattleSoldierEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier TEXTURE =
			Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

	public BattleSoldierRenderer(EntityRendererProvider.Context context) {
		super(
				context,
				new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
				0.5F
		);
		this.addLayer(new HumanoidArmorLayer<>(
				this,
				ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), HumanoidModel::new),
				context.getEquipmentRenderer()
		));
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
