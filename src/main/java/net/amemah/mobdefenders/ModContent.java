package net.amemah.mobdefenders;

import net.amemah.mobdefenders.block.AllySpawnerBlock;
import net.amemah.mobdefenders.block.DrownedAllySpawnerBlock;
import net.amemah.mobdefenders.entity.BlazeAlly;
import net.amemah.mobdefenders.entity.GhastAlly;
import net.amemah.mobdefenders.entity.GhastAllyFireball;
import net.amemah.mobdefenders.entity.BruteAlly;
import net.amemah.mobdefenders.entity.MagmaCubeAlly;
import net.amemah.mobdefenders.entity.DrownedAlly;
import net.amemah.mobdefenders.entity.ZombieAlly;
import net.amemah.mobdefenders.entity.SkeletonAlly;
import net.amemah.mobdefenders.entity.WitherSkeletonAlly;
import net.amemah.mobdefenders.entity.SpiderAlly;
import net.amemah.mobdefenders.entity.CreeperAlly;
import net.amemah.mobdefenders.entity.EndermanAlly;
import net.amemah.mobdefenders.entity.SlimeAlly;
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
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
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
	public static final Block IRON_REINFORCED_PLANKS = Registry.register(BuiltInRegistries.BLOCK,
		id("iron_reinforced_planks"), new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
			.setId(ResourceKey.create(Registries.BLOCK, id("iron_reinforced_planks")))));
	public static final Block GOLD_REINFORCED_NETHER_BRICK = Registry.register(BuiltInRegistries.BLOCK,
		id("gold_reinforced_nether_brick"), new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICKS)
			.setId(ResourceKey.create(Registries.BLOCK, id("gold_reinforced_nether_brick")))));
	public static final Block ZOMBIE_ALLY_SPAWNER = spawnerBlock("zombie_ally_spawner");
	public static final Block SKELETON_ALLY_SPAWNER = spawnerBlock("skeleton_ally_spawner");
	public static final Block SPIDER_ALLY_SPAWNER = spawnerBlock("spider_ally_spawner");
	public static final Block CREEPER_ALLY_SPAWNER = spawnerBlock("creeper_ally_spawner");
	public static final Block ENDERMAN_ALLY_SPAWNER = spawnerBlock("enderman_ally_spawner");
	public static final Block SLIME_ALLY_SPAWNER = spawnerBlock("slime_ally_spawner");
	public static final Block BRUTE_ALLY_SPAWNER = spawnerBlock("brute_ally_spawner", SoundType.NETHER_BRICKS);
	public static final Block MAGMA_CUBE_ALLY_SPAWNER = spawnerBlock("magma_cube_ally_spawner");
	public static final Block BLAZE_ALLY_SPAWNER = spawnerBlock("blaze_ally_spawner");
	public static final Block GHAST_ALLY_SPAWNER = spawnerBlock("ghast_ally_spawner");
	public static final Block WITHER_SKELETON_ALLY_SPAWNER = spawnerBlock("wither_skeleton_ally_spawner");
	public static final Block DROWNED_ALLY_SPAWNER = Registry.register(BuiltInRegistries.BLOCK,
		id("drowned_ally_spawner"), new DrownedAllySpawnerBlock(spawnerProperties("drowned_ally_spawner", SoundType.WOOD)));
	public static final Item COPPER_REINFORCED_PLANKS_ITEM = blockItem("copper_reinforced_planks", COPPER_REINFORCED_PLANKS);
	public static final Item IRON_REINFORCED_PLANKS_ITEM = blockItem("iron_reinforced_planks", IRON_REINFORCED_PLANKS);
	public static final Item GOLD_REINFORCED_NETHER_BRICK_ITEM = blockItem("gold_reinforced_nether_brick", GOLD_REINFORCED_NETHER_BRICK);
	public static final Item ZOMBIE_ALLY_SPAWNER_ITEM = blockItem("zombie_ally_spawner", ZOMBIE_ALLY_SPAWNER);
	public static final Item SKELETON_ALLY_SPAWNER_ITEM = blockItem("skeleton_ally_spawner", SKELETON_ALLY_SPAWNER);
	public static final Item SPIDER_ALLY_SPAWNER_ITEM = blockItem("spider_ally_spawner", SPIDER_ALLY_SPAWNER);
	public static final Item CREEPER_ALLY_SPAWNER_ITEM = blockItem("creeper_ally_spawner", CREEPER_ALLY_SPAWNER);
	public static final Item ENDERMAN_ALLY_SPAWNER_ITEM = blockItem("enderman_ally_spawner", ENDERMAN_ALLY_SPAWNER);
	public static final Item SLIME_ALLY_SPAWNER_ITEM = blockItem("slime_ally_spawner", SLIME_ALLY_SPAWNER);
	public static final Item DROWNED_ALLY_SPAWNER_ITEM = blockItem("drowned_ally_spawner", DROWNED_ALLY_SPAWNER);
	public static final Item BRUTE_ALLY_SPAWNER_ITEM = blockItem("brute_ally_spawner", BRUTE_ALLY_SPAWNER);
	public static final Item MAGMA_CUBE_ALLY_SPAWNER_ITEM = blockItem("magma_cube_ally_spawner", MAGMA_CUBE_ALLY_SPAWNER);
	public static final Item BLAZE_ALLY_SPAWNER_ITEM = blockItem("blaze_ally_spawner", BLAZE_ALLY_SPAWNER);
	public static final Item GHAST_ALLY_SPAWNER_ITEM = blockItem("ghast_ally_spawner", GHAST_ALLY_SPAWNER);
	public static final Item WITHER_SKELETON_ALLY_SPAWNER_ITEM = blockItem("wither_skeleton_ally_spawner", WITHER_SKELETON_ALLY_SPAWNER);
	public static final EntityType<ZombieAlly> ZOMBIE_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("zombie_ally"), EntityType.Builder.of(ZombieAlly::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("zombie_ally"))));
	public static final EntityType<SkeletonAlly> SKELETON_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("skeleton_ally"), EntityType.Builder.of(SkeletonAlly::new, MobCategory.CREATURE)
			.sized(.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("skeleton_ally"))));
	public static final EntityType<WitherSkeletonAlly> WITHER_SKELETON_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("wither_skeleton_ally"), EntityType.Builder.of(WitherSkeletonAlly::new, MobCategory.CREATURE)
			.sized(.7F, 2.4F).eyeHeight(2.1F).immuneTo(Blocks.WITHER_ROSE).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("wither_skeleton_ally"))));
	public static final EntityType<SpiderAlly> SPIDER_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("spider_ally"), EntityType.Builder.of(SpiderAlly::new, MobCategory.CREATURE)
			.sized(1.4F, .9F).eyeHeight(.65F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("spider_ally"))));
	public static final EntityType<CreeperAlly> CREEPER_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("creeper_ally"), EntityType.Builder.of(CreeperAlly::new, MobCategory.CREATURE)
			.sized(.6F, 1.7F).eyeHeight(1.445F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("creeper_ally"))));
	public static final EntityType<EndermanAlly> ENDERMAN_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("enderman_ally"), EntityType.Builder.of(EndermanAlly::new, MobCategory.CREATURE)
			.sized(.6F, 2.9F).eyeHeight(2.55F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("enderman_ally"))));
	public static final EntityType<SlimeAlly> SLIME_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("slime_ally"), EntityType.Builder.of(SlimeAlly::new, MobCategory.CREATURE)
			.sized(.52F, .52F).eyeHeight(.325F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("slime_ally"))));
	public static final EntityType<DrownedAlly> DROWNED_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("drowned_ally"), EntityType.Builder.of(DrownedAlly::new, MobCategory.CREATURE)
			.sized(.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("drowned_ally"))));
	public static final EntityType<BruteAlly> BRUTE_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("brute_ally"), EntityType.Builder.of(BruteAlly::new, MobCategory.CREATURE)
			.sized(.6F, 1.95F).eyeHeight(1.79F).fireImmune().clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("brute_ally"))));
	public static final EntityType<MagmaCubeAlly> MAGMA_CUBE_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("magma_cube_ally"), EntityType.Builder.of(MagmaCubeAlly::new, MobCategory.CREATURE)
			.sized(.52F, .52F).eyeHeight(.325F).fireImmune().clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("magma_cube_ally"))));
	public static final EntityType<BlazeAlly> BLAZE_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("blaze_ally"), EntityType.Builder.of(BlazeAlly::new, MobCategory.CREATURE)
			.sized(.6F, 1.8F).fireImmune().clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("blaze_ally"))));
	public static final EntityType<GhastAlly> GHAST_ALLY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("ghast_ally"), EntityType.Builder.of(GhastAlly::new, MobCategory.CREATURE)
			.sized(.5F, .875F).eyeHeight(.7F).fireImmune().clientTrackingRange(8)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("ghast_ally"))));
	public static final EntityType<GhastAllyFireball> GHAST_ALLY_FIREBALL = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		id("ghast_ally_fireball"), EntityType.Builder.<GhastAllyFireball>of(GhastAllyFireball::new, MobCategory.MISC)
			.noLootTable().sized(1, 1).clientTrackingRange(4).updateInterval(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("ghast_ally_fireball"))));
	public static final MenuType<AllySpawnerMenu> ALLY_SPAWNER_MENU = Registry.register(BuiltInRegistries.MENU,
		id("ally_spawner"), new MenuType<>(AllySpawnerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	private static Block spawnerBlock(String name) {
		return spawnerBlock(name, SoundType.WOOD);
	}

	private static Block spawnerBlock(String name, SoundType sound) {
		return Registry.register(BuiltInRegistries.BLOCK, id(name), new AllySpawnerBlock(spawnerProperties(name, sound)));
	}

	private static BlockBehaviour.Properties spawnerProperties(String name, SoundType sound) {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).sound(sound)
			.setId(ResourceKey.create(Registries.BLOCK, id(name)));
	}

	private static Item blockItem(String name, Block block) {
		return Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block,
			new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).useBlockDescriptionPrefix()));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(ZOMBIE_ALLY, ZombieAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(SKELETON_ALLY, SkeletonAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(WITHER_SKELETON_ALLY, WitherSkeletonAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(SPIDER_ALLY, SpiderAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(CREEPER_ALLY, CreeperAlly.createAttributes());
		FabricDefaultAttributeRegistry.register(ENDERMAN_ALLY, EndermanAlly.createAttributes());
		FabricDefaultAttributeRegistry.register(SLIME_ALLY, SlimeAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(DROWNED_ALLY, DrownedAlly.createAllyAttributes());
		FabricDefaultAttributeRegistry.register(BRUTE_ALLY, PiglinBrute.createAttributes());
		FabricDefaultAttributeRegistry.register(MAGMA_CUBE_ALLY, MagmaCube.createAttributes());
		FabricDefaultAttributeRegistry.register(BLAZE_ALLY, Blaze.createAttributes());
		FabricDefaultAttributeRegistry.register(GHAST_ALLY, Ghast.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> {
				entries.accept(ZOMBIE_ALLY_SPAWNER_ITEM);
				entries.accept(SKELETON_ALLY_SPAWNER_ITEM);
				entries.accept(SPIDER_ALLY_SPAWNER_ITEM);
				entries.accept(CREEPER_ALLY_SPAWNER_ITEM);
				entries.accept(ENDERMAN_ALLY_SPAWNER_ITEM);
				entries.accept(SLIME_ALLY_SPAWNER_ITEM);
				entries.accept(DROWNED_ALLY_SPAWNER_ITEM);
				entries.accept(BRUTE_ALLY_SPAWNER_ITEM);
				entries.accept(MAGMA_CUBE_ALLY_SPAWNER_ITEM);
				entries.accept(BLAZE_ALLY_SPAWNER_ITEM);
				entries.accept(GHAST_ALLY_SPAWNER_ITEM);
				entries.accept(WITHER_SKELETON_ALLY_SPAWNER_ITEM);
			});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
			.register(entries -> {
				entries.accept(COPPER_REINFORCED_PLANKS_ITEM);
				entries.accept(IRON_REINFORCED_PLANKS_ITEM);
				entries.accept(GOLD_REINFORCED_NETHER_BRICK_ITEM);
			});
	}
	private ModContent() { }
}
