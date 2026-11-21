package net.amemah.mobdefenders.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Brute equipment and animations, without the hostile piglin brain competing with guard goals. */
public final class BruteAlly extends AbstractPiglin implements Defender {
	public BruteAlly(EntityType<? extends AbstractPiglin> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setImmuneToZombification(true);
		var axe = new ItemStack(Items.GOLDEN_AXE);
		axe.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		setItemSlot(EquipmentSlot.MAINHAND, axe);
		setDropChance(EquipmentSlot.MAINHAND, 0);
	}

	@Override protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false) {
			@Override public void start() {
				super.start();
				playAngrySound();
			}
		});
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}

	@Override public LivingEntity getTarget() { return asValidTarget(getTargetUnchecked()); } // Use the goal target, not brain memory.
	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }
	@Override public void setImmuneToZombification(boolean immune) { super.setImmuneToZombification(true); }
	@Override protected boolean canHunt() { return false; }
	@Override public void setCanPickUpLoot(boolean allowed) { super.setCanPickUpLoot(false); }
	@Override public boolean wantsToPickUp(ServerLevel level, ItemStack stack) { return false; }
	@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		return data; // Keep the issued axe and skip vanilla spawn randomization.
	}

	@Override public PiglinArmPose getArmPose() {
		return isAggressive() && isHoldingMeleeWeapon() ? PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON : PiglinArmPose.DEFAULT;
	}
	@Override public void playAmbientSound() { makeSound(SoundEvents.PIGLIN_BRUTE_AMBIENT); }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PIGLIN_BRUTE_HURT; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.PIGLIN_BRUTE_DEATH; }
	@Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.PIGLIN_BRUTE_STEP, .15F, 1); }
	private void playAngrySound() { playSound(SoundEvents.PIGLIN_BRUTE_ANGRY, getSoundVolume(), getVoicePitch()); }
	@Override protected void playConvertedSound() { } // Conversion is permanently disabled.
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
