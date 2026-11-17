package net.amemah.mobdefenders;

import net.amemah.mobdefenders.entity.*;
import net.amemah.mobdefenders.spawner.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class SpiderAllySpawnerGameTests {
	@GameTest(padding = 24)
	public void spiderRecipeUsesStringPlanksAndNormalSpiderEye(GameTestHelper h) {
		var ingredients = new java.util.ArrayList<ItemStack>();
		for (String row : java.util.List.of("SES", "SCS", "SSS")) for (char c : row.toCharArray())
			ingredients.add(new ItemStack(c == 'S' ? Items.STRING : c == 'E' ? Items.SPIDER_EYE
				: ModContent.COPPER_REINFORCED_PLANKS_ITEM));
		var input = CraftingInput.of(3, 3, ingredients);
		var recipe = h.getLevel().getServer().getRecipeManager()
			.getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("spider_ally_spawner"))
			&& result.is(ModContent.SPIDER_ALLY_SPAWNER_ITEM) && result.getCount() == 1,
			"Requested SES/SCS/SSS recipe must yield one spider spawner");
		ingredients.set(1, new ItemStack(Items.FERMENTED_SPIDER_EYE));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), h.getLevel()),
			"Fermented spider eye must not replace normal spider eye");
		ingredients.set(1, new ItemStack(Items.SPIDER_EYE));
		ingredients.set(4, new ItemStack(Items.OAK_PLANKS));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, ingredients), h.getLevel()),
			"Ordinary planks must not replace copper reinforced planks");
		h.succeed();
	}
	private BlockPos place(GameTestHelper h, boolean immediate) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		h.setBlock(new BlockPos(3, 1, 3), ModContent.SPIDER_ALLY_SPAWNER);
		BlockPos pos = h.absolutePos(new BlockPos(3, 1, 3));
		if (immediate) AllySpawnerData.get(h.getLevel()).source(pos).readyAt = h.getLevel().getGameTime();
		return pos;
	}
	private SpiderAlly ally(GameTestHelper h, BlockPos pos) {
		var source = AllySpawnerData.get(h.getLevel()).source(pos);
		h.assertTrue(source.entity != null, "Expected occupied spider source");
		return (SpiderAlly) h.getLevel().getEntity(source.entity);
	}
	@GameTest(maxTicks = 2900, padding = 32)
	public void timersCapPersistenceAndBreakCleanup(GameTestHelper h) {
		BlockPos pos = place(h, false);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		var source = data.source(pos);
		h.assertTrue(source.kind == SpawnerKind.SPIDER_ALLY && source.readyAt == level.getGameTime() + 900,
			"Spider must start with the shared 45-second wait");
		h.runAtTickTime(890, () -> h.assertTrue(source.entity == null, "No early spider"));
		h.runAtTickTime(940, () -> {
			SpiderAlly spider = ally(h, pos);
			h.assertTrue(spider.getMaxHealth() == 16 && spider.getAttributeValue(Attributes.ATTACK_DAMAGE) == 2
				&& spider.getNavigation() instanceof WallClimberNavigation, "Expected spider stats and climbing navigation");
			spider.kill(level);
			h.assertTrue(source.entity == null && source.readyAt == level.getGameTime() + 1800, "Shared 90-second replacement wait");
			var restored = AllySpawnerData.read(data.write()).source(pos);
			h.assertTrue(restored.kind == SpawnerKind.SPIDER_ALLY && restored.readyAt == source.readyAt, "Kind and deadline must persist");
		});
		h.runAtTickTime(2730, () -> h.assertTrue(source.entity == null, "No early replacement"));
		h.runAtTickTime(2780, () -> {
			SpiderAlly spider = ally(h, pos);
			h.assertTrue(level.getEntitiesOfClass(SpiderAlly.class, spider.getBoundingBox().inflate(6),
				e -> e.isAlive() && data.owner(e.getUUID()) == source).size() == 1, "Hard cap one");
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			h.assertTrue(!spider.isAlive() && data.owner(spider.getUUID()) == null, "Breaking source retires spider");
			h.succeed();
		});
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void friendlyFiltersDaylightAndActualMelee(GameTestHelper h) {
		BlockPos pos = place(h, true);
		h.setTime(6000);
		h.runAtTickTime(30, () -> {
			var spider = ally(h, pos);
			var level = h.getLevel();
			var player = h.makeMockPlayer(GameType.SURVIVAL);
			var creeper = EntityType.CREEPER.create(level, EntitySpawnReason.COMMAND);
			var zombie = ModContent.ZOMBIE_ALLY.create(level, EntitySpawnReason.COMMAND);
			var skeleton = ModContent.SKELETON_ALLY.create(level, EntitySpawnReason.COMMAND);
			var friend = ModContent.SPIDER_ALLY.create(level, EntitySpawnReason.COMMAND);
			var golem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
			for (var e : java.util.List.of(player, creeper, zombie, skeleton, friend, golem)) {
				e.snapTo(spider.position(), 0, 0);
				spider.setTarget(e);
				h.assertTrue(!spider.canAttack(e) && spider.getTarget() == null, "Friendly and Creeper filters must reject forced targets");
			}
			h.assertTrue(!golem.canAttack(spider), "Golems must remain friendly");
			var enemy = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			enemy.snapTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5, 0, 0);
			enemy.setNoAi(true);
			enemy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			level.addFreshEntity(enemy);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(enemy.getHealth() < enemy.getMaxHealth(),
				"Spider must acquire and melee hostiles even in daylight")).thenExecute(() -> {
					h.assertTrue(!spider.isOnFire() && spider.getHealth() == 16 && spider.getPassengers().isEmpty(),
						"No sun damage or hostile jockey");
					enemy.discard();
				}).thenSucceed();
		});
	}
	@GameTest(maxTicks = 600, padding = 32)
	public void wanderRecallAndStanding(GameTestHelper h) {
		BlockPos pos = place(h, true);
		var data = AllySpawnerData.get(h.getLevel());
		data.toggle(pos);
		h.runAtTickTime(30, () -> {
			var spider = ally(h, pos);
			Vec3 start = spider.position();
			h.startSequence().thenWaitUntil(() -> h.assertTrue(spider.position().distanceToSqr(start) > 2.25,
				"Spider must patrol")).thenExecute(() -> {
					spider.setNoAi(true);
					spider.teleportTo(pos.getX() + 13.5, pos.getY(), pos.getZ() + .5);
				}).thenIdle(3).thenExecute(() -> {
					h.assertTrue(spider.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .2, "Twelve-block recall");
					data.toggle(pos);
					spider.setNoAi(false);
					spider.teleportTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5);
				}).thenWaitUntil(() -> h.assertTrue(spider.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
					"Standing must return spider to source")).thenSucceed();
		});
	}
	@GameTest(padding = 24)
	public void allAlliesPauseBetweenWalksButCanStillTarget(GameTestHelper h) {
		for (var kind : SpawnerKind.values()) checkPatrolPause(h, kind);
		h.succeed();
	}
	private void checkPatrolPause(GameTestHelper h, SpawnerKind kind) {
		var mob = kind.create(h.getLevel());
		try {
			Class<?> type = Class.forName("net.amemah.mobdefenders.entity.DefenderBehavior$WanderAroundSpawnerGoal");
			var constructor = type.getDeclaredConstructor(PathfinderMob.class);
			constructor.setAccessible(true);
			var goal = (Goal) constructor.newInstance(mob);
			var next = type.getDeclaredField("nextAttempt");
			next.setAccessible(true);
			for (int i = 0; i < 100; i++) {
				goal.stop();
				int pause = next.getInt(goal) - mob.tickCount;
				h.assertTrue(pause >= 60 && pause <= 100, kind + " must pause 3-5 seconds");
			}
			var enemy = EntityType.ZOMBIE.create(h.getLevel(), EntitySpawnReason.COMMAND);
			enemy.snapTo(mob.position(), 0, 0);
			mob.setTarget(enemy);
			h.assertTrue(mob.getTarget() == enemy, "Patrol cooldown must not prevent combat targeting");
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
