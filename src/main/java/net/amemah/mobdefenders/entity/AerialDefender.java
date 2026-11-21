package net.amemah.mobdefenders.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

import java.util.function.DoubleSupplier;

/** Shared flight controls and idle bounds for Blaze and Ghast guards. */
public interface AerialDefender extends Defender {
	int ROAM_HEIGHT = 7;

	static Vec3 limitIdleMovement(Mob mob, MoverType type, Vec3 movement) {
		if (type != MoverType.SELF || mob.getTarget() != null) return movement;
		var source = DefenderBehavior.source(mob);
		if (source == null) return movement;
		double min = source.pos.getY(), max = min + ROAM_HEIGHT;
		double dy = Mth.clamp(movement.y, Math.min(0, min - mob.getY()), Math.max(0, max - mob.getY()));
		if (dy == movement.y) return movement;
		mob.setDeltaMovement(mob.getDeltaMovement().multiply(1, 0, 1));
		return new Vec3(movement.x, dy, movement.z);
	}

	final class FlightControl extends MoveControl {
		private final DoubleSupplier speed;

		FlightControl(Mob mob, DoubleSupplier speed) {
			super(mob);
			this.speed = speed;
		}

		@Override public void tick() {
			mob.setNoGravity(true);
			if (operation != Operation.MOVE_TO) {
				mob.setDeltaMovement(mob.getDeltaMovement().scale(.35));
				return;
			}
			operation = Operation.WAIT;
			Vec3 delta = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
			double distance = delta.length();
			if (distance < .02) {
				mob.setDeltaMovement(Vec3.ZERO);
				return;
			}
			if (delta.horizontalDistanceSqr() > .001)
				mob.setYRot(rotlerp(mob.getYRot(), (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90, 90));
			mob.setDeltaMovement(delta.scale(Math.min(distance * .6, speedModifier * speed.getAsDouble()) / distance));
		}
	}
}
