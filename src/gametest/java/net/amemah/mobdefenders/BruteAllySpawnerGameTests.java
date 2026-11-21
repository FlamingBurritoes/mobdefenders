package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.BruteAlly;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;

public class BruteAllySpawnerGameTests {
	@GameTest(maxTicks = 600, padding = 40, skyAccess = true)
	public void sharedLifecycleAndGoldenAxeCombat(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		BlockPos relative = new BlockPos(3, 1, 3);
		h.setBlock(relative, ModContent.BRUTE_ALLY_SPAWNER);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		var source = data.source(h.absolutePos(relative));
		var settings = AllySpawnerConfig.get(SpawnerKind.BRUTE_ALLY);
		h.assertTrue(source.kind == SpawnerKind.BRUTE_ALLY && source.standing && source.entities().isEmpty()
			&& source.readyAt == level.getGameTime() + settings.initialDelayTicks(), "Brute must use the shared initial wait and mode");
		h.runAtTickTime(25, () -> {
			h.assertTrue(source.entities().isEmpty(), "Do not spawn before the deadline");
			source.readyAt = level.getGameTime();
		});
		h.runAtTickTime(50, () -> {
			h.assertTrue(source.entities().size() == 1, "Shared single-ally cap");
			var ally = (BruteAlly) level.getEntity(source.entity());
			var post = Vec3.atBottomCenterOf(source.pos.above());
			h.assertTrue(ally.position().distanceToSqr(post) < .1 && ally.getHealth() == 50 && !ally.isOnFire(),
				"Standing Brute should spawn on the post with vanilla health and sunlight immunity");
			h.assertTrue(ally.getMainHandItem().is(Items.GOLDEN_AXE)
				&& ally.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 7
				&& ally.getAttributeValue(Attributes.ATTACK_DAMAGE) == 13
				&& Math.abs(ally.getAttributeValue(Attributes.MOVEMENT_SPEED) - .35) < .00001,
				"Expected vanilla Brute stats with its axe equipped");
			var player = GameTestPlayers.spawnSurvival(level, h.absoluteVec(new Vec3(1.5, 1, 3.5)));
			h.assertTrue(ally.hurtServer(level, level.damageSources().playerAttack(player), 1), "Fixture should land a player hit");
			ally.setTarget(player);
			h.assertTrue(!ally.canAttack(player) && ally.getTarget() == null, "Do not retaliate against players");
			player.discard();
			data.toggle(source.pos);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(
				ally.position().subtract(post).horizontalDistanceSqr() > 2.25, "Same ally should patrol in Wander"))
				.thenExecute(() -> {
					data.toggle(source.pos);
					var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(5.5, 1, 3.5));
					enemy.setPersistenceRequired();
					enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
					enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
					enemy.setHealth(40);
					h.startSequence().thenWaitUntil(() -> h.assertTrue(enemy.getHealth() <= 27, "Brute should land its axe attack"))
						.thenExecute(enemy::discard)
						.thenWaitUntil(() -> h.assertTrue(ally.position().distanceToSqr(post) < .1,
							"Standing should return the Brute to the source after combat"))
						.thenExecute(() -> {
							ally.kill(level);
							h.assertTrue(source.entities().isEmpty() && source.readyAt == level.getGameTime() + settings.delayTicks(),
								"Shared replacement wait");
						}).thenSucceed();
				});
		});
	}

	@GameTest(maxTicks = 450, padding = 36, skyAccess = true)
	public void survivesLavaFireAndOverworldZombification(GameTestHelper h) {
		var level = h.getLevel();
		for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
			h.setBlock(new BlockPos(x, 0, z), Blocks.NETHERRACK);
			if (x != 3 || z != 3) h.setBlock(new BlockPos(x, 1, z), Blocks.NETHERRACK);
		}
		h.setBlock(new BlockPos(3, 1, 3), Blocks.LAVA);
		// Prevent the shared float goal from visibly leaving the lava; unlike NoAI, this still ticks conversion.
		var ally = h.spawnWithNoFreeWill(ModContent.BRUTE_ALLY, new Vec3(3.5, 1, 3.5));
		ally.setNoGravity(true);
		ally.setImmuneToZombification(false);
		ally.setTimeInOverworld(400);
		h.assertTrue(level.environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, ally.position()),
			"Fixture must be in a dimension where normal brutes convert");
		var damage = level.damageSources();
		for (var source : new net.minecraft.world.damagesource.DamageSource[]{damage.lava(), damage.inFire(), damage.onFire(), damage.hotFloor()})
			h.assertTrue(!ally.hurtServer(level, source, 4), "Fire and lava damage must be ignored: " + source);
		h.runAtTickTime(20, () -> h.assertTrue(!ally.isNoAi() && ally.isInLava() && !ally.isConverting(),
			"Brute must be submerged with conversion ticks enabled"));
		h.runAtTickTime(360, () -> {
			h.assertTrue(ally.isAlive() && !ally.isConverting(), "Guard must stay unconverted in the Overworld");
			h.assertTrue(ally.isInLava() && !ally.isOnFire() && ally.getHealth() == 50,
				"Guard must survive prolonged lava: pos=" + ally.position() + ", health=" + ally.getHealth()
					+ ", on fire=" + ally.isOnFire());
			h.assertTrue(ally.hurtServer(level, damage.generic(), 1), "Fire immunity must not block unrelated damage");
			h.succeed();
		});
	}

	@GameTest(padding = 24)
	public void recipesRequireTheExactIngredients(GameTestHelper h) {
		var nugget = Items.GOLD_NUGGET;
		checkRecipe(h, "gold_reinforced_nether_brick", ModContent.GOLD_REINFORCED_NETHER_BRICK_ITEM,
			4, Items.NETHER_BRICKS, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT,
			Items.NETHER_BRICK, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT);
		checkRecipe(h, "brute_ally_spawner", ModContent.BRUTE_ALLY_SPAWNER_ITEM,
			1, Items.IRON_AXE, nugget, Items.GOLDEN_AXE, nugget, nugget, ModContent.GOLD_REINFORCED_NETHER_BRICK_ITEM,
			nugget, nugget, Items.GOLD_BLOCK, nugget);
		h.succeed();
	}

	@GameTest
	public void allModBlocksUseOakOrNetherBrickSounds(GameTestHelper h) {
		var oak = Blocks.OAK_PLANKS.defaultBlockState();
		var nether = Blocks.NETHER_BRICKS.defaultBlockState();
		for (var kind : SpawnerKind.values())
			checkBlockSounds(h, kind.block().defaultBlockState(), kind == SpawnerKind.BRUTE_ALLY ? nether : oak);
		checkBlockSounds(h, ModContent.DROWNED_ALLY_SPAWNER.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), oak);
		checkBlockSounds(h, ModContent.COPPER_REINFORCED_PLANKS.defaultBlockState(), oak);
		checkBlockSounds(h, ModContent.IRON_REINFORCED_PLANKS.defaultBlockState(), oak);
		checkBlockSounds(h, ModContent.GOLD_REINFORCED_NETHER_BRICK.defaultBlockState(), nether);
		h.succeed();
	}

	private void checkBlockSounds(GameTestHelper h, BlockState block, BlockState reference) {
		var actual = block.getSoundType();
		var expected = reference.getSoundType();
		h.assertTrue(actual.getPlaceSound() == expected.getPlaceSound() && actual.getBreakSound() == expected.getBreakSound(),
			block + " must use the placement and break sounds of " + reference);
	}

	private void checkRecipe(GameTestHelper h, String id, Item output, int changedSlot, Item incorrect, Item... ingredients) {
		var grid = new ArrayList<>(Arrays.stream(ingredients).map(ItemStack::new).toList());
		var level = h.getLevel();
		var input = CraftingInput.of(3, 3, grid);
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id(id)) && result.is(output) && result.getCount() == 1,
			"Expected one " + id);
		grid.set(changedSlot, new ItemStack(incorrect));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Wrong ingredient must not match " + id);
	}
}
