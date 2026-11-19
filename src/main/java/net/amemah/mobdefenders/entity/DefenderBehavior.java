package net.amemah.mobdefenders.entity;

import net.amemah.mobdefenders.config.AllySpawnerConfig;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.AllySpawnerManager;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** One guard/patrol implementation for every defender; entity subclasses supply their vanilla combat. */
public final class DefenderBehavior {
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
	public static void addGoals(PathfinderMob mob, GoalSelector goals, GoalSelector targets) {
		goals.addGoal(0, new FloatGoal(mob));
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
		private final PathfinderMob mob;
		private Path path;
		private int nextAttempt;
		WanderAroundSpawnerGoal(PathfinderMob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE));
		}
		@Override public boolean canUse() {
			var source = source(mob);
			if (source == null || source.standing || mob.getTarget() != null || mob.tickCount < nextAttempt
				|| !(mob.level() instanceof ServerLevel level)) return false;
			nextAttempt = mob.tickCount + 20;
			int radius = settings(mob).roamRadius();
			int pathRadius = AllySpawnerManager.inside(source.pos, mob.position(), radius)
				? radius : settings(mob).recallRadius();
			for (int attempt = 0; attempt < 24; attempt++) {
				int dx = mob.getRandom().nextInt(radius * 2 + 1) - radius;
				int dz = mob.getRandom().nextInt(radius * 2 + 1) - radius;
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = 3; dy >= -3; dy--) {
					BlockPos feet = source.pos.offset(dx, dy, dz);
					Vec3 destination = Vec3.atBottomCenterOf(feet);
					if (mob.position().distanceToSqr(destination) < 2.25
						|| !AllySpawnerManager.inside(source.pos, destination, radius)
						|| !AllySpawnerManager.safe(level, mob, destination)) continue;
					Path candidate = mob.getNavigation().createPath(feet, 0);
					if (candidate == null || !candidate.canReach()) continue;
					boolean inside = true;
					for (int i = 0; i < candidate.getNodeCount(); i++) {
						if (!AllySpawnerManager.inside(source.pos, Vec3.atBottomCenterOf(candidate.getNodePos(i)), pathRadius)) {
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
		private final PathfinderMob mob;
		private int repathTicks;
		private int stuckTicks;
		private Vec3 lastProgress;
		ReturnToGuardGoal(PathfinderMob mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE)); }
		@Override public void start() {
			repathTicks = 0;
			stuckTicks = 0;
			lastProgress = mob.position();
		}
		@Override public boolean canUse() {
			var source = source(mob);
			return source != null && mob.getTarget() == null && (source.standing
				|| !AllySpawnerManager.inside(source.pos, mob.position(), settings(mob).roamRadius()));
		}
		@Override public boolean canContinueToUse() { return canUse(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void tick() {
			var source = source(mob);
			if (source == null || !(mob.level() instanceof ServerLevel level)) return;
			Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
			if (mob.position().distanceToSqr(top) < .09) {
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
				if (mob.onGround()) {
					mob.getNavigation().stop();
					// Wander only needs to re-enter the patrol area; Standing must reach the post.
					int reach = source.standing ? 0 : Math.max(0, settings(mob).roamRadius() - 1);
					Path path = mob.getNavigation().createPath(source.pos.above(), reach,
						Math.max(32, Math.min(64, settings(mob).recallRadius() * 2)));
					mob.getNavigation().moveTo(path, 1);
				}
			}
			if (source.standing && mob.blockPosition().equals(source.pos.above()) && mob.getNavigation().isDone()) {
				// Navigation may finish within the destination block but off-center. Walk the last few
				// centimetres through the movement controller rather than teleporting onto the exact center.
				mob.getMoveControl().setWantedPosition(top.x, top.y, top.z, 1);
			}
			if (stuckTicks >= STUCK_TICKS_BEFORE_TELEPORT && AllySpawnerManager.safe(level, mob, top)) {
				mob.getNavigation().stop();
				mob.setDeltaMovement(Vec3.ZERO);
				mob.teleportTo(top.x, top.y, top.z);
				mob.fallDistance = 0;
				lastProgress = mob.position();
				stuckTicks = 0;
			}
		}
		@Override public void stop() { mob.getNavigation().stop(); }
	}
	private DefenderBehavior() { }
}
