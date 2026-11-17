package net.amemah.mobdefenders.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Read-only old-ID compatibility. New saves/resources always use the canonical zombie_ally_spawner ID. */
@Mixin(MappedRegistry.class)
public abstract class LegacySpawnerIdMixin<T> {
	@Shadow public abstract ResourceKey<? extends Registry<T>> key();
	private Identifier mobdefenders$canonical(Identifier id) {
		if ((key().equals(Registries.BLOCK) || key().equals(Registries.ITEM))
			&& id.getNamespace().equals("mobdefenders") && id.getPath().equals("zombie_spawner"))
			return Identifier.fromNamespaceAndPath("mobdefenders", "zombie_ally_spawner");
		return id;
	}
	@ModifyVariable(method = {
		"getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;",
		"get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;",
		"containsKey(Lnet/minecraft/resources/Identifier;)Z"
	}, at = @At("HEAD"), argsOnly = true)
	private Identifier mobdefenders$oldIdentifier(Identifier id) { return mobdefenders$canonical(id); }
	@ModifyVariable(method = {
		"getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
		"get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
		"containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
	}, at = @At("HEAD"), argsOnly = true)
	private ResourceKey<T> mobdefenders$oldKey(ResourceKey<T> value) {
		Identifier canonical = mobdefenders$canonical(value.identifier());
		return canonical.equals(value.identifier()) ? value : ResourceKey.create(value.registryKey(), canonical);
	}
}
