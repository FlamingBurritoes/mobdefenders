package net.amemah.mobdefenders.mixin;

import net.amemah.mobdefenders.entity.Defender;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.1.2 handles daylight burning in Mob, selected by the burn_in_daylight entity-type tag. */
@Mixin(Mob.class)
public abstract class SunSafeDefenderMixin {
	@Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
	private void mobdefenders$skipSunlight(CallbackInfo ci) {
		if ((Object) this instanceof Defender) ci.cancel();
	}
}
