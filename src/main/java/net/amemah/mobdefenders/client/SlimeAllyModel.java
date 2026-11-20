package net.amemah.mobdefenders.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

public final class SlimeAllyModel {
	public static final ModelLayerLocation INNER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath("mobdefenders", "slime_ally"), "inner");
	public static final ModelLayerLocation OUTER = new ModelLayerLocation(INNER.model(), "outer");

	public static LayerDefinition inner() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 17, -3, 6, 6, 6), PartPose.ZERO);
		root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(32, 0).addBox(-3.3F, 18, -3.5F, 2, 2, 2), PartPose.ZERO);
		root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(32, 4).addBox(1.3F, 18, -3.5F, 2, 2, 2), PartPose.ZERO);
		root.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(32, 8).addBox(-1, 21, -3.5F, 1, 1, 1), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	public static LayerDefinition outer() {
		var mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-4, 16, -4, 8, 8, 8), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}
	private SlimeAllyModel() { }
}
