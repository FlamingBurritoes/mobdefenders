package net.amemah.mobdefenders;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.amemah.mobdefenders.entity.*;
import net.amemah.mobdefenders.menu.AllySpawnerMenu;
import net.amemah.mobdefenders.spawner.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public class SkeletonAllySpawnerGameTests {
	private BlockPos place(GameTestHelper h, boolean immediate) {
		for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		h.setBlock(new BlockPos(4, 1, 4), ModContent.SKELETON_ALLY_SPAWNER);
		BlockPos pos = h.absolutePos(new BlockPos(4, 1, 4));
		if (immediate) AllySpawnerData.get(h.getLevel()).source(pos).readyAt = h.getLevel().getGameTime();
		return pos;
	}
	private SkeletonAlly ally(GameTestHelper h, BlockPos pos) {
		var source = AllySpawnerData.get(h.getLevel()).source(pos);
		h.assertTrue(source != null && source.entity() != null, "Skeleton source must have one occupied slot");
		return (SkeletonAlly) h.getLevel().getEntity(source.entity());
	}

	@GameTest(maxTicks = 2900, padding = 32)
	public void skeletonTimersCapAndMixedOwnership(GameTestHelper h) {
		BlockPos pos = place(h, false);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		var source = data.source(pos);
		h.assertTrue(source.kind == SpawnerKind.SKELETON_ALLY && source.readyAt == level.getGameTime() + 900,
			"Skeleton must use the same forty-five-second initial wait");
		BlockPos zombiePos = pos.offset(3, 0, 0);
		level.setBlockAndUpdate(zombiePos, ModContent.ZOMBIE_ALLY_SPAWNER.defaultBlockState());
		h.runAtTickTime(890, () -> h.assertTrue(source.entity() == null, "No early initial spawn"));
		h.runAtTickTime(940, () -> {
			SkeletonAlly skeleton = ally(h, pos);
			h.assertTrue(skeleton.getMainHandItem().is(Items.BOW) && skeleton.getMaxHealth() == 20
				&& skeleton.getAttributeValue(Attributes.MOVEMENT_SPEED) == .25
				&& skeleton.getAttributeValue(Attributes.ARMOR) == 0, "Expected vanilla normal skeleton equipment/stats");
			h.assertTrue(data.source(zombiePos).entity() != null, "Nearby zombie source must independently own a zombie");
			skeleton.kill(level);
			h.assertTrue(source.entity() == null && source.readyAt == level.getGameTime() + 1800,
				"Skeleton death must start ninety-second replacement wait");
			var restored = AllySpawnerData.read(data.write());
			h.assertTrue(restored.source(pos).kind == SpawnerKind.SKELETON_ALLY
				&& restored.source(pos).readyAt == source.readyAt, "Saved kind/deadline must survive");
		});
		h.runAtTickTime(2730, () -> h.assertTrue(source.entity() == null, "No early replacement"));
		h.runAtTickTime(2780, () -> {
			SkeletonAlly skeleton = ally(h, pos);
			h.assertTrue(level.getEntitiesOfClass(SkeletonAlly.class, skeleton.getBoundingBox().inflate(6),
				e -> e.isAlive() && data.owner(e.getUUID()) == source).size() == 1, "Exactly one skeleton per source");
			var zombie = level.getEntity(data.source(zombiePos).entity());
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			h.assertTrue(!skeleton.isAlive() && zombie.isAlive(), "Source destruction must not affect the nearby zombie");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 400, padding = 32)
	public void actualBowCombatAndFriendlyArrowFiltering(GameTestHelper h) {
		BlockPos pos = place(h, true);
		h.runAtTickTime(30, () -> {
			SkeletonAlly skeleton = ally(h, pos);
			var level = h.getLevel();
			var player = h.makeMockPlayer(GameType.SURVIVAL);
			var zombieAlly = ModContent.ZOMBIE_ALLY.create(level, EntitySpawnReason.COMMAND);
			var skeletonAlly = ModContent.SKELETON_ALLY.create(level, EntitySpawnReason.COMMAND);
			var creeper = EntityType.CREEPER.create(level, EntitySpawnReason.COMMAND);
			var cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
			var golem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
			var enemy = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			for (var e : List.of(player, zombieAlly, skeletonAlly, creeper, cow, golem, enemy))
				e.snapTo(pos.getX() + 3.5, pos.getY(), pos.getZ() + .5, 0, 0);
			ProbeArrow arrow = new ProbeArrow(level, skeleton);
			for (var friend : List.of(player, zombieAlly, skeletonAlly, creeper, cow, golem)) {
				h.assertTrue(!skeleton.canAttack(friend) && !arrow.canHit(friend), "Targets and stray arrows must exclude friendly/passive mobs and Creepers");
			}
			h.assertTrue(!zombieAlly.canAttack(skeleton) && !golem.canAttack(skeleton), "All defender types and golems must remain friendly");
			h.assertTrue(skeleton.canAttack(enemy) && arrow.canHit(enemy), "Hostile zombie must remain a valid attack/arrow target");
			skeleton.startFreezeConversion(0);
			h.assertTrue(!skeleton.isFreezeConverting(), "Skeleton must not convert into a hostile Stray");
			enemy.setNoAi(true);
			enemy.setPersistenceRequired();
			enemy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			level.addFreshEntity(enemy);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(
				enemy.getHealth() < enemy.getMaxHealth() || level.getEntitiesOfClass(AbstractArrow.class,
					skeleton.getBoundingBox().inflate(12), a -> a.getOwner() == skeleton).size() > 0,
				"Vanilla ranged AI must acquire the enemy and fire real arrows")).thenExecute(() -> {
					enemy.discard();
					skeleton.setTarget(null);
				}).thenSucceed();
		});
	}

	@GameTest(maxTicks = 500, padding = 32)
	public void skeletonMenuPatrolRecallAndStanding(GameTestHelper h) {
		BlockPos pos = place(h, true);
		var level = h.getLevel();
		var player = h.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(Vec3.atBottomCenterOf(pos.above()), 0, 0);
		var menu = new AllySpawnerMenu(1, player.getInventory(), ContainerLevelAccess.create(level, pos));
		h.assertTrue(menu.stillValid(player) && menu.standing() && menu.clickMenuButton(player, 0) && !menu.standing(),
			"Skeleton block must share the working Standing/Wander menu");
		h.runAtTickTime(30, () -> {
			SkeletonAlly skeleton = ally(h, pos);
			Vec3 start = skeleton.position();
			h.startSequence().thenWaitUntil(() -> h.assertTrue(skeleton.position().distanceToSqr(start) > 2.25,
				"Skeleton Wander must actually patrol")).thenExecute(() -> {
					skeleton.setNoAi(true);
					skeleton.setNoGravity(true);
					skeleton.teleportTo(pos.getX() + 13.5, pos.getY() + 1, pos.getZ() + .5);
				}).thenIdle(3).thenExecute(() -> {
					h.assertTrue(skeleton.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
						"Skeleton must recall onto its block beyond twelve blocks");
					menu.clickMenuButton(player, 0);
					skeleton.setNoAi(false);
					skeleton.setNoGravity(false);
					skeleton.teleportTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5);
				}).thenWaitUntil(() -> h.assertTrue(skeleton.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
					"Standing must return the same skeleton to its post")).thenSucceed();
		});
	}

	@GameTest(padding = 24)
	public void skeletonRecipeRequiresNormalArrows(GameTestHelper h) {
		List<ItemStack> ingredients = new ArrayList<>();
		for (String row : List.of("BAB", "ACA", "BAB")) for (char c : row.toCharArray())
			ingredients.add(new ItemStack(c == 'B' ? Items.BONE : c == 'A' ? Items.ARROW : ModContent.COPPER_REINFORCED_PLANKS_ITEM));
		var input = CraftingInput.of(3, 3, ingredients);
		var recipe = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("skeleton_ally_spawner"))
			&& recipe.value().assemble(input).is(ModContent.SKELETON_ALLY_SPAWNER_ITEM), "Requested skeleton recipe must craft the spawner");
		ingredients.set(1, new ItemStack(Items.SPECTRAL_ARROW));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), h.getLevel()), "Spectral arrows must not replace normal arrows");
		ingredients.set(1, new ItemStack(Items.TIPPED_ARROW));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), h.getLevel()), "Tipped arrows must not replace normal arrows");
		h.succeed();
	}

	@GameTest(maxTicks = 220, padding = 32, skyAccess = true)
	public void skeletonSunSafetyAndUnloadedBreakCleanup(GameTestHelper h) {
		BlockPos pos = place(h, true);
		h.setTime(6000);
		h.runAtTickTime(30, () -> {
			SkeletonAlly skeleton = ally(h, pos);
			h.assertTrue(skeleton.is(net.minecraft.tags.EntityTypeTags.BURN_IN_DAYLIGHT)
				&& h.getLevel().canSeeSky(skeleton.blockPosition())
				&& h.getLevel().environmentAttributes().getValue(
					net.minecraft.world.attribute.EnvironmentAttributes.MONSTERS_BURN, skeleton.position()),
				"Test must exercise real direct-daylight conditions");
			skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			h.runAtTickTime(100, () -> {
				h.assertTrue(skeleton.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 0, "Sunlight must not wear skeleton helmets");
				skeleton.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			});
			h.runAtTickTime(200, () -> {
				h.assertTrue(!skeleton.isOnFire() && skeleton.getHealth() == 20, "Skeleton ally must survive open sunlight");
				var saved = new AllySpawnerData();
				saved.register(pos, 100, SpawnerKind.SKELETON_ALLY);
				UUID unloaded = UUID.randomUUID();
				saved.occupy(saved.source(pos), unloaded);
				saved.unregister(h.getLevel(), pos);
				var restored = AllySpawnerData.read(saved.write());
				h.assertTrue(restored.consumePendingDeath(unloaded) && !restored.consumePendingDeath(unloaded),
					"Unloaded skeleton break cleanup must persist exactly once");
				h.succeed();
			});
		});
	}

	@GameTest(padding = 24)
	public void legacyZombieIdsDecodeToCanonicalIdsAndOldOwnershipSurvives(GameTestHelper h) {
		var old = ModContent.id("zombie_spawner");
		var canonical = ModContent.id("zombie_ally_spawner");
		h.assertTrue(BuiltInRegistries.BLOCK.getValue(old) == ModContent.ZOMBIE_ALLY_SPAWNER
			&& !BuiltInRegistries.BLOCK.keySet().contains(old)
			&& BuiltInRegistries.BLOCK.getKey(ModContent.ZOMBIE_ALLY_SPAWNER).equals(canonical), "Old lookup must resolve without registering an old block");
		var state = BlockState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
			"{\"Name\":\"mobdefenders:zombie_spawner\",\"Properties\":{\"facing\":\"east\"}}")).getOrThrow();
		h.assertTrue(state.is(ModContent.ZOMBIE_ALLY_SPAWNER), "Old saved blockstates must decode");
		var stack = ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
			"{\"id\":\"mobdefenders:zombie_spawner\",\"count\":1}")).getOrThrow();
		h.assertTrue(stack.is(ModContent.ZOMBIE_ALLY_SPAWNER_ITEM), "Old saved inventory stacks must decode");
		BlockPos pos = place(h, false);
		var data = new AllySpawnerData();
		data.register(pos, 1234);
		UUID uuid = UUID.randomUUID();
		data.occupy(data.source(pos), uuid);
		var saved = data.write();
		saved.getListOrEmpty("sources").getCompoundOrEmpty(0).remove("kind");
		var restored = AllySpawnerData.read(saved);
		h.assertTrue(restored.source(pos).kind == SpawnerKind.ZOMBIE_ALLY && restored.owner(uuid) != null
			&& restored.source(pos).readyAt == 2134, "Pre-skeleton saves must retain zombie ownership/deadlines");
		h.succeed();
	}

	@GameTest(maxTicks = 100, padding = 32)
	public void replacingSpawnerKindKillsOldAllyAndStartsFreshWait(GameTestHelper h) {
		BlockPos pos = place(h, true);
		h.runAtTickTime(30, () -> {
			SkeletonAlly skeleton = ally(h, pos);
			var level = h.getLevel();
			var data = AllySpawnerData.get(level);
			level.setBlockAndUpdate(pos, ModContent.ZOMBIE_ALLY_SPAWNER.defaultBlockState());
			h.assertTrue(!skeleton.isAlive() && data.owner(skeleton.getUUID()) == null, "Replacement must retire the old kind's ally");
			h.assertTrue(data.source(pos) != null && data.source(pos).kind == SpawnerKind.ZOMBIE_ALLY
				&& data.source(pos).readyAt == level.getGameTime() + 900, "New kind must start its own initial wait");
			level.setBlockAndUpdate(pos, ModContent.SKELETON_ALLY_SPAWNER.defaultBlockState());
			h.assertTrue(data.source(pos) != null && data.source(pos).kind == SpawnerKind.SKELETON_ALLY
				&& data.source(pos).entity() == null && data.source(pos).readyAt == level.getGameTime() + 900,
				"Switching back must retain a new inactive source, not an old UUID/deadline");
			h.succeed();
		});
	}

	private static class ProbeArrow extends Arrow {
		ProbeArrow(Level level, LivingEntity owner) { super(level, owner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW)); }
		boolean canHit(Entity target) { return super.canHitEntity(target); }
	}
}
