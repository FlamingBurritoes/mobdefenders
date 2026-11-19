package net.amemah.mobdefenders.client;

import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;

/** Retains vanilla walking, fuse swelling/flashing and charged-creeper rendering. */
public final class CreeperAllyRenderer extends CreeperRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/creeper_ally.png");
	public CreeperAllyRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new CreeperModel(context.bakeLayer(CreeperAllyModel.LAYER));
	}
	@Override public Identifier getTextureLocation(CreeperRenderState state) { return TEXTURE; }
}
