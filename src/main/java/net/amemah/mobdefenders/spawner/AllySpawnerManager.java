package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.ModContent;
import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.Defender;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

public final class AllySpawnerManager {
	public static void initialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> AllySpawnerConfig.load());
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
			if (entity instanceof Defender && entity.level() instanceof ServerLevel server)
				AllySpawnerData.get(server).death(entity.getUUID(), server.getGameTime());
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, server) -> {
			if (entity instanceof Defender && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy())
				AllySpawnerData.get(server).death(entity.getUUID(), server.getGameTime());
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, server) -> {
			if (entity instanceof Defender && AllySpawnerData.get(server).consumePendingDeath(entity.getUUID()))
				entity.kill(server);
		});
		ServerTickEvents.END_LEVEL_TICK.register(AllySpawnerManager::tick);
	}
	private static void tick(ServerLevel level) {
		AllySpawnerData data = AllySpawnerData.get(level);
		for (var source : data.sources()) {
			var settings = AllySpawnerConfig.get(source.kind);
			if (source.entity != null && level.getEntity(source.entity) instanceof PathfinderMob ally && ally instanceof Defender && ally.isAlive()) {
				int radius = settings.roamRadius();
				ally.setHomeTo(source.pos, radius);
				if (ally.getTarget() != null && !ally.canAttack(ally.getTarget())) ally.setTarget(null);
				if (!inside(source.pos, ally.position(), settings.recallRadius())) {
					Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
					Vec3 destination = safe(level, ally, top) ? top : findPosition(level, source, ally, radius, true);
					ally.getNavigation().stop();
					ally.setTarget(null);
					if (destination != null) {
						ally.setDeltaMovement(Vec3.ZERO);
						ally.teleportTo(destination.x, destination.y, destination.z);
						ally.fallDistance = 0;
					}
				}
			}
			if (level.getGameTime() % 20 != 0 || !level.hasChunkAt(source.pos)) continue;
			if (!level.getBlockState(source.pos).is(source.kind.block())) {
				data.unregister(level, source.pos);
				continue;
			}
			if (!level.isPositionEntityTicking(source.pos) || source.entity != null || level.getGameTime() < source.readyAt) continue;
			PathfinderMob ally = source.kind.create(level);
			if (ally == null) continue;
			Vec3 position = findPosition(level, source, ally, settings.spawnRadius(), false);
			if (position == null) continue;
			ally.snapTo(position, level.getRandom().nextFloat() * 360, 0);
			ally.setHomeTo(source.pos, settings.roamRadius());
			// No vanilla finalizeSpawn: no babies, jockeys, leader modifiers or extra zombies.
			if (level.addFreshEntity(ally)) {
				data.occupy(source, ally.getUUID());
				level.sendParticles(ParticleTypes.SMOKE, ally.getX(), ally.getY() + 1, ally.getZ(), 20, .35, .6, .35, .02);
				ally.spawnAnim();
			}
		}
	}
	public static boolean insideHorizontal(BlockPos origin, Vec3 point, int radius) {
		double x = point.x - origin.getX() - .5, z = point.z - origin.getZ() - .5;
		return x * x + z * z <= radius * radius;
	}
	public static boolean inside(BlockPos origin, Vec3 point, int radius) {
		return insideHorizontal(origin, point, radius) && Math.abs(point.y - origin.getY()) <= radius;
	}
	private static Vec3 findPosition(ServerLevel level, AllySpawnerData.Source source, PathfinderMob ally, int radius, boolean recall) {
		Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
		if (source.standing && safe(level, ally, top)) return top;
		if (source.standing && !recall) return null;
		for (int i = 0; i < 48; i++) {
			int dx = level.getRandom().nextInt(radius * 2 + 1) - radius;
			int dz = level.getRandom().nextInt(radius * 2 + 1) - radius;
			if ((dx == 0 && dz == 0) || dx * dx + dz * dz > radius * radius) continue;
			for (int dy = 3; dy >= -3; dy--) {
				Vec3 point = new Vec3(source.pos.getX() + dx + .5, source.pos.getY() + dy, source.pos.getZ() + dz + .5);
				if (inside(source.pos, point, AllySpawnerConfig.get(source.kind).roamRadius()) && safe(level, ally, point)) return point;
			}
		}
		return null;
	}
	public static boolean safe(ServerLevel level, PathfinderMob ally, Vec3 point) {
		BlockPos feet = BlockPos.containing(point);
		var box = ally.getBoundingBox().move(point.subtract(ally.position()));
		// Check the whole bounding box before querying collision/entities; never load adjacent chunks.
		if (!level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
			|| !level.hasChunkAt(BlockPos.containing(box.maxX, box.maxY, box.maxZ))
			|| !level.getWorldBorder().isWithinBounds(feet)) return false;
		BlockPos ground = feet.below();
		return level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
			&& level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()
			&& level.noCollision(ally, box) && level.getEntities(ally, box, Entity::isAlive).isEmpty();
	}
	private AllySpawnerManager() { }
}
