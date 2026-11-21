package net.amemah.mobdefenders.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;

/** The supplied segment geometry and UV layout match the native animated Magma Cube model. */
public final class MagmaCubeAllyRenderer extends MagmaCubeRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/magma_cube_ally.png");
	public MagmaCubeAllyRenderer(EntityRendererProvider.Context context) { super(context); }
	@Override public Identifier getTextureLocation(SlimeRenderState state) { return TEXTURE; }
}
