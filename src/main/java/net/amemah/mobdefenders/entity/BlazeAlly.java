package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class BlazeAlly extends Blaze implements AerialDefender {
	public BlazeAlly(EntityType<? extends Blaze> type, Level level) {
		super(type, level);
		moveControl = new FlightControl(this, () -> getAttributeValue(Attributes.MOVEMENT_SPEED));
		setNoGravity(true);
		setPersistenceRequired();
	}

	@Override protected PathNavigation createNavigation(Level level) { return new FlyingPathNavigation(this, level); }

	@Override protected void registerGoals() {
		goalSelector.addGoal(4, new Goal() {
			private int attackStep;
			private int attackTime;

			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
			@Override public boolean canUse() { return getTarget() != null && canAttack(getTarget()); }
			@Override public boolean requiresUpdateEveryTick() { return true; }
			@Override public void start() { attackStep = 0; }
			@Override public void stop() {
				getNavigation().stop();
				setAggressive(false);
			}

			@Override public void tick() {
				var enemy = getTarget();
				if (enemy == null || !canAttack(enemy) || !(level() instanceof ServerLevel server)) return;
				attackTime--;
				getLookControl().setLookAt(enemy, 10, 10);
				if (!getSensing().hasLineOfSight(enemy)) {
					if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(enemy, 1);
					return;
				}
				getNavigation().stop();
				double distance = distanceToSqr(enemy);
				if (distance < 4) {
					if (attackTime <= 0) {
						attackTime = 20;
						doHurtTarget(server, enemy);
					}
				} else if (attackTime <= 0) {
					// Vanilla windup, three six-tick shots, then a 100-tick cooldown.
					switch (++attackStep) {
						case 1 -> {
							attackTime = 60;
							setAggressive(true);
						}
						case 2, 3, 4 -> {
							attackTime = 6;
							shoot(server, enemy, distance);
						}
						default -> {
							attackTime = 100;
							attackStep = 0;
							setAggressive(false);
						}
					}
				}
			}
		});
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}

	private void shoot(ServerLevel level, LivingEntity enemy, double distance) {
		double spread = Math.sqrt(Math.sqrt(distance)) * .5 * 2.297;
		var aim = new Vec3(getRandom().triangle(enemy.getX() - getX(), spread),
			enemy.getY(.5) - getY(.5), getRandom().triangle(enemy.getZ() - getZ(), spread));
		if (!isSilent()) level.levelEvent(null, 1018, blockPosition(), 0);
		var shot = new SmallFireball(level, this, aim.normalize());
		shot.setPos(getX(), getY(.5) + .5, getZ());
		level.addFreshEntity(shot);
	}

	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }
	@Override public boolean isSensitiveToWater() { return false; }
	@Override public boolean canBreatheUnderwater() { return true; }
	@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return source.getDirectEntity() instanceof Snowball || super.isInvulnerableTo(level, source);
	}
	@Override public boolean isOnFire() { return isAggressive(); } // Native charge visuals, synced by Mob.
	@Override protected void customServerAiStep(ServerLevel level) {
		// Flight is driven by navigation instead of vanilla Blaze's unbounded climb toward its target.
	}
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}

	@Override public void move(MoverType type, Vec3 movement) {
		super.move(type, AerialDefender.limitIdleMovement(this, type, movement));
	}
}
