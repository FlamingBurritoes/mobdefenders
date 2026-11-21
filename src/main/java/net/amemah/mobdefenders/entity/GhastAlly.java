package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class GhastAlly extends Ghast implements AerialDefender {
	public GhastAlly(EntityType<? extends Ghast> type, Level level) {
		super(type, level);
		moveControl = new FlightControl(this, () -> getAttributeValue(Attributes.FLYING_SPEED));
		setNoGravity(true);
		setPersistenceRequired();
	}

	@Override protected PathNavigation createNavigation(Level level) { return new FlyingPathNavigation(this, level); }

	@Override protected void registerGoals() {
		goalSelector.addGoal(4, new Goal() {
			private int charge;
			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
			@Override public boolean canUse() { return getTarget() != null && canAttack(getTarget()); }
			@Override public boolean requiresUpdateEveryTick() { return true; }
			@Override public void start() { charge = 0; }
			@Override public void stop() {
				getNavigation().stop();
				setCharging(false);
			}
			@Override public void tick() {
				var target = getTarget();
				if (target == null || !canAttack(target) || !(level() instanceof ServerLevel server)) return;
				getLookControl().setLookAt(target, 30, 30);
				if (!getSensing().hasLineOfSight(target)) {
					if (charge > 0) charge--;
					if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(target, 1);
				} else {
					getNavigation().stop();
					if (++charge == 10 && !isSilent()) server.levelEvent(null, 1015, blockPosition(), 0);
					if (charge == 20) {
						shoot(server, target);
						charge = -40;
					}
				}
				setCharging(charge > 10);
			}
		});
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}

	private void shoot(ServerLevel server, LivingEntity target) {
		var from = position().add(0, getBbHeight() * .75, 0);
		var aim = target.position().add(0, target.getBbHeight() * .5, 0);
		// Scale the muzzle offset to this model rather than skipping four blocks as vanilla does.
		if (!isSilent()) server.levelEvent(null, 1016, blockPosition(), 0);
		server.addFreshEntity(new GhastAllyFireball(server, this, from, aim.subtract(from)));
	}

	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }
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
