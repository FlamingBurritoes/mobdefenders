package net.amemah.mobdefenders.client;

import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Head/rod dimensions and UVs match the animated vanilla model. */
public final class BlazeAllyRenderer extends BlazeRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/blaze_ally.png");
	public BlazeAllyRenderer(EntityRendererProvider.Context context) { super(context); }
	@Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
