package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.entity.SkeletonAlly;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

public final class SkeletonAllyRenderer extends AbstractSkeletonRenderer<SkeletonAlly, SkeletonRenderState> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/skeleton_ally.png");
	public SkeletonAllyRenderer(EntityRendererProvider.Context context) {
		super(context, SkeletonAllyModel.LAYER, ModelLayers.SKELETON_ARMOR);
	}
	@Override public Identifier getTextureLocation(SkeletonRenderState state) { return TEXTURE; }
	@Override public SkeletonRenderState createRenderState() { return new SkeletonRenderState(); }
}
