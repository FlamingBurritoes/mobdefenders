package net.amemah.mobdefenders;

import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class DefenderDetectionGameTests {
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void creeperUsesTenBlocksFromItselfNotTheSpawner(GameTestHelper h) {
		mobCenteredRange(h, SpawnerKind.CREEPER_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void creeperDetectsAndRetainsTargetsThroughWalls(GameTestHelper h) {
		throughWall(h, SpawnerKind.CREEPER_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void spiderUsesTenBlocksFromItselfNotTheSpawner(GameTestHelper h) {
		mobCenteredRange(h, SpawnerKind.SPIDER_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void spiderDetectsAndRetainsTargetsThroughWalls(GameTestHelper h) {
		throughWall(h, SpawnerKind.SPIDER_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void zombieUsesTenBlocksFromItselfNotTheSpawner(GameTestHelper h) {
		mobCenteredRange(h, SpawnerKind.ZOMBIE_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void skeletonUsesTenBlocksFromItselfNotTheSpawner(GameTestHelper h) {
		mobCenteredRange(h, SpawnerKind.SKELETON_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void zombieDetectsAndRetainsTargetsThroughWalls(GameTestHelper h) {
		throughWall(h, SpawnerKind.ZOMBIE_ALLY);
	}
	@GameTest(maxTicks = 400, padding = 32, skyAccess = true)
	public void skeletonDetectsAndRetainsTargetsThroughWalls(GameTestHelper h) {
		throughWall(h, SpawnerKind.SKELETON_ALLY);
	}

	private BlockPos place(GameTestHelper h, SpawnerKind kind) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++)
			h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		h.setBlock(new BlockPos(1, 1, 1), kind.block());
		BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
		AllySpawnerData.get(h.getLevel()).source(pos).readyAt = h.getLevel().getGameTime();
		return pos;
	}
	private PathfinderMob ally(GameTestHelper h, BlockPos pos) {
		var source = AllySpawnerData.get(h.getLevel()).source(pos);
		h.assertTrue(source.entity != null, "Expected spawned defender");
		return (PathfinderMob) h.getLevel().getEntity(source.entity);
	}

	private void mobCenteredRange(GameTestHelper h, SpawnerKind kind) {
		BlockPos pos = place(h, kind);
		h.runAtTickTime(30, () -> {
			PathfinderMob mob = ally(h, pos);
			var enemy = EntityType.ZOMBIE.create(h.getLevel(), EntitySpawnReason.COMMAND);
			Vec3 post = Vec3.atBottomCenterOf(pos.above());
			mob.teleportTo(post.x + 5, post.y, post.z);
			enemy.snapTo(mob.getX() + 10, mob.getY(), mob.getZ(), 0, 0);
			h.assertTrue(mob.canAttack(enemy), "Exactly ten blocks from the ally must qualify, regardless of distance to source");
			enemy.snapTo(mob.getX() + 10.01, mob.getY(), mob.getZ(), 0, 0);
			h.assertTrue(!mob.canAttack(enemy), "Beyond ten blocks must be rejected");
			enemy.snapTo(mob.getX(), mob.getY() + 10.01, mob.getZ(), 0, 0);
			h.assertTrue(!mob.canAttack(enemy), "Detection uses three-dimensional distance");
			mob.teleportTo(post.x + 11, post.y, post.z);
			enemy.snapTo(post, 0, 0);
			h.assertTrue(!mob.canAttack(enemy), "Enemy beside the source must still be ignored when too far from the ally");
			mob.teleportTo(post.x + 5, post.y - 1, post.z);
			mob.setDeltaMovement(Vec3.ZERO);
			// Inside the test structure: >6 from source but <10 from the ally.
			enemy.snapTo(pos.getX() + 6.5, pos.getY(), pos.getZ() + 3.5, 0, 0);
			enemy.setNoAi(true);
			enemy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			h.getLevel().addFreshEntity(enemy);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(mob.getTarget() == enemy || enemy.getHealth() < enemy.getMaxHealth(),
				"Real target acquisition must work outside the old six-block source circle")).thenExecute(enemy::discard).thenSucceed();
		});
	}

	private void throughWall(GameTestHelper h, SpawnerKind kind) {
		BlockPos pos = place(h, kind);
		for (int y = 1; y <= 4; y++) for (int z = 0; z <= 3; z++)
			h.setBlock(new BlockPos(4, y, z), Blocks.STONE);
		h.runAtTickTime(30, () -> {
			PathfinderMob mob = ally(h, pos);
			// Keep the defender fixed so this tests detection, not finding a visible route around the wall.
			mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
			var enemy = EntityType.ZOMBIE.create(h.getLevel(), EntitySpawnReason.COMMAND);
			enemy.snapTo(pos.getX() + 5.5, pos.getY(), pos.getZ() + .5, 0, 0);
			enemy.setNoAi(true);
			enemy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			h.getLevel().addFreshEntity(enemy);
			h.startSequence().thenWaitUntil(() -> h.assertTrue(mob.getTarget() == enemy,
				"An eligible enemy must be detected through an opaque wall"))
				.thenIdle(100).thenExecute(() -> {
					h.assertTrue(!mob.getSensing().hasLineOfSight(enemy), "The wall must remain opaque throughout the test");
					h.assertTrue(mob.getTarget() == enemy, "Losing sight must not clear a living in-range enemy");
					h.assertTrue(enemy.getHealth() == enemy.getMaxHealth(), "Detection must not let attacks damage an enemy through solid blocks");
					enemy.discard();
				}).thenSucceed();
		});
	}
}
