package net.amemah.mobdefenders.client;

import net.minecraft.resources.Identifier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Native translation of the supplied OptiFine-format zombie_ally.bbmodel, not an OptiFine dependency. */
public final class ZombieAllyModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "zombie_ally"), "main");

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Blockbench's CEM coordinates become Minecraft (-x, 24-y, z).
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
		// Vanilla HumanoidModel expects the headwear under head, named "hat".
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
			.addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5F)), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
			.addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
			.addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
			.addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
		// Preserve the supplied exact 0..4/-4..0 leg bounds, around +/-1.9 joint pivots.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
			.addBox(-2.1F, 0, -2, 4, 12, 4), PartPose.offset(-1.9F, 12, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
			.addBox(-1.9F, 0, -2, 4, 12, 4), PartPose.offset(1.9F, 12, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	private ZombieAllyModel() { }
}
