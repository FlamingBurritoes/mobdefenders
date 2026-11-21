package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class MagmaCubeAlly extends MagmaCube implements Defender {
	public static final int LARGE = 4;

	public MagmaCubeAlly(EntityType<? extends MagmaCube> type, Level level) {
		super(type, level);
		moveControl = new CubeAllyBehavior.HopControl(this);
		setPersistenceRequired();
		if (!level.isClientSide()) setSize(LARGE, true);
	}

	@Override public void setSize(int size, boolean heal) {
		int tier = size >= LARGE ? LARGE : size >= 2 ? 2 : 1;
		super.setSize(tier, false);
		getAttribute(Attributes.ARMOR).setBaseValue(0); // Like the Slime Ally, HP is the whole size-based defense.
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(switch (tier) {
			case LARGE -> 22;
			case 2 -> 12;
			default -> 7;
		});
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(switch (tier) {
			case LARGE -> 8;
			case 2 -> 5;
			default -> 2.5;
		});
		if (heal) setHealth(getMaxHealth());
	}

	@Override protected void registerGoals() { CubeAllyBehavior.addGoals(this, goalSelector, targetSelector, this::dealDamage); }
	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }
	@Override protected float getAttackDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); } // No vanilla +2.
	@Override protected void dealDamage(LivingEntity target) {
		if (!(level() instanceof ServerLevel server) || !isAlive() || !canAttack(target)
			|| !isWithinMeleeAttackRange(target) || !hasLineOfSight(target)
			|| !target.hurtServer(server, damageSources().mobAttack(this), getAttackDamage())) return;
		if (getRandom().nextInt(100) < 33 && !target.fireImmune()) target.igniteForSeconds(5);
		playSound(SoundEvents.SLIME_ATTACK, 1, 1 + (getRandom().nextFloat() - getRandom().nextFloat()) * .2F);
		setLastHurtMob(target);
	}

	@Override public void playerTouch(Player player) { }
	@Override protected void doPush(Entity entity) {
		if (entity instanceof MagmaCubeAlly other && CubeAllyBehavior.sameFamily(this, other)) return;
		super.doPush(entity);
	}
	@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		return data; // No random vanilla size at command spawn.
	}
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
