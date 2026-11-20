package net.amemah.mobdefenders.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.amemah.mobdefenders.entity.SlimeAlly;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Owned ally splitting is handled at death by the spawner, not by vanilla's random removal split. */
@Mixin(Slime.class)
public abstract class SlimeAllySplitMixin {
	@ModifyExpressionValue(method = "remove", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/monster/Slime;getSize()I"))
	private int mobdefenders$skipVanillaAllySplit(int size) {
		return (Object) this instanceof SlimeAlly ? 1 : size;
	}
}
