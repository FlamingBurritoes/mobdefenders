package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.entity.WitherSkeletonAlly;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

/** The supplied mirrored limbs, hat and UVs match the Skeleton Ally mesh. */
public final class WitherSkeletonAllyRenderer extends AbstractSkeletonRenderer<WitherSkeletonAlly, SkeletonRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "wither_skeleton_ally"), "main");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/wither_skeleton_ally.png");

	public WitherSkeletonAllyRenderer(EntityRendererProvider.Context context) {
		super(context, LAYER, ModelLayers.WITHER_SKELETON_ARMOR);
	}

	@Override public Identifier getTextureLocation(SkeletonRenderState state) { return TEXTURE; }
	@Override public SkeletonRenderState createRenderState() { return new SkeletonRenderState(); }
}
