package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.SlimeAlly;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
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
import java.util.UUID;

public class SlimeAllySpawnerGameTests {
	private AllySpawnerData.Source place(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var relative = new BlockPos(3, 1, 3);
		h.setBlock(relative, ModContent.SLIME_ALLY_SPAWNER);
		var source = AllySpawnerData.get(h.getLevel()).source(h.absolutePos(relative));
		h.assertTrue(source.readyAt == h.getLevel().getGameTime()
			+ AllySpawnerConfig.get(SpawnerKind.SLIME_ALLY).initialDelayTicks(), "Shared initial delay");
		source.readyAt = h.getLevel().getGameTime();
		return source;
	}

	private List<SlimeAlly> family(GameTestHelper h, AllySpawnerData.Source source, int count, int tier, float health, double attack) {
		h.assertTrue(source.entities().size() == count, "Expected " + count + " tracked slimes");
		var members = new ArrayList<SlimeAlly>();
		for (UUID id : source.entities()) {
			h.assertTrue(h.getLevel().getEntity(id) instanceof SlimeAlly, "Tracked child must exist");
			var slime = (SlimeAlly) h.getLevel().getEntity(id);
			h.assertTrue(slime.isAlive() && slime.getSize() == tier && slime.getMaxHealth() == health
				&& slime.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack, "Wrong slime tier or attributes");
			members.add(slime);
		}
		return members;
	}

	@GameTest(maxTicks = 350, padding = 36)
	public void fullSplitTreeHasEightOwnersAndOnlyFinalDeathStartsCooldown(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		h.runAtTickTime(30, () -> {
			long deadline = source.readyAt;
			family(h, source, 1, 4, 18, 5).getFirst().kill(level);
			family(h, source, 2, 3, 12, 3.5).forEach(m -> m.kill(level));
			family(h, source, 4, 2, 8, 2).forEach(m -> m.kill(level));
			var small = family(h, source, 8, 1, 4, 1);
			h.assertTrue(source.readyAt == deadline, "Splits must not start cooldown");
			var restored = AllySpawnerData.read(data.write());
			h.assertTrue(restored.source(source.pos).entities().equals(source.entities()), "All eight UUIDs persist");
			for (var id : source.entities()) h.assertTrue(restored.owner(id) == restored.source(source.pos), "Reverse ownership persists");
			for (int i = 0; i < 7; i++) small.get(i).kill(level);
			h.assertTrue(source.entities().size() == 1 && source.readyAt == deadline, "One living member still blocks replacement");
			small.getLast().kill(level);
			h.assertTrue(source.entities().isEmpty() && source.readyAt == level.getGameTime()
				+ AllySpawnerConfig.get(source.kind).delayTicks(), "Only last death starts cooldown");
		});
		h.runAtTickTime(100, () -> {
			h.assertTrue(source.entities().isEmpty() && level.getEntitiesOfClass(SlimeAlly.class,
				new net.minecraft.world.phys.AABB(source.pos).inflate(4)).isEmpty(), "No extra vanilla splits or early replacement");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 1250, padding = 36)
	public void enemyDetectionResetsQuietTimeAndMergeUsesHealthAndSize(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		List<SlimeAlly> children = new ArrayList<>();
		h.runAtTickTime(30, () -> {
			family(h, source, 1, 4, 18, 5).getFirst().kill(level);
			children.addAll(family(h, source, 2, 3, 12, 3.5));
			children.getLast().setHealth(6);
			children.forEach(m -> m.setNoAi(true));
		});
		h.runAtTickTime(350, () -> {
			h.assertTrue(source.entities().size() == 2, "Cannot merge before 20 seconds");
			var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.5, 1, 3.5));
			enemy.setPersistenceRequired();
			h.runAtTickTime(500, () -> {
				h.assertTrue(source.entities().size() == 2, "Enemy in detection range prevents merge");
				enemy.discard();
			});
		});
		h.runAtTickTime(880, () -> h.assertTrue(source.entities().size() == 2, "Enemy must reset the entire quiet window"));
		h.runAtTickTime(945, () -> {
			var merged = family(h, source, 1, 4, 18, 5).getFirst();
			h.assertTrue(merged.getHealth() == 13.5F && children.stream().allMatch(Entity::isRemoved),
				"Two semi-large slimes at full and half HP merge into one with 13.5 HP");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 500, padding = 36)
	public void followsWanderPathsAndEngagesHostiles(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		h.runAtTickTime(30, () -> {
			var slime = family(h, source, 1, 4, 18, 5).getFirst();
			var start = slime.position();
			AllySpawnerData.get(level).toggle(source.pos);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(slime.position().subtract(start).horizontalDistanceSqr() > 2.25,
				"Slime uses shared Wander path with hops")).thenExecute(() -> {
				var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(5.5, 1, 4.5));
				enemy.setPersistenceRequired();
				var creeper = EntityType.CREEPER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				h.assertTrue(!slime.canAttack(creeper), "Defenders must not target creepers");
				h.startSequence().thenWaitUntil(() -> h.assertTrue(enemy.getHealth() < enemy.getMaxHealth(),
					"Slime must attack a real hostile")).thenExecute(enemy::discard).thenSucceed();
			});
		});
	}

	@GameTest(maxTicks = 150, padding = 36)
	public void sourceBreakRetiresWholeFamilyWithoutSplitting(GameTestHelper h) {
		var source = place(h);
		var level = h.getLevel();
		h.runAtTickTime(30, () -> {
			family(h, source, 1, 4, 18, 5).getFirst().kill(level);
			var members = family(h, source, 2, 3, 12, 3.5);
			UUID missing = UUID.randomUUID();
			var data = AllySpawnerData.get(level);
			data.occupy(source, missing);
			level.setBlockAndUpdate(source.pos, Blocks.AIR.defaultBlockState());
			h.assertTrue(data.source(source.pos) == null && members.stream().noneMatch(Entity::isAlive), "Breaking the source retires the family");
			h.assertTrue(AllySpawnerData.read(data.write()).consumePendingDeath(missing), "Cleanup for missing UUID persists");
		});
		h.runAtTickTime(80, () -> {
			h.assertTrue(level.getEntitiesOfClass(SlimeAlly.class, new net.minecraft.world.phys.AABB(source.pos).inflate(4)).isEmpty(),
				"Retiring a family must not spawn offspring");
			h.succeed();
		});
	}

	@GameTest(padding = 24)
	public void slimeRecipeRequiresEightBallsAndIronPlanks(GameTestHelper h) {
		List<ItemStack> grid = new ArrayList<>();
		for (int i = 0; i < 9; i++)
			grid.add(new ItemStack(i == 4 ? ModContent.IRON_REINFORCED_PLANKS_ITEM : Items.SLIME_BALL));
		var input = CraftingInput.of(3, 3, grid);
		var level = h.getLevel();
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("slime_ally_spawner"))
			&& recipe.value().assemble(input).is(ModContent.SLIME_ALLY_SPAWNER_ITEM), "Exact slime recipe");
		grid.set(0, ItemStack.EMPTY);
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "No missing corners");
		h.succeed();
	}
}
