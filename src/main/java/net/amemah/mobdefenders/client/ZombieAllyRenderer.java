package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.ModContent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** Retains vanilla walking, attack, hurt/death, equipment and held-item rendering. */
public final class ZombieAllyRenderer extends ZombieRenderer {
	private static final Identifier TEXTURE = ModContent.id("textures/entity/zombie_ally.png");

	public ZombieAllyRenderer(EntityRendererProvider.Context context) {
		// The spawner produces adults only; both age slots use the supplied rig.
		super(context, ZombieAllyModel.LAYER, ZombieAllyModel.LAYER,
			ModelLayers.ZOMBIE_ARMOR, ModelLayers.ZOMBIE_BABY_ARMOR);
	}

	@Override public Identifier getTextureLocation(ZombieRenderState state) { return TEXTURE; }
}
