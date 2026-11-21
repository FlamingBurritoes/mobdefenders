package net.amemah.mobdefenders.entity;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MagmaCubeAllyGameTests {
	private AllySpawnerData.Source place(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var pos = new BlockPos(3, 1, 3);
		h.setBlock(pos, ModContent.MAGMA_CUBE_ALLY_SPAWNER);
		var level = h.getLevel();
		var source = AllySpawnerData.get(level).source(h.absolutePos(pos));
		h.assertTrue(source.kind == SpawnerKind.MAGMA_CUBE_ALLY && source.standing && source.readyAt == level.getGameTime()
			+ AllySpawnerConfig.get(source.kind).initialDelayTicks(), "Shared Standing mode and initial deadline");
		source.readyAt = level.getGameTime();
		return source;
	}

	private List<MagmaCubeAlly> family(GameTestHelper h, AllySpawnerData.Source source, int count, int size, float hp, double damage) {
		h.assertTrue(source.entities().size() == count, "Wrong roster size");
		var members = new ArrayList<MagmaCubeAlly>();
		for (var id : source.entities()) {
			var cube = (MagmaCubeAlly) h.getLevel().getEntity(id);
			h.assertTrue(cube != null && cube.isAlive() && cube.getSize() == size && cube.getMaxHealth() == hp
				&& cube.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage, "Wrong tier or stats");
			members.add(cube);
		}
		return members;
	}

	@GameTest(maxTicks = 160, padding = 36)
	public void splitsIntoTwoMediumThenFourSmallAndSavesRoster(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		h.runAtTickTime(30, () -> {
			long deadline = source.readyAt;
			var large = family(h, source, 1, 4, 22, 8).getFirst();
			h.assertTrue(large.fireImmune(), "Native magma fire immunity");
			large.kill(level);
			family(h, source, 2, 2, 12, 5).forEach(m -> m.kill(level));
			var small = family(h, source, 4, 1, 7, 2.5);
			var restored = AllySpawnerData.read(AllySpawnerData.get(level).write()).source(source.pos);
			h.assertTrue(restored.entities().equals(source.entities()) && restored.readyAt == deadline,
				"All four owners persist and no split schedules replacement");
			for (int i = 0; i < 3; i++) small.get(i).kill(level);
			h.assertTrue(source.entities().size() == 1 && source.readyAt == deadline, "Last survivor blocks replacement");
			small.getLast().kill(level);
			h.assertTrue(source.entities().isEmpty() && source.readyAt == level.getGameTime()
				+ AllySpawnerConfig.get(source.kind).delayTicks(), "Only final death starts replacement");
		});
		h.runAtTickTime(100, () -> {
			h.assertTrue(level.getEntitiesOfClass(MagmaCubeAlly.class, new net.minecraft.world.phys.AABB(source.pos).inflate(4)).isEmpty(),
				"No extra vanilla splits or early replacement");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 680, padding = 36)
	public void mergesMixedTiersAfterFullQuietWindow(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		var old = new ArrayList<MagmaCubeAlly>();
		h.runAtTickTime(30, () -> {
			family(h, source, 1, 4, 22, 8).getFirst().kill(level);
			var medium = family(h, source, 2, 2, 12, 5);
			medium.getFirst().kill(level);
			for (var id : source.entities()) {
				var cube = (MagmaCubeAlly) level.getEntity(id);
				cube.setNoAi(true);
				if (cube.getSize() == 1) cube.setHealth(3.5F);
				old.add(cube);
			}
			var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.5, 1, 3.5));
			enemy.setPersistenceRequired();
			h.runAtTickTime(150, enemy::discard);
		});
		h.runAtTickTime(530, () -> h.assertTrue(source.entities().size() == 3, "Enemy detection delays the merge"));
		h.runAtTickTime(590, () -> {
			var merged = family(h, source, 1, 4, 22, 8).getFirst();
			h.assertTrue(merged.getHealth() == 16.5F && old.stream().allMatch(Entity::isRemoved),
				"A full medium and two half-health small cubes merge at 75% HP");
			h.succeed();
		});
	}

	@GameTest(padding = 28)
	public void allTiersDealExactMeleeDamageAndRollForFiveSecondBurn(GameTestHelper h) {
		var cube = h.spawnWithNoFreeWill(ModContent.MAGMA_CUBE_ALLY, new Vec3(3.5, 2, 3.5));
		var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.25, 2, 3.5));
		enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
		var cow = h.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.5, 2, 4.25));
		for (int size : new int[]{1, 2, 4}) {
			cube.setSize(size, true);
			float damage = size == 1 ? 2.5F : size == 2 ? 5 : 8;
			for (int roll : new int[]{32, 33}) {
				long seed = 0;
				while (RandomSource.create(seed).nextInt(100) != roll) seed++;
				cube.getRandom().setSeed(seed);
				enemy.setHealth(20);
				enemy.invulnerableTime = 0;
				enemy.clearFire();
				cube.dealDamage(enemy);
				h.assertTrue(enemy.getHealth() == 20 - damage, "Landed damage must match the tier");
				h.assertTrue(enemy.getRemainingFireTicks() == (roll == 32 ? 100 : 0), "33% boundary and 100-tick burn");
				enemy.clearFire();
				cube.getRandom().setSeed(seed);
				cube.dealDamage(enemy);
				h.assertTrue(!enemy.isOnFire(), "Rejected hit must not roll for fire");
			}
			cube.dealDamage(cow);
			h.assertTrue(cow.getHealth() == cow.getMaxHealth() && !cow.isOnFire(), "Never hit or ignite a passive mob");
		}
		h.succeed();
	}

	@GameTest(padding = 24)
	public void recipeUsesEightMagmaCreamAndGoldBrick(GameTestHelper h) {
		var grid = new ArrayList<ItemStack>();
		for (int i = 0; i < 9; i++)
			grid.add(new ItemStack(i == 4 ? ModContent.GOLD_REINFORCED_NETHER_BRICK_ITEM : Items.MAGMA_CREAM));
		var input = CraftingInput.of(3, 3, grid);
		var level = h.getLevel();
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("magma_cube_ally_spawner"))
			&& result.is(ModContent.MAGMA_CUBE_ALLY_SPAWNER_ITEM) && result.getCount() == 1, "Exact Magma Cube Spawner recipe");
		grid.set(4, new ItemStack(Items.NETHER_BRICKS));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Center must be gold reinforced");
		h.succeed();
	}
}
