package net.amemah.mobdefenders;

import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class GuardReturnGameTests {
	@GameTest(maxTicks = 500, padding = 40, skyAccess = true)
	public void wanderingSpiderWalksBackInsidePatrolAfterCombat(GameTestHelper h) {
		returnToPatrol(h, SpawnerKind.SPIDER_ALLY);
	}
	@GameTest(maxTicks = 600, padding = 40, skyAccess = true)
	public void zombieWalksAroundWallAndOntoPostWithoutSnapping(GameTestHelper h) {
		walkAroundWall(h, SpawnerKind.ZOMBIE_ALLY);
	}
	@GameTest(maxTicks = 600, padding = 40, skyAccess = true)
	public void skeletonWalksAroundWallAndOntoPostWithoutSnapping(GameTestHelper h) {
		walkAroundWall(h, SpawnerKind.SKELETON_ALLY);
	}
	@GameTest(maxTicks = 500, padding = 40, skyAccess = true)
	public void zombieTriesUnreachablePostBeforeSafeFallback(GameTestHelper h) {
		unreachablePost(h, SpawnerKind.ZOMBIE_ALLY);
	}
	@GameTest(maxTicks = 500, padding = 40, skyAccess = true)
	public void skeletonTriesUnreachablePostBeforeSafeFallback(GameTestHelper h) {
		unreachablePost(h, SpawnerKind.SKELETON_ALLY);
	}
	@GameTest(maxTicks = 500, padding = 40, skyAccess = true)
	public void wanderingZombieWalksBackInsidePatrolAfterCombat(GameTestHelper h) {
		returnToPatrol(h, SpawnerKind.ZOMBIE_ALLY);
	}
	@GameTest(maxTicks = 500, padding = 40, skyAccess = true)
	public void wanderingSkeletonWalksBackInsidePatrolAfterCombat(GameTestHelper h) {
		returnToPatrol(h, SpawnerKind.SKELETON_ALLY);
	}

	private void returnToPatrol(GameTestHelper h, SpawnerKind kind) {
		BlockPos pos = place(h, kind, new BlockPos(1, 1, 1));
		AllySpawnerData.get(h.getLevel()).toggle(pos);
		PathfinderMob[] observed = {null};
		Vec3[] previous = {null};
		double[] walked = {0};
		boolean[] returning = {false};
		h.onEachTick(() -> {
			if (!returning[0]) return;
			double step = observed[0].position().distanceTo(previous[0]);
			h.assertTrue(step < .9, "Wander must walk back after combat, not teleport at the patrol edge");
			walked[0] += step;
			previous[0] = observed[0].position();
		});
		h.runAtTickTime(30, () -> {
			PathfinderMob mob = ally(h, pos);
			mob.teleportTo(pos.getX() + 5.75, pos.getY(), pos.getZ() + 5.5);
			mob.setDeltaMovement(Vec3.ZERO);
			mob.getNavigation().stop();
			double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
			mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			var enemy = EntityType.ZOMBIE.create(h.getLevel(), EntitySpawnReason.COMMAND);
			enemy.snapTo(pos.getX() + 6.5, pos.getY(), pos.getZ() + 6.5, 0, 0);
			enemy.setNoAi(true);
			enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
			enemy.setHealth(200);
			enemy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			h.getLevel().addFreshEntity(enemy);
			mob.setTarget(enemy);
			h.runAtTickTime(80, () -> {
				h.assertTrue(mob.getTarget() == enemy, "Wandering defender must retain its active combat target outside patrol range");
				h.assertTrue(!net.amemah.mobdefenders.spawner.AllySpawnerManager.inside(pos, mob.position(), 6),
					"Fixture must be outside patrol but inside emergency recall");
				enemy.discard();
				mob.setTarget(null);
				mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
				observed[0] = mob;
				previous[0] = mob.position();
				returning[0] = true;
				h.startSequence().thenWaitUntil(() -> h.assertTrue(
					net.amemah.mobdefenders.spawner.AllySpawnerManager.inside(pos, mob.position(), 6),
					"Once combat ends, Wander must pathfind back into the source's patrol area")
				).thenExecute(() -> h.assertTrue(walked[0] > 1, "Return must include physical walking"))
					.thenExecuteAfter(40, () -> h.assertTrue(
						!AllySpawnerData.get(h.getLevel()).source(pos).standing
							&& net.amemah.mobdefenders.spawner.AllySpawnerManager.inside(pos, mob.position(), 6),
						"After returning, the same ally must remain in Wander mode inside its patrol area")).thenSucceed();
			});
		});
	}

	private BlockPos place(GameTestHelper h, SpawnerKind kind, int height) {
		return place(h, kind, new BlockPos(2, height, 4));
	}
	private BlockPos place(GameTestHelper h, SpawnerKind kind, BlockPos relative) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++)
			h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		h.setBlock(relative, kind.block());
		BlockPos pos = h.absolutePos(relative);
		AllySpawnerData.get(h.getLevel()).source(pos).readyAt = h.getLevel().getGameTime();
		return pos;
	}
	private PathfinderMob ally(GameTestHelper h, BlockPos pos) {
		var source = AllySpawnerData.get(h.getLevel()).source(pos);
		h.assertTrue(source.entity != null, "Expected source-bound ally");
		return (PathfinderMob) h.getLevel().getEntity(source.entity);
	}

	private void walkAroundWall(GameTestHelper h, SpawnerKind kind) {
		BlockPos pos = place(h, kind, 1);
		// Keep the whole detour inside the empty test structure, not across its enclosing barriers.
		for (int z = 1; z <= 6; z++) for (int y = 1; y <= 3; y++)
			h.setBlock(new BlockPos(4, y, z), Blocks.STONE);
		PathfinderMob[] observed = {null};
		Vec3[] previous = {null};
		double[] walked = {0};
		boolean[] arrived = {false};
		// Register the full sampler before ticking; scheduling hundreds of callbacks from inside a
		// running callback can invalidate Minecraft's GameTest scheduled-task map iterator.
		h.onEachTick(() -> {
			if (observed[0] == null || arrived[0]) return;
			double step = observed[0].position().distanceTo(previous[0]);
			h.assertTrue(step < .9, "Reachable guard return must walk/jump, not snap; step=" + step);
			walked[0] += step;
			previous[0] = observed[0].position();
		});
		h.runAtTickTime(30, () -> {
			PathfinderMob mob = ally(h, pos);
			Vec3 start = new Vec3(pos.getX() + 4.5, pos.getY(), pos.getZ() + .5);
			mob.teleportTo(start.x, start.y, start.z);
			mob.setDeltaMovement(Vec3.ZERO);
			mob.getNavigation().stop();
			Vec3 top = Vec3.atBottomCenterOf(pos.above());
			previous[0] = mob.position();
			observed[0] = mob;
			h.startSequence().thenWaitUntil(() ->
				h.assertTrue(mob.position().distanceToSqr(top) < .1,
					"Must navigate around wall onto post; pos=" + mob.position() + ", top=" + top
						+ ", walked=" + walked[0] + ", path=" + mob.getNavigation().getPath()
						+ ", done=" + mob.getNavigation().isDone() + ", target=" + mob.getTarget())
			).thenExecute(() -> {
				h.assertTrue(mob.position().distanceTo(previous[0]) < .9, "The arrival tick must not hide a final teleport");
				arrived[0] = true;
				h.assertTrue(walked[0] > 6, "Return must take a real detour; measured distance=" + walked[0]);
			}).thenSucceed();
		});
	}

	private void unreachablePost(GameTestHelper h, SpawnerKind kind) {
		BlockPos pos = place(h, kind, 4); // Floating post: no physically walkable route to its top.
		h.runAtTickTime(30, () -> {
			PathfinderMob mob = ally(h, pos);
			mob.teleportTo(pos.getX() + 3.5, pos.getY() - 3, pos.getZ() + .5);
			mob.setDeltaMovement(Vec3.ZERO);
			mob.getNavigation().stop();
			h.runAtTickTime(180, () -> h.assertTrue(mob.getY() < pos.getY() + .5,
				"Failed/partial path must be retried, not cause an immediate or five-second teleport"));
			h.runAtTickTime(440, () -> {
				h.assertTrue(mob.position().distanceToSqr(Vec3.atBottomCenterOf(pos.above())) < .1,
					"After genuinely getting stuck, return safely onto the post; pos=" + mob.position()
						+ ", top=" + Vec3.atBottomCenterOf(pos.above()) + ", path=" + mob.getNavigation().getPath());
				h.succeed();
			});
		});
	}
}
