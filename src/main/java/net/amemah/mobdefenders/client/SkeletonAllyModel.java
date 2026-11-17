package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

/** Native translation of skeleton_ally.bbmodel: original 64x32 UVs, thin mirrored limbs and pivots. */
public final class SkeletonAllyModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "skeleton_ally"), "main");
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
			.addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(.5F)), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
			.addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
			.addBox(-1, -2, -1, 2, 12, 2), PartPose.offset(-5, 2, 0));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
			.addBox(-1, -2, -1, 2, 12, 2), PartPose.offset(5, 2, 0));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
			.addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-2, 12, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
			.addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(2, 12, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}
	private SkeletonAllyModel() { }
}
