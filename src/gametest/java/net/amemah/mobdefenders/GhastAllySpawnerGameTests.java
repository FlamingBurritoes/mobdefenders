package net.amemah.mobdefenders;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.GhastAlly;
import net.amemah.mobdefenders.entity.GhastAllyFireball;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.AllySpawnerManager;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class GhastAllySpawnerGameTests {
	@GameTest(maxTicks = 650, padding = 40, skyAccess = true)
	public void smallGhastFliesInSevenBlockBandAndReturnsToStandingPost(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var relative = new BlockPos(3, 1, 3);
		h.setBlock(relative, ModContent.GHAST_ALLY_SPAWNER);
		var level = h.getLevel();
		var data = AllySpawnerData.get(level);
		var source = data.source(h.absolutePos(relative));
		var settings = AllySpawnerConfig.get(SpawnerKind.GHAST_ALLY);
		h.assertTrue(source.kind == SpawnerKind.GHAST_ALLY && source.standing && source.entities().isEmpty()
			&& source.readyAt == level.getGameTime() + settings.initialDelayTicks(), "Shared Standing and initial deadline");
		source.readyAt = level.getGameTime();
		var post = Vec3.atBottomCenterOf(source.pos.above());
		h.runAtTickTime(30, () -> {
			var ghast = (GhastAlly) level.getEntity(source.entity());
			h.assertTrue(source.entities().size() == 1 && ghast.position().distanceToSqr(post) < .1
				&& ghast.getHealth() == 10 && ghast.getBbHeight() == .875F && ghast.fireImmune()
				&& ghast.getAttributeValue(Attributes.FLYING_SPEED) == .06, "Vanilla stats, small hitbox and the shared spawn post");
			var testPoint = Vec3.atBottomCenterOf(source.pos.offset(2, 0, 0));
			for (int sign : new int[]{-1, 1}) {
				double limit = source.pos.getY() + (sign > 0 ? 7 : 0);
				ghast.snapTo(testPoint.x, limit - sign * .2, testPoint.z, 0, 0);
				ghast.move(MoverType.SELF, new Vec3(0, sign * 2, 0));
				h.assertTrue(Math.abs(ghast.getY() - limit) < .01, "Idle flight must stop at both ends of its altitude band");
			}
			ghast.snapTo(post, 0, 0);
			data.toggle(source.pos);
			h.startSequence().thenWaitUntil(() -> {
				h.assertTrue(ghast.getY() - post.y > .75 && ghast.getY() <= source.pos.getY() + 7
					&& AllySpawnerManager.insideHorizontal(source.pos, ghast.position(), settings.roamRadius()),
					"Physically fly upward in Wander within patrol: " + ghast.position());
			}).thenExecute(() -> data.toggle(source.pos)).thenWaitUntil(() ->
				h.assertTrue(ghast.position().distanceToSqr(post) < .1, "Standing must return to the post"))
				.thenExecute(() -> {
					ghast.kill(level);
					h.assertTrue(source.entities().isEmpty() && source.readyAt == level.getGameTime() + settings.delayTicks(),
						"Shared replacement timer");
				}).thenSucceed();
		});
	}

	@GameTest(maxTicks = 150, padding = 40, skyAccess = true)
	public void acquiresAndShootsAHostileWithVanillaDirectDamage(GameTestHelper h) {
		var level = h.getLevel();
		var ghast = h.spawn(ModContent.GHAST_ALLY, new Vec3(2.5, 2, 3.5));
		var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(5.75, 2, 3.5));
		enemy.setPersistenceRequired();
		enemy.setNoGravity(true);
		enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
		var cow = h.spawnWithNoFreeWill(EntityType.COW, new Vec3(4, 2, 3.5));
		cow.setNoGravity(true);
		h.startSequence().thenWaitUntil(() -> h.assertTrue(ghast.getTarget() == enemy && ghast.isCharging(),
			"Charge only against the hostile")).thenWaitUntil(() ->
			h.assertTrue(enemy.getHealth() <= 14, "Real fireball must reach the hostile with at least six damage"))
			.thenExecute(() -> h.assertTrue(cow.getHealth() == cow.getMaxHealth(), "Shot passes through the cow"))
			.thenExecute(enemy::discard).thenSucceed();
	}

	@GameTest(maxTicks = 90, padding = 40, skyAccess = true)
	public void blastProtectsBlocksItemsAndFriendliesEvenWithoutOwner(GameTestHelper h) {
		var level = h.getLevel();
		for (int z = 2; z <= 4; z++) for (int y = 1; y <= 4; y++)
			h.setBlock(new BlockPos(5, y, z), Blocks.OAK_PLANKS);
		var source = h.spawnWithNoFreeWill(ModContent.GHAST_ALLY, new Vec3(2, 2, 3.5));
		var enemy = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.6));
		enemy.setPersistenceRequired();
		enemy.setNoGravity(true);
		enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
		var ally = h.spawnWithNoFreeWill(ModContent.ZOMBIE_ALLY, new Vec3(4.4, 2, 2.7));
		ally.setNoGravity(true);
		var player = GameTestPlayers.spawnSurvival(level, h.absoluteVec(new Vec3(3.6, 2, 3.5)));
		player.setNoGravity(true);
		var center = h.absoluteVec(new Vec3(4.5, 2.6, 3.5));
		var drop = new ItemEntity(level, center.x, center.y, center.z, new ItemStack(Items.GHAST_TEAR), 0, 0, 0);
		drop.setNoGravity(true);
		drop.setNeverPickUp();
		level.addFreshEntity(drop);
		var shot = new GhastAllyFireball(level, source, h.absoluteVec(new Vec3(2.6, 2.6, 3.5)), new Vec3(1, 0, 0));
		shot.setOwner(null);
		level.addFreshEntity(shot);
		h.startSequence().thenWaitUntil(() -> h.assertTrue(shot.isRemoved(), "Fireball must collide with the wall"))
			.thenExecute(() -> {
				h.assertTrue(enemy.getHealth() < enemy.getMaxHealth(), "Hostile beside the impact must take splash damage");
				h.assertTrue(ally.getHealth() == ally.getMaxHealth() && player.getHealth() == player.getMaxHealth(),
					"No splash damage to allies or players");
				h.assertTrue(drop.isAlive() && drop.getItem().is(Items.GHAST_TEAR) && drop.getDeltaMovement().lengthSqr() == 0,
					"No damage, collision or knockback for the dropped item");
				for (int z = 2; z <= 4; z++) for (int y = 1; y <= 4; y++)
					h.assertTrue(h.getBlockState(new BlockPos(5, y, z)).is(Blocks.OAK_PLANKS), "Wall must survive");
				player.discard();
				enemy.discard();
			}).thenSucceed();
	}

	@GameTest(padding = 24)
	public void recipeUsesGhastTearsGunpowderAndGoldBrick(GameTestHelper h) {
		var grid = new ArrayList<ItemStack>();
		for (char key : "TUTUGUTUT".toCharArray()) grid.add(new ItemStack(switch (key) {
			case 'T' -> Items.GHAST_TEAR;
			case 'U' -> Items.GUNPOWDER;
			default -> ModContent.GOLD_REINFORCED_NETHER_BRICK_ITEM;
		}));
		var input = CraftingInput.of(3, 3, grid);
		var level = h.getLevel();
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		var result = recipe.value().assemble(input);
		h.assertTrue(recipe.id().identifier().equals(ModContent.id("ghast_ally_spawner"))
			&& result.is(ModContent.GHAST_ALLY_SPAWNER_ITEM) && result.getCount() == 1, "Exact Ghast Spawner recipe");
		grid.set(4, new ItemStack(Items.NETHER_BRICKS));
		h.assertTrue(!recipe.value().matches(CraftingInput.of(3, 3, grid), level), "Center must be gold reinforced");
		h.succeed();
	}
}
