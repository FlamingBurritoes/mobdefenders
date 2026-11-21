package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.entity.GhastAlly;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class GhastAllyRenderer extends MobRenderer<GhastAlly, LivingEntityRenderState, GhastAllyModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/ghast_ally.png");

	public GhastAllyRenderer(EntityRendererProvider.Context context) {
		super(context, new GhastAllyModel(context.bakeLayer(GhastAllyModel.LAYER)), .3F);
	}

	@Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
	@Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
