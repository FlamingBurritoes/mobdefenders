package net.amemah.mobdefenders;

import net.amemah.mobdefenders.block.ZombieSpawnerBlock;
import net.amemah.mobdefenders.entity.ZombieAlly;
import net.amemah.mobdefenders.menu.ZombieSpawnerMenu;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModContent {
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("mobdefenders", path);
	}

	public static final Block COPPER_REINFORCED_PLANKS = Registry.register(BuiltInRegistries.BLOCK,
		id("copper_reinforced_planks"), new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
			.setId(ResourceKey.create(Registries.BLOCK, id("copper_reinforced_planks")))));
	public static final Block ZOMBIE_SPAWNER = Registry.register(BuiltInRegistries.BLOCK,
		id("zombie_spawner"), new ZombieSpawnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
			.setId(ResourceKey.create(Registries.BLOCK, id("zombie_spawner")))));
	public static final Item COPPER_REINFORCED_PLANKS_ITEM = blockItem("copper_reinforced_planks", COPPER_REINFORCED_PLANKS);
	public static final Item ZOMBIE_SPAWNER_ITEM = blockItem("zombie_spawner", ZOMBIE_SPAWNER);
	public static final EntityType<ZombieAlly> ZOMBIE_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("zombie_ally"), EntityType.Builder.of(ZombieAlly::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("zombie_ally"))));
	public static final MenuType<ZombieSpawnerMenu> ZOMBIE_MENU = Registry.register(BuiltInRegistries.MENU,
		id("zombie_spawner"), new MenuType<>(ZombieSpawnerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	private static Item blockItem(String name, Block block) {
		return Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block,
			new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).useBlockDescriptionPrefix()));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(ZOMBIE_ALLY, ZombieAlly.createAllyAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> entries.accept(ZOMBIE_SPAWNER_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
			.register(entries -> entries.accept(COPPER_REINFORCED_PLANKS_ITEM));
	}
	private ModContent() { }
}
