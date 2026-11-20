package net.amemah.mobdefenders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class SlimeAllyRenderer extends SlimeRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/slime_ally.png");

	public SlimeAllyRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new SlimeModel(context.bakeLayer(SlimeAllyModel.INNER));
		layers.removeIf(SlimeOuterLayer.class::isInstance);
		var shell = new SlimeModel(context.bakeLayer(SlimeAllyModel.OUTER));
		addLayer(new RenderLayer<>(this) {
			@Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SlimeRenderState state,
					float yRot, float xRot) {
				boolean outline = state.isInvisible && state.appearsGlowing();
				if (state.isInvisible && !outline) return;
				collector.order(1).submitModel(shell, state, pose,
					outline ? RenderTypes.outline(TEXTURE) : RenderTypes.entityTranslucent(TEXTURE),
					light, getOverlayCoords(state, 0), state.outlineColor, null);
			}
		});
	}

	@Override public Identifier getTextureLocation(SlimeRenderState state) { return TEXTURE; }
}
