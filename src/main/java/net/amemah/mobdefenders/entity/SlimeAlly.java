package net.amemah.mobdefenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

public final class SlimeAlly extends Slime implements Defender {
	public static final int LARGE = 4;

	public SlimeAlly(EntityType<? extends Slime> type, Level level) {
		super(type, level);
		moveControl = new HopControl();
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
		goalSelector.addGoal(2, new Goal() {
			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
			@Override public boolean canUse() { return getTarget() != null && canAttack(getTarget()); }
			@Override public boolean requiresUpdateEveryTick() { return true; }
			@Override public void tick() {
				var enemy = getTarget();
				if (enemy == null || !canAttack(enemy)) return;
				getLookControl().setLookAt(enemy, 30, 30);
				if (!isWithinMeleeAttackRange(enemy) || !getSensing().hasLineOfSight(enemy)) {
					if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(enemy, 1);
				} else {
					getNavigation().stop();
					dealDamage(enemy);
				}
			}
			@Override public void stop() { getNavigation().stop(); }
		});
		DefenderBehavior.addGoals(this, goalSelector, targetSelector);
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
		var source = DefenderBehavior.source(this);
		if (entity instanceof SlimeAlly other && source != null && source == DefenderBehavior.source(other)) return;
		super.doPush(entity);
	}

	/** Follow ordinary path nodes with hops, instead of vanilla slime's unbounded random hopping. */
	private final class HopControl extends MoveControl {
		private int cooldown;
		HopControl() { super(SlimeAlly.this); }
		@Override public void tick() {
			if (cooldown > 0) cooldown--;
			if (operation != Operation.MOVE_TO) {
				mob.setZza(0);
				return;
			}
			operation = Operation.WAIT;
			double dx = wantedX - getX(), dz = wantedZ - getZ();
			double horizontal = dx * dx + dz * dz;
			if (horizontal < .0144 && Math.abs(wantedY - getY()) < .35) {
				mob.setZza(0);
				return;
			}
			if (horizontal > .0001) mob.setYRot(rotlerp(getYRot(), (float) Math.toDegrees(Math.atan2(dz, dx)) - 90, 90));
			if (onGround() && cooldown > 0) {
				mob.setZza(0);
				return;
			}
			double arrivalSpeed = Math.min(1, Math.sqrt(horizontal) * 2.5);
			mob.setSpeed((float) (speedModifier * getAttributeValue(Attributes.MOVEMENT_SPEED) * arrivalSpeed));
			if (onGround()) {
				cooldown = getTarget() == null ? 10 : 5;
				getJumpControl().jump();
			}
		}
	}
}
