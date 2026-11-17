	package net.amemah.mobdefenders.mixin;

import net.amemah.mobdefenders.entity.ZombieAlly;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zombie inheritance implies Enemy; do not let friendly golems mistake this ally for a hostile. */
@Mixin(Mob.class)
public abstract class FriendlyGolemMixin {
	@Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
	private void mobdefenders$excludeAlly(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof AbstractGolem && target instanceof ZombieAlly) cir.setReturnValue(false);
	}
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void mobdefenders$preventContactAggression(LivingEntity target, CallbackInfo ci) {
		if ((Object) this instanceof AbstractGolem && target instanceof ZombieAlly) ci.cancel();
	}
}
