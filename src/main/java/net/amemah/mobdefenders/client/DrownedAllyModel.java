package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

/** The supplied two-layer geometry, UVs and pivots, mapped onto the native humanoid hierarchy. */
public final class DrownedAllyModel {
	public static final ModelLayerLocation INNER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mobdefenders", "drowned_ally"), "inner");
	public static final ModelLayerLocation OUTER = new ModelLayerLocation(INNER.model(), "outer");

	public static LayerDefinition create(float inflate) {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var deformation = new CubeDeformation(inflate);
		var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-4, -8, -4, 8, 8, 8, deformation), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
			.addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(inflate + .5F)), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
			.addBox(-4, 0, -2, 8, 12, 4, deformation), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
			.addBox(-3, -2, -2, 4, 12, 4, deformation), PartPose.offset(-5, 2, 0));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48)
			.addBox(-1, -2, -2, 4, 12, 4, deformation), PartPose.offset(5, 2, 0));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
			.addBox(-2, 0, -2, 4, 12, 4, deformation), PartPose.offset(-1.9F, 12, .1F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48)
			.addBox(-2, 0, -2, 4, 12, 4, deformation), PartPose.offset(1.9F, 12, .1F));
		return LayerDefinition.create(mesh, 64, 64);
	}
	private DrownedAllyModel() { }
}
