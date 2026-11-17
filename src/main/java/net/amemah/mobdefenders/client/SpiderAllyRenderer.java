package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.entity.SpiderAlly;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** No vanilla red-eye overlay over the supplied ally texture. */
public final class SpiderAllyRenderer extends MobRenderer<SpiderAlly, LivingEntityRenderState, SpiderModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mobdefenders", "textures/entity/spider_ally.png");
	public SpiderAllyRenderer(EntityRendererProvider.Context context) {
		super(context, new SpiderModel(context.bakeLayer(SpiderAllyModel.LAYER)), .8F);
	}
	@Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
	@Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
	@Override protected float getFlipDegrees() { return 180; }
}
