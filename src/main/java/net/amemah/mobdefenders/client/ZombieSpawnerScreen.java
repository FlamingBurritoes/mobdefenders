package net.amemah.mobdefenders.client;

import net.amemah.mobdefenders.menu.ZombieSpawnerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ZombieSpawnerScreen extends AbstractContainerScreen<ZombieSpawnerMenu> {
	private Button toggle;
	public ZombieSpawnerScreen(ZombieSpawnerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 98);
	}
	private Component modeText() {
		return Component.translatable(menu.standing() ? "gui.mobdefenders.standing" : "gui.mobdefenders.wander");
	}
	@Override protected void init() {
		super.init();
		toggle = addRenderableWidget(Button.builder(modeText(), button -> {
			if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
		}).bounds(leftPos + 38, topPos + 52, 100, 20).build());
	}
	@Override protected void containerTick() {
		super.containerTick();
		toggle.setMessage(modeText());
	}
	@Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
		super.extractContents(graphics, mouseX, mouseY, partialTick);
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, 8, 8, 0xFF404040, false);
		graphics.text(font, Component.translatable("gui.mobdefenders.guard_type"), 38, 35, 0xFF404040, false);
	}
}
