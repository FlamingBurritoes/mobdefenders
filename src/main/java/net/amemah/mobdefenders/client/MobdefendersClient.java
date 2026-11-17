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
		MenuScreens.register(ModContent.ZOMBIE_MENU, ZombieSpawnerScreen::new);
	}
}
