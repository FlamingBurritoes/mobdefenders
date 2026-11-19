package net.amemah.mobdefenders;

import net.amemah.mobdefenders.entity.*;
import net.amemah.mobdefenders.menu.AllySpawnerMenu;
import net.amemah.mobdefenders.spawner.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public class CreeperAllySpawnerGameTests {
	private BlockPos place(GameTestHelper h, boolean immediate) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		h.setBlock(new BlockPos(3, 1, 3), ModContent.CREEPER_ALLY_SPAWNER);
		BlockPos pos = h.absolutePos(new BlockPos(3, 1, 3));
		if (immediate) AllySpawnerData.get(h.getLevel()).source(pos).readyAt = h.getLevel().getGameTime();
		return pos;
	}
	private CreeperAlly ally(GameTestHelper h, BlockPos pos) {
		var source = AllySpawnerData.get(h.getLevel()).source(pos);
		h.assertTrue(source.entity != null, "Expected one owned creeper");
		return (CreeperAlly) h.getLevel().getEntity(source.entity);
	}
	@GameTest(maxTicks = 2900, padding = 40)
	public void normalFuseDiscardsAndStartsSharedReplacementTimer(GameTestHelper h) {
		BlockPos pos = place(h, false);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		var source = data.source(pos);
		h.assertTrue(source.kind == SpawnerKind.CREEPER_ALLY && source.readyAt == level.getGameTime() + 900,
			"Shared 45-second initial wait");
		h.runAtTickTime(890, () -> h.assertTrue(source.entity == null, "No early initial spawn"));
		h.runAtTickTime(940, () -> {
			var creeper = ally(h, pos);
			h.assertTrue(creeper.getMaxHealth() == 20 && creeper.getAttributeValue(Attributes.MOVEMENT_SPEED) == .25,
				"Unmodified vanilla creeper attributes");
			creeper.ignite();
			h.runAtTickTime(960, () -> h.assertTrue(creeper.isAlive(), "Vanilla fuse must not explode before 30 ticks"));
			h.runAtTickTime(990, () -> {
				h.assertTrue(creeper.isRemoved() && source.entity == null, "Self-explosion must release owned slot");
				long remaining = source.readyAt - level.getGameTime();
				h.assertTrue(remaining > 1750 && remaining <= 1800, "Discard starts normal 90-second replacement delay");
				var restored = AllySpawnerData.read(data.write()).source(pos);
				h.assertTrue(restored.kind == SpawnerKind.CREEPER_ALLY && restored.readyAt == source.readyAt,
					"Creeper kind/deadline must survive saves");
			});
		});
		h.runAtTickTime(2720, () -> h.assertTrue(source.entity == null, "No early replacement"));
		h.runAtTickTime(2810, () -> {
			var creeper = ally(h, pos);
			h.assertTrue(level.getEntitiesOfClass(CreeperAlly.class, creeper.getBoundingBox().inflate(6),
				e -> e.isAlive() && data.owner(e.getUUID()) == source).size() == 1, "Hard cap one creeper");
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			h.assertTrue(!creeper.isAlive() && data.owner(creeper.getUUID()) == null, "Source break must retire its creeper");
			h.succeed();
		});
	}
	@GameTest(maxTicks = 250, padding = 40)
	public void actualSwellAIApproachesHostilesAndExplodes(GameTestHelper h) {
		BlockPos pos = place(h, true);
		h.runAtTickTime(30, () -> {
			var creeper = ally(h, pos);
			var enemy = EntityType.HUSK.create(h.getLevel(), EntitySpawnReason.COMMAND);
			enemy.snapTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5, 0, 0);
			enemy.setNoAi(true);
			h.getLevel().addFreshEntity(enemy);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(creeper.getSwelling(1) > 0,
				"Native swell goal must acquire nearby hostile and animate fuse"))
				.thenWaitUntil(() -> h.assertTrue(creeper.isRemoved(), "Native fuse must trigger safe detonation"))
				.thenExecute(() -> {
					h.assertTrue(enemy.getHealth() < enemy.getMaxHealth(), "Explosion must damage hostile");
					h.assertTrue(h.getLevel().getBlockState(pos).is(ModContent.CREEPER_ALLY_SPAWNER), "Spawner must survive");
					enemy.discard();
				}).thenSucceed();
		});
	}
	@GameTest(padding = 40)
	public void normalBlastProtectsFriendsPlayersNeutralsAndBlocks(GameTestHelper h) {
		safeBlast(h, false);
	}
	@GameTest(padding = 48)
	public void chargedBlastStillProtectsFriendsAndBlocks(GameTestHelper h) {
		safeBlast(h, true);
	}
	private void safeBlast(GameTestHelper h, boolean charged) {
		BlockPos pos = place(h, true);
		var level = h.getLevel();
		var creeper = ModContent.CREEPER_ALLY.create(level, EntitySpawnReason.COMMAND);
		Vec3 center = h.absoluteVec(new Vec3(3.5, 1, 3.5));
		creeper.snapTo(center, 0, 0);
		creeper.setNoAi(true);
		level.addFreshEntity(creeper);
		if (charged) creeper.thunderHit(level, EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND));
		h.assertTrue(creeper.isPowered() == charged, "Expected charged state");
		var player = h.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		var cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
		var golem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
		var vanillaCreeper = EntityType.CREEPER.create(level, EntitySpawnReason.COMMAND);
		var zombie = ModContent.ZOMBIE_ALLY.create(level, EntitySpawnReason.COMMAND);
		var skeleton = ModContent.SKELETON_ALLY.create(level, EntitySpawnReason.COMMAND);
		var spider = ModContent.SPIDER_ALLY.create(level, EntitySpawnReason.COMMAND);
		var friendCreeper = ModContent.CREEPER_ALLY.create(level, EntitySpawnReason.COMMAND);
		var enderman = EntityType.ENDERMAN.create(level, EntitySpawnReason.COMMAND);
		var piglin = EntityType.PIGLIN.create(level, EntitySpawnReason.COMMAND);
		var zombifiedPiglin = EntityType.ZOMBIFIED_PIGLIN.create(level, EntitySpawnReason.COMMAND);
		var calmSpider = EntityType.SPIDER.create(level, EntitySpawnReason.COMMAND);
		List<LivingEntity> protectedMobs = List.of(player, cow, golem, vanillaCreeper, zombie, skeleton, spider,
			friendCreeper);
		Map<LivingEntity, Float> health = new HashMap<>();
		for (int i = 0; i < protectedMobs.size(); i++) {
			var mob = protectedMobs.get(i);
			double angle = i * Math.PI * 2 / protectedMobs.size();
			mob.snapTo(center.x + Math.cos(angle) * 2.5, center.y, center.z + Math.sin(angle) * 2.5, 0, 0);
			mob.setDeltaMovement(Vec3.ZERO);
			mob.setNoGravity(true);
			if (mob instanceof Mob ai) { ai.setNoAi(true); level.addFreshEntity(mob); }
			health.put(mob, mob.getHealth());
			creeper.setTarget(mob);
			h.assertTrue(!creeper.canAttack(mob) && creeper.getTarget() == null, "Shared friendly/Creeper exclusions");
		}
		var enemy = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
		List<Mob> enemies = List.of(enemy, enderman, piglin, zombifiedPiglin, calmSpider);
		for (int i = 0; i < enemies.size(); i++) {
			var monster = enemies.get(i);
			double angle = i * Math.PI * 2 / enemies.size();
			monster.snapTo(center.x + Math.cos(angle) * 1.5, center.y, center.z + Math.sin(angle) * 1.5, 0, 0);
			monster.setNoAi(true);
			monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
			monster.setHealth(200);
			level.addFreshEntity(monster);
			creeper.setTarget(monster);
			h.assertTrue(creeper.canAttack(monster) && creeper.getTarget() == monster,
				"Monster targeting must include spiders, piglins and endermen");
		}
		h.setBlock(new BlockPos(3, 1, 1), Blocks.OAK_PLANKS);
		h.setBlock(new BlockPos(3, 1, 5), Blocks.TNT);
		creeper.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		creeper.explodeSafely(3);
		for (var monster : enemies)
			h.assertTrue(monster.getHealth() < 200, "Normal or charged blast must hurt " + monster.getType());
		for (var mob : protectedMobs) {
			h.assertTrue(mob.isAlive() && mob.getHealth() == health.get(mob), "Blast must not damage " + mob.getType());
			h.assertTrue(mob.getDeltaMovement().lengthSqr() == 0, "Blast must not knock back friends/neutrals/player");
			mob.discard();
		}
		h.assertTrue(level.getBlockState(pos).is(ModContent.CREEPER_ALLY_SPAWNER), "Own spawner must survive");
		h.assertTrue(h.getBlockState(new BlockPos(3, 1, 1)).is(Blocks.OAK_PLANKS)
			&& h.getBlockState(new BlockPos(3, 1, 5)).is(Blocks.TNT), "Blocks/TNT must not be destroyed or primed");
		h.assertTrue(level.getEntitiesOfClass(AreaEffectCloud.class, creeper.getBoundingBox().inflate(8)).isEmpty(),
			"No unfiltered lingering potion cloud may harm friends");
		enemies.forEach(Entity::discard);
		h.succeed();
	}
	@GameTest(maxTicks = 150, padding = 40)
	public void ordinaryHostileCreeperStillDamagesMobsAndTerrain(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var level = h.getLevel();
		var creeper = EntityType.CREEPER.create(level, EntitySpawnReason.COMMAND);
		creeper.snapTo(h.absoluteVec(new Vec3(3.5, 1, 3.5)), 0, 0);
		creeper.setNoAi(true);
		level.addFreshEntity(creeper);
		var cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
		cow.snapTo(creeper.getX() + 1.5, creeper.getY(), creeper.getZ(), 0, 0);
		cow.setNoAi(true);
		level.addFreshEntity(cow);
		h.setBlock(new BlockPos(3, 1, 4), Blocks.GLASS);
		creeper.ignite();
		h.runAtTickTime(45, () -> {
			h.assertTrue(creeper.isRemoved(), "Ordinary creeper must still detonate");
			h.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Ordinary creeper still hurts passive mobs");
			h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).isAir(), "Ordinary creeper still destroys blocks");
			cow.discard();
			h.succeed();
		});
	}
	@GameTest(maxTicks = 500, padding = 40)
	public void creeperWalksBackIntoPatrolAfterDistantCombatEnds(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		h.setBlock(new BlockPos(1, 1, 1), ModContent.CREEPER_ALLY_SPAWNER);
		BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
		var source = AllySpawnerData.get(h.getLevel()).source(pos);
		source.readyAt = h.getLevel().getGameTime();
		AllySpawnerData.get(h.getLevel()).toggle(pos);
		Vec3[] last = {null};
		CreeperAlly[] returning = {null};
		h.onEachTick(() -> {
			if (returning[0] == null) return;
			h.assertTrue(returning[0].position().distanceTo(last[0]) < .9, "Post-combat return must walk, not snap");
			last[0] = returning[0].position();
		});
		h.runAtTickTime(30, () -> {
			var creeper = ally(h, pos);
			creeper.teleportTo(pos.getX() + 5.75, pos.getY(), pos.getZ() + 5.5);
			creeper.setDeltaMovement(Vec3.ZERO);
			creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			var enemy = EntityType.HUSK.create(h.getLevel(), EntitySpawnReason.COMMAND);
			enemy.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + 5.5, 0, 0);
			enemy.setNoAi(true);
			h.getLevel().addFreshEntity(enemy);
			creeper.setTarget(enemy);
			h.runAtTickTime(80, () -> {
				h.assertTrue(creeper.isAlive() && creeper.getTarget() == enemy, "Combat target outside patrol must remain eligible");
				h.assertTrue(!AllySpawnerManager.inside(pos, creeper.position(), 6), "Fixture starts outside patrol");
				enemy.discard();
				creeper.setTarget(null);
				creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.25);
				returning[0] = creeper;
				last[0] = creeper.position();
				h.startSequence().thenWaitUntil(() -> h.assertTrue(AllySpawnerManager.inside(pos, creeper.position(), 6),
					"Creeper must walk back inside patrol once its enemy is gone")).thenSucceed();
			});
		});
	}
	@GameTest(maxTicks = 700, padding = 40, skyAccess = true)
	public void sharedMenuWanderRecallStandingAndSunSafety(GameTestHelper h) {
		BlockPos pos = place(h, true);
		h.setTime(6000);
		var player = h.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(Vec3.atBottomCenterOf(pos.above()), 0, 0);
		var menu = new AllySpawnerMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos));
		h.assertTrue(menu.stillValid(player) && menu.standing() && menu.clickMenuButton(player, 0) && !menu.standing(),
			"Same working Standing/Wander menu");
		h.runAtTickTime(30, () -> {
			var creeper = ally(h, pos);
			Vec3 start = creeper.position();
			h.startSequence().thenWaitUntil(() -> h.assertTrue(creeper.position().distanceToSqr(start) > 2.25,
				"Wander must physically patrol")).thenExecute(() -> {
					h.assertTrue(!creeper.isOnFire() && creeper.getHealth() == 20, "Sunlight-safe defender");
					creeper.setNoAi(true);
					creeper.teleportTo(pos.getX() + 13.5, pos.getY(), pos.getZ() + .5);
				}).thenIdle(3).thenExecute(() -> {
					h.assertTrue(creeper.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .2, "Twelve-block recall");
					menu.clickMenuButton(player, 0);
					creeper.setNoAi(false);
					creeper.teleportTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5);
				}).thenWaitUntil(() -> h.assertTrue(creeper.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
					"Standing returns same creeper to top")).thenSucceed();
		});
	}
	@GameTest(padding = 24)
	public void bothRequestedRecipesMatchAndRejectSubstitutes(GameTestHelper h) {
		checkRecipe(h, List.of("GGG", "GIG", "GGG"), Map.of('G', Items.GUNPOWDER, 'I', ModContent.IRON_REINFORCED_PLANKS_ITEM),
			"creeper_ally_spawner", ModContent.CREEPER_ALLY_SPAWNER_ITEM, ModContent.COPPER_REINFORCED_PLANKS_ITEM);
		checkRecipe(h, List.of("III", "IPI", "III"), Map.of('I', Items.IRON_INGOT, 'P', Items.OAK_PLANKS),
			"iron_reinforced_planks", ModContent.IRON_REINFORCED_PLANKS_ITEM, Items.SPRUCE_PLANKS);
		h.succeed();
	}
	private void checkRecipe(GameTestHelper h, List<String> rows, Map<Character, Item> keys, String id, Item output, Item wrongCenter) {
		List<ItemStack> items = new ArrayList<>();
		for (String row : rows) for (char c : row.toCharArray()) items.add(new ItemStack(keys.get(c)));
		var input = CraftingInput.of(3, 3, items);
		var recipe = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id(id)) && result.is(output) && result.getCount() == 1, "Requested " + id + " recipe");
		items.set(4, new ItemStack(wrongCenter));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, items), h.getLevel()), "Incorrect center must not substitute");
	}
}
