package net.amemah.mobdefenders.client;

import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;

/** Retains vanilla combat jitter and posing, without painting purple eyes over the supplied blue eyes. */
public final class EndermanAllyRenderer extends EndermanRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/enderman_ally.png");

	public EndermanAllyRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new EndermanModel<>(context.bakeLayer(EndermanAllyModel.LAYER));
		layers.removeIf(EnderEyesLayer.class::isInstance);
	}
	@Override public Identifier getTextureLocation(EndermanRenderState state) { return TEXTURE; }
}
