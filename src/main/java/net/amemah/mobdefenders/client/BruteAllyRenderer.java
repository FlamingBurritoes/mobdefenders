package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.entity.BruteAlly;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.piglin.AdultPiglinModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;

public final class BruteAllyRenderer extends HumanoidMobRenderer<BruteAlly, PiglinRenderState, AdultPiglinModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/brute_ally.png");

	public BruteAllyRenderer(EntityRendererProvider.Context context) {
		super(context, new BruteAllyModel(context.bakeLayer(BruteAllyModel.LAYER)),
			new BruteAllyModel(context.bakeLayer(BruteAllyModel.LAYER)), .5F, PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS);
		addLayer(new HumanoidArmorLayer<>(this,
			ArmorModelSet.bake(ModelLayers.PIGLIN_BRUTE_ARMOR, context.getModelSet(), AdultPiglinModel::new), context.getEquipmentRenderer()));
	}

	@Override public PiglinRenderState createRenderState() { return new PiglinRenderState(); }
	@Override public Identifier getTextureLocation(PiglinRenderState state) { return TEXTURE; }
	@Override public void extractRenderState(BruteAlly ally, PiglinRenderState state, float partialTick) {
		super.extractRenderState(ally, state, partialTick);
		state.isBrute = true;
		state.armPose = ally.getArmPose();
	}
}
