package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.WitherSkeletonAlly;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class WitherSkeletonAllySpawnerGameTests {
	@GameTest(maxTicks = 400, padding = 40, skyAccess = true)
	public void standingGuardWithersEnemiesWithItsStoneSword(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var relative = new BlockPos(3, 1, 3);
		h.setBlock(relative, ModContent.WITHER_SKELETON_ALLY_SPAWNER);
		h.setTime(6000);
		var level = h.getLevel();
		var source = AllySpawnerData.get(level).source(h.absolutePos(relative));
		var settings = AllySpawnerConfig.get(SpawnerKind.WITHER_SKELETON_ALLY);
		h.assertTrue(source.kind == SpawnerKind.WITHER_SKELETON_ALLY && source.standing && source.entities().isEmpty()
			&& source.readyAt == level.getGameTime() + settings.initialDelayTicks(), "Shared Standing mode and initial wait");
		source.readyAt = level.getGameTime();
		h.runAtTickTime(30, () -> {
			h.assertTrue(source.entities().size() == 1, "Expected one owned Wither Skeleton Ally");
			var ally = (WitherSkeletonAlly) level.getEntity(source.entity());
			var post = Vec3.atBottomCenterOf(source.pos.above());
			h.assertTrue(ally.position().distanceToSqr(post) < .1 && ally.getHealth() == 20 && !ally.isOnFire(),
				"Expected the Standing post, vanilla health and sunlight safety");
			h.assertTrue(ally.getMainHandItem().is(Items.STONE_SWORD) && ally.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 4
				&& ally.getAttributeValue(Attributes.ATTACK_DAMAGE) == 8
				&& ally.getAttributeValue(Attributes.MOVEMENT_SPEED) == .25, "Expected vanilla Normal sword damage and movement");

			var friendly = h.spawnWithNoFreeWill(ModContent.BRUTE_ALLY, new Vec3(2.5, 1, 3.5));
			ally.setTarget(friendly);
			h.assertTrue(ally.getTarget() == null && !ally.doHurtTarget(level, friendly)
				&& !friendly.hasEffect(MobEffects.WITHER) && friendly.getHealth() == friendly.getMaxHealth(),
				"Even a direct attack call cannot hit or wither an allied piglin");
			friendly.discard();
			var player = GameTestPlayers.spawnSurvival(level, h.absoluteVec(new Vec3(2.5, 1, 3.5)));
			h.assertTrue(!ally.doHurtTarget(level, player) && !player.hasEffect(MobEffects.WITHER), "Never wither a player");
			player.discard();

			var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(5.5, 1, 3.5));
			enemy.setPersistenceRequired();
			enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
			h.startSequence().thenWaitUntil(() -> {
				var wither = enemy.getEffect(MobEffects.WITHER);
				// Depending on entity tick order, Wither may already have dealt its first point of damage.
				h.assertTrue(wither != null && wither.getAmplifier() == 0 && wither.getDuration() >= 198
					&& (enemy.getHealth() == 12 || enemy.getHealth() == 11),
					"A landed sword hit must deal eight damage and apply ten seconds of Wither I");
			}).thenExecute(enemy::discard)
				.thenWaitUntil(() -> h.assertTrue(ally.position().distanceToSqr(post) < .1, "Return to the Standing post"))
				.thenExecute(() -> {
					ally.kill(level);
					h.assertTrue(source.entities().isEmpty() && source.readyAt == level.getGameTime() + settings.delayTicks(),
						"Shared replacement wait");
				}).thenSucceed();
		});
	}

	@GameTest(padding = 24)
	public void recipeRequiresBonesCoalAndGoldBrick(GameTestHelper h) {
		var grid = new ArrayList<ItemStack>();
		for (char key : "BCBCGCBCB".toCharArray()) grid.add(new ItemStack(switch (key) {
			case 'B' -> Items.BONE;
			case 'C' -> Items.COAL;
			default -> ModContent.GOLD_REINFORCED_NETHER_BRICK_ITEM;
		}));
		var level = h.getLevel();
		var input = CraftingInput.of(3, 3, grid);
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("wither_skeleton_ally_spawner"))
			&& result.is(ModContent.WITHER_SKELETON_ALLY_SPAWNER_ITEM) && result.getCount() == 1, "Exact Wither Skeleton recipe");
		grid.set(1, new ItemStack(Items.CHARCOAL));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Recipe requires coal, not charcoal");
		h.succeed();
	}
}
