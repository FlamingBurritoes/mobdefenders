package net.amemah.mobdefenders.entity;

import net.amemah.mobdefenders.config.ZombieSpawnerConfig;
import net.amemah.mobdefenders.spawner.ZombieSpawnerData;
import net.amemah.mobdefenders.spawner.ZombieSpawnerManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

/** Vanilla zombie movement, melee, sounds and animation, with exclusively friendly targeting. */
public final class ZombieAlly extends Zombie implements Defender {
	public ZombieAlly(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}
	public static AttributeSupplier.Builder createAllyAttributes() {
		return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 3)
			.add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.ARMOR, 2)
			.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0);
	}
	@Override protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new ZombieAttackGoal(this, 1, false));
		goalSelector.addGoal(4, new ReturnToGuardGoal());
		goalSelector.addGoal(7, new WanderAroundSpawnerGoal());
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
			(entity, level) -> validEnemy(entity)));
	}
	public static boolean validEnemy(LivingEntity entity) {
		return entity instanceof Enemy && !(entity instanceof Creeper) && !(entity instanceof ZombieAlly) && entity.isAlive();
	}
	@Override public boolean canAttack(LivingEntity entity) {
		return validEnemy(entity) && withinGuardRange(entity.position()) && super.canAttack(entity);
	}
	@Override public void setTarget(LivingEntity target) {
		super.setTarget(target == null || canAttack(target) ? target : null);
	}
	private ZombieSpawnerData.Source source() {
		return level() instanceof ServerLevel server ? ZombieSpawnerData.get(server).owner(getUUID()) : null;
	}
	private boolean standing() {
		var source = source();
		return source == null || source.standing;
	}
	private boolean withinGuardRange(Vec3 position) {
		var source = source();
		return source == null || ZombieSpawnerManager.inside(source.pos, position, ZombieSpawnerConfig.get().roamRadius());
	}
	@Override protected boolean convertsInWater() { return false; }
	@Override protected boolean isSunSensitive() { return false; }
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) { return source() == null && super.canUsePortal(passenger); }
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || source() == null) && super.canTeleport(from, to);
	}

	/** Sample reachable destinations around the source, not around the mob or beyond an invisible wall. */
	private final class WanderAroundSpawnerGoal extends Goal {
		private Path path;
		private int nextAttempt;
		WanderAroundSpawnerGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
		@Override public boolean canUse() {
			var source = source();
			if (source == null || source.standing || getTarget() != null || tickCount < nextAttempt
				|| !(level() instanceof ServerLevel server)) return false;
			nextAttempt = tickCount + 20;
			int radius = ZombieSpawnerConfig.get().roamRadius();
			int pathRadius = ZombieSpawnerManager.inside(source.pos, position(), radius)
				? radius : ZombieSpawnerConfig.get().recallRadius();
			for (int attempt = 0; attempt < 24; attempt++) {
				int dx = getRandom().nextInt(radius * 2 + 1) - radius;
				int dz = getRandom().nextInt(radius * 2 + 1) - radius;
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = 3; dy >= -3; dy--) {
					BlockPos feet = source.pos.offset(dx, dy, dz);
					Vec3 destination = Vec3.atBottomCenterOf(feet);
					if (position().distanceToSqr(destination) < 2.25
						|| !ZombieSpawnerManager.inside(source.pos, destination, radius)
						|| !ZombieSpawnerManager.safe(server, ZombieAlly.this, destination)) continue;
					Path candidate = getNavigation().createPath(feet, 0);
					if (candidate == null || !candidate.canReach()) continue;
					boolean inside = true;
					for (int i = 0; i < candidate.getNodeCount(); i++) {
						if (!ZombieSpawnerManager.inside(source.pos,
							Vec3.atBottomCenterOf(candidate.getNodePos(i)), pathRadius)) {
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
		@Override public void start() { getNavigation().moveTo(path, 1); }
		@Override public boolean canContinueToUse() {
			return !standing() && getTarget() == null && !getNavigation().isDone();
		}
		@Override public void stop() {
			getNavigation().stop();
			path = null;
			nextAttempt = tickCount + 20;
		}
	}

	private final class ReturnToGuardGoal extends Goal {
		private int travelTicks;
		ReturnToGuardGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
		@Override public void start() { travelTicks = 0; }
		@Override public boolean canUse() {
			return standing() && source() != null && getTarget() == null;
		}
		@Override public boolean canContinueToUse() { return canUse(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void tick() {
			var source = source();
			if (source == null || !(level() instanceof ServerLevel server)) return;
			Vec3 top = Vec3.atBottomCenterOf(source.pos.above());
			if (position().distanceToSqr(top) < 0.09) {
				travelTicks = 0;
				getNavigation().stop();
				if (ZombieSpawnerManager.safe(server, ZombieAlly.this, top)) teleportTo(top.x, top.y, top.z);
			} else if (++travelTicks % 20 == 0) {
				// A one-block platform may be unreachable from a hole; return safely, never into solids.
				boolean path = getNavigation().moveTo(top.x, top.y, top.z, 1);
				if ((!path || travelTicks >= 100 || position().distanceToSqr(top) < 2.25)
					&& ZombieSpawnerManager.safe(server, ZombieAlly.this, top)) {
					getNavigation().stop();
					teleportTo(top.x, top.y, top.z);
				}
			}
		}
		@Override public void stop() { getNavigation().stop(); }
	}
}
