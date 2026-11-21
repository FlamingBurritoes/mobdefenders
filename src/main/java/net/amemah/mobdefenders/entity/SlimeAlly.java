package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class SlimeAlly extends Slime implements Defender {
	public static final int LARGE = 4;

	public SlimeAlly(EntityType<? extends Slime> type, Level level) {
		super(type, level);
		moveControl = new CubeAllyBehavior.HopControl(this);
		setPersistenceRequired();
		if (!level.isClientSide()) setSize(LARGE, true);
	}

	public static AttributeSupplier.Builder createAllyAttributes() {
		return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE, 5);
	}

	@Override public void setSize(int size, boolean heal) {
		int tier = Mth.clamp(size, 1, LARGE);
		super.setSize(tier, false);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(switch (tier) {
			case 4 -> 18;
			case 3 -> 12;
			case 2 -> 8;
			default -> 4;
		});
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(switch (tier) {
			case 4 -> 5;
			case 3 -> 3.5;
			case 2 -> 2;
			default -> 1;
		});
		if (heal) setHealth(getMaxHealth());
	}

	@Override protected void registerGoals() {
		CubeAllyBehavior.addGoals(this, goalSelector, targetSelector, this::dealDamage);
	}

	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }
	@Override protected void dealDamage(LivingEntity target) {
		if (canAttack(target)) super.dealDamage(target);
	}
	@Override public void playerTouch(Player player) { }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
	@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		return data; // No random vanilla size at command spawn.
	}
	@Override protected void doPush(Entity entity) {
		if (entity instanceof SlimeAlly other && CubeAllyBehavior.sameFamily(this, other)) return;
		super.doPush(entity);
	}
}
