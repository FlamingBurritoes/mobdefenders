package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum SpawnerKind {
	ZOMBIE_ALLY, SKELETON_ALLY, SPIDER_ALLY;

	public Block block() {
		return switch (this) {
			case ZOMBIE_ALLY -> ModContent.ZOMBIE_ALLY_SPAWNER;
			case SKELETON_ALLY -> ModContent.SKELETON_ALLY_SPAWNER;
			case SPIDER_ALLY -> ModContent.SPIDER_ALLY_SPAWNER;
		};
	}
	public PathfinderMob create(ServerLevel level) {
		return switch (this) {
			case ZOMBIE_ALLY -> ModContent.ZOMBIE_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case SKELETON_ALLY -> ModContent.SKELETON_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case SPIDER_ALLY -> ModContent.SPIDER_ALLY.create(level, EntitySpawnReason.SPAWNER);
		};
	}
	public static SpawnerKind forBlock(BlockState state) {
		return state.is(ModContent.SPIDER_ALLY_SPAWNER) ? SPIDER_ALLY
			: state.is(ModContent.SKELETON_ALLY_SPAWNER) ? SKELETON_ALLY : ZOMBIE_ALLY;
	}
	public static boolean isSpawner(BlockState state) {
		return state.is(ModContent.ZOMBIE_ALLY_SPAWNER) || state.is(ModContent.SKELETON_ALLY_SPAWNER)
			|| state.is(ModContent.SPIDER_ALLY_SPAWNER);
	}
}
