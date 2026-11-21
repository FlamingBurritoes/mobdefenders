package net.amemah.mobdefenders.entity;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.AllySpawnerManager;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** One guard/patrol implementation for every defender; entity subclasses supply their vanilla combat. */
public final class DefenderBehavior {
	public static final int UNDERWATER_ROAM_HEIGHT = 7;

	public static boolean underwaterPatrol(Level level, Mob mob, BlockPos source) {
		return mob instanceof DrownedAlly && level.getFluidState(source.above()).is(FluidTags.WATER);
	}
	private static boolean insidePatrol(Mob mob, BlockPos source, Vec3 point, int radius) {
		if (mob instanceof AerialDefender)
			return AllySpawnerManager.insideHorizontal(source, point, radius)
				&& point.y >= source.getY() && point.y <= source.getY() + AerialDefender.ROAM_HEIGHT;
		if (underwaterPatrol(mob.level(), mob, source))
			return AllySpawnerManager.insideHorizontal(source, point, radius)
				&& Math.abs(point.y - source.getY()) <= UNDERWATER_ROAM_HEIGHT;
		return AllySpawnerManager.inside(source, point, radius);
	}
	private static boolean patrolMedium(Mob mob, BlockPos feet, boolean underwater) {
		return !(mob instanceof DrownedAlly) || mob.level().getFluidState(feet).is(FluidTags.WATER) == underwater;
	}
	public static AllySpawnerData.Source source(Mob mob) {
		return mob.level() instanceof ServerLevel level ? AllySpawnerData.get(level).owner(mob.getUUID()) : null;
	}
	public static boolean standing(Mob mob) {
		var source = source(mob);
		return source == null || source.standing;
	}
	public static AllySpawnerConfig.Settings settings(Mob mob) {
		var source = source(mob);
		return AllySpawnerConfig.get(source != null ? source.kind : SpawnerKind.forEntity(mob));
	}
	public static boolean validEnemy(LivingEntity target) {
		return target instanceof Enemy && !(target instanceof Creeper) && !(target instanceof Defender) && target.isAlive();
	}
	public static boolean canAttack(Mob mob, LivingEntity target) {
		int radius = settings(mob).detectionRadius();
		return validEnemy(target) && mob.distanceToSqr(target) <= (double) radius * radius;
	}
	public static void addGoals(Mob mob, GoalSelector goals, GoalSelector targets) {
		if (!(mob instanceof DrownedAlly) && !(mob instanceof AerialDefender)) goals.addGoal(0, new FloatGoal(mob));
		goals.addGoal(6, new ReturnToGuardGoal(mob));
		goals.addGoal(7, new WanderAroundSpawnerGoal(mob));
		goals.addGoal(8, new LookAtPlayerGoal(mob, Player.class, 8));
		goals.addGoal(8, new RandomLookAroundGoal(mob));
		targets.addGoal(1, new DefenderTargetGoal(mob));
	}

	private static final class DefenderTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
		DefenderTargetGoal(Mob mob) {
			super(mob, LivingEntity.class, 10, false, false, (target, level) -> validEnemy(target));
			// mustSee controls retention; acquisition also has its own visibility condition.
			targetConditions.ignoreLineOfSight();
		}
		@Override protected double getFollowDistance() { return settings(mob).detectionRadius(); }
	}

	private static final class WanderAroundSpawnerGoal extends Goal {
		private final Mob mob;
		private Path path;
		private int nextAttempt;
		WanderAroundSpawnerGoal(Mob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE));
		}
		@Override public boolean canUse() {
			var source = source(mob);
			if (source == null || source.standing || mob.getTarget() != null || mob.tickCount < nextAttempt
				|| !(mob.level() instanceof ServerLevel level)) return false;
			nextAttempt = mob.tickCount + 20;
			int radius = settings(mob).roamRadius();
			boolean underwater = underwaterPatrol(level, mob, source.pos);
			boolean flying = mob instanceof AerialDefender;
			int height = flying ? AerialDefender.ROAM_HEIGHT : underwater ? UNDERWATER_ROAM_HEIGHT : 3;
			int minHeight = flying ? 0 : -height;
			int heightCount = height - minHeight + 1;
			int pathRadius = insidePatrol(mob, source.pos, mob.position(), radius)
				? radius : settings(mob).recallRadius();
			for (int attempt = 0; attempt < 24; attempt++) {
				int dx = mob.getRandom().nextInt(radius * 2 + 1) - radius;
				int dz = mob.getRandom().nextInt(radius * 2 + 1) - radius;
				if (dx * dx + dz * dz > radius * radius) continue;
				int startHeight = underwater || flying ? mob.getRandom().nextInt(heightCount) : heightCount - 1;
				for (int step = 0; step < heightCount; step++) {
					int dy = (startHeight - step + heightCount) % heightCount + minHeight;
					BlockPos feet = source.pos.offset(dx, dy, dz);
					Vec3 destination = Vec3.atBottomCenterOf(feet);
					if (mob.position().distanceToSqr(destination) < 2.25
						|| !insidePatrol(mob, source.pos, destination, radius)
						|| !patrolMedium(mob, feet, underwater)
						|| !(flying ? AllySpawnerManager.safeFlight(level, mob, destination)
							: AllySpawnerManager.safe(level, mob, destination))) continue;
					Path candidate = mob.getNavigation().createPath(feet, 0);
					if (candidate == null || !candidate.canReach()) continue;
					boolean inside = true;
					for (int i = 0; i < candidate.getNodeCount(); i++) {
						BlockPos node = candidate.getNodePos(i);
						if (!insidePatrol(mob, source.pos, Vec3.atBottomCenterOf(node), pathRadius)
							|| !patrolMedium(mob, node, underwater)) {
							inside = false;
							break;
						}
					}
					if (inside) {
						path = candidate;
						return true;
					}
				}
			}
			return false;
		}
		@Override public void start() { mob.getNavigation().moveTo(path, 1); }
		@Override public boolean canContinueToUse() {
			return !standing(mob) && mob.getTarget() == null && !mob.getNavigation().isDone();
		}
		@Override public void stop() {
			mob.getNavigation().stop();
			path = null;
			// Only idle strolling is delayed; combat and guard return have independent goals.
			nextAttempt = mob.tickCount + 60 + mob.getRandom().nextInt(41);
		}
	}

	private static final class ReturnToGuardGoal extends Goal {
		private static final int STUCK_TICKS_BEFORE_TELEPORT = 200;
		private final Mob mob;
		private int repathTicks;
		private int stuckTicks;
		private Vec3 lastProgress;
		private Vec3 standingDestination;
		ReturnToGuardGoal(Mob mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE)); }
		@Override public void start() {
			repathTicks = 0;
			stuckTicks = 0;
			lastProgress = mob.position();
			standingDestination = null;
		}
		@Override public boolean canUse() {
			var source = source(mob);
			if (source == null || mob.getTarget() != null) return false;
			if (source.standing) return !source.kind.usesStandingArea() || !holdingStandingArea(source);
			return !insidePatrol(mob, source.pos, mob.position(), settings(mob).roamRadius())
				|| !patrolMedium(mob, mob.blockPosition(), underwaterPatrol(mob.level(), mob, source.pos));
		}
		private boolean holdingStandingArea(AllySpawnerData.Source source) {
			return mob.onGround() && !mob.isInLiquid() && AllySpawnerManager.insideStandingArea(source.pos, mob.blockPosition());
		}
		@Override public boolean canContinueToUse() { return canUse(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void tick() {
			var source = source(mob);
			if (source == null || !(mob.level() instanceof ServerLevel level)) return;
			boolean wideStanding = source.standing && source.kind.usesStandingArea();
			Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
			if (wideStanding ? holdingStandingArea(source) : mob.position().distanceToSqr(top) < .09) {
				stuckTicks = 0;
				lastProgress = mob.position();
				mob.getNavigation().stop();
				return;
			}
			// Walking progress, not total travel time: a long valid detour must never trigger a snap.
			if (lastProgress == null || mob.position().distanceToSqr(lastProgress) >= .0625) {
				lastProgress = mob.position();
				stuckTicks = 0;
			} else {
				stuckTicks++;
			}
			if (--repathTicks <= 0) {
				repathTicks = 20;
				// Even an incomplete path can walk toward the platform before a later retry finds a route.
				// Use zero destination tolerance: moveTo(x,y,z) accepts a nearby node, which can leave
				// the mob at the base of the post. Drop cached partial paths before trying again.
				if (mob.onGround() || mob instanceof AerialDefender || (mob instanceof DrownedAlly && mob.isInWater())) {
					mob.getNavigation().stop();
					if (wideStanding) {
						var route = AllySpawnerManager.standingRoute(level, source, mob);
						standingDestination = route == null ? null : route.destination();
						mob.getNavigation().moveTo(route == null ? null : route.path(), 1);
					} else {
						// Wander only needs to re-enter the patrol area; exact-post Standing must reach the top.
						int reach = source.standing || mob instanceof DrownedAlly || mob instanceof AerialDefender
							? 0 : Math.max(0, settings(mob).roamRadius() - 1);
						Path path = mob.getNavigation().createPath(source.pos.above(), reach,
							Math.max(32, Math.min(64, settings(mob).recallRadius() * 2)));
						mob.getNavigation().moveTo(path, 1);
					}
				}
			}
			if (wideStanding && mob.onGround() && mob.getNavigation().isDone() && standingDestination != null
				&& mob.position().distanceToSqr(standingDestination) < 4 && AllySpawnerManager.safe(level, mob, standingDestination)) {
				// Wide mobs can finish a path just short of the destination column.
				mob.getMoveControl().setWantedPosition(standingDestination.x, standingDestination.y, standingDestination.z, 1);
			} else if (source.standing && !wideStanding && (mob.blockPosition().equals(source.pos.above())
				|| mob instanceof AerialDefender && mob.position().distanceToSqr(top) < 1 && AllySpawnerManager.safe(level, mob, top))
				&& mob.getNavigation().isDone()) {
				// Navigation may finish within the destination block but off-center. Walk the last few
				// centimetres through the movement controller rather than teleporting onto the exact center.
				mob.getMoveControl().setWantedPosition(top.x, top.y, top.z, 1);
			}
			if (stuckTicks >= STUCK_TICKS_BEFORE_TELEPORT) {
				Vec3 destination = wideStanding ? AllySpawnerManager.findStandingPosition(level, source, mob)
					: AllySpawnerManager.safe(level, mob, top) ? top : null;
				if (destination == null) return;
				mob.getNavigation().stop();
				mob.setDeltaMovement(Vec3.ZERO);
				mob.teleportTo(destination.x, destination.y, destination.z);
				mob.fallDistance = 0;
				lastProgress = mob.position();
				stuckTicks = 0;
			}
		}
		@Override public void stop() { mob.getNavigation().stop(); }
	}
	private DefenderBehavior() { }
}
