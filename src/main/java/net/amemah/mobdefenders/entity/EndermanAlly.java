package net.amemah.mobdefenders.entity;

import net.amemah.mobdefenders.spawner.AllySpawnerManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** Native Enderman combat pose and movement, with no player provocation or block handling. */
public final class EndermanAlly extends EnderMan implements Defender {
	public EndermanAlly(EntityType<? extends EnderMan> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	@Override protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false));
		targetSelector.addGoal(0, new HurtByTargetGoal(this) {
			@Override public boolean canUse() {
				var attacker = EndermanAlly.this.getLastHurtByMob();
				return attacker != null && EndermanAlly.this.canAttack(attacker) && super.canUse();
			}
			@Override protected double getFollowDistance() {
				return DefenderBehavior.settings(EndermanAlly.this).detectionRadius();
			}
		});
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}

	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) {
		super.setTarget(target != null && canAttack(target) ? target : null);
	}
	@Override public void setBeingStaredAt() { }
	@Override public void setCarriedBlock(BlockState state) { }
	// Use the active enemy as the only source of the native combat pose, not persistent player anger.
	@Override public void updatePersistentAnger(ServerLevel level, boolean updateAnger) { }
	// EnderMan's only custom AI step is the random daylight retreat.
	@Override protected void customServerAiStep(ServerLevel level) { }

	@Override public boolean isSensitiveToWater() { return false; }
	@Override public boolean canBreatheUnderwater() { return true; }
	@Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// A direct clean-water splash can reach EnderMan's special damage handler.
		if (source.getDirectEntity() instanceof AbstractThrownPotion potion
			&& potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override protected boolean teleport() {
		if (getTarget() == null || !(level() instanceof ServerLevel level)) return false;
		var source = DefenderBehavior.source(this);
		if (source == null) return super.teleport();

		int roam = DefenderBehavior.settings(this).roamRadius();
		double x = getX() + (getRandom().nextDouble() - .5) * roam * 2;
		double z = getZ() + (getRandom().nextDouble() - .5) * roam * 2;
		for (int dy = 2; dy >= -2; dy--) {
			Vec3 destination = new Vec3(x, blockPosition().getY() + dy, z);
			if (!AllySpawnerManager.inside(source.pos, destination, roam)
				|| !AllySpawnerManager.safe(level, this, destination)) continue;
			Vec3 previous = position();
			if (!randomTeleport(destination.x, destination.y, destination.z, true)) continue;
			level.gameEvent(this, GameEvent.TELEPORT, previous);
			if (!isSilent()) {
				level.playSound(null, previous.x, previous.y, previous.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1, 1);
				playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
			}
			return true;
		}
		return false;
	}

	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
