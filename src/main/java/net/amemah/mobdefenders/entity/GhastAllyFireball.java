package net.amemah.mobdefenders.entity;

import net.amemah.mobdefenders.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Keeps its protection after deflection, owner loss, or a save/reload. */
public final class GhastAllyFireball extends LargeFireball {
	public GhastAllyFireball(EntityType<? extends LargeFireball> type, Level level) {
		super(type, level);
	}

	public GhastAllyFireball(Level level, GhastAlly owner, Vec3 start, Vec3 direction) {
		this(ModContent.GHAST_ALLY_FIREBALL, level);
		setOwner(owner);
		setPos(start.x, start.y, start.z);
		setDeltaMovement(direction.normalize().scale(accelerationPower));
	}

	@Override protected boolean canHitEntity(Entity target) {
		return DefenderExplosion.eligible(target) && super.canHitEntity(target);
	}

	@Override protected void onHit(HitResult hit) {
		if (!(level() instanceof ServerLevel server) || isRemoved()) return;
		if (hit instanceof EntityHitResult contact) {
			if (!DefenderExplosion.eligible(contact.getEntity())) return;
			super.onHitEntity(contact); // Vanilla six-point direct hit.
		}
		server.explode(this, null, DefenderExplosion.HOSTILES_ONLY, getX(), getY(), getZ(), 1, false, Level.ExplosionInteraction.NONE);
		discard();
	}
}
