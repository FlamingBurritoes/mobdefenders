package net.amemah.mobdefenders.mixin;

import net.amemah.mobdefenders.entity.Defender;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ally fireballs still collide and disappear on blocks, but do not start fires. */
@Mixin(SmallFireball.class)
public abstract class FriendlyFireballMixin {
	@Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
	private void mobdefenders$preventBlockIgnition(BlockHitResult hit, CallbackInfo ci) {
		if (((SmallFireball) (Object) this).getOwner() instanceof Defender) ci.cancel();
	}
}
