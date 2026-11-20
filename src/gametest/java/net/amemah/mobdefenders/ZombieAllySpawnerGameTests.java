package net.amemah.mobdefenders;

import net.amemah.mobdefenders.entity.ZombieAlly;
import net.amemah.mobdefenders.menu.AllySpawnerMenu;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.AllySpawnerManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ZombieAllySpawnerGameTests {
	private BlockPos place(GameTestHelper helper, boolean immediate) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
			helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE); // Sun protection, not a behavior change.
		}
		helper.setBlock(new BlockPos(4, 1, 4), ModContent.ZOMBIE_ALLY_SPAWNER);
		BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
		if (immediate) AllySpawnerData.get(helper.getLevel()).source(pos).readyAt = helper.getLevel().getGameTime();
		return pos;
	}
	private ZombieAlly ally(GameTestHelper helper, BlockPos pos) {
		var source = AllySpawnerData.get(helper.getLevel()).source(pos);
		helper.assertTrue(source != null && source.entity() != null, "Expected an occupied spawner");
		return (ZombieAlly) helper.getLevel().getEntity(source.entity());
	}

	@GameTest(maxTicks = 2900, padding = 16)
	public void initialDelaySingleCapAndNinetySecondReplacement(GameTestHelper helper) {
		BlockPos pos = place(helper, false);
		var data = AllySpawnerData.get(helper.getLevel());
		var source = data.source(pos);
		helper.assertTrue(source.readyAt == helper.getLevel().getGameTime() + 900, "Initial delay must be forty-five seconds");
		helper.runAtTickTime(890, () -> helper.assertTrue(source.entity() == null, "Spawned before initial deadline"));
		helper.runAtTickTime(940, () -> {
			ZombieAlly first = ally(helper, pos);
			helper.assertTrue(first.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1, "Standing spawn must be on the block");
			helper.assertTrue(!first.isBaby() && !first.canPickUpLoot(), "Spawner must produce an adult without loot pickup");
			UUID uuid = first.getUUID();
			first.kill(helper.getLevel());
			helper.assertTrue(source.entity() == null && source.readyAt == helper.getLevel().getGameTime() + 1800,
				"Death must free one slot and schedule exactly ninety seconds");
			data.death(uuid, helper.getLevel().getGameTime() + 100);
			helper.assertTrue(source.readyAt == helper.getLevel().getGameTime() + 1800, "Duplicate death must not restart timer");
		});
		helper.runAtTickTime(2730, () -> helper.assertTrue(source.entity() == null, "Respawned before full cooldown"));
		helper.runAtTickTime(2780, () -> {
			ZombieAlly replacement = ally(helper, pos);
			long count = helper.getLevel().getEntitiesOfClass(ZombieAlly.class,
				replacement.getBoundingBox().inflate(6), LivingEntity::isAlive).stream()
				.filter(e -> data.owner(e.getUUID()) == source).count();
			helper.assertTrue(count == 1, "Exactly one live entity per source");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 120, padding = 32)
	public void adultAndNoPickupEvenWhenRequestedBySpawnOrSave(GameTestHelper h) {
		BlockPos pos = place(h, true);
		var level = h.getLevel();
		h.runAtTickTime(30, () -> {
			var zombie = ally(h, pos);
			float adultHeight = zombie.getBbHeight();
			zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.COMMAND,
				new Zombie.ZombieGroupData(true, true));
			zombie.setBaby(true);
			zombie.setCanPickUpLoot(true);
			h.assertTrue(!zombie.isBaby() && !zombie.canPickUpLoot(), "Spawn and setter requests cannot enable baby or pickup");

			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			zombie.saveWithoutId(output);
			var tag = output.buildResult();
			tag.putBoolean("IsBaby", true);
			tag.putBoolean("CanPickUpLoot", true);
			zombie.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			h.assertTrue(!zombie.isBaby() && zombie.getBbHeight() == adultHeight
				&& zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) == .23,
				"Legacy baby data cannot change adult dimensions or speed");

			var sword = new ItemStack(Items.DIAMOND_SWORD);
			h.assertTrue(!zombie.canPickUpLoot() && !zombie.wantsToPickUp(level, sword), "Legacy pickup flag must be rejected");
			var drop = new ItemEntity(level, zombie.getX(), zombie.getY(), zombie.getZ(), sword, 0, 0, 0);
			drop.setNoPickUpDelay();
			level.addFreshEntity(drop);
			h.runAtTickTime(90, () -> {
				h.assertTrue(drop.isAlive() && drop.getItem().is(Items.DIAMOND_SWORD) && zombie.getMainHandItem().isEmpty(),
					"An equippable item at the zombie's feet must remain on the ground");
				h.succeed();
			});
		});
	}

	@GameTest(maxTicks = 500, padding = 16)
	public void targetsOnlyHostilesAndReturnsToGuard(GameTestHelper helper) {
		BlockPos pos = place(helper, true);
		helper.runAtTickTime(30, () -> {
			ZombieAlly ally = ally(helper, pos);
			var zombie = EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			var creeper = EntityType.CREEPER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			var cow = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			var other = ModContent.ZOMBIE_ALLY.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			var player = helper.makeMockPlayer(GameType.SURVIVAL);
			for (var entity : List.of(zombie, creeper, cow, other, player)) {
				entity.snapTo(Vec3.atBottomCenterOf(pos.above()), 0, 0);
			}
			helper.assertTrue(ally.canAttack(zombie), "Must accept a hostile zombie");
			helper.assertTrue(!ally.canAttack(creeper) && !ally.canAttack(cow) && !ally.canAttack(player) && !ally.canAttack(other),
				"Must exclude creepers, passive mobs, players and allies");
			helper.assertTrue(ally.getMaxHealth() == 20 && ally.getAttributeValue(Attributes.ATTACK_DAMAGE) == 3
				&& ally.getAttributeValue(Attributes.ARMOR) == 2, "Expected normal adult zombie stats");
			helper.assertTrue(!ally.isPreventingPlayerRest(helper.getLevel(), player), "Friendly ally must not prevent sleep");
			var golem = EntityType.IRON_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			helper.assertTrue(!golem.canAttack(ally), "Friendly golems must not attack the ally");
			golem.setTarget(ally);
			helper.assertTrue(golem.getTarget() == null, "Golem contact targeting must also exclude the ally");
			zombie.snapTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5, 0, 0);
			zombie.setNoAi(true);
			zombie.setPersistenceRequired();
			helper.getLevel().addFreshEntity(zombie);
			helper.startSequence().thenWaitUntil(() ->
				helper.assertTrue(ally.getTarget() == zombie || zombie.getHealth() < zombie.getMaxHealth(), "Must actually acquire/engage hostile mob")
			).thenExecute(() -> {
				zombie.discard();
				ally.setTarget(null);
				ally.teleportTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5);
			}).thenWaitUntil(() -> {
				helper.assertTrue(ally.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
					"Must return to block after combat; position=" + ally.position() + ", top=" + Vec3.atBottomCenterOf(pos.above())
						+ ", target=" + ally.getTarget() + ", standing=" + AllySpawnerData.get(helper.getLevel()).source(pos).standing);
			}).thenSucceed();
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void menuPersistenceRangeAndBreakCleanup(GameTestHelper helper) {
		BlockPos pos = place(helper, true);
		var level = helper.getLevel();
		var data = AllySpawnerData.get(level);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5, 0, 0);
		var menu = new AllySpawnerMenu(1, player.getInventory(), ContainerLevelAccess.create(level, pos));
		helper.assertTrue(menu.standing(), "First/default option must be Standing");
		helper.assertTrue(!menu.clickMenuButton(player, 9) && menu.standing(), "Unknown button IDs must not change mode");
		helper.assertTrue(menu.clickMenuButton(player, 0) && !menu.standing(), "Button must switch to Wander");
		var saved = AllySpawnerData.read(data.write());
		helper.assertTrue(!saved.source(pos).standing, "Guard mode must survive serialization");
		level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST));
		helper.assertTrue(!data.source(pos).standing, "State rotation must retain mode/source");
		helper.runAtTickTime(30, () -> {
			ZombieAlly ally = ally(helper, pos);
			var restoredOwned = AllySpawnerData.read(data.write());
			helper.assertTrue(restoredOwned.source(pos).entity().equals(ally.getUUID()), "Live ownership must survive save");
			helper.assertTrue(AllySpawnerManager.insideHorizontal(pos, ally.position(), 6), "Idle wandering stays near source");
			ally.setNoAi(true);
			ally.setNoGravity(true);
			ally.teleportTo(pos.getX() + 5.5, pos.getY() + 1, pos.getZ() + .5);
			ally.move(MoverType.SELF, new Vec3(3, 0, 0));
			helper.assertTrue(ally.getX() > pos.getX() + 7.5, "Movement beyond roam range must not hit an invisible wall");
			ally.teleportTo(pos.getX() + 10.5, pos.getY() + 1, pos.getZ() + .5);
			helper.runAtTickTime(35, () -> {
				helper.assertTrue(ally.getX() > pos.getX() + 9.5, "Must not recall between roam and twelve-block radius");
				ally.teleportTo(pos.getX() + 13.5, pos.getY() + 1, pos.getZ() + .5);
				helper.runAtTickTime(40, () -> {
					helper.assertTrue(ally.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
						"Beyond twelve blocks must recall to the top even in Wander mode");
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
					helper.assertTrue(data.source(pos) == null && data.owner(ally.getUUID()) == null, "Breaking must clear source and ownership");
					helper.assertTrue(!ally.isAlive() && !ally.isRemoved(), "Breaking must use a normal death, not instant discard");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(maxTicks = 500, padding = 24)
	public void wanderActuallyWalksAndSwitchingToStandingReturnsHome(GameTestHelper helper) {
		BlockPos pos = place(helper, true);
		helper.runAtTickTime(30, () -> {
			ZombieAlly ally = ally(helper, pos);
			Vec3 start = ally.position();
			AllySpawnerData.get(helper.getLevel()).toggle(pos);
			helper.startSequence().thenWaitUntil(() -> {
				helper.assertTrue(ally.position().distanceToSqr(start) > 2.25, "Wander must actually walk away from its idle post");
				helper.assertTrue(AllySpawnerManager.inside(pos, ally.position(), 6), "Wander destination must stay within roam radius");
			}).thenExecute(() -> AllySpawnerData.get(helper.getLevel()).toggle(pos))
				.thenWaitUntil(() -> helper.assertTrue(ally.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
					"Switching back to Standing must return the same ally to the block")).thenSucceed();
		});
	}

	@GameTest(maxTicks = 240, padding = 24, skyAccess = true)
	public void daylightDoesNotIgniteDefendersOrWearHelmets(GameTestHelper helper) {
		BlockPos pos = place(helper, true);
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++)
			helper.setBlock(new BlockPos(x, 4, z), Blocks.AIR);
		helper.setTime(6000);
		helper.runAtTickTime(30, () -> {
			ZombieAlly ally = ally(helper, pos);
			helper.assertTrue(ally.is(net.minecraft.tags.EntityTypeTags.BURN_IN_DAYLIGHT),
				"Test-only tag must exercise the actual shared vanilla burning path");
			helper.assertTrue(helper.getLevel().canSeeSky(ally.blockPosition()), "Test requires direct sky access");
			helper.assertTrue(helper.getLevel().environmentAttributes().getValue(
				net.minecraft.world.attribute.EnvironmentAttributes.MONSTERS_BURN, ally.position()), "Test requires burning daylight");
			ally.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			helper.runAtTickTime(100, () -> {
				helper.assertTrue(ally.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 0, "Daylight must not wear defender helmets");
				ally.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			});
			helper.runAtTickTime(200, () -> {
				helper.assertTrue(!ally.isOnFire() && ally.getHealth() == ally.getMaxHealth(), "Unprotected defender must remain unharmed in sunlight");
				ally.igniteForSeconds(5);
				helper.runAtTickTime(230, () -> {
					helper.assertTrue(ally.getHealth() < ally.getMaxHealth(), "Sun immunity must not accidentally grant ordinary fire immunity");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void obstructedTopRetriesWithoutCreatingExtras(GameTestHelper helper) {
		BlockPos pos = place(helper, true);
		helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
		helper.runAtTickTime(35, () -> {
			helper.assertTrue(AllySpawnerData.get(helper.getLevel()).source(pos).entity() == null, "Blocked top must not spawn into solids");
			helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
		});
		helper.runAtTickTime(65, () -> {
			helper.assertTrue(ally(helper, pos).isAlive(), "Must retry when the obstruction is removed");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 24)
	public void recallWaitsForSafeGroundInsteadOfTeleportingIntoSolids(GameTestHelper helper) {
		BlockPos pos = place(helper, true);
		helper.runAtTickTime(30, () -> {
			ZombieAlly ally = ally(helper, pos);
			ally.setNoAi(true);
			ally.setNoGravity(true);
			ally.teleportTo(pos.getX() + 13.5, pos.getY() + 1, pos.getZ() + .5);
			// Block the whole return search volume, including natural ground outside our small floor.
			for (int x = -2; x <= 10; x++) for (int z = -2; z <= 10; z++) for (int y = -2; y <= 5; y++) {
				if (x != 4 || y != 1 || z != 4) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
			}
			helper.runAtTickTime(35, () -> {
				helper.assertTrue(ally.getX() > pos.getX() + 12.5, "No safe return spot must defer teleport");
				helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
				helper.getLevel().setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState());
				helper.runAtTickTime(40, () -> {
					helper.assertTrue(ally.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
						"Recall must retry and use the unblocked source top");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(padding = 16)
	public void unloadedOwnershipAndPendingBreakCleanupSurviveSave(GameTestHelper helper) {
		BlockPos pos = place(helper, false);
		var level = helper.getLevel();
		var data = AllySpawnerData.get(level);
		UUID unloaded = UUID.randomUUID();
		data.occupy(data.source(pos), unloaded);
		var restored = AllySpawnerData.read(data.write());
		helper.assertTrue(restored.owner(unloaded) != null && restored.source(pos).entity().equals(unloaded), "Unloaded UUID must remain owned");
		restored.unregister(level, pos);
		var afterBreak = AllySpawnerData.read(restored.write());
		helper.assertTrue(afterBreak.consumePendingDeath(unloaded) && !afterBreak.consumePendingDeath(unloaded), "Unloaded cleanup must persist and be consumed once");
		helper.succeed();
	}

	@GameTest(padding = 16)
	public void requestedRecipes(GameTestHelper helper) {
		checkRecipe(helper, "copper_reinforced_planks", Items.COPPER_INGOT, Items.OAK_PLANKS, ModContent.COPPER_REINFORCED_PLANKS_ITEM);
		checkRecipe(helper, "zombie_ally_spawner", Items.ROTTEN_FLESH, ModContent.COPPER_REINFORCED_PLANKS_ITEM, ModContent.ZOMBIE_ALLY_SPAWNER_ITEM);
		helper.succeed();
	}
	private void checkRecipe(GameTestHelper helper, String name, Item outer, Item center, Item output) {
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < 9; i++) items.add(new ItemStack(i == 4 ? center : outer));
		var input = CraftingInput.of(3, 3, items);
		var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
		helper.assertTrue(recipe.id().identifier().equals(ModContent.id(name)), "Wrong recipe matched");
		var result = recipe.value().assemble(input);
		helper.assertTrue(result.is(output) && result.getCount() == 1, "Recipe must produce one requested block");
		items.set(4, new ItemStack(Items.DIRT));
		helper.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, items), helper.getLevel()), "Wrong center ingredient must be rejected");
	}
}
