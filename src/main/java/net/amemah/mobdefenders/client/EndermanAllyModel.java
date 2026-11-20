package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

/** Supplied CEM boxes and pivots in native EndermanModel part names. */
public final class EndermanAllyModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "enderman_ally"), "main");

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, -15, 0));
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 16)
			.addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(-.5F)), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 16)
			.addBox(-4, 0, -2, 8, 12, 4), PartPose.offset(0, -15, 0));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0)
			.addBox(-1, -2, -1, 2, 30, 2), PartPose.offset(-5, -13, 0));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 0).mirror()
			.addBox(-1, -2, -1, 2, 30, 2), PartPose.offset(5, -13, 0));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0)
			.addBox(-1, 0, -1, 2, 30, 2), PartPose.offset(-2, -6, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).mirror()
			.addBox(-1, 0, -1, 2, 30, 2), PartPose.offset(2, -6, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}
	private EndermanAllyModel() { }
}
