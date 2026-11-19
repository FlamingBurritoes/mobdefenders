package net.amemah.mobdefenders.mixin;

import net.amemah.mobdefenders.entity.CreeperAlly;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace only the ally's private detonation hook, leaving every vanilla creeper unchanged. */
@Mixin(Creeper.class)
public abstract class FriendlyCreeperExplosionMixin {
	@Shadow private int explosionRadius;
	@Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
	private void mobdefenders$friendlyBlast(CallbackInfo ci) {
		if ((Object) this instanceof CreeperAlly ally) {
			ally.explodeSafely(explosionRadius);
			ci.cancel();
		}
	}
}
