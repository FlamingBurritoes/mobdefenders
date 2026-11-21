package net.amemah.mobdefenders.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Slime;

import java.util.EnumSet;
import java.util.function.Consumer;

/** Shared path-based hops and contact combat for both split-capable allies. */
final class CubeAllyBehavior {
	static void addGoals(Slime cube, GoalSelector goals, GoalSelector targets, Consumer<LivingEntity> hit) {
		goals.addGoal(2, new Goal() {
			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
			@Override public boolean canUse() { return cube.getTarget() != null && cube.canAttack(cube.getTarget()); }
			@Override public boolean requiresUpdateEveryTick() { return true; }
			@Override public void tick() {
				var enemy = cube.getTarget();
				if (enemy == null || !cube.canAttack(enemy)) return;
				cube.getLookControl().setLookAt(enemy, 30, 30);
				if (!cube.isWithinMeleeAttackRange(enemy) || !cube.getSensing().hasLineOfSight(enemy)) {
					if (cube.tickCount % 10 == 0 || cube.getNavigation().isDone()) cube.getNavigation().moveTo(enemy, 1);
				} else {
					cube.getNavigation().stop();
					hit.accept(enemy);
				}
			}
			@Override public void stop() { cube.getNavigation().stop(); }
		});
		DefenderBehavior.addGoals(cube, goals, targets);
	}

	static boolean sameFamily(Slime cube, Slime other) {
		var source = DefenderBehavior.source(cube);
		return source != null && source == DefenderBehavior.source(other);
	}

	static final class HopControl extends MoveControl {
		private int cooldown;
		HopControl(Slime cube) { super(cube); }

		@Override public void tick() {
			if (cooldown > 0) cooldown--;
			if (operation != Operation.MOVE_TO) {
				mob.setZza(0);
				return;
			}
			operation = Operation.WAIT;
			double dx = wantedX - mob.getX(), dz = wantedZ - mob.getZ();
			double horizontal = dx * dx + dz * dz;
			if (horizontal < .0144 && Math.abs(wantedY - mob.getY()) < .35) {
				mob.setZza(0);
				return;
			}
			if (horizontal > .0001) mob.setYRot(rotlerp(mob.getYRot(), (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90), 90));
			if (mob.onGround() && cooldown > 0) {
				mob.setZza(0);
				return;
			}
			double arrivalSpeed = Math.min(1, Math.sqrt(horizontal) * 2.5);
			mob.setSpeed((float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * arrivalSpeed));
			if (mob.onGround()) {
				cooldown = mob.getTarget() == null ? 10 : 5;
				mob.getJumpControl().jump();
			}
		}
	}

	private CubeAllyBehavior() { }
}
