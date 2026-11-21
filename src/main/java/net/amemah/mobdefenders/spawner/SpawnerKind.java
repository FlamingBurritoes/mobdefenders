package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum SpawnerKind {
	ZOMBIE_ALLY, SKELETON_ALLY, SPIDER_ALLY, CREEPER_ALLY, ENDERMAN_ALLY, SLIME_ALLY, DROWNED_ALLY, BRUTE_ALLY,
	MAGMA_CUBE_ALLY, BLAZE_ALLY, GHAST_ALLY, WITHER_SKELETON_ALLY;

	public int capacity() { return this == SLIME_ALLY ? 8 : this == MAGMA_CUBE_ALLY ? 4 : 1; }
	public boolean hasSplitFamily() { return capacity() > 1; }
	public boolean usesStandingArea() { return this == SPIDER_ALLY || hasSplitFamily(); }
	public String configKey() { return name().toLowerCase(java.util.Locale.ROOT) + "_spawner"; }
	public static SpawnerKind forEntity(Mob mob) {
		if (mob instanceof ZombieAlly) return ZOMBIE_ALLY;
		if (mob instanceof SkeletonAlly) return SKELETON_ALLY;
		if (mob instanceof SpiderAlly) return SPIDER_ALLY;
		if (mob instanceof CreeperAlly) return CREEPER_ALLY;
		if (mob instanceof EndermanAlly) return ENDERMAN_ALLY;
		if (mob instanceof SlimeAlly) return SLIME_ALLY;
		if (mob instanceof DrownedAlly) return DROWNED_ALLY;
		if (mob instanceof BruteAlly) return BRUTE_ALLY;
		if (mob instanceof MagmaCubeAlly) return MAGMA_CUBE_ALLY;
		if (mob instanceof BlazeAlly) return BLAZE_ALLY;
		if (mob instanceof GhastAlly) return GHAST_ALLY;
		if (mob instanceof WitherSkeletonAlly) return WITHER_SKELETON_ALLY;
		return null;
	}

	public Block block() {
		return switch (this) {
			case ZOMBIE_ALLY -> ModContent.ZOMBIE_ALLY_SPAWNER;
			case SKELETON_ALLY -> ModContent.SKELETON_ALLY_SPAWNER;
			case SPIDER_ALLY -> ModContent.SPIDER_ALLY_SPAWNER;
			case CREEPER_ALLY -> ModContent.CREEPER_ALLY_SPAWNER;
			case ENDERMAN_ALLY -> ModContent.ENDERMAN_ALLY_SPAWNER;
			case SLIME_ALLY -> ModContent.SLIME_ALLY_SPAWNER;
			case DROWNED_ALLY -> ModContent.DROWNED_ALLY_SPAWNER;
			case BRUTE_ALLY -> ModContent.BRUTE_ALLY_SPAWNER;
			case MAGMA_CUBE_ALLY -> ModContent.MAGMA_CUBE_ALLY_SPAWNER;
			case BLAZE_ALLY -> ModContent.BLAZE_ALLY_SPAWNER;
			case GHAST_ALLY -> ModContent.GHAST_ALLY_SPAWNER;
			case WITHER_SKELETON_ALLY -> ModContent.WITHER_SKELETON_ALLY_SPAWNER;
		};
	}
	public Mob create(ServerLevel level) {
		return switch (this) {
			case ZOMBIE_ALLY -> ModContent.ZOMBIE_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case SKELETON_ALLY -> ModContent.SKELETON_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case SPIDER_ALLY -> ModContent.SPIDER_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case CREEPER_ALLY -> ModContent.CREEPER_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case ENDERMAN_ALLY -> ModContent.ENDERMAN_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case SLIME_ALLY -> ModContent.SLIME_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case DROWNED_ALLY -> ModContent.DROWNED_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case BRUTE_ALLY -> ModContent.BRUTE_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case MAGMA_CUBE_ALLY -> ModContent.MAGMA_CUBE_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case BLAZE_ALLY -> ModContent.BLAZE_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case GHAST_ALLY -> ModContent.GHAST_ALLY.create(level, EntitySpawnReason.SPAWNER);
			case WITHER_SKELETON_ALLY -> ModContent.WITHER_SKELETON_ALLY.create(level, EntitySpawnReason.SPAWNER);
		};
	}
	public static SpawnerKind forBlock(BlockState state) {
		return state.is(ModContent.WITHER_SKELETON_ALLY_SPAWNER) ? WITHER_SKELETON_ALLY
			: state.is(ModContent.GHAST_ALLY_SPAWNER) ? GHAST_ALLY
			: state.is(ModContent.BLAZE_ALLY_SPAWNER) ? BLAZE_ALLY
			: state.is(ModContent.MAGMA_CUBE_ALLY_SPAWNER) ? MAGMA_CUBE_ALLY
			: state.is(ModContent.BRUTE_ALLY_SPAWNER) ? BRUTE_ALLY
			: state.is(ModContent.DROWNED_ALLY_SPAWNER) ? DROWNED_ALLY
			: state.is(ModContent.SLIME_ALLY_SPAWNER) ? SLIME_ALLY
			: state.is(ModContent.ENDERMAN_ALLY_SPAWNER) ? ENDERMAN_ALLY
			: state.is(ModContent.CREEPER_ALLY_SPAWNER) ? CREEPER_ALLY
			: state.is(ModContent.SPIDER_ALLY_SPAWNER) ? SPIDER_ALLY
			: state.is(ModContent.SKELETON_ALLY_SPAWNER) ? SKELETON_ALLY : ZOMBIE_ALLY;
	}
	public static boolean isSpawner(BlockState state) {
		return state.is(ModContent.ZOMBIE_ALLY_SPAWNER) || state.is(ModContent.SKELETON_ALLY_SPAWNER)
			|| state.is(ModContent.SPIDER_ALLY_SPAWNER) || state.is(ModContent.CREEPER_ALLY_SPAWNER)
			|| state.is(ModContent.ENDERMAN_ALLY_SPAWNER) || state.is(ModContent.SLIME_ALLY_SPAWNER)
			|| state.is(ModContent.DROWNED_ALLY_SPAWNER) || state.is(ModContent.BRUTE_ALLY_SPAWNER)
			|| state.is(ModContent.MAGMA_CUBE_ALLY_SPAWNER) || state.is(ModContent.BLAZE_ALLY_SPAWNER)
			|| state.is(ModContent.GHAST_ALLY_SPAWNER) || state.is(ModContent.WITHER_SKELETON_ALLY_SPAWNER);
	}
}
