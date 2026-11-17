package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.sounds.SoundEvents;

/** Vanilla skeleton bow/strafe combat, shared friendly targeting, and no hostile Stray conversion. */
public final class SkeletonAlly extends Skeleton implements Defender {
	public SkeletonAlly(EntityType<? extends Skeleton> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		setDropChance(EquipmentSlot.MAINHAND, 0);
	}
	public static AttributeSupplier.Builder createAllyAttributes() {
		return Skeleton.createAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, .25)
			.add(Attributes.ARMOR, 0);
	}
	@Override protected void registerGoals() {
		// AbstractSkeleton's constructor/reassessWeaponGoal installs its real bow/melee goal at priority 4.
		goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6, 1, 1.2));
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}
	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target == null || canAttack(target) ? target : null); }
	@Override public void performRangedAttack(LivingEntity target, float power) {
		if (!canAttack(target) || !(level() instanceof ServerLevel level)) return;
		ItemStack bow = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
		ItemStack ammunition = getProjectile(bow);
		AbstractArrow arrow = getArrow(ammunition, power, bow);
		double dx = target.getX() - getX(), dz = target.getZ() - getZ();
		double dy = target.getY(1.0 / 3.0) - arrow.getY() + Math.sqrt(dx * dx + dz * dz) * .2;
		// Same vanilla shot, pinned to Normal's inaccuracy (14 - 2*4) on every world difficulty.
		Projectile.spawnProjectileUsingShoot(arrow, level, ammunition, dx, dy, dz, 1.6F, 6);
		playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (getRandom().nextFloat() * .4F + .8F));
	}
	@Override protected AbstractArrow getArrow(ItemStack ammunition, float power, ItemStack bow) {
		AbstractArrow arrow = super.getArrow(ammunition, power, bow);
		arrow.setBaseDamage(power * 2 + getRandom().triangle(.22, .57425)); // Vanilla Normal's random damage distribution.
		return arrow;
	}
	@Override protected int getHardAttackInterval() { return getAttackInterval(); } // Normal's 40-tick bow cooldown.
	@Override public void startFreezeConversion(int ticks) { setFreezeConverting(false); }
	@Override protected void doFreezeConversion() { setFreezeConverting(false); }
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}
}
