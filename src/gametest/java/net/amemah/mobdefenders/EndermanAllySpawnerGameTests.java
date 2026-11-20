package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.EndermanAlly;
import net.amemah.mobdefenders.spawner.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EndermanAllySpawnerGameTests {
	private void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	@GameTest(maxTicks = 250, padding = 40, skyAccess = true)
	public void ignoresPlayerStareAndHitsButFightsEnemies(GameTestHelper h) {
		floor(h);
		h.setTime(6000);
		var level = h.getLevel();
		var ally = h.spawn(ModContent.ENDERMAN_ALLY, new Vec3(3.5, 1, 3.5));
		var player = GameTestPlayers.spawnSurvival(level, h.absoluteVec(new Vec3(1.5, 1, 3.5)));
		player.lookAt(EntityAnchorArgument.Anchor.EYES, ally.getEyePosition());
		h.assertTrue(level.players().contains(player) && !player.isCreative() && !player.isSpectator(),
			"Stare fixture must be visible to world queries as a survival player");
		h.runAtTickTime(40, () -> {
			h.assertTrue(!ally.isCreepy() && !ally.hasBeenStaredAt() && ally.getTarget() == null,
				"Direct eye contact from a survival player must not provoke the ally");
			h.assertTrue(ally.getHealth() == ally.getMaxHealth() && !ally.isOnFire(), "Daylight must not hurt the ally");
			h.assertTrue(ally.hurtServer(level, level.damageSources().playerAttack(player), 1),
				"Fixture must land a player hit");
			h.assertTrue(!ally.isCreepy() && !ally.canAttack(player), "Player attack must not provoke the ally");
		});
		h.runAtTickTime(65, () -> {
			h.assertTrue(!ally.isCreepy() && ally.getTarget() == null, "Do not retaliate against players");
			player.discard();
			var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(5.5, 1, 3.5));
			enemy.setPersistenceRequired();
			ally.hurtServer(level, level.damageSources().mobAttack(enemy), 2);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(ally.isCreepy() && ally.getTarget() == enemy,
				"An eligible attacker must trigger the native combat pose"))
				.thenWaitUntil(() -> h.assertTrue(enemy.getHealth() < enemy.getMaxHealth(), "Ally must land a melee hit"))
				.thenExecute(enemy::discard)
				.thenWaitUntil(() -> h.assertTrue(!ally.isCreepy() && ally.getTarget() == null,
					"Combat pose must clear when the enemy is gone")).thenSucceed();
		});
	}

	@GameTest(maxTicks = 250, padding = 32)
	public void waterAndBlockHandlingAreDisabled(GameTestHelper h) {
		floor(h);
		var level = h.getLevel();
		for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(3, y, 3), Blocks.WATER);
		var ally = h.spawnWithNoFreeWill(ModContent.ENDERMAN_ALLY, new Vec3(3.5, 1, 3.5));
		ally.setNoGravity(true);
		ally.setAirSupply(0);
		ally.setCarriedBlock(Blocks.DIRT.defaultBlockState());
		h.assertTrue(ally.getCarriedBlock() == null, "The ally must not take or carry blocks");
		var bottle = new ThrownSplashPotion(level, ally.getX(), ally.getY(), ally.getZ(),
			PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER));
		h.assertTrue(!ally.hurtServer(level, level.damageSources().indirectMagic(bottle, null), 1),
			"Clean-water splash must not hurt the ally");
		h.runAtTickTime(80, () -> {
			h.assertTrue(ally.isInWater() && !ally.isSensitiveToWater() && ally.canBreatheUnderwater(),
				"Fixture must keep the ally submerged and immune");
			h.assertTrue(ally.getHealth() == ally.getMaxHealth() && !ally.isCreepy(),
				"Submersion, drowning and clean water must not damage or enrage it");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 500, padding = 40)
	public void spawnerSharesLifecycleAndRecall(GameTestHelper h) {
		floor(h);
		var level = h.getLevel();
		BlockPos relative = new BlockPos(3, 1, 3);
		BlockPos pos = h.absolutePos(relative);
		h.setBlock(relative, ModContent.ENDERMAN_ALLY_SPAWNER);
		var data = AllySpawnerData.get(level);
		var source = data.source(pos);
		var settings = AllySpawnerConfig.get(SpawnerKind.ENDERMAN_ALLY);
		h.assertTrue(source.kind == SpawnerKind.ENDERMAN_ALLY && source.standing
			&& source.readyAt == level.getGameTime() + settings.initialDelayTicks(), "Same initial timer and Standing mode");
		source.readyAt = level.getGameTime();
		h.runAtTickTime(30, () -> {
			var ally = (EndermanAlly) level.getEntity(source.entity());
			h.assertTrue(ally != null && ally.getHealth() == 40, "Spawner owns one vanilla-stat Enderman Ally");
			var post = Vec3.atBottomCenterOf(pos.above());
			h.assertTrue(ally.position().distanceToSqr(post) < .1, "Standing spawns on the post");
			data.toggle(pos);
			var start = ally.position();
			h.startSequence().thenWaitUntil(() -> h.assertTrue(ally.position().distanceToSqr(start) > 2.25,
				"Wander must physically patrol"))
				.thenExecute(() -> {
					ally.setNoAi(true);
					ally.teleportTo(post.x, post.y + settings.recallRadius() + 2, post.z);
				}).thenIdle(3).thenExecute(() -> {
					h.assertTrue(ally.position().distanceToSqr(post) < .2, "Emergency recall must restore the post");
					ally.kill(level);
					h.assertTrue(source.entity() == null && source.readyAt == level.getGameTime() + settings.delayTicks(),
						"Death schedules the shared replacement timer");
				}).thenSucceed();
		});
	}

	@GameTest(padding = 24)
	public void ringRecipeRequiresEightPearlsAndIronPlanks(GameTestHelper h) {
		var level = h.getLevel();
		List<ItemStack> grid = new ArrayList<>();
		for (char key : "EEEEIEEEE".toCharArray()) grid.add(switch (key) {
			case 'E' -> new ItemStack(Items.ENDER_PEARL);
			case 'I' -> new ItemStack(ModContent.IRON_REINFORCED_PLANKS_ITEM);
			default -> ItemStack.EMPTY;
		});
		var input = CraftingInput.of(3, 3, grid);
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("enderman_ally_spawner"))
			&& result.is(ModContent.ENDERMAN_ALLY_SPAWNER_ITEM) && result.getCount() == 1, "Exact ring recipe");
		grid.set(0, ItemStack.EMPTY);
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Corners must contain pearls");
		grid.set(0, new ItemStack(Items.ENDER_PEARL));
		grid.set(4, new ItemStack(ModContent.COPPER_REINFORCED_PLANKS_ITEM));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Iron, not copper");
		h.succeed();
	}
}
