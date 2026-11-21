package net.amemah.mobdefenders.spawner;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.entity.AerialDefender;
import net.amemah.mobdefenders.entity.Defender;
import net.amemah.mobdefenders.entity.DefenderBehavior;
import net.amemah.mobdefenders.entity.DrownedAlly;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AllySpawnerManager {
	public static void initialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> AllySpawnerConfig.load());
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
			if (entity instanceof Defender && entity.level() instanceof ServerLevel server) {
				var data = AllySpawnerData.get(server);
				var source = data.owner(entity.getUUID());
				if (entity instanceof Slime cube && source != null && source.kind.hasSplitFamily()
					&& SpawnerKind.forEntity(cube) == source.kind && CubeFamily.split(server, data, source, cube)) return;
				data.death(entity.getUUID(), server.getGameTime());
			}
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
			for (var uuid : source.entities()) {
				if (!(level.getEntity(uuid) instanceof Mob ally) || !(ally instanceof Defender) || !ally.isAlive()) continue;
				int radius = settings.roamRadius();
				ally.setHomeTo(source.pos, radius);
				if (ally.getTarget() != null && !ally.canAttack(ally.getTarget())) ally.setTarget(null);
				if (!inside(source.pos, ally.position(), settings.recallRadius())) {
					Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
					Vec3 destination = source.standing && source.kind.usesStandingArea()
						? findStandingPosition(level, source, ally, e -> false)
						: safe(level, ally, top) ? top : findPosition(level, source, ally, radius, true, e -> false);
					ally.getNavigation().stop();
					ally.setTarget(null);
					if (destination != null) {
						ally.setDeltaMovement(Vec3.ZERO);
						ally.teleportTo(destination.x, destination.y, destination.z);
						ally.fallDistance = 0;
					}
				}
			}
			if (source.kind.hasSplitFamily()) CubeFamily.tryMerge(level, data, source);
			if (level.getGameTime() % 20 != 0 || !level.hasChunkAt(source.pos)) continue;
			if (!level.getBlockState(source.pos).is(source.kind.block())) {
				data.unregister(level, source.pos);
				continue;
			}
			if (!level.isPositionEntityTicking(source.pos) || !source.entities().isEmpty() || level.getGameTime() < source.readyAt) continue;
			Mob ally = source.kind.create(level);
			if (ally == null) continue;
			Vec3 position = findPosition(level, source, ally, settings.spawnRadius(), false, e -> false);
			if (position == null) continue;
			ally.snapTo(position, level.getRandom().nextFloat() * 360, 0);
			ally.setHomeTo(source.pos, settings.roamRadius());
			// No vanilla finalizeSpawn: no babies, jockeys, leader modifiers or extra zombies.
			if (level.addFreshEntity(ally)) {
				data.occupy(source, ally.getUUID());
				spawnEffects(level, ally);
			}
		}
	}

	static void spawnEffects(ServerLevel level, Mob ally) {
		level.sendParticles(ParticleTypes.SMOKE, ally.getX(), ally.getY() + 1, ally.getZ(), 20, .35, .6, .35, .02);
		ally.spawnAnim();
	}
	public static boolean insideHorizontal(BlockPos origin, Vec3 point, int radius) {
		double x = point.x - origin.getX() - .5, z = point.z - origin.getZ() - .5;
		return x * x + z * z <= radius * radius;
	}
	public static boolean inside(BlockPos origin, Vec3 point, int radius) {
		return insideHorizontal(origin, point, radius) && Math.abs(point.y - origin.getY()) <= radius;
	}
	private static final int STANDING_HEIGHT = 3;

	public static boolean insideStandingArea(BlockPos origin, BlockPos feet) {
		return Math.abs(feet.getX() - origin.getX()) <= 1 && Math.abs(feet.getZ() - origin.getZ()) <= 1
			&& Math.abs(feet.getY() - origin.getY()) <= STANDING_HEIGHT;
	}

	private static Stream<Vec3> standingPositions(BlockPos origin) {
		// Prefer the usual top; otherwise check every column (including corners) on nearby levels.
		return Stream.concat(Stream.of(origin.above()), Stream.of(1, 0, 2, -1, 3, -2, -STANDING_HEIGHT)
			.flatMap(dy -> BlockPos.betweenClosedStream(origin.offset(-1, dy, -1), origin.offset(1, dy, 1)))
			.filter(feet -> !feet.equals(origin.above()))).map(Vec3::atBottomCenterOf);
	}

	private static Vec3 findStandingPosition(ServerLevel level, AllySpawnerData.Source source, Mob ally, Predicate<Entity> ignore) {
		return standingPositions(source.pos).filter(point -> safe(level, ally, point, ignore)).findFirst().orElse(null);
	}

	public static Vec3 findStandingPosition(ServerLevel level, AllySpawnerData.Source source, Mob ally) {
		return findStandingPosition(level, source, ally, e -> false);
	}

	public record StandingRoute(Path path, Vec3 destination) { }

	public static StandingRoute standingRoute(ServerLevel level, AllySpawnerData.Source source, Mob ally) {
		var destinations = new LinkedHashMap<BlockPos, Vec3>();
		// Vanilla path nodes are anchored to a mob's footprint, not always to its center.
		double nodeOffset = (int) (ally.getBbWidth() + 1) * .5;
		standingPositions(source.pos).filter(point -> safe(level, ally, point)).forEach(point -> {
			BlockPos node = BlockPos.containing(point.x - nodeOffset + .5, point.y, point.z - nodeOffset + .5);
			Vec3 endpoint = new Vec3(node.getX() + nodeOffset, node.getY(), node.getZ() + nodeOffset);
			if (safe(level, ally, endpoint)) destinations.put(node, point);
		});
		Path path = ally.getNavigation().createPath(destinations.keySet(), 0);
		return path == null ? null : new StandingRoute(path, path.canReach() ? destinations.get(path.getTarget()) : null);
	}

	static Vec3 findPosition(ServerLevel level, AllySpawnerData.Source source, Mob ally, int radius, boolean recall,
			Predicate<Entity> ignore) {
		if (source.standing && source.kind.usesStandingArea()) return findStandingPosition(level, source, ally, ignore);
		Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
		if (source.standing && safe(level, ally, top, ignore)) return top;
		if (source.standing && !recall) return null;
		boolean underwater = DefenderBehavior.underwaterPatrol(level, ally, source.pos);
		boolean flying = ally instanceof AerialDefender;
		int roam = AllySpawnerConfig.get(source.kind).roamRadius();
		int height = flying && recall ? AerialDefender.ROAM_HEIGHT : underwater && recall ? DefenderBehavior.UNDERWATER_ROAM_HEIGHT : 3;
		for (int i = 0; i < 48; i++) {
			int dx = level.getRandom().nextInt(radius * 2 + 1) - radius;
			int dz = level.getRandom().nextInt(radius * 2 + 1) - radius;
			if ((dx == 0 && dz == 0) || dx * dx + dz * dz > radius * radius) continue;
			for (int dy = height; dy >= (flying ? 0 : -height); dy--) {
				Vec3 point = new Vec3(source.pos.getX() + dx + .5, source.pos.getY() + dy, source.pos.getZ() + dz + .5);
				if (ally instanceof DrownedAlly
					&& level.getFluidState(BlockPos.containing(point)).is(FluidTags.WATER) != underwater) continue;
				if ((underwater || flying ? insideHorizontal(source.pos, point, roam) : inside(source.pos, point, roam))
					&& (flying && recall ? safeFlight(level, ally, point) : safe(level, ally, point, ignore))) return point;
			}
		}
		return null;
	}
	public static boolean safe(ServerLevel level, Mob ally, Vec3 point) {
		return safe(level, ally, point, e -> false);
	}
	public static boolean safeFlight(ServerLevel level, Mob ally, Vec3 point) {
		BlockPos feet = BlockPos.containing(point);
		return safe(level, ally, point, e -> false, false)
			&& level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty();
	}
	static boolean safe(ServerLevel level, Mob ally, Vec3 point, Predicate<Entity> ignore) {
		return safe(level, ally, point, ignore, true);
	}
	static boolean safe(ServerLevel level, Mob ally, Vec3 point, Predicate<Entity> ignore, boolean requireGround) {
		BlockPos feet = BlockPos.containing(point);
		AABB box = ally.getBoundingBox().move(point.subtract(ally.position()));
		// Check the whole bounding box before querying collision/entities; never load adjacent chunks.
		if (!level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
			|| !level.hasChunkAt(BlockPos.containing(box.maxX, box.maxY, box.maxZ))
			|| !level.getWorldBorder().isWithinBounds(feet)) return false;
		BlockPos ground = feet.below();
		var lowerFluid = level.getFluidState(feet);
		var upperFluid = level.getFluidState(feet.above());
		boolean supported = level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP);
		boolean dry = lowerFluid.isEmpty() && upperFluid.isEmpty();
		boolean water = ally instanceof DrownedAlly && lowerFluid.is(FluidTags.WATER)
			&& (upperFluid.is(FluidTags.WATER) || supported && upperFluid.isEmpty());
		return (!requireGround || supported && dry || water)
			&& level.noBlockCollision(ally, box) && level.getEntities(ally, box, e -> e.isAlive() && !ignore.test(e)).isEmpty();
	}
	private AllySpawnerManager() { }
}
