package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.resources.Identifier;

/** The supplied six boxes, UVs and pivots exactly match the native vanilla creeper layer. */
public final class CreeperAllyModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "creeper_ally"), "main");
	public static LayerDefinition createBodyLayer() {
		return CreeperModel.createBodyLayer(CubeDeformation.NONE);
	}
	private CreeperAllyModel() { }
}
