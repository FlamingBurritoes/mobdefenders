package net.amemah.mobdefenders.config;

import net.amemah.mobdefenders.entity.DefenderBehavior;
import net.amemah.mobdefenders.spawner.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import java.util.UUID;

/** Overrides are installed only during synchronous assertions, never across a world tick. */
public class AllySpawnerConfigGameTests {
	@GameTest
	public void perKindTimersAndDetectionUseResolvedSettings(GameTestHelper h) {
		checkResolvedSettings(h, false);
	}
	@GameTest
	public void undeclaredJsonUsesSpeciesCodeDefaultsForTimersAndDetection(GameTestHelper h) {
		checkResolvedSettings(h, true);
	}
	private void checkResolvedSettings(GameTestHelper h, boolean codeDefaultsOnly) {
		try {
			var field = AllySpawnerConfig.class.getDeclaredField("current");
			field.setAccessible(true);
			Object previous = field.get(null);
			try {
				var config = codeDefaultsOnly ? AllySpawnerConfig.parseConfiguration("{}", kind -> switch (kind) {
					case ZOMBIE_ALLY -> new AllySpawnerConfig.Settings(20, 3, 6, 12, 4);
					case SKELETON_ALLY -> new AllySpawnerConfig.Settings(40, 3, 6, 12, 6);
					case SPIDER_ALLY -> new AllySpawnerConfig.Settings(90, 3, 6, 12, 10);
					case CREEPER_ALLY -> new AllySpawnerConfig.Settings(60, 3, 6, 12, 8);
				}) : AllySpawnerConfig.parseConfiguration("""
					{"spawners":{
					 "zombie_ally_spawner":{"respawnDelaySeconds":20,"detectionRadius":4},
					 "skeleton_ally_spawner":{"respawnDelaySeconds":40,"detectionRadius":6},
					 "creeper_ally_spawner":{"respawnDelaySeconds":60,"detectionRadius":8}}}
					""");
				field.set(null, config);
				var data = new AllySpawnerData();
				int index = 0;
				for (var kind : SpawnerKind.values()) {
					var expected = config.forKind(kind);
					var pos = new BlockPos(index++, 1, 1);
					data.register(pos, 1000, kind);
					var source = data.source(pos);
					h.assertTrue(source.readyAt == 1000 + expected.initialDelayTicks(),
						"Initial delay must use " + kind + " settings");
					UUID uuid = UUID.randomUUID();
					data.occupy(source, uuid);
					data.death(uuid, 2000);
					h.assertTrue(source.readyAt == 2000 + expected.delayTicks(),
						"Replacement delay must use " + kind + " settings");
					long deadline = source.readyAt;
					var restored = AllySpawnerData.read(data.write()).source(pos);
					h.assertTrue(restored.readyAt == deadline && restored.kind == kind, "Saved deadlines/kinds stay intact");
					var mob = kind.create(h.getLevel());
					h.assertTrue(DefenderBehavior.settings(mob).equals(expected),
						"Unowned/summoned allies must resolve their own species too");
					var enemy = EntityType.HUSK.create(h.getLevel(), EntitySpawnReason.COMMAND);
					enemy.snapTo(mob.getX() + expected.detectionRadius(), mob.getY(), mob.getZ(), 0, 0);
					h.assertTrue(mob.canAttack(enemy), "Exact per-kind detection boundary qualifies");
					enemy.snapTo(enemy.getX() + .01, enemy.getY(), enemy.getZ(), 0, 0);
					h.assertTrue(!mob.canAttack(enemy), "Beyond per-kind detection boundary must not qualify");
				}
			} finally {
				field.set(null, previous);
			}
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
		h.succeed();
	}
}
