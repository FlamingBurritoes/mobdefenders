package net.amemah.mobdefenders;

import net.amemah.mobdefenders.block.AllySpawnerBlock;
import net.amemah.mobdefenders.entity.ZombieAlly;
import net.amemah.mobdefenders.entity.SkeletonAlly;
import net.amemah.mobdefenders.entity.SpiderAlly;
import net.amemah.mobdefenders.menu.AllySpawnerMenu;
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
	public static final Block ZOMBIE_ALLY_SPAWNER = spawnerBlock("zombie_ally_spawner");
	public static final Block SKELETON_ALLY_SPAWNER = spawnerBlock("skeleton_ally_spawner");
	public static final Block SPIDER_ALLY_SPAWNER = spawnerBlock("spider_ally_spawner");
	public static final Item COPPER_REINFORCED_PLANKS_ITEM = blockItem("copper_reinforced_planks", COPPER_REINFORCED_PLANKS);
	public static final Item ZOMBIE_ALLY_SPAWNER_ITEM = blockItem("zombie_ally_spawner", ZOMBIE_ALLY_SPAWNER);
	public static final Item SKELETON_ALLY_SPAWNER_ITEM = blockItem("skeleton_ally_spawner", SKELETON_ALLY_SPAWNER);
	public static final Item SPIDER_ALLY_SPAWNER_ITEM = blockItem("spider_ally_spawner", SPIDER_ALLY_SPAWNER);
	public static final EntityType<ZombieAlly> ZOMBIE_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("zombie_ally"), EntityType.Builder.of(ZombieAlly::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("zombie_ally"))));
	public static final EntityType<SkeletonAlly> SKELETON_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("skeleton_ally"), EntityType.Builder.of(SkeletonAlly::new, MobCategory.CREATURE)
			.sized(.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("skeleton_ally"))));
	public static final EntityType<SpiderAlly> SPIDER_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("spider_ally"), EntityType.Builder.of(SpiderAlly::new, MobCategory.CREATURE)
			.sized(1.4F, .9F).eyeHeight(.65F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("spider_ally"))));
	public static final MenuType<AllySpawnerMenu> ALLY_SPAWNER_MENU = Registry.register(BuiltInRegistries.MENU,
		id("ally_spawner"), new MenuType<>(AllySpawnerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	private static Block spawnerBlock(String name) {
		return Registry.register(BuiltInRegistries.BLOCK, id(name), new AllySpawnerBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).setId(ResourceKey.create(Registries.BLOCK, id(name)))));
	}

	private static Item blockItem(String name, Block block) {
		return Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block,
			new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).useBlockDescriptionPrefix()));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(ZOMBIE_ALLY, ZombieAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(SKELETON_ALLY, SkeletonAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(SPIDER_ALLY, SpiderAlly.createAllyAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> {
				entries.accept(ZOMBIE_ALLY_SPAWNER_ITEM);
				entries.accept(SKELETON_ALLY_SPAWNER_ITEM);
				entries.accept(SPIDER_ALLY_SPAWNER_ITEM);
			});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
			.register(entries -> entries.accept(COPPER_REINFORCED_PLANKS_ITEM));
	}
	private ModContent() { }
}
