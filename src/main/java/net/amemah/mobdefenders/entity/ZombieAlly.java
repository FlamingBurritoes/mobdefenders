package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Vanilla zombie melee/animation with shared defender targeting and guard behavior. */
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
		goalSelector.addGoal(2, new ZombieAttackGoal(this, 1, false));
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}
	public static boolean validEnemy(LivingEntity target) { return DefenderBehavior.validEnemy(target); }
	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target == null || canAttack(target) ? target : null); }
	@Override protected boolean convertsInWater() { return false; }
	@Override protected boolean isSunSensitive() { return false; }
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
