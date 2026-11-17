package net.amemah.mobdefenders.mixin;

import net.amemah.mobdefenders.entity.Defender;
import net.amemah.mobdefenders.entity.DefenderBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defender arrows pass through players, friendly/passive mobs and Creepers instead of causing friendly fire. */
@Mixin(Projectile.class)
public abstract class FriendlyProjectileMixin {
	@Shadow public abstract Entity getOwner();
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void mobdefenders$onlyHitEnemies(Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (getOwner() instanceof Defender && target instanceof LivingEntity living && !DefenderBehavior.validEnemy(living))
			cir.setReturnValue(false);
	}
}
