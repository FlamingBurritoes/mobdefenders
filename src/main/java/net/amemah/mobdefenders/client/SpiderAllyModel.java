package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

/** Supplied boxes/UVs/pivots with the vanilla resting leg rotations required by SpiderModel. */
public final class SpiderAllyModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "spider_ally"), "main");
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4)
			.addBox(-4, -4, -8, 8, 8, 8), PartPose.offset(0, 15, -3));
		root.addOrReplaceChild("body0", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 15, 0));
		root.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 12)
			.addBox(-5, -4, -6, 10, 8, 12), PartPose.offset(0, 15, 9));
		String[] pairs = {"hind_leg", "middle_hind_leg", "middle_front_leg", "front_leg"};
		float[] yaw = {(float) Math.PI / 4, (float) Math.PI / 8, -(float) Math.PI / 8, -(float) Math.PI / 4};
		float[] roll = {-(float) Math.PI / 4, -.58119464F, -.58119464F, -(float) Math.PI / 4};
		for (int i = 0; i < pairs.length; i++) {
			root.addOrReplaceChild("right_" + pairs[i], CubeListBuilder.create().texOffs(18, 0)
				.addBox(-15, -1, -1, 16, 2, 2), PartPose.offsetAndRotation(-4, 15, 4 - i * 3, 0, yaw[i], roll[i]));
			root.addOrReplaceChild("left_" + pairs[i], CubeListBuilder.create().texOffs(18, 0)
				.addBox(-1, -1, -1, 16, 2, 2), PartPose.offsetAndRotation(4, 15, 4 - i * 3, 0, -yaw[i], -roll[i]));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}
	private SpiderAllyModel() { }
}
