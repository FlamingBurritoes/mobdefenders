package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.BlazeAlly;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.AllySpawnerManager;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class BlazeAllySpawnerGameTests {
	private AllySpawnerData.Source place(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var pos = new BlockPos(3, 1, 3);
		h.setBlock(pos, ModContent.BLAZE_ALLY_SPAWNER);
		var level = h.getLevel();
		var source = AllySpawnerData.get(level).source(h.absolutePos(pos));
		h.assertTrue(source.kind == SpawnerKind.BLAZE_ALLY && source.standing && source.entities().isEmpty()
			&& source.readyAt == level.getGameTime() + AllySpawnerConfig.get(source.kind).initialDelayTicks(),
			"Shared Standing and initial deadline");
		source.readyAt = level.getGameTime();
		return source;
	}

	private BlazeAlly ally(GameTestHelper h, AllySpawnerData.Source source) {
		h.assertTrue(source.entities().size() == 1, "Expected one owned Blaze Ally");
		var mob = h.getLevel().getEntity(source.entity());
		h.assertTrue(mob instanceof BlazeAlly, "Wrong ally type");
		return (BlazeAlly) mob;
	}

	@GameTest(maxTicks = 550, padding = 40, skyAccess = true)
	public void fliesInWanderThenReturnsToStandingPost(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		var post = Vec3.atBottomCenterOf(source.pos.above());
		h.runAtTickTime(30, () -> {
			var blaze = ally(h, source);
			h.assertTrue(blaze.position().distanceToSqr(post) < .1 && blaze.getHealth() == 20
				&& blaze.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6
				&& Math.abs(blaze.getAttributeValue(Attributes.MOVEMENT_SPEED) - .23) < .00001, "Shared spawn and normal stats");
			data.toggle(source.pos);
			h.startSequence().thenWaitUntil(() -> {
				var pos = blaze.position();
				h.assertTrue(pos.y >= source.pos.getY() && pos.y <= source.pos.getY() + 7, "Idle flight must stay in the vertical band");
				h.assertTrue(AllySpawnerManager.insideHorizontal(source.pos, pos, 6) && pos.y - post.y > .75,
					"Wander must fly above the post within patrol; pos=" + pos);
			}).thenExecute(() -> data.toggle(source.pos)).thenWaitUntil(() ->
				h.assertTrue(blaze.position().distanceToSqr(post) < .1, "Standing must fly back to the post; pos=" + blaze.position())
			).thenExecute(() -> {
				blaze.kill(level);
				h.assertTrue(source.entities().isEmpty() && source.readyAt == level.getGameTime()
					+ AllySpawnerConfig.get(source.kind).delayTicks(), "Shared replacement deadline");
			}).thenSucceed();
		});
	}

	@GameTest(maxTicks = 80, padding = 36, skyAccess = true)
	public void idleInertiaStopsAtPatrolCeilingAndFloor(GameTestHelper h) {
		var source = place(h);
		h.runAtTickTime(30, () -> {
			var blaze = ally(h, source);
			AllySpawnerData.get(h.getLevel()).toggle(source.pos);
			var start = Vec3.atBottomCenterOf(source.pos.offset(2, 0, 0));
			for (int sign : new int[]{1, -1}) {
				double limit = start.y + (sign > 0 ? 7 : 0);
				blaze.snapTo(start.x, limit - sign * .2, start.z, 0, 0);
				blaze.move(MoverType.SELF, new Vec3(0, sign * 2, 0));
				h.assertTrue(Math.abs(blaze.getY() - limit) < .01, "Idle movement stops at the flight band boundary");
			}
			h.succeed();
		});
	}

	@GameTest(maxTicks = 440, padding = 36, skyAccess = true)
	public void waterSnowballsAndSplashWaterDoNotHurt(GameTestHelper h) {
		var level = h.getLevel();
		for (int x = 2; x <= 5; x++) for (int z = 2; z <= 4; z++) for (int y = 1; y <= 4; y++)
			h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
		var blaze = h.spawnWithNoFreeWill(ModContent.BLAZE_ALLY, new Vec3(3.5, 1, 3.5));
		var plain = h.spawnWithNoFreeWill(EntityType.BLAZE, new Vec3(4.5, 1, 3.5));
		plain.setNoGravity(true);
		plain.setPersistenceRequired();
		h.assertTrue(!blaze.isSensitiveToWater() && plain.isSensitiveToWater(), "Rain and water use the same vanilla sensitivity");
		var snowball = new Snowball(level, blaze.getX(), blaze.getY(), blaze.getZ(), new ItemStack(Items.SNOWBALL)) {
			void hit(BlazeAlly target) { onHitEntity(new EntityHitResult(target)); }
		};
		snowball.hit(blaze);
		var water = new ThrownSplashPotion(level, blaze.getX(), blaze.getY(), blaze.getZ(),
			PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)) {
			void splash(BlazeAlly target) { onHit(new EntityHitResult(target)); }
		};
		water.splash(blaze);
		h.assertTrue(blaze.getHealth() == 20 && plain.getHealth() < plain.getMaxHealth(),
			"Snowball and actual water splash must not hurt the ally; vanilla control takes splash damage");
		plain.discard();
		h.runAtTickTime(380, () -> {
			h.assertTrue(blaze.isUnderWater() && blaze.isAlive() && blaze.getHealth() == 20, "Neither water contact nor oxygen loss hurts the ally");
			h.assertTrue(blaze.hurtServer(level, level.damageSources().generic(), 1), "Unrelated damage remains enabled");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 210, padding = 40, skyAccess = true)
	public void firesThreeShotBurstAtHostiles(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		var found = new HashSet<UUID>();
		h.onEachTick(() -> {
			if (h.getTick() < 35) return;
			var blaze = ally(h, source);
			level.getEntitiesOfClass(SmallFireball.class, blaze.getBoundingBox().inflate(8),
				f -> f.getOwner() == blaze).forEach(shot -> found.add(shot.getUUID()));
		});
		h.runAtTickTime(35, () -> {
			var blaze = ally(h, source);
			var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.5, 2, 3.5));
			enemy.setNoGravity(true);
			enemy.setPersistenceRequired();
			blaze.setTarget(enemy);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(found.size() == 3, "Expected a three-fireball burst"))
				.thenExecute(enemy::discard).thenSucceed();
		});
	}

	@GameTest(padding = 28)
	public void allyFireballDoesNotIgniteBlockOnImpact(GameTestHelper h) {
		var blaze = h.spawnWithNoFreeWill(ModContent.BLAZE_ALLY, new Vec3(2.5, 1, 3.5));
		var level = h.getLevel();
		var relative = new BlockPos(4, 1, 3);
		h.setBlock(relative, Blocks.NETHERRACK);
		var pos = h.absolutePos(relative);
		var fireball = new SmallFireball(level, blaze, new Vec3(1, 0, 0)) {
			void hit(BlockHitResult impact) { onHit(impact); }
		};
		fireball.hit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		h.assertTrue(fireball.isRemoved() && level.getBlockState(pos.above()).isAir(), "Impact must consume the fireball without starting a fire");
		h.succeed();
	}

	@GameTest(padding = 24)
	public void recipeUsesEightBlazeRodsAndGoldBrick(GameTestHelper h) {
		var grid = new ArrayList<ItemStack>();
		for (int i = 0; i < 9; i++) grid.add(new ItemStack(i == 4 ? ModContent.GOLD_REINFORCED_NETHER_BRICK_ITEM : Items.BLAZE_ROD));
		var input = CraftingInput.of(3, 3, grid);
		var level = h.getLevel();
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("blaze_ally_spawner"))
			&& result.is(ModContent.BLAZE_ALLY_SPAWNER_ITEM) && result.getCount() == 1, "Exact Blaze Spawner recipe");
		grid.set(4, new ItemStack(Items.NETHER_BRICKS));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Center must be gold reinforced");
		h.succeed();
	}
}
