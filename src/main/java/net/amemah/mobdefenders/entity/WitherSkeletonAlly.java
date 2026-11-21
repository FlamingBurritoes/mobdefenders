package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Native stone-sword combat and Wither on hit, with shared defender targeting. */
public final class WitherSkeletonAlly extends WitherSkeleton implements Defender {
	public WitherSkeletonAlly(EntityType<? extends WitherSkeleton> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
		setDropChance(EquipmentSlot.MAINHAND, 0);
	}

	public static AttributeSupplier.Builder createAllyAttributes() {
		// Vanilla assigns this base value during finalizeSpawn, which ally spawners skip.
		return WitherSkeleton.createAttributes().add(Attributes.ATTACK_DAMAGE, 4);
	}

	@Override protected void registerGoals() {
		// AbstractSkeleton installs its weapon goal separately; don't install vanilla hostile targets.
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}

	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}

	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }

	@Override public boolean doHurtTarget(ServerLevel level, Entity target) {
		return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level, target);
	}

	@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		return data; // Keep the issued stone sword, including on command spawns.
	}

	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
