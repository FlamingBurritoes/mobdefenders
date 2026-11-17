package net.amemah.mobdefenders.menu;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AllySpawnerMenu extends AbstractContainerMenu {
	private final ContainerLevelAccess access;
	private final DataSlot mode;

	public AllySpawnerMenu(int id, Inventory inventory) { this(id, inventory, ContainerLevelAccess.NULL); }
	public AllySpawnerMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModContent.ALLY_SPAWNER_MENU, id);
		this.access = access;
		mode = addDataSlot(new DataSlot() {
			private int clientMode;
			@Override public int get() {
				return access.evaluate((level, pos) -> {
					var source = AllySpawnerData.get((ServerLevel) level).source(pos);
					return source == null || source.standing ? 0 : 1;
				}, clientMode);
			}
			@Override public void set(int value) { clientMode = value; }
		});
	}
	public boolean standing() { return mode.get() == 0; }
	@Override public boolean clickMenuButton(Player player, int button) {
		if (button != 0 || !stillValid(player)) return false;
		access.execute((level, pos) -> {
			if (level instanceof ServerLevel server) AllySpawnerData.get(server).toggle(pos);
		});
		broadcastChanges();
		return true;
	}
	@Override public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> SpawnerKind.isSpawner(level.getBlockState(pos))
			&& player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64, true);
	}
	@Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
