package net.amemah.mobdefenders.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/** Amphibious defender with a concurrent close-range strike and vanilla-style trident throws. */
public final class DrownedAlly extends Drowned implements Defender {
	public DrownedAlly(EntityType<? extends Drowned> type, Level level) {
		super(type, level);
		moveControl = new GuardSwimControl();
		setPersistenceRequired();
		setCanPickUpLoot(false);
		var trident = new ItemStack(Items.TRIDENT);
		// Keep Normal Drowned melee damage rather than adding a player-held trident's attack modifier.
		trident.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		trident.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		setItemSlot(EquipmentSlot.MAINHAND, trident);
		setDropChance(EquipmentSlot.MAINHAND, 0);
	}

	public static AttributeSupplier.Builder createAllyAttributes() {
		return Drowned.createAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, .23)
			.add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.ARMOR, 2)
			.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0);
	}

	@Override protected void registerGoals() {
		// No hostile vanilla goals (seek water/land, surface swimming, turtles or turtle eggs).
		goalSelector.addGoal(2, new RangedAttackGoal(this, 1, 40, 10) {
			private int nextStrike;
			@Override public boolean canUse() {
				var enemy = getTarget();
				return enemy != null && canAttack(enemy) && super.canUse();
			}
			@Override public boolean canContinueToUse() { return canUse(); }
			@Override public void start() {
				setAggressive(true);
				startUsingItem(InteractionHand.MAIN_HAND);
			}
			@Override public void stop() {
				super.stop();
				getNavigation().stop();
				stopUsingItem();
				setAggressive(false);
			}
			@Override public void tick() {
				super.tick();
				var enemy = getTarget();
				if (level() instanceof ServerLevel server && enemy != null && canAttack(enemy)
					&& tickCount >= nextStrike && isWithinMeleeAttackRange(enemy) && getSensing().hasLineOfSight(enemy)) {
					nextStrike = tickCount + 20;
					swing(InteractionHand.MAIN_HAND);
					doHurtTarget(server, enemy);
				}
			}
		});
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
	}

	@Override public boolean canAttack(LivingEntity target) {
		return DefenderBehavior.canAttack(this, target) && super.canAttack(target);
	}
	@Override public void setTarget(LivingEntity target) { super.setTarget(target != null && canAttack(target) ? target : null); }
	@Override public boolean wantsToPickUp(ServerLevel level, ItemStack stack) { return false; }
	@Override public boolean canPickUpLoot() { return false; }
	@Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		return data; // No vanilla equipment rolls, babies or jockeys, even for summons.
	}
	@Override protected boolean isSunSensitive() { return false; }
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return false; }
	@Override public boolean canUsePortal(boolean passenger) {
		return DefenderBehavior.source(this) == null && super.canUsePortal(passenger);
	}
	@Override public boolean canTeleport(Level from, Level to) {
		return (from == to || DefenderBehavior.source(this) == null) && super.canTeleport(from, to);
	}

	@Override public void performRangedAttack(LivingEntity enemy, float power) {
		if (!(level() instanceof ServerLevel server) || !canAttack(enemy)) return;
		ItemStack trident = getMainHandItem();
		if (!trident.is(Items.TRIDENT)) return;
		var projectile = new ThrownTrident(server, this, trident);
		projectile.pickup = AbstractArrow.Pickup.DISALLOWED;
		double dx = enemy.getX() - getX(), dz = enemy.getZ() - getZ();
		double dy = enemy.getY(1.0 / 3) - projectile.getY() + Math.sqrt(dx * dx + dz * dz) * .2;
		Projectile.spawnProjectileUsingShoot(projectile, server, trident, dx, dy, dz, 1.6F, 6);
		playSound(SoundEvents.DROWNED_SHOOT, 1, 1 / (getRandom().nextFloat() * .4F + .8F));
	}

	@Override public void updateSwimming() {
		// Enable vanilla swim travel/pose without installing any water-seeking or surface-seeking goal.
		setSearchingForLand(isInWater());
		super.updateSwimming();
	}

	@Override public void move(MoverType type, Vec3 movement) {
		if (type == MoverType.SELF && isInWater() && getTarget() == null) {
			var source = DefenderBehavior.source(this);
			if (source != null) {
				double min = source.pos.getY() - DefenderBehavior.UNDERWATER_ROAM_HEIGHT;
				double max = source.pos.getY() + DefenderBehavior.UNDERWATER_ROAM_HEIGHT;
				// Inertia cannot cross the patrol ceiling/floor. If combat took us outside, allow only progress inward.
				double dy = Mth.clamp(movement.y, Math.min(0, min - getY()), Math.max(0, max - getY()));
				if (dy != movement.y) {
					movement = new Vec3(movement.x, dy, movement.z);
					setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
				}
			}
		}
		super.move(type, movement);
	}

	private final class GuardSwimControl extends MoveControl {
		GuardSwimControl() { super(DrownedAlly.this); }
		@Override public void tick() {
			if (!isInWater()) {
				super.tick();
				return;
			}
			setSpeed(0);
			if (operation != Operation.MOVE_TO) {
				setDeltaMovement(getDeltaMovement().scale(.5));
				return;
			}
			operation = Operation.WAIT;
			Vec3 delta = new Vec3(wantedX - getX(), wantedY - getY(), wantedZ - getZ());
			double distance = delta.length();
			if (distance < .04) {
				setDeltaMovement(Vec3.ZERO);
				return;
			}
			if (delta.horizontalDistanceSqr() > .001)
				setYRot(rotlerp(getYRot(), (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90), 90));
			double pace = speedModifier * getAttributeValue(Attributes.MOVEMENT_SPEED) * .65;
			setDeltaMovement(delta.scale(Math.min(distance * .4, pace) / distance));
		}
	}
}
