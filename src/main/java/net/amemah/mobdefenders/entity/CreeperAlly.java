package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;

/** Vanilla creeper stats/fuse/chase, shared guard AI and a hostile-only, terrain-safe blast. */
public final class CreeperAlly extends Creeper implements Defender {
	private static final ExplosionDamageCalculator SAFE_BLAST = new ExplosionDamageCalculator() {
		private boolean eligible(Entity entity) {
			return entity instanceof LivingEntity living && DefenderBehavior.validEnemy(living);
		}
		@Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) { return eligible(entity); }
		@Override public float getKnockbackMultiplier(Entity entity) { return eligible(entity) ? 1 : 0; }
	};

	public CreeperAlly(EntityType<? extends Creeper> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}
	@Override protected void registerGoals() {
		goalSelector.addGoal(2, new SwellGoal(this));
		goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6, 1, 1.2));
		goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6, 1, 1.2));
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1, false));
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}
	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target == null || canAttack(target) ? target : null); }

	/** Called only at the vanilla fuse's detonation point by FriendlyCreeperExplosionMixin. */
	public void explodeSafely(int radius) {
		if (!(level() instanceof ServerLevel level)) return;
		dead = true;
		level.explode(this, null, SAFE_BLAST, getX(), getY(), getZ(),
			radius * (isPowered() ? 2F : 1F), false, Level.ExplosionInteraction.NONE);
		// Do not leave an unfiltered potion cloud or secondary death-effect blast near friends.
		// Permanent discard is observed by the shared manager and starts the replacement timer.
		discard();
	}
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
