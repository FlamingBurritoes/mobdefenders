package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class MobdefendersClient implements ClientModInitializer {
	@Override public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(ZombieAllyModel.LAYER, ZombieAllyModel::createBodyLayer);
		EntityRendererRegistry.register(ModContent.ZOMBIE_ALLY, ZombieAllyRenderer::new);
		ModelLayerRegistry.registerModelLayer(SkeletonAllyModel.LAYER, SkeletonAllyModel::createBodyLayer);
		EntityRendererRegistry.register(ModContent.SKELETON_ALLY, SkeletonAllyRenderer::new);
		ModelLayerRegistry.registerModelLayer(SpiderAllyModel.LAYER, SpiderAllyModel::createBodyLayer);
		EntityRendererRegistry.register(ModContent.SPIDER_ALLY, SpiderAllyRenderer::new);
		ModelLayerRegistry.registerModelLayer(CreeperAllyModel.LAYER, CreeperAllyModel::createBodyLayer);
		EntityRendererRegistry.register(ModContent.CREEPER_ALLY, CreeperAllyRenderer::new);
		ModelLayerRegistry.registerModelLayer(EndermanAllyModel.LAYER, EndermanAllyModel::createBodyLayer);
		EntityRendererRegistry.register(ModContent.ENDERMAN_ALLY, EndermanAllyRenderer::new);
		ModelLayerRegistry.registerModelLayer(SlimeAllyModel.INNER, SlimeAllyModel::inner);
		ModelLayerRegistry.registerModelLayer(SlimeAllyModel.OUTER, SlimeAllyModel::outer);
		EntityRendererRegistry.register(ModContent.SLIME_ALLY, SlimeAllyRenderer::new);
		ModelLayerRegistry.registerModelLayer(DrownedAllyModel.INNER, () -> DrownedAllyModel.create(0));
		ModelLayerRegistry.registerModelLayer(DrownedAllyModel.OUTER, () -> DrownedAllyModel.create(.25F));
		EntityRendererRegistry.register(ModContent.DROWNED_ALLY, DrownedAllyRenderer::new);
		MenuScreens.register(ModContent.ALLY_SPAWNER_MENU, AllySpawnerScreen::new);
	}
}
