package net.amemah.mobdefenders.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;

final class DefenderExplosion {
	static final ExplosionDamageCalculator HOSTILES_ONLY = new ExplosionDamageCalculator() {
		@Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) { return eligible(entity); }
		@Override public float getKnockbackMultiplier(Entity entity) { return eligible(entity) ? 1 : 0; }
	};

	static boolean eligible(Entity entity) {
		return entity instanceof LivingEntity living && DefenderBehavior.validEnemy(living);
	}

	private DefenderExplosion() { }
}
